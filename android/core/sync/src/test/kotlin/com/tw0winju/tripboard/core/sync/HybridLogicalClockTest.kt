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
    fun `encode 후 decode하면 원래 값으로 돌아온다`() {
        val original = Hlc(1730779200000L, 42, "node:1")   // nodeId에 일부러 ':' 넣기

        val restored = Hlc.decode(original.encode())

        assertEquals(original, restored)
    }

    @Test
    fun `encode한 문자열의 정렬 순서는 Hlc 비교 순서와 같다`() {
        val a = Hlc(1000L, 9, "A")
        val b = Hlc(1000L, 10, "A")   // counter 9 vs 10 함정
        val c = Hlc(2000L, 0, "A")

        assertTrue(a < b && b < c)
        assertTrue(a.encode() < b.encode())
        assertTrue(b.encode() < c.encode())
    }
}
