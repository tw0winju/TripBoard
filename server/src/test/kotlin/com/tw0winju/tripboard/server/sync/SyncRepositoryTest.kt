package com.tw0winju.tripboard.server.sync

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Flyway가 V1__init.sql을 컨테이너에 적용한 뒤 돌아간다.
 * 테스트마다 다른 tripId를 쓰므로 서로 간섭하지 않고, 테이블을 비울 필요도 없다.
 *
 * Docker Desktop이 아니라 Colima를 쓰는 머신에서는 ~/.testcontainers.properties에
 *   docker.host=unix:///Users/<me>/.colima/default/docker.sock
 *   ryuk.disabled=true
 * 가 필요하다. 기본 소켓(/var/run/docker.sock)은 Docker Desktop 것이고,
 * 정리용 사이드카(Ryuk)가 Colima VM 안에서 뜨지 못한다. Ryuk 없이도 @Container가 종료 시 컨테이너를 지운다.
 */
@SpringBootTest
@Testcontainers
class SyncRepositoryTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }

    @Autowired
    lateinit var repository: SyncRepository

    private val tripId = UUID.randomUUID().toString()

    /** Hlc.encode()와 같은 형식: 15자리 wallTime : 5자리 counter : nodeId */
    private fun hlc(wallTime: Long, counter: Int = 0, node: String = "A") =
        "${wallTime.toString().padStart(15, '0')}:${counter.toString().padStart(5, '0')}:$node"

    private fun memo(value: String, hlc: String) = FieldChange("trip_item", "i1", "memo", value, hlc)

    @Test
    fun `더 새 HLC만 반영되고, 더 오래된 것과 같은 것의 재전송은 무시된다`() {
        val first = repository.push(tripId, listOf(memo("처음", hlc(1_000))))
        assertEquals(listOf("처음"), first.accepted.map { it.value })

        val newer = repository.push(tripId, listOf(memo("더 새것", hlc(2_000, node = "B"))))
        assertEquals(listOf("더 새것"), newer.accepted.map { it.value })

        val older = repository.push(tripId, listOf(memo("오래된 편집", hlc(1_500))))
        assertTrue(older.accepted.isEmpty(), "오래된 HLC는 서버 값을 덮어쓰면 안 된다")

        val resent = repository.push(tripId, listOf(memo("더 새것", hlc(2_000, node = "B"))))
        assertTrue(resent.accepted.isEmpty(), "같은 변경을 다시 보내도 결과가 달라지면 안 된다")

        val snapshot = repository.pull(tripId, afterSeq = 0)
        assertEquals(listOf("더 새것"), snapshot.changes.map { it.value })
    }

    @Test
    fun `pull은 커서 이후 변경만 순번 순으로 돌려준다`() {
        val created = repository.push(
            tripId,
            listOf(
                FieldChange("trip", tripId, "title", "제주", hlc(1_000)),
                FieldChange("trip_item", "i1", "title", "공항", hlc(1_001)),
                FieldChange("trip_item", "i1", "position", "a0", hlc(1_001)),
            ),
        )
        val cursor = created.accepted.last().seq

        val none = repository.pull(tripId, afterSeq = cursor)
        assertTrue(none.changes.isEmpty())
        assertEquals(cursor, none.nextCursor, "받은 게 없으면 커서는 그대로")

        repository.push(tripId, listOf(memo("9시 출발", hlc(2_000))))

        val delta = repository.pull(tripId, afterSeq = cursor)
        assertEquals(listOf("memo"), delta.changes.map { it.field })
        assertTrue(delta.nextCursor > cursor)
        assertFalse(delta.hasMore)

        val snapshot = repository.pull(tripId, afterSeq = 0)
        assertEquals(listOf("title", "title", "position", "memo"), snapshot.changes.map { it.field })
        assertEquals(snapshot.changes.map { it.seq }, snapshot.changes.map { it.seq }.sorted())
    }

    @Test
    fun `limit에 걸리면 hasMore가 켜지고 nextCursor로 이어서 받을 수 있다`() {
        repository.push(
            tripId,
            (1..5).map { FieldChange("trip_item", "i$it", "title", "항목 $it", hlc(1_000L + it)) },
        )

        val page1 = repository.pull(tripId, afterSeq = 0, limit = 2)
        assertEquals(listOf("i1", "i2"), page1.changes.map { it.entityId })
        assertTrue(page1.hasMore)

        val page2 = repository.pull(tripId, afterSeq = page1.nextCursor, limit = 2)
        assertEquals(listOf("i3", "i4"), page2.changes.map { it.entityId })

        val page3 = repository.pull(tripId, afterSeq = page2.nextCursor, limit = 2)
        assertEquals(listOf("i5"), page3.changes.map { it.entityId })
        assertFalse(page3.hasMore)
    }

    @Test
    fun `갱신된 필드는 새 순번을 받아 커서 뒤로 다시 나타난다`() {
        val first = repository.push(tripId, listOf(memo("처음", hlc(1_000)))).accepted.single()
        val second = repository.push(tripId, listOf(memo("수정", hlc(2_000)))).accepted.single()
        assertTrue(second.seq > first.seq)

        val delta = repository.pull(tripId, afterSeq = first.seq)
        assertEquals(listOf("수정"), delta.changes.map { it.value })
    }

    @Test
    fun `모르는 필드가 섞이면 묶음 전체가 거부된다`() {
        assertFailsWith<DataIntegrityViolationException> {
            repository.push(
                tripId,
                listOf(
                    memo("정상", hlc(1_000)),
                    FieldChange("trip_item", "i1", "color", "red", hlc(1_000)),
                ),
            )
        }
        assertTrue(repository.pull(tripId, afterSeq = 0).changes.isEmpty(), "정상 변경도 함께 롤백돼야 한다")
    }

    @Test
    fun `빈 묶음은 아무것도 하지 않는다`() {
        assertTrue(repository.push(tripId, emptyList()).accepted.isEmpty())
        assertTrue(repository.pull(tripId, afterSeq = 0).changes.isEmpty())
    }
}
