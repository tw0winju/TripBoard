package com.tw0winju.tripboard.server.sync

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

/** 앱이 push하는 변경 한 건. 앱의 RemoteChange와 같은 모양이고 hlc는 Hlc.encode() 문자열이다. */
data class FieldChange(
    val entityType: String,
    val entityId: String,
    val field: String,
    val value: String,
    val hlc: String,
)

/** 서버가 받아들인 필드 값. seq가 곧 pull 커서다. */
data class SyncedField(
    val entityType: String,
    val entityId: String,
    val field: String,
    val value: String,
    val hlc: String,
    val seq: Long,
)

/**
 * @param accepted LWW에서 이겨서 저장된 변경. 같은 여행을 보는 다른 세션에 이것만 브로드캐스트하면 된다.
 *                 진 변경(서버 값이 더 새것)은 여기 없다. 앱은 보낸 변경이 여기 없어도 outbox에서 지워야 한다.
 *                 서버 값은 다음 pull에서 받는다.
 */
data class PushResult(val accepted: List<SyncedField>)

/**
 * @param nextCursor 다음 pull에 넘길 커서. 받은 게 없으면 넘긴 커서 그대로.
 * @param hasMore    limit에 걸려 잘렸을 가능성. true면 nextCursor로 바로 다시 pull한다.
 */
data class PullResult(
    val changes: List<SyncedField>,
    val nextCursor: Long,
    val hasMore: Boolean,
)

/**
 * lww_fields 저장소. 설계 근거는 db/migration/V1__init.sql 머리 주석에.
 *
 * 병합 판정은 전부 SQL(ON CONFLICT ... WHERE hlc < EXCLUDED.hlc) 안에서 끝낸다.
 * 코틀린 쪽에는 read-modify-write가 없어서, 같은 필드에 동시에 push가 와도 잠금 걱정이 없다.
 */
@Repository
class SyncRepository(private val jdbc: JdbcClient) {

    /**
     * 한 여행에 대한 변경 묶음을 한 트랜잭션으로 반영한다.
     *
     * 순서
     *  1. trips 행을 만들거나 갱신하면서 순번 n개를 예약한다. 이 UPDATE가 행 잠금을 잡아
     *     커밋까지 같은 여행의 다른 push를 기다리게 한다 → 순번 발급 순서 == 커밋 순서.
     *  2. 변경마다 조건부 upsert. 반영된 행만 RETURNING으로 돌아온다.
     *
     * 모르는 필드나 형식이 틀린 HLC가 섞여 있으면 CHECK 제약에 걸려 묶음 전체가 롤백된다
     * (DataIntegrityViolationException). 일부만 반영하는 것보다 앱이 버그를 빨리 알아차리는 쪽을 택했다.
     */
    @Transactional
    fun push(tripId: String, changes: List<FieldChange>): PushResult {
        if (changes.isEmpty()) return PushResult(emptyList())

        val baseSeq = reserveSeq(tripId, changes.size)

        val accepted = changes.mapIndexedNotNull { i, change ->
            upsert(tripId, change, seq = baseSeq + i + 1)
        }
        return PushResult(accepted)
    }

    /** [afterSeq] 이후의 변경을 순번 순으로. 0을 넘기면 여행 전체 스냅샷이다. */
    fun pull(tripId: String, afterSeq: Long, limit: Int = DEFAULT_PULL_LIMIT): PullResult {
        require(limit > 0) { "limit must be positive: $limit" }

        val changes = jdbc.sql(
            """
            SELECT entity_type, entity_id, field, value, hlc, seq
              FROM lww_fields
             WHERE trip_id = :tripId AND seq > :afterSeq
             ORDER BY seq
             LIMIT :limit
            """,
        )
            .param("tripId", tripId)
            .param("afterSeq", afterSeq)
            .param("limit", limit)
            .query(::toSyncedField)
            .list()

        return PullResult(
            changes = changes,
            nextCursor = changes.lastOrNull()?.seq ?: afterSeq,
            hasMore = changes.size == limit,
        )
    }

    /** 순번 [count]개를 예약하고, 예약 구간 바로 앞 번호를 돌려준다. 발급된 번호는 base+1 .. base+count. */
    private fun reserveSeq(tripId: String, count: Int): Long =
        jdbc.sql(
            """
            INSERT INTO trips (id, last_seq) VALUES (:tripId, :count)
            ON CONFLICT (id) DO UPDATE SET last_seq = trips.last_seq + EXCLUDED.last_seq
            RETURNING last_seq - :count AS base_seq
            """,
        )
            .param("tripId", tripId)
            .param("count", count)
            .query(Long::class.java)
            .single()

    /** LWW에서 이기면 저장된 행을, 지면(서버 HLC가 같거나 더 새것) null을 돌려준다. */
    private fun upsert(tripId: String, change: FieldChange, seq: Long): SyncedField? =
        jdbc.sql(
            """
            INSERT INTO lww_fields (trip_id, entity_type, entity_id, field, value, hlc, seq)
            VALUES (:tripId, :entityType, :entityId, :field, :value, :hlc, :seq)
            ON CONFLICT (trip_id, entity_type, entity_id, field) DO UPDATE
               SET value = EXCLUDED.value, hlc = EXCLUDED.hlc, seq = EXCLUDED.seq, updated_at = now()
             WHERE lww_fields.hlc < EXCLUDED.hlc
            RETURNING entity_type, entity_id, field, value, hlc, seq
            """,
        )
            .param("tripId", tripId)
            .param("entityType", change.entityType)
            .param("entityId", change.entityId)
            .param("field", change.field)
            .param("value", change.value)
            .param("hlc", change.hlc)
            .param("seq", seq)
            .query(::toSyncedField)
            .optional()
            .orElse(null)

    private fun toSyncedField(rs: ResultSet, @Suppress("UNUSED_PARAMETER") rowNum: Int) = SyncedField(
        entityType = rs.getString("entity_type"),
        entityId = rs.getString("entity_id"),
        field = rs.getString("field"),
        value = rs.getString("value"),
        hlc = rs.getString("hlc"),
        seq = rs.getLong("seq"),
    )

    companion object {
        const val DEFAULT_PULL_LIMIT = 500
    }
}
