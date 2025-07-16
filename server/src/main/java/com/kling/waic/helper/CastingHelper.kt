package com.kling.waic.helper

import com.kling.waic.entity.Casting
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

    fun list(type: TaskType, keyword: String, score: Double?, count: Int):
            Pair<Boolean, List<Casting>> {
        val castingQueue = "${castingQueuePrefix}${type}"
        
        // If no keyword search, use Redis pagination directly for better performance
        if (keyword.isBlank()) {
            return listWithoutKeyword(castingQueue, score, count)
        }
        
        // When keyword search is needed, fetch more data for filtering
        // Use batch fetching strategy to avoid loading all data at once
        return listWithKeyword(castingQueue, keyword, score, count)
    }
    
    private fun listWithoutKeyword(castingQueue: String, score: Double?, count: Int): Pair<Boolean, List<Casting>> {
        // No keyword search, use Redis pagination directly
        val castingNames = if (score != null) {
            jedis.zrevrangeByScore(castingQueue, "(${score}", "0", 0, count + 1)
        } else {
            jedis.zrevrange(castingQueue, 0, count.toLong())
        }
        
        log.info("Retrieved casting names (no keyword): queue={}, score={}, count={}, names={}", 
                castingQueue, score, castingNames.size, castingNames.size)
        
        if (castingNames.isEmpty()) {
            return Pair(false, emptyList())
        }
        
        // Get casting details
        val castings = getCastingDetails(castingNames.take(count))
        val hasMore = castingNames.size > count
        
        return Pair(hasMore, castings)
    }
    
    private fun listWithKeyword(castingQueue: String, keyword: String, score: Double?, count: Int): Pair<Boolean, List<Casting>> {
        // With keyword search, need to fetch data in batches for filtering
        val batchSize = maxOf(count * 3, 50) // Batch size, at least 3 times the required count
        val resultCastings = mutableListOf<Casting>()
        var currentScore = score
        var hasMore = false
        
        while (resultCastings.size < count) {
            val castingNames = if (currentScore != null) {
                jedis.zrevrangeByScore(castingQueue, "(${currentScore}", "0", 0, batchSize)
            } else {
                jedis.zrevrange(castingQueue, 0, (batchSize - 1).toLong())
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
            
            // Add to results
            val remainingCount = count - resultCastings.size
            val toAdd = filteredBatch.take(remainingCount)
            resultCastings.addAll(toAdd)
            
            // Update currentScore to the minimum score of this batch for next query
            currentScore = batchCastings.minOfOrNull { it.score }
            
            // If this batch has fewer items than batchSize, no more data available
            if (castingNames.size < batchSize) {
                break
            }
            
            // If filtered data is more than what we added, there might be more matching data
            if (filteredBatch.size > toAdd.size) {
                hasMore = true
                break
            }
        }
        
        // If result count equals requested count and there's still data to query, there might be more
        if (resultCastings.size == count && currentScore != null) {
            // Query a small batch to confirm if there's more matching data
            val nextBatch = jedis.zrevrangeByScore(castingQueue, "(${currentScore}", "0", 0, 10)
            if (nextBatch.isNotEmpty()) {
                val nextCastings = getCastingDetails(nextBatch)
                val hasMatchingNext = nextCastings.any { casting ->
                    casting.name.contains(keyword, ignoreCase = true)
                }
                hasMore = hasMatchingNext
            }
        }
        
        log.info("List castings result (with keyword): keyword='{}', score={}, count={}, " +
                "returned={}, hasMore={}", keyword, score, count, resultCastings.size, hasMore)
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

    fun screen(type: TaskType, num: Long): List<Casting> {
        TODO("Not yet implemented")
    }


}