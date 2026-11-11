package com.tw0winju.tripboard.core.data.repository

import com.tw0winju.tripboard.core.model.TripItem
import kotlinx.coroutines.flow.Flow

interface TripItemRepository {
    suspend fun updateMemo(itemId: String, memo: String)
    fun observePendingCount(): Flow<Int>              // 4번 화면 배지
    fun observeItems(tripId: String): Flow<List<TripItem>>   // 4번 화면 목록
}
