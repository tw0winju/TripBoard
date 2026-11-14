package com.tw0winju.tripboard.server.sync

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * /ws/sync 위로 오가는 JSON. "type" 필드로 구분한다.
 *
 * 앱 → 서버
 *   {"type":"pull","tripId":"t1","afterSeq":0}
 *   {"type":"push","tripId":"t1","changes":[{"entityType":"trip_item","entityId":"i1","field":"memo","value":"...","hlc":"..."}]}
 * 서버 → 앱
 *   {"type":"changes","tripId":"t1","changes":[...SyncedField],"nextCursor":12,"hasMore":false}
 *       pull의 응답이기도 하고, 다른 기기가 push했을 때의 실시간 알림이기도 하다. 앱은 한 코드 경로로 처리하면 된다.
 *   {"type":"pushed","tripId":"t1","accepted":[...SyncedField]}
 *       push한 기기에게만. accepted에 없는 변경은 서버 값이 이긴 것이니 outbox에서 지우고 다음 pull로 받는다.
 *   {"type":"error","message":"..."}
 *
 * 구독: pull을 한 번 보낸 여행은 연결이 끊길 때까지 실시간 알림을 받는다. (따라잡기 → 실시간, 순서가 자연스럽다)
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(ClientMessage.Pull::class, name = "pull"),
    JsonSubTypes.Type(ClientMessage.Push::class, name = "push"),
)
sealed interface ClientMessage {
    val tripId: String

    data class Pull(
        override val tripId: String,
        val afterSeq: Long = 0,
        val limit: Int = SyncRepository.DEFAULT_PULL_LIMIT,
    ) : ClientMessage

    data class Push(
        override val tripId: String,
        val changes: List<FieldChange>,
    ) : ClientMessage
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(ServerMessage.Changes::class, name = "changes"),
    JsonSubTypes.Type(ServerMessage.Pushed::class, name = "pushed"),
    JsonSubTypes.Type(ServerMessage.Error::class, name = "error"),
)
sealed interface ServerMessage {
    data class Changes(
        val tripId: String,
        val changes: List<SyncedField>,
        val nextCursor: Long,
        val hasMore: Boolean,
    ) : ServerMessage

    data class Pushed(val tripId: String, val accepted: List<SyncedField>) : ServerMessage

    data class Error(val message: String) : ServerMessage
}
