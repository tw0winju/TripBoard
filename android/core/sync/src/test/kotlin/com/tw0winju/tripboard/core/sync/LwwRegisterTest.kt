package com.tw0winju.tripboard.core.sync

import org.junit.Test
import kotlin.test.assertEquals

class LwwRegisterTest {

    @Test
    fun `merge는 교환법칙을 만족한다`() {
        val a  = LwwRegister("오사카로 이동", Hlc(1000L, 0, "A"))
        val b = LwwRegister("숙소에서 타코야끼", Hlc(1500L, 0, "B"))

        assertEquals(a.merge(b), b.merge(a))
    }

    @Test
    fun `merge는 멱등이다`() {
        val a  = LwwRegister("오사카로 이동", Hlc(1000L, 0, "A"))
        assertEquals(a, a.merge(a))
    }

    @Test
    fun `타임스탬프가 같으면 nodeId로 결정적으로 고른다`() {
        val a  = LwwRegister("오사카로 이동", Hlc(1000L, 0, "A"))
        val b  = LwwRegister("숙소에서 타코야끼", Hlc(1000L, 0, "B"))

        assertEquals(b, a.merge(b))
    }

    @Test
    fun `더 나중에 쓴 값이 남는다`() {
        val a  = LwwRegister("오사카로 이동", Hlc(1000L, 0, "A"))
        val b = LwwRegister("숙소에서 타코야끼", Hlc(1500L, 0, "B"))

        assertEquals(b, a.merge(b))
    }
}
