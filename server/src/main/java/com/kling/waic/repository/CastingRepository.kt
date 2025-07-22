package com.kling.waic.repository

import com.kling.waic.entity.Casting
import com.kling.waic.entity.CastingListResult
import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskOperateAction
import com.kling.waic.entity.TaskType
import com.kling.waic.utils.IdUtils
import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.stereotype.Repository
import redis.clients.jedis.commands.JedisCommands
import java.time.Instant

@Repository
class CastingRepository(
    private val jedis: JedisCommands
) {
    private val castingQueuePrefix = "casting_queue_"
    private val castingPinnedPrefix = "casting_pinned_"
    private val screenLatestCursorPrefix = "screen_latest_cursor_"
    private val screenEarliestCursorPrefix = "screen_earliest_cursor_"

    fun addToCastingQueue(task: Task): Casting {
        val score = Instant.now().toEpochMilli().toDouble()
        val castingName = "casting:${task.name}"
        val casting = Casting(
            id = IdUtils.generateId(),
            name = castingName,
            task = task,
            score = score,
        )

        val value = ObjectMapperUtils.toJSON(casting)
        jedis.set(castingName, value)
        log.info("Set casting to Redis, castingName: {}, value: {}", castingName, value)

        val castingQueue = "${castingQueuePrefix}${task.type}"
        jedis.zadd(castingQueue, score, castingName)
        log.info(
            "Added casting to queue, queue: {}, castingName: {}, score: {}",
            castingQueue,
            castingName,
            score
        )
        return casting
    }

    fun getPinned(type: TaskType): Casting? {
        val castingPinnedKey = "${castingPinnedPrefix}${type}"
        val castingName = jedis.get(castingPinnedKey) ?: return null
        log.debug("Got pinned castingName: {}", castingName)

        val castingValue = jedis.get(castingName)
            ?: throw IllegalArgumentException("Casting not exists for castingName: $castingName")
        val casting = ObjectMapperUtils.fromJSON(castingValue, Casting::class.java)!!
        log.info("Retrieved pinned type: {}, casting: {}", type, casting)
        return casting
    }

    fun operate(type: TaskType,
                name: String,
                action: TaskOperateAction): Boolean {
        val value = jedis.get(name)
            ?: throw IllegalArgumentException("Casting not found: $name")
        val casting = ObjectMapperUtils.fromJSON(value, Casting::class.java)!!
        if (type != casting.task.type) {
            throw IllegalArgumentException("Casting type mismatch: expected $type, but found ${casting.task.type}")
        }

        val castingPinnedKey = "${castingPinnedPrefix}${type}"
        val castingQueue = "${castingQueuePrefix}${type}"

        when (action) {
            TaskOperateAction.PIN -> {
                jedis.set(castingPinnedKey, name)
            }

            TaskOperateAction.UNPIN -> {
                jedis.del(castingPinnedKey)
            }

            TaskOperateAction.PROMOTE -> {
                removeFromCastingQueue(type, name)
                addToCastingQueue(casting.task)
            }

            TaskOperateAction.DELETE -> {
                jedis.zrem(castingQueue, name)
            }
        }
        log.info("Operated casting: {}, action: {}", name, action)
        return true
    }

    private fun removeFromCastingQueue(type: TaskType, name: String) {
        val castingQueue = "${castingQueuePrefix}${type}"

        val screenLatestCursorKey = "${screenLatestCursorPrefix}${type}"
        val screenEarliestCursorKey = "${screenEarliestCursorPrefix}${type}"
        val cursor = jedis.zrank(castingQueue, name)

        val screenLatestCursor = jedis.get(screenLatestCursorKey)
        val screenEarliestCursor = jedis.get(screenEarliestCursorKey)
        if (cursor < (screenLatestCursor.toLongOrNull() ?: 0L)) {
            jedis.decr(screenLatestCursorKey)
        }
        if (cursor < (screenEarliestCursor.toLongOrNull() ?: 0L)) {
            jedis.decr(screenEarliestCursorKey)
        }
        log.info("removeFromCastingQueue, type: $type, name: $name, " +
                "cursor: $cursor, screenLatestCursor: $screenLatestCursor, screenEarliestCursor: $screenEarliestCursor")
        jedis.zrem(castingQueue, name)
    }

    fun list(type: TaskType,
             keyword: String,
             score: Double?,
             pageSize: Int,
             pageNum: Int): CastingListResult {
        val castingQueue = "${castingQueuePrefix}${type}"

        // Calculate total count based on whether there's keyword filtering
        val totalCount = if (keyword.isBlank()) {
            // No keyword filter, get total count directly from Redis
            if (score != null) {
                // Count items with score less than the given score
                jedis.zcount(castingQueue, "0", "${score}").toInt()
            } else {
                // Count all items in the queue
                jedis.zcard(castingQueue).toInt()
            }
        } else {
            // With keyword filter, need to count matching items
            countCastingsWithKeyword(castingQueue, keyword, score)
        }

        // Get paginated results
        val result = if (keyword.isBlank()) {
            listWithoutKeyword(castingQueue, score, pageSize, pageNum)
        } else {
            listWithKeyword(castingQueue, keyword, score, pageSize, pageNum)
        }

        return CastingListResult(
            total = totalCount,
            score = score ?: result.second.firstOrNull()?.score,
            hasMore = result.first,
            castings = result.second
        )
    }

    private fun countCastingsWithKeyword(castingQueue: String, keyword: String, score: Double?): Int {
        val batchSize = 100
        var count = 0
        var offset = 0L
        var currentScore = score

        while (true) {
            val castingNames = if (currentScore != null) {
                jedis.zrevrangeByScore(castingQueue, "${currentScore}", "0", offset.toInt(), batchSize)
            } else {
                jedis.zrevrange(castingQueue, offset, offset + batchSize - 1)
            }

            if (castingNames.isEmpty()) break

            val batchCastings = getCastingDetails(castingNames)
            count += batchCastings.count { casting ->
                casting.name.contains(keyword, ignoreCase = true)
            }

            if (currentScore != null) {
                currentScore = batchCastings.minOfOrNull { it.score }
                if (currentScore == null) break
            } else {
                offset += batchSize
            }

            if (castingNames.size < batchSize) break
        }

        return count
    }

    private fun listWithoutKeyword(castingQueue: String,
                                   score: Double?,
                                   pageSize: Int,
                                   pageNum: Int): Pair<Boolean, List<Casting>> {
        val offset = (pageNum - 1) * pageSize

        val castingNames = if (score != null) {
            jedis.zrevrangeByScore(castingQueue, "${score}", "0", offset, pageSize + 1)
        } else {
            jedis.zrevrange(castingQueue, offset.toLong(), (offset + pageSize).toLong())
        }

        log.debug("Retrieved casting names (no keyword): queue={}, score={}, pageSize={}, pageNum={}, offset={}, names={}",
                castingQueue, score, pageSize, pageNum, offset, castingNames.size)

        if (castingNames.isEmpty()) {
            return Pair(false, emptyList())
        }

        val castings = getCastingDetails(castingNames.take(pageSize))
        val hasMore = castingNames.size > pageSize

        return Pair(hasMore, castings)
    }

    private fun listWithKeyword(castingQueue: String,
                                keyword: String,
                                score: Double?,
                                pageSize: Int,
                                pageNum: Int): Pair<Boolean, List<Casting>> {
        val targetOffset = (pageNum - 1) * pageSize
        val batchSize = maxOf(pageSize * 5, 100)
        val resultCastings = mutableListOf<Casting>()
        var currentScore = score
        var processedCount = 0
        var hasMore = false

        while (true) {
            val castingNames = if (currentScore != null) {
                jedis.zrevrangeByScore(castingQueue, "${currentScore}", "0", 0, batchSize)
            } else {
                val startIndex = if (score == null) processedCount.toLong() else 0L
                jedis.zrevrange(castingQueue, startIndex, startIndex + batchSize - 1)
            }

            if (castingNames.isEmpty()) {
                break
            }

            val batchCastings = getCastingDetails(castingNames)

            val filteredBatch = batchCastings.filter { casting ->
                casting.name.contains(keyword, ignoreCase = true)
            }

            val filteredAfterOffset = if (processedCount < targetOffset) {
                val skipCount = minOf(filteredBatch.size, targetOffset - processedCount)
                processedCount += skipCount
                filteredBatch.drop(skipCount)
            } else {
                filteredBatch
            }

            if (processedCount >= targetOffset) {
                val remainingNeeded = pageSize - resultCastings.size
                val toAdd = filteredAfterOffset.take(remainingNeeded)
                resultCastings.addAll(toAdd)

                if (filteredAfterOffset.size > toAdd.size) {
                    hasMore = true
                    break
                }

                if (resultCastings.size >= pageSize) {
                    val remainingFiltered = filteredAfterOffset.drop(toAdd.size)
                    if (remainingFiltered.isNotEmpty()) {
                        hasMore = true
                        break
                    }
                }
            } else {
                processedCount += filteredBatch.size
            }

            currentScore = batchCastings.minOfOrNull { it.score }

            if (castingNames.size < batchSize) {
                break
            }
        }

        if (!hasMore && resultCastings.size == pageSize && currentScore != null) {
            val nextBatch = jedis.zrevrangeByScore(castingQueue, "${currentScore}", "0", 0, 10)
            if (nextBatch.isNotEmpty()) {
                val nextCastings = getCastingDetails(nextBatch)
                hasMore = nextCastings.any { casting ->
                    casting.name.contains(keyword, ignoreCase = true)
                }
            }
        }

        log.debug("List castings result (with keyword): keyword='{}', score={}, pageSize={}, pageNum={}, " +
                "returned={}, hasMore={}", keyword, score, pageSize, pageNum, resultCastings.size, hasMore)
        return Pair(hasMore, resultCastings)
    }

    private fun getCastingDetails(castingNames: List<String>): List<Casting> {
        val castings = mutableListOf<Casting>()
        for (castingName in castingNames) {
            val value = jedis.get(castingName)
                ?: throw java.lang.IllegalArgumentException("Casting not found: $castingName")
            val casting = ObjectMapperUtils.fromJSON(value, Casting::class.java)
            castings.add(casting!!)
        }
        return castings
    }

    fun screen(type: TaskType, num: Long): List<Casting> {
        val resultCastings = mutableListOf<Casting>()

//        val pinned = getPinned(type)
//        if (pinned != null) {
//            resultCastings.add(pinned)
//            log.info("Screen existing pinned, added to resultCastings")
//        }
//        val actualNum = num - resultCastings.size
//        log.debug("doScreen with actualNum: {}, num: {}", actualNum, num)

        doScreen(type, num, resultCastings)
        return resultCastings
    }

    private fun doScreen(type: TaskType, actualNum: Long,
                         resultCastings: MutableList<Casting>): List<Casting> {
        val castingQueue = "${castingQueuePrefix}${type}"
        val screenLatestCursorKey = "${screenLatestCursorPrefix}${type}"
        val screenEarliestCursorKey = "${screenEarliestCursorPrefix}${type}"

        // Get total count of items in the queue
        val totalCount = jedis.zcard(castingQueue)
        if (totalCount == 0L) {
//            log.info("No castings available for screen display, type: {}", type)
            return emptyList()
        }

        log.debug("Screen display request: type={}, actualNum={}, totalCount={}", type, actualNum, totalCount)

        // Get current cursor positions (default to 0 if not exists)
        val latestCursor = jedis.get(screenLatestCursorKey)?.toLongOrNull() ?: 0L
        val earliestCursor = jedis.get(screenEarliestCursorKey)?.toLongOrNull() ?: 0L

        var newLatestCursor = latestCursor
        var newEarliestCursor = earliestCursor

        // Strategy 1: Get latest items first (from highest score, descending order)
        val availableLatest = maxOf(0L, totalCount - latestCursor)
        val latestToTake = minOf(actualNum, availableLatest)

        if (latestToTake > 0) {
            // Get latest items using ZREVRANGE (highest score first)
            val latestCastingNames =
                jedis.zrange(castingQueue, latestCursor, latestCursor + latestToTake - 1)
            val latestCastings = getCastingDetails(latestCastingNames)
            resultCastings.addAll(latestCastings)
            newLatestCursor = latestCursor + latestToTake

            log.debug(
                "Retrieved {} latest castings, cursor moved from {} to {}",
                latestCastings.size, latestCursor, newLatestCursor
            )
        }

        // Strategy 2: If still need more items, get earliest items (from lowest score, ascending order)
        val remainingNeeded = actualNum - resultCastings.size
        if (remainingNeeded > 0) {
            val availableEarliest = maxOf(0L, newLatestCursor - earliestCursor)
            val earliestToTake = minOf(remainingNeeded, availableEarliest)

            if (earliestToTake > 0) {
                // Get earliest items using ZRANGE (lowest score first)
                val earliestCastingNames =
                    jedis.zrange(castingQueue, earliestCursor, earliestCursor + earliestToTake - 1)
                val earliestCastings = getCastingDetails(earliestCastingNames)
                resultCastings.addAll(earliestCastings)
                newEarliestCursor = earliestCursor + earliestToTake

                log.debug(
                    "Retrieved {} earliest castings, cursor moved from {} to {}",
                    earliestCastings.size, earliestCursor, newEarliestCursor
                )
            }
        }

        // If newEarliestCursor catch up with newLatestCursor, reset it to 0
        if (newEarliestCursor >= newLatestCursor) {
            log.debug("Earliest cursor reached end, resetting to 0")
            newEarliestCursor = 0L
        }

        // Update cursor positions in Redis
        jedis.set(screenLatestCursorKey, newLatestCursor.toString())
        jedis.set(screenEarliestCursorKey, newEarliestCursor.toString())

        // If actualNum > resultCastings.size, add remaining screens
        if (actualNum > resultCastings.size) {
            doScreen(type, actualNum - resultCastings.size, resultCastings)
        }

        log.debug(
            "Screen display completed: type={}, requested={}, returned={}, " +
                    "latestCursor: {}→{}, earliestCursor: {}→{}",
            type, actualNum, resultCastings.size, latestCursor, newLatestCursor,
            earliestCursor, newEarliestCursor
        )
        return resultCastings
    }
}