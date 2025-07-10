package com.kling.waic.test

import com.kling.waic.WAICExpressApplication
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    classes = [WAICExpressApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
abstract class SpringBaseTest {
}