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
import com.tw0winju.tripboard.core.sync.LwwRegister
import com.tw0winju.tripboard.core.sync.RemoteChange
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

    suspend fun applyRemoteChange(change: RemoteChange) {

        if (change.entityType != EntityTypes.TRIP_ITEM) return

        clock.receive(change.hlc)   // ① 시계부터 당기기

        database.withTransaction {
            val entity = tripItemsDao.getById(change.entityId) ?: return@withTransaction

            // 1. 비교할 로컬 값, HLC 고르기 (모르는 필드면 무시)
            val (localValue, localHlc) = when (change.field) {
                TripItemFields.MEMO -> entity.memo to entity.memoHlc
                TripItemFields.TITLE -> entity.title to entity.titleHlc
                else -> return@withTransaction
            }

            val local = LwwRegister(localValue, Hlc.decode(localHlc))
            val remote = LwwRegister(change.value, change.hlc)

            // 2. 로컬이 이기거나 같으면 할 일 없음
            if (local.merge(remote) == local) return@withTransaction

            // 3. 원격 값 쓰기
            when (change.field) {
                TripItemFields.MEMO -> tripItemsDao.updateMemo(change.entityId, change.value, change.hlc.encode())
                TripItemFields.TITLE -> tripItemsDao.updateTitle(change.entityId, change.value, change.hlc.encode())
            }

            // 4. 진 로컬 변경은 보낼 필요 없음
            pendingChangeDao.delete(change.entityType, change.entityId, change.field)
        }
    }

    override fun observePendingCount(): Flow<Int> = pendingChangeDao.observeCount()


    override fun observeItems(tripId: String): Flow<List<TripItem>> =
        tripItemsDao.observeByTrip(tripId).map { entities -> entities.map { it.toModel() } }


    private fun TripItemEntity.toModel(): TripItem {
        val decodedTitleHlc = Hlc.decode(titleHlc)
        val decodedMemoHlc = Hlc.decode(memoHlc)
        return TripItem(
            id = id,
            title = title,
            createdAt = createdAt,
            tripId = tripId,
            memo = memo,
            titleMeta = FieldMeta(
                editedAt = decodedTitleHlc.wallTime,
                editedBy = decodedTitleHlc.nodeId,
                isPending = false   // TODO: outbox와 combine해서 채우기
            ),
            memoMeta = FieldMeta(
                editedAt = decodedMemoHlc.wallTime,
                editedBy = decodedMemoHlc.nodeId,
                isPending = false   // TODO: outbox와 combine해서 채우기
            ),
        )
    }
}
