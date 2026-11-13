package com.tw0winju.tripboard.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.tw0winju.tripboard.core.database.entity.PendingChangeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingChangeDao {
    @Upsert
    suspend fun upsert(pendingChange: PendingChangeEntity)

    @Query("SELECT COUNT(*) FROM pending_changes")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM pending_changes ORDER BY hlc")
    suspend fun getAll(): List<PendingChangeEntity>

    @Query("DELETE FROM pending_changes WHERE entityType = :entityType AND entityId = :entityId AND field = :field")
    suspend fun delete(entityType: String, entityId: String, field: String): Int
}
