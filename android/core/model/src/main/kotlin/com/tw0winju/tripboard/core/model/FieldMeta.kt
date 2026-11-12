package com.tw0winju.tripboard.core.model

data class FieldMeta(
    val editedBy: String,     // 누가
    val editedAt: Long,       // 언제 (밀리초)
    val isPending: Boolean,   // 아직 서버에 안 보냄
)
