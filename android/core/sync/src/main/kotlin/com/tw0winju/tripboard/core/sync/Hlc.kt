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

    override fun compareTo(other: Hlc): Int =
        compareValuesBy(this, other, { it.wallTime }, { it.counter }, { it.nodeId })

    // format : 123451234512345:12345:A
    fun encode(): String {
        val w = wallTime.toString().padStart(15, '0')
        val c = counter.toString().padStart(5, '0')
        return "$w:$c:$nodeId"
    }

    companion object {
        fun decode(value: String): Hlc {
            val (w, c, n) = value.split(":", limit = 3)
            return Hlc(w.toLong(), c.toInt(), n)
        }
    }
}

/**
 * @param physicalClock 테스트에서 시간을 고정할 수 있게 주입받는다.
 */
class HybridLogicalClock(
    private val nodeId: String,
    private val physicalClock: () -> Long = System::currentTimeMillis,
) {

    private var last: Hlc = Hlc(wallTime = 0L, counter = 0, nodeId = nodeId)

    /** 로컬 이벤트(편집)가 일어날 때 */
    fun now(): Hlc {
        val pt = physicalClock()

        last = if (pt > last.wallTime) {
            Hlc(wallTime = pt, counter = 0, nodeId = nodeId)
        } else {
            Hlc(wallTime = last.wallTime, counter = last.counter + 1, nodeId = nodeId)
        }
        return last
    }

    /** 원격 타임스탬프를 받았을 때. 로컬 시계를 앞으로 당긴다. */
    fun receive(remote: Hlc): Hlc {
        val pt = physicalClock()
        val wall = maxOf(last.wallTime, remote.wallTime, pt)

        val counter = when {
            wall == last.wallTime && wall == remote.wallTime -> maxOf(last.counter, remote.counter) + 1
            wall == last.wallTime -> last.counter + 1
            wall == remote.wallTime -> remote.counter + 1
            else -> 0
        }

        last = Hlc(wallTime = wall, counter = counter, nodeId = nodeId)
        return last
    }
}
