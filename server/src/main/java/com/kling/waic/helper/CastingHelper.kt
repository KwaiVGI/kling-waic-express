package com.kling.waic.helper

import com.kling.waic.entity.Casting
import com.kling.waic.entity.CastingListResult
import com.kling.waic.entity.Task
import com.kling.waic.entity.TaskOperateAction
import com.kling.waic.entity.TaskType
import com.kling.waic.utils.ObjectMapperUtils
import com.kling.waic.utils.Slf4j.Companion.log
import org.springframework.stereotype.Component
import redis.clients.jedis.Jedis
import java.time.Instant
import java.util.*

@Component
class CastingHelper(
    private val jedis: Jedis
) {
    private val castingQueuePrefix = "casting_queue_"
    private val castingPinnedPrefix = "casting_pinned_"
    private val screenLatestCursorPrefix = "screen_latest_cursor_"
    private val screenEarliestCursorPrefix = "screen_earliest_cursor_"

    fun addToCastingQueue(task: Task): Casting {
        val score = Instant.now().toEpochMilli().toDouble()
        val castingName = "casting:${task.name}"
        val casting = Casting(
            id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
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
        val value = jedis.get(castingPinnedKey) ?: return null

        val casting = ObjectMapperUtils.fromJSON(value, Casting::class.java)!!
        log.info("Retrieved pinned type: {}, casting: {}", type, casting)
        return casting
    }

    fun operate(type: TaskType,
                name: String,
                action: TaskOperateAction): Casting {
        val value = jedis.get(name)
            ?: throw IllegalArgumentException("Casting not found: $name")
        val casting = ObjectMapperUtils.fromJSON(value, Casting::class.java)!!
        if (type != casting.task.type) {
            throw IllegalArgumentException("Casting type mismatch: expected $type, but found ${casting.task.type}")
        }

        val castingPinnedKey = "${castingPinnedPrefix}${type}"
        val castingQueue = "${castingQueuePrefix}${type}"

        val newCasting = when (action) {
            TaskOperateAction.PIN -> {
                jedis.set(castingPinnedKey, name)
                casting
            }

            TaskOperateAction.UNPIN -> {
                jedis.del(castingPinnedKey)
                casting
            }

            TaskOperateAction.PROMOTE -> {
                jedis.zrem(castingQueue, name)
                addToCastingQueue(casting.task)
            }

            TaskOperateAction.DELETE -> {
                jedis.zrem(castingQueue, name)
                casting
            }
        }
        log.info("Operated casting: {}, action: {}, newCasting: {}", name, action, newCasting)
        return newCasting
    }

    fun count(type: TaskType,
             keyword: String,
             score: Double?,
             pageSize: Int,
             pageNum: Int): Int {
        val castingQueue = "${castingQueuePrefix}${type}"

        // Get total count first for pagination calculation
        val totalCount = if (keyword.isBlank()) {
            // No keyword filter, get total count directly from Redis
            jedis.zcard(castingQueue).toInt()
        } else {
            // With keyword filter, need to count matching items
            countMatchingCastings(castingQueue, keyword)
        }
        return totalCount
    }

    fun list(type: TaskType,
             keyword: String,
             score: Double?,
             pageSize: Int,
             pageNum: Int): CastingListResult {
        val castingQueue = "${castingQueuePrefix}${type}"
        
        // If no keyword search, use Redis pagination directly for better performance
        if (keyword.isBlank()) {
            val result = listWithoutKeyword(castingQueue, score, pageSize, pageNum)
            return CastingListResult(
                hasMore = result.first,
                castings = result.second,
                score = score ?: result.second.first().score
            )
        }
        
        // When keyword search is needed, fetch more data for filtering
        val result = listWithKeyword(castingQueue, keyword, score, pageSize, pageNum)
        return CastingListResult(
            hasMore = result.first,
            castings = result.second,
            score = score ?: result.second.first().score
        )
    }
    
    private fun countMatchingCastings(castingQueue: String, keyword: String): Int {
        // For keyword search, we need to count all matching items
        // This is expensive but necessary for accurate pagination
        val batchSize = 100
        var count = 0
        var offset = 0L
        
        while (true) {
            val castingNames = jedis.zrevrange(castingQueue, offset, offset + batchSize - 1)
            if (castingNames.isEmpty()) break
            
            val batchCastings = getCastingDetails(castingNames)
            count += batchCastings.count { casting ->
                casting.name.contains(keyword, ignoreCase = true)
            }
            
            offset += batchSize
            if (castingNames.size < batchSize) break
        }
        
        return count
    }
    
    private fun listWithoutKeyword(castingQueue: String, score: Double?, pageSize: Int, pageNum: Int): Pair<Boolean, List<Casting>> {
        // Calculate offset based on pageNum (1-based)
        val offset = (pageNum - 1) * pageSize
        
        val castingNames = if (score != null) {
            // Score-based pagination: get items with score less than the given score
            jedis.zrevrangeByScore(castingQueue, "(${score}", "0", offset, pageSize + 1)
        } else {
            // Regular pagination: get items by rank
            jedis.zrevrange(castingQueue, offset.toLong(), (offset + pageSize).toLong())
        }
        
        log.info("Retrieved casting names (no keyword): queue={}, score={}, pageSize={}, pageNum={}, offset={}, names={}", 
                castingQueue, score, pageSize, pageNum, offset, castingNames.size)
        
        if (castingNames.isEmpty()) {
            return Pair(false, emptyList())
        }
        
        // Get casting details
        val castings = getCastingDetails(castingNames.take(pageSize))
        val hasMore = castingNames.size > pageSize
        
        return Pair(hasMore, castings)
    }
    
    private fun listWithKeyword(castingQueue: String, keyword: String, score: Double?, pageSize: Int, pageNum: Int): Pair<Boolean, List<Casting>> {
        // For keyword search, we need to fetch and filter data
        val targetOffset = (pageNum - 1) * pageSize
        val batchSize = maxOf(pageSize * 5, 100) // Larger batch size for filtering
        val resultCastings = mutableListOf<Casting>()
        var currentScore = score
        var processedCount = 0
        var hasMore = false
        
        while (true) {
            val castingNames = if (currentScore != null) {
                jedis.zrevrangeByScore(castingQueue, "(${currentScore}", "0", 0, batchSize)
            } else {
                val startIndex = if (score == null) processedCount.toLong() else 0L
                jedis.zrevrange(castingQueue, startIndex, startIndex + batchSize - 1)
            }
            
            if (castingNames.isEmpty()) {
                break // No more data available
            }
            
            // Get details for this batch
            val batchCastings = getCastingDetails(castingNames)
            
            // Filter data matching the keyword
            val filteredBatch = batchCastings.filter { casting ->
                casting.name.contains(keyword, ignoreCase = true)
            }
            
            // Skip items before target offset
            val filteredAfterOffset = if (processedCount < targetOffset) {
                val skipCount = minOf(filteredBatch.size, targetOffset - processedCount)
                processedCount += skipCount
                filteredBatch.drop(skipCount)
            } else {
                filteredBatch
            }
            
            // Add to results if we've reached the target page
            if (processedCount >= targetOffset) {
                val remainingNeeded = pageSize - resultCastings.size
                val toAdd = filteredAfterOffset.take(remainingNeeded)
                resultCastings.addAll(toAdd)
                
                // Check if there are more items after what we added
                if (filteredAfterOffset.size > toAdd.size) {
                    hasMore = true
                    break
                }
                
                // If we have enough results, check if there are more matching items
                if (resultCastings.size >= pageSize) {
                    // Continue processing to check for more items
                    val remainingFiltered = filteredAfterOffset.drop(toAdd.size)
                    if (remainingFiltered.isNotEmpty()) {
                        hasMore = true
                        break
                    }
                }
            } else {
                processedCount += filteredBatch.size
            }
            
            // Update currentScore for next iteration
            currentScore = batchCastings.minOfOrNull { it.score }
            
            // If this batch has fewer items than batchSize, no more data available
            if (castingNames.size < batchSize) {
                break
            }
        }
        
        // Final check for hasMore if we haven't determined it yet
        if (!hasMore && resultCastings.size == pageSize && currentScore != null) {
            val nextBatch = jedis.zrevrangeByScore(castingQueue, "(${currentScore}", "0", 0, 10)
            if (nextBatch.isNotEmpty()) {
                val nextCastings = getCastingDetails(nextBatch)
                hasMore = nextCastings.any { casting ->
                    casting.name.contains(keyword, ignoreCase = true)
                }
            }
        }
        
        log.info("List castings result (with keyword): keyword='{}', score={}, pageSize={}, pageNum={}, " +
                "returned={}, hasMore={}", keyword, score, pageSize, pageNum, resultCastings.size, hasMore)
        return Pair(hasMore, resultCastings)
    }
    
    private fun getCastingDetails(castingNames: List<String>): List<Casting> {
        val castings = mutableListOf<Casting>()
        for (castingName in castingNames) {
            try {
                val value = jedis.get(castingName)
                if (value != null) {
                    val casting = ObjectMapperUtils.fromJSON(value, Casting::class.java)
                    if (casting != null) {
                        castings.add(casting)
                    }
                }
            } catch (e: Exception) {
                log.warn("Failed to parse casting: {}, error: {}", castingName, e.message)
            }
        }
        return castings.sortedByDescending { it.score }
    }

    // todo: check the logic
    fun screen(type: TaskType, num: Long): List<Casting> {
        val castingQueue = "${castingQueuePrefix}${type}"
        val screenLatestCursorKey = "${screenLatestCursorPrefix}${type}"
        val screenEarliestCursorKey = "${screenEarliestCursorPrefix}${type}"
        
        // Get total count of items in the queue
        val totalCount = jedis.zcard(castingQueue)
        if (totalCount == 0L) {
            log.info("No castings available for screen display, type: {}", type)
            return emptyList()
        }
        
        log.info("Screen display request: type={}, num={}, totalCount={}", type, num, totalCount)
        
        // Get current cursor positions (default to 0 if not exists)
        val latestCursor = jedis.get(screenLatestCursorKey)?.toLongOrNull() ?: 0L
        val earliestCursor = jedis.get(screenEarliestCursorKey)?.toLongOrNull() ?: 0L
        
        val resultCastings = mutableListOf<Casting>()
        var newLatestCursor = latestCursor
        var newEarliestCursor = earliestCursor
        
        // Strategy 1: Get latest items first (from highest score, descending order)
        val availableLatest = maxOf(0L, totalCount - latestCursor)
        val latestToTake = minOf(num, availableLatest)
        
        if (latestToTake > 0) {
            // Get latest items using ZREVRANGE (highest score first)
            val latestCastingNames = jedis.zrevrange(castingQueue, latestCursor, latestCursor + latestToTake - 1)
            val latestCastings = getCastingDetails(latestCastingNames)
            resultCastings.addAll(latestCastings)
            newLatestCursor = latestCursor + latestToTake
            
            log.info("Retrieved {} latest castings, cursor moved from {} to {}", 
                    latestCastings.size, latestCursor, newLatestCursor)
        }
        
        // Strategy 2: If still need more items, get earliest items (from lowest score, ascending order)
        val remainingNeeded = num - resultCastings.size
        if (remainingNeeded > 0) {
            val availableEarliest = maxOf(0L, totalCount - earliestCursor)
            val earliestToTake = minOf(remainingNeeded, availableEarliest)
            
            if (earliestToTake > 0) {
                // Get earliest items using ZRANGE (lowest score first)
                val earliestCastingNames = jedis.zrange(castingQueue, earliestCursor, earliestCursor + earliestToTake - 1)
                val earliestCastings = getCastingDetails(earliestCastingNames)
                resultCastings.addAll(earliestCastings)
                newEarliestCursor = earliestCursor + earliestToTake
                
                log.info("Retrieved {} earliest castings, cursor moved from {} to {}", 
                        earliestCastings.size, earliestCursor, newEarliestCursor)
            }
        }
        
        // Handle cursor reset and collision logic
        // Reset cursors when they reach the end
        if (newLatestCursor >= totalCount) {
            log.info("Latest cursor reached end, resetting to 0")
            newLatestCursor = 0L
        }
        
        if (newEarliestCursor >= totalCount) {
            log.info("Earliest cursor reached end, resetting to 0")
            newEarliestCursor = 0L
        }
        
        // Handle cursor collision: when earliest cursor catches up with latest cursor
        // Latest cursor works on ZREVRANGE (0=highest score, totalCount-1=lowest score)
        // Earliest cursor works on ZRANGE (0=lowest score, totalCount-1=highest score)
        // Collision happens when: earliestCursor >= (totalCount - latestCursor)
        // This means earliest cursor is accessing the same items as latest cursor
        val latestAccessedFromBottom = totalCount - newLatestCursor
        if (newEarliestCursor >= latestAccessedFromBottom && newLatestCursor > 0) {
            log.info("Cursor collision detected: earliest cursor ({}) caught up with latest cursor range (bottom index: {}), resetting earliest cursor to 0", 
                    newEarliestCursor, latestAccessedFromBottom)
            newEarliestCursor = 0L
        }
        
        // Update cursor positions in Redis
        jedis.set(screenLatestCursorKey, newLatestCursor.toString())
        jedis.set(screenEarliestCursorKey, newEarliestCursor.toString())
        
        // Sort final results by score descending for consistent display order
        val sortedResults = resultCastings.sortedByDescending { it.score }
        
        log.info("Screen display completed: type={}, requested={}, returned={}, " +
                "latestCursor: {}→{}, earliestCursor: {}→{}", 
                type, num, sortedResults.size, latestCursor, newLatestCursor, 
                earliestCursor, newEarliestCursor)
        
        return sortedResults
    }


}