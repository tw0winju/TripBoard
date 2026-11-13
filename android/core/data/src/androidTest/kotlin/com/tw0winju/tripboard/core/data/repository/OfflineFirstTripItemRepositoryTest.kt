package com.tw0winju.tripboard.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tw0winju.tripboard.core.database.TripBoardDatabase
import com.tw0winju.tripboard.core.database.entity.EntityTypes
import com.tw0winju.tripboard.core.database.entity.TripItemEntity
import com.tw0winju.tripboard.core.database.entity.TripItemFields
import com.tw0winju.tripboard.core.sync.Hlc
import com.tw0winju.tripboard.core.sync.HybridLogicalClock
import com.tw0winju.tripboard.core.sync.RemoteChange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        insertItem("", Hlc(0L, 0, "A"))

        // when: 메모를 세 번 수정
        repo.updateMemo("item-1", "A")
        repo.updateMemo("item-1", "B")
        repo.updateMemo("item-1", "C")

        // then
        assertEquals(1, repo.observePendingCount().first())
        assertEquals("C", repo.observeItems("trip-1").first()[0].memo)
        assertEquals("C", db.pendingChangeDao().getAll().single().value)
    }

    @Test
    fun remote_newer_change_overwrites_local_memo() = runTest {
        // given: 로컬 메모 "C", hlc 1000
        insertItem("C", Hlc(1000L, 0, "A"))

        // when: 지수 기기("B")에서 온 더 최신 변경 반영
        repo.applyRemoteChange(remoteMemo("포장해서", Hlc(2000L, 0, "B")))

        // then
        assertEquals("포장해서", repo.observeItems("trip-1").first()[0].memo)
    }

    @Test
    fun local_newer_memo_is_kept_when_older_remote_change_arrives() = runTest {
        // given: 로컬 메모 "C"가 hlc 3000으로 더 최신
        insertItem(memo = "C", memoHlc = Hlc(3000L, 0, "A"))

        // when: 더 오래된 원격 변경(hlc 2000) 도착
        repo.applyRemoteChange(remoteMemo(value = "포장해서", hlc = Hlc(2000L, 0, "B")))

        // then: 로컬 값이 그대로 남는다
        assertEquals("C", repo.observeItems("trip-1").first()[0].memo)
    }

    @Test
    fun local_edit_after_remote_change_gets_later_hlc_than_remote() = runTest {
        // given: 물리 시계는 1000에 고정(setUp), 원격 변경은 hlc 2000
        insertItem(memo = "C", memoHlc = Hlc(1000L, 0, "A"))
        repo.applyRemoteChange(remoteMemo(value = "포장해서", hlc = Hlc(2000L, 0, "B")))

        // when: 그 직후 내가 메모를 수정
        repo.updateMemo("item-1", "D")

        // then: 내 수정의 시각이 원격(2000)보다 뒤여야 다른 기기에서도 내 수정이 이긴다
        //       clock.receive()가 없으면 물리 시계 1000으로 찍혀서 실패한다
        val memoMeta = repo.observeItems("trip-1").first()[0].memoMeta
        assertTrue(memoMeta.editedAt >= 2000L, "editedAt=${memoMeta.editedAt}")
        assertEquals("A", memoMeta.editedBy)
    }

    private suspend fun insertItem(memo: String, memoHlc: Hlc) {
        db.tripItemsDao().upsert(
            TripItemEntity(
                id = "item-1", tripId = "trip-1",
                title = "이치란 라멘", titleHlc = memoHlc.encode(),
                memo = memo, memoHlc = memoHlc.encode(),
                createdAt = 0L,
            )
        )
    }

    private fun remoteMemo(value: String, hlc: Hlc) = RemoteChange(
        entityType = EntityTypes.TRIP_ITEM,
        entityId = "item-1",
        field = TripItemFields.MEMO,
        value = value,
        hlc = hlc,
    )
}
