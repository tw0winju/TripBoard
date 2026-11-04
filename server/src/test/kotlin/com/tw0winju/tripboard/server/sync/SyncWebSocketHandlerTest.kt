package com.tw0winju.tripboard.server.sync

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * @SpringBootTest는 DB가 필요하므로, 붙일 때 Testcontainers(PostgreSQL)를 함께 도입하는 걸 추천.
 */
class SyncWebSocketHandlerTest {

    @Test
    fun `핸들러를 생성할 수 있다`() {
        assertNotNull(SyncWebSocketHandler())
    }
}
