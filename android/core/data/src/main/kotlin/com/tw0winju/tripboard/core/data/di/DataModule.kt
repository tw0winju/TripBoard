package com.tw0winju.tripboard.core.data.di

import com.tw0winju.tripboard.core.data.repository.OfflineFirstTripRepository
import com.tw0winju.tripboard.core.data.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindTripRepository(impl: OfflineFirstTripRepository): TripRepository
}
