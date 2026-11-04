package com.tw0winju.tripboard.core.sync

/**
 * Observed-Remove Set. 여행 멤버, 태그, 체크리스트 항목 등에 쓸 후보.
 *
 * 생각해 볼 것
 *  - add마다 고유 태그(HLC 등)를 붙이고, remove는 "관찰한 태그"만 지운다.
 *  - 동시 add/remove가 충돌하면 add가 이긴다(add-wins). 왜 그런지 설명할 수 있어야 한다.
 *  - 툼스톤이 계속 쌓이는 문제: 언제 GC할 수 있을까?
 *  - 불변(immutable)으로 만들지, 가변으로 만들지.
 */
class OrSet<E> {

    val elements: Set<E>
        get() = TODO("직접 구현")

    fun add(element: E, tag: Hlc): Unit = TODO("직접 구현")

    fun remove(element: E): Unit = TODO("직접 구현")

    fun merge(other: OrSet<E>): OrSet<E> = TODO("직접 구현")
}
