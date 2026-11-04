package com.tw0winju.tripboard.core.sync

import org.junit.Ignore
import org.junit.Test

/** 구현 전에 테스트부터 채워 보기(TDD). 구현이 끝나면 @Ignore를 지운다. */
@Ignore("HLC 구현 후 해제")
class HybridLogicalClockTest {

    @Test
    fun `물리 시계가 멈춰 있어도 now는 단조 증가한다`() {
        // given: physicalClock = { 1000L } 고정
        // when: now()를 여러 번 호출
        // then: 매번 이전 값보다 크다
    }

    @Test
    fun `원격 시각이 더 미래면 receive 후 로컬 시각이 따라간다`() {
    }

    @Test
    fun `encode한 문자열의 정렬 순서는 Hlc 비교 순서와 같다`() {
    }
}
