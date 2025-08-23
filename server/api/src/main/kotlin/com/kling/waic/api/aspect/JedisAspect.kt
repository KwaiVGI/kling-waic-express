package com.kling.waic.api.aspect

import com.kling.waic.component.utils.ActivityUtils
import com.kling.waic.component.utils.ActivityUtils.COLON
import com.kling.waic.component.utils.Slf4j.Companion.log
import com.kling.waic.component.utils.ThreadContextUtils
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

@Aspect
@Component
class JedisAspect {

    @Around("execution(* redis.clients.jedis.commands.JedisCommands.*(..))")
    fun around(pjp: ProceedingJoinPoint): Any? {
        log.debug("Before action on JedisCommands, signature: {}", pjp.signature)

        val activity = ThreadContextUtils.getActivity()
        val methodSignature = pjp.signature as MethodSignature
        val method = methodSignature.method
        val parameterTypes = method.parameterTypes
        val methodName = method.name
        val args = pjp.args

        val result = if (args.isNullOrEmpty()
            || activity.isEmpty()
            || methodName in setOf("auth")
        ) {
            pjp.proceed()
        } else if (methodName == "eval") {
            when {
                // eval(String script, int keyCount, String... params)
                parameterTypes.size > 2
                        && parameterTypes[1] == Int::class.java
                        && parameterTypes[2].isArray -> {
                    val keyCount = args[1] as Int
                    val params = args[2] as Array<String>
                    val keys = params.take(keyCount)
                    val argv = params.drop(keyCount)
                    proceedWithNewKeys(pjp, keys, argv, argIndex = 2)
                }

                // eval(String script, List<String> keys, List<String> args)
                parameterTypes.size > 2
                        && List::class.java.isAssignableFrom(parameterTypes[1])
                        && List::class.java.isAssignableFrom(parameterTypes[2]) -> {
                    val keys = args[1] as List<String>
                    val argv = args[2] as List<String>
                    proceedWithNewKeys(pjp, keys, argv, argIndex = 1)
                }

                else -> pjp.proceed()
            }
        } else {
            // 默认认为第 0 个参数是 redis key
            val redisKey = args[0] as String
            val newRedisKey = ActivityUtils.generateNewKey(redisKey, COLON)
            val newArgs = args.copyOf()
            newArgs[0] = newRedisKey

            log.info(
                "Aspect on jedis method: $methodName take effect, " +
                        "redisKey: $redisKey, newRedisKey: $newRedisKey"
            )
            pjp.proceed(newArgs)
        }

        log.debug("Post action on JedisCommands, signature: {}", pjp.signature)
        return result
    }

    private fun proceedWithNewKeys(
        pjp: ProceedingJoinPoint,
        keys: List<String>,
        argv: Any?,
        argIndex: Int
    ): Any? {
        val newKeys = keys.map { ActivityUtils.generateNewKey(it, COLON) }
        val newArgs = pjp.args.copyOf()

        // eval with keyCount + params  → keys.toTypedArray()
        // eval with List<String>       → keys 本身就是 List
        newArgs[argIndex] = if (newArgs[argIndex] is Array<*>) {
            (newKeys + (argv as List<String>)).toTypedArray()
        } else {
            newKeys
        }

        log.info(
            "Aspect on jedis method: ${pjp.signature.name} take effect, " +
                    "keys: $keys, newKeys: $newKeys, argv: $argv"
        )
        return pjp.proceed(newArgs)
    }
}
