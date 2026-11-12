package com.tw0winju.tripboard.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tw0winju.tripboard.core.database.TripBoardDatabase
import com.tw0winju.tripboard.core.database.entity.TripItemEntity
import com.tw0winju.tripboard.core.sync.Hlc
import com.tw0winju.tripboard.core.sync.HybridLogicalClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class OfflineFirstTripItemRepositoryTest {

    private lateinit var db: TripBoardDatabase
    private lateinit var repo: OfflineFirstTripItemRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TripBoardDatabase::class.java).build()
        val clock = HybridLogicalClock(nodeId = "A", physicalClock = { 1000L })
        repo = OfflineFirstTripItemRepository(db, db.tripItemsDao(), db.pendingChangeDao(), clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun memo_edited_three_times_leaves_one_pending_change() = runTest {
        // given: 고칠 항목 하나
        val initial = Hlc(0L, 0, "A").encode()
        db.tripItemsDao().upsert(
            TripItemEntity(
                id = "item-1", tripId = "trip-1",
                title = "이치란 라멘", titleHlc = initial,
                memo = "", memoHlc = initial,
                createdAt = 0L,
            )
        )

        // when: 메모를 세 번 수정
        repo.updateMemo("item-1", "A")
        repo.updateMemo("item-1", "B")
        repo.updateMemo("item-1", "C")

        // then
        assertEquals(1, repo.observePendingCount().first())
        assertEquals("C", repo.observeItems("trip-1").first()[0].memo)
        assertEquals("C", db.pendingChangeDao().getAll().single().value)
    }
}
