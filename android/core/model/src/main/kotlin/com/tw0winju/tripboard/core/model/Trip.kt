package com.tw0winju.tripboard.core.model

/**
 * 앱 전체에서 쓰는 도메인 모델. Room/네트워크 타입에 의존하지 않는다.
 *
 * TODO(동기화 설계): 필드 단위 LWW로 가면 title 같은 필드마다 HLC가 붙어야 한다.
 *  - 도메인 모델에 메타데이터를 노출할지, 데이터 계층에만 둘지 정해 보기.
 */
data class Trip(
    val id: String,
    val title: String,
    val createdAt: Long,
)
