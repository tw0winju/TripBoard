package com.tw0winju.tripboard.core.data.repository

import com.tw0winju.tripboard.core.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun observeTrips(): Flow<List<Trip>>
    suspend fun createTrip(title: String)
}
