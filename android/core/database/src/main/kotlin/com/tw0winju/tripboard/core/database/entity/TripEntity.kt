package com.tw0winju.tripboard.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TODO(동기화 설계)
 *  - 필드별 HLC 컬럼(title_hlc 등)을 둘지, 변경 로그(outbox) 테이블을 따로 둘지
 *  - 삭제는 실제 DELETE가 아니라 툼스톤(deleted_hlc)으로?
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
)
