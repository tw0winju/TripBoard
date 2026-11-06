package com.tw0winju.tripboard.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips_item")
data class TripItemEntity(
    @PrimaryKey
    val id: String,
    val tripId: String,

    val title: String,
    val titleHlc: String,

    val memo: String,
    val memoHlc: String,

    val createdAt: Long,
)
