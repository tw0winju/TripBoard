package com.tw0winju.tripboard.core.database.di

import android.content.Context
import androidx.room.Room
import com.tw0winju.tripboard.core.database.TripBoardDatabase
import com.tw0winju.tripboard.core.database.dao.PendingChangeDao
import com.tw0winju.tripboard.core.database.dao.TripDao
import com.tw0winju.tripboard.core.database.dao.TripItemsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TripBoardDatabase =
        Room.databaseBuilder(context, TripBoardDatabase::class.java, "tripboard.db").build()

    @Provides
    fun provideTripDao(database: TripBoardDatabase): TripDao = database.tripDao()

    @Provides
    fun provideTripItemDao(database: TripBoardDatabase): TripItemsDao = database.tripItemsDao()

    @Provides
    fun providePendingChangeDao(database: TripBoardDatabase): PendingChangeDao = database.pendingChangeDao()
}
