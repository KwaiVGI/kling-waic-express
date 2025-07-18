package com.kling.waic.utils

import kotlinx.coroutines.*
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
class CoroutineUtils {
    
    companion object {
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        /**
         * Execute a suspend function in a blocking manner for Spring controllers
         */
        fun <T> runSuspend(block: suspend CoroutineScope.() -> T): T {
            val contextMap = MDC.getCopyOfContextMap()

            return runBlocking(Dispatchers.Default) {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap)
                } else {
                    MDC.clear()
                }

                try {
                    block()
                } finally {
                    MDC.clear() // 避免线程复用导致的污染
                }
            }
        }
        
        /**
         * Execute a suspend function asynchronously and return CompletableFuture
         */
        fun <T> runAsync(block: suspend CoroutineScope.() -> T): CompletableFuture<T> {
            val future = CompletableFuture<T>()
            applicationScope.launch {
                try {
                    val result = block()
                    future.complete(result)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
            return future
        }
    }
}
