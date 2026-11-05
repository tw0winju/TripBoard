package com.tw0winju.tripboard.core.sync

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HybridLogicalClockTest {

    @Test
    fun `물리 시계가 멈춰 있어도 now는 단조 증가한다`() {
        // given: physicalClock = { 1000L } 고정
        val clock = HybridLogicalClock(nodeId = "A", physicalClock = { 1000L })

        // when: now()를 여러 번 호출
        val first = clock.now()
        val second = clock.now()

        // then: 매번 이전 값보다 크다
        assertTrue(second > first)
    }

    @Test
    fun `원격 시각이 더 미래면 receive 후 로컬 시각이 따라간다`() {
        val clock = HybridLogicalClock(nodeId = "A", physicalClock = { 1000L })
        val remote = Hlc(2000, 3, "B")
        val result = clock.receive(remote)
        assertEquals(Hlc(2000, 4, "A"), result)
    }

    @Test
    fun `encode한 문자열의 정렬 순서는 Hlc 비교 순서와 같다`() {
    }
}
