package com.tw0winju.tripboard.core.data.repository

import androidx.room.withTransaction
import com.tw0winju.tripboard.core.database.TripBoardDatabase
import com.tw0winju.tripboard.core.database.dao.PendingChangeDao
import com.tw0winju.tripboard.core.database.dao.TripItemsDao
import com.tw0winju.tripboard.core.database.entity.EntityTypes
import com.tw0winju.tripboard.core.database.entity.PendingChangeEntity
import com.tw0winju.tripboard.core.database.entity.TripItemEntity
import com.tw0winju.tripboard.core.database.entity.TripItemFields
import com.tw0winju.tripboard.core.model.FieldMeta
import com.tw0winju.tripboard.core.model.TripItem
import com.tw0winju.tripboard.core.sync.Hlc
import com.tw0winju.tripboard.core.sync.HybridLogicalClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class OfflineFirstTripItemRepository @Inject constructor(
    private val database: TripBoardDatabase,
    private val tripItemsDao: TripItemsDao,
    private val pendingChangeDao: PendingChangeDao,
    private val clock: HybridLogicalClock
) : TripItemRepository {

    override suspend fun updateMemo(itemId: String, memo: String) {
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

    override fun observePendingCount(): Flow<Int> = pendingChangeDao.observeCount()


    override fun observeItems(tripId: String): Flow<List<TripItem>> =
        tripItemsDao.observeByTrip(tripId).map { entities -> entities.map { it.toModel() } }


    private fun TripItemEntity.toModel(): TripItem {
        val titleHlc = Hlc.decode(titleHlc)
        val memoHlc = Hlc.decode(memoHlc)
        return TripItem(
            id = id,
            title = title,
            createdAt = createdAt,
            tripId = tripId,
            memo = memo,
            titleMeta = FieldMeta(
                editedAt = titleHlc.wallTime,
                editedBy = titleHlc.nodeId,
                isPending = false   // TODO: outbox와 combine해서 채우기
            ),
            memoMeta = FieldMeta(
                editedAt = memoHlc.wallTime,
                editedBy = memoHlc.nodeId,
                isPending = false   // TODO: outbox와 combine해서 채우기
            ),
        )
    }
}
