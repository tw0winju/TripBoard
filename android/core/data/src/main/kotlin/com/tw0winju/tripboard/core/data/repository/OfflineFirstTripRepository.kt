package com.tw0winju.tripboard.core.data.repository

import com.tw0winju.tripboard.core.database.dao.TripDao
import com.tw0winju.tripboard.core.database.entity.TripEntity
import com.tw0winju.tripboard.core.model.Trip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

/**
 * 오프라인 퍼스트: UI는 항상 로컬 DB만 구독하고, 네트워크는 DB를 갱신하는 역할만 한다.
 *
 * TODO: createTrip 시 outbox에 변경을 기록하고 SyncWorker를 깨우기
 */
internal class OfflineFirstTripRepository @Inject constructor(
    private val tripDao: TripDao,
) : TripRepository {

    override fun observeTrips(): Flow<List<Trip>> =
        tripDao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun createTrip(title: String) {
        tripDao.upsert(
            TripEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}

private fun TripEntity.toModel() = Trip(id = id, title = title, createdAt = createdAt)
