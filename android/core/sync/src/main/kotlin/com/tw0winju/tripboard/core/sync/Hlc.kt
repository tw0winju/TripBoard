package com.tw0winju.tripboard.core.sync

/**
 * Hybrid Logical Clock 타임스탬프.
 *
 * 생각해 볼 것
 *  - 비교 순서: wallTime → counter → nodeId (nodeId는 동률을 깨는 용도)
 *  - 직렬화 포맷: 문자열 정렬 순서 == 시간 순서가 되게 만들면 DB 인덱스/정렬이 쉬워진다.
 *  - counter 오버플로, 너무 미래의 원격 시각(clock drift)을 어떻게 다룰지.
 */
data class Hlc(
    val wallTime: Long,
    val counter: Int,
    val nodeId: String,
) : Comparable<Hlc> {

    override fun compareTo(other: Hlc): Int = TODO("직접 구현")

    fun encode(): String = TODO("직접 구현")

    companion object {
        fun decode(value: String): Hlc = TODO("직접 구현")
    }
}

/**
 * @param physicalClock 테스트에서 시간을 고정할 수 있게 주입받는다.
 */
class HybridLogicalClock(
    private val nodeId: String,
    private val physicalClock: () -> Long = System::currentTimeMillis,
) {
    /** 로컬 이벤트(편집)가 일어날 때 */
    fun now(): Hlc = TODO("직접 구현")

    /** 원격 타임스탬프를 받았을 때. 로컬 시계를 앞으로 당긴다. */
    fun receive(remote: Hlc): Hlc = TODO("직접 구현")
}
