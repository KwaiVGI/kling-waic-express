package com.kling.waic.component.utils

import kotlinx.coroutines.*
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.CoroutineContext

@Component
class CoroutineUtils {
    
    companion object {
        private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        
        /**
         * Execute a suspend function in a blocking manner for Spring controllers
         * Preserves the original thread context
         */
        fun <T> runSuspend(block: suspend CoroutineScope.() -> T): T {
            val contextMap = MDC.getCopyOfContextMap()
            val currentThread = Thread.currentThread()
            
            // Create a custom dispatcher that preserves the original thread
            val originalThreadDispatcher = object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    if (Thread.currentThread() == currentThread) {
                        block.run()
                    } else {
                        // If we're not on the original thread, we need to switch back
                        // For this case, we'll use runBlocking to ensure synchronous execution
                        runBlocking { block.run() }
                    }
                }
            }

            return runBlocking(originalThreadDispatcher) {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap)
                } else {
                    MDC.clear()
                }

                try {
                    block()
                } finally {
                    // Restore original MDC context instead of clearing
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap)
                    } else {
                        MDC.clear()
                    }
                }
            }
        }
        
        /**
         * Execute a suspend function asynchronously and return CompletableFuture
         * This one can use different threads as it's async by nature
         */
        fun <T> runAsync(block: suspend CoroutineScope.() -> T): CompletableFuture<T> {
            val future = CompletableFuture<T>()
            val contextMap = MDC.getCopyOfContextMap()
            
            applicationScope.launch {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap)
                } else {
                    MDC.clear()
                }
                
                try {
                    val result = block()
                    future.complete(result)
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                } finally {
                    MDC.clear()
                }
            }
            return future
        }
    }
}
