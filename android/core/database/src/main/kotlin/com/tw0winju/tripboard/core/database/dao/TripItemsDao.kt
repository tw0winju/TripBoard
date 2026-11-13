package com.tw0winju.tripboard.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tw0winju.tripboard.core.database.entity.TripItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripItemsDao {
    @Query("SELECT * FROM trips_item WHERE tripId = :tripId ORDER BY createdAt")
    fun observeByTrip(tripId: String): Flow<List<TripItemEntity>>

    @Upsert
    suspend fun upsert(tripItems: TripItemEntity)

    @Query("UPDATE trips_item SET memo = :memo, memoHlc = :memoHlc WHERE id = :id")
    suspend fun updateMemo(id: String, memo: String, memoHlc: String)

    @Query("SELECT * FROM trips_item WHERE id = :id")
    suspend fun getById(id: String): TripItemEntity?

    @Query("UPDATE trips_item SET title = :title, titleHlc = :titleHlc WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, titleHlc: String)
}
