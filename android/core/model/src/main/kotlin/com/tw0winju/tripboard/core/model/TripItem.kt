package com.tw0winju.tripboard.core.model

data class TripItem(
    val id: String,
    val tripId: String,
    val title: String,
    val titleMeta: FieldMeta,
    val memo: String,
    val memoMeta: FieldMeta,
    val createdAt: Long,
)
