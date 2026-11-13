package com.tw0winju.tripboard.core.sync

data class RemoteChange(
    val entityType: String,
    val entityId: String,
    val field: String,
    val value: String,
    val hlc: Hlc,
)
