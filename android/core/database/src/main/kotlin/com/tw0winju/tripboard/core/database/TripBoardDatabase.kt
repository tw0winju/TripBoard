package com.tw0winju.tripboard.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tw0winju.tripboard.core.database.dao.TripDao
import com.tw0winju.tripboard.core.database.dao.TripItemsDao
import com.tw0winju.tripboard.core.database.entity.TripEntity
import com.tw0winju.tripboard.core.database.entity.TripItemEntity

@Database(
    entities = [TripEntity::class, TripItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TripBoardDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun tripItemsDao(): TripItemsDao
}
