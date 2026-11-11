package com.tw0winju.tripboard.core.data.di

import com.tw0winju.tripboard.core.data.repository.OfflineFirstTripItemRepository
import com.tw0winju.tripboard.core.data.repository.OfflineFirstTripRepository
import com.tw0winju.tripboard.core.data.repository.TripItemRepository
import com.tw0winju.tripboard.core.data.repository.TripRepository
import com.tw0winju.tripboard.core.sync.HybridLogicalClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindTripRepository(impl: OfflineFirstTripRepository): TripRepository

    @Binds
    abstract fun bindTripItemRepository(impl: OfflineFirstTripItemRepository): TripItemRepository

    companion object {
        @Provides
        @Singleton
        // TODO: nodeId를 DataStore에 저장해 앱 재시작 후에도 유지
        fun provideClock(): HybridLogicalClock =
            HybridLogicalClock(nodeId = UUID.randomUUID().toString())
    }
}
