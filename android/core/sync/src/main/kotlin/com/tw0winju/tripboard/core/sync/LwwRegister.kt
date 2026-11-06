package com.tw0winju.tripboard.core.sync

/**
 * Last-Writer-Wins 레지스터. 필드 하나에 하나씩 붙는다.
 *
 * merge가 지켜야 할 성질: 교환법칙, 결합법칙, 멱등성 → 테스트로 먼저 고정해 보기.
 */
data class LwwRegister<T>(
    val value: T,
    val timestamp: Hlc,
) {
    fun merge(other: LwwRegister<T>): LwwRegister<T> =
        if (other.timestamp > timestamp) other else this
}
