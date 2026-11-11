package com.tw0winju.tripboard.core.data.repository

import androidx.room.withTransaction
import com.tw0winju.tripboard.core.database.TripBoardDatabase
import com.tw0winju.tripboard.core.database.dao.PendingChangeDao
import com.tw0winju.tripboard.core.database.dao.TripItemsDao
import com.tw0winju.tripboard.core.database.entity.EntityTypes
import com.tw0winju.tripboard.core.database.entity.PendingChangeEntity
import com.tw0winju.tripboard.core.database.entity.TripItemFields
import com.tw0winju.tripboard.core.sync.HybridLogicalClock
import javax.inject.Inject

internal class TripItemRepository @Inject constructor(
    private val database: TripBoardDatabase,
    private val tripItemsDao: TripItemsDao,
    private val pendingChangeDao: PendingChangeDao,
    private val clock: HybridLogicalClock
) {

    suspend fun updateMemo(itemId: String, memo: String) {
        val hlc = clock.now().encode()
        database.withTransaction {
            tripItemsDao.updateMemo(itemId, memo, hlc)
            pendingChangeDao.upsert(
                PendingChangeEntity(
                    entityId = itemId,
                    entityType = EntityTypes.TRIP_ITEM,
                    field = TripItemFields.MEMO,
                    value = memo,
                    hlc = hlc
                )
            )
        }
    }
}
