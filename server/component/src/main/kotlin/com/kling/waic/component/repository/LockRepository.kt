package com.kling.waic.component.repository

import com.kling.waic.component.utils.ActivityUtils
import com.kling.waic.component.utils.ActivityUtils.COLON
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

@Repository
class LockRepository(
    private val redissonClient: RedissonClient
) {

    fun <T> executeWithLock(
        lockKey: String,
        lockWaitTime: Long = 5,
        lockLeaseTime: Long = 30,
        lockTimeUnit: TimeUnit = TimeUnit.SECONDS,
        action: () -> T
    ): T {
        val newLockKey = ActivityUtils.generateNewKey(lockKey, COLON)
        val lock = redissonClient.getLock(newLockKey)
        val locked = lock.tryLock(
            lockWaitTime,
            lockLeaseTime,
            lockTimeUnit
        )
        return if (locked) {
            try {
                action()
            } finally {
                lock.unlock()
            }
        } else {
            throw IllegalStateException(
                "failed to acquired lock, lockKey: $lockKey, lockWaitTime: $lockWaitTime, lockLeaseTime: $lockLeaseTime"
            )
        }
    }
}