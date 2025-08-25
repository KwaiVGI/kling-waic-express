package com.kling.waic.component.test

import com.kling.waic.component.entity.Casting
import com.kling.waic.component.utils.ObjectMapperUtils
import org.junit.jupiter.api.Test


class CastingTest {

    @Test
    fun test() {
        val value = """
            {
              "id": 5716378999077949009,
              "name": "casting:No.100003",
              "task": {
                "id": 5278019614729850489,
                "name": "No.100003",
                "input": {
                  "type": "STYLED_IMAGE",
                  "image": "https://cdn-kling-waic-aws-cn-staging.klingai.com/guanghe/request-images/No.100003-QaXOX1NgcQ_Tvw4djpkcZK470nGw5xaLlio0AvQgQPY.jpg"
                },
                "taskIds": [
                  "788570377261326397",
                  "788570377483464731",
                  "788570377802387517",
                  "788570378053890065",
                  "788570378414604380",
                  "788570378792091648",
                  "788570379140206669",
                  "788570379492528202"
                ],
                "status": "SUCCEED",
                "type": "STYLED_IMAGE",
                "filename": "No.100003-QaXOX1NgcQ_Tvw4djpkcZK470nGw5xaLlio0AvQgQPY.jpg",
                "outputs": {
                  "type": "IMAGE",
                  "url": "https://cdn-kling-waic-aws-cn-staging.klingai.com/guanghe/output-images/No.100003-QaXOX1NgcQ_Tvw4djpkcZK470nGw5xaLlio0AvQgQPY.jpg",
                  "thumbnailUrl": "https://cdn-kling-waic-aws-cn-staging.klingai.com/guanghe/output-images/No.100003-QaXOX1NgcQ_Tvw4djpkcZK470nGw5xaLlio0AvQgQPY-thumbnail.jpg"
                },
                "createTime": 1756038348.230599993,
                "updateTime": 1756038384.638808019,
                "elapsedTimeInSeconds": 31
              },
              "score": 1.75603838464E+12
            }
        """.trimIndent()
        val casting = ObjectMapperUtils.fromJSON(value, Casting::class.java)
        println(casting)
    }
}