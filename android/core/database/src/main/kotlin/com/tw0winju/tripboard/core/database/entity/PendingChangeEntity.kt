package com.tw0winju.tripboard.core.database.entity

import androidx.room.Entity

@Entity(tableName = "pending_changes", primaryKeys = ["entityId", "entityType", "field"])
data class PendingChangeEntity(
    val entityId: String,
    val entityType: String,
    val field: String,
    val value: String,
    val hlc: String
)

object EntityTypes {
    const val TRIP_ITEM = "trip_item"
}

object TripItemFields {
    const val TITLE = "title";
    const val MEMO = "memo"
}
