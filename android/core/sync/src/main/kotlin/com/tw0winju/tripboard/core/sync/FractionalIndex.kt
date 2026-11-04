package com.tw0winju.tripboard.core.sync

/**
 * 분수 인덱스(fractional indexing). 일정 항목 순서를 재정렬해도 다른 항목을 건드리지 않게 한다.
 *
 * 생각해 볼 것
 *  - 키는 문자열(사전순 비교). 두 키 사이에 항상 새 키를 만들 수 있어야 한다.
 *  - 두 기기가 같은 위치에 동시에 끼워 넣으면 같은 키가 나올 수 있다 → 동률 처리(예: 뒤에 nodeId)?
 *  - 같은 자리에 반복 삽입하면 키가 계속 길어진다. 감수할지, 리밸런싱할지.
 */
object FractionalIndex {
    /** before == null 이면 맨 앞, after == null 이면 맨 뒤 */
    fun between(before: String?, after: String?): String = TODO("직접 구현")
}
