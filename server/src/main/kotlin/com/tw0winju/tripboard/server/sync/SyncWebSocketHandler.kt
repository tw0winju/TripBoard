package com.tw0winju.tripboard.server.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

/**
 * 서버 역할: 병합 권위자가 아니라 "LWW 레지스터 저장소 + 중계". 판정은 SyncRepository의 SQL이 한다.
 * 이 클래스는 JSON ↔ 메시지 변환, 세션 관리, 브로드캐스트만 맡는다.
 */
@Component
class SyncWebSocketHandler(
    private val repository: SyncRepository,
    private val registry: TripSessionRegistry,
    private val objectMapper: ObjectMapper,
) : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 원본 세션 id → 전송 직렬화 데코레이터. WebSocketSession.sendMessage는 동시 호출이 안 되는데,
     * 브로드캐스트는 다른 세션의 스레드에서 들어오므로 반드시 이걸 거쳐 보낸다.
     */
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session.id] = ConcurrentWebSocketSessionDecorator(session, SEND_TIMEOUT_MS, SEND_BUFFER_BYTES)
        log.info("connected: {}", session.id)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val safe = sessions[session.id] ?: return
        try {
            when (val msg = objectMapper.readValue<ClientMessage>(message.payload)) {
                is ClientMessage.Pull -> handlePull(safe, msg)
                is ClientMessage.Push -> handlePush(safe, msg)
            }
        } catch (e: Exception) {
            log.warn("bad message from {}: {}", session.id, e.toString())
            send(safe, ServerMessage.Error(e.toUserMessage()))
        }
    }

    private fun handlePull(session: WebSocketSession, msg: ClientMessage.Pull) {
        // 따라잡기를 시작한 여행은 이후 실시간 알림도 받는다
        registry.subscribe(msg.tripId, session)
        val result = repository.pull(msg.tripId, msg.afterSeq, msg.limit)
        send(session, ServerMessage.Changes(msg.tripId, result.changes, result.nextCursor, result.hasMore))
    }

    private fun handlePush(session: WebSocketSession, msg: ClientMessage.Push) {
        rejectFutureHlc(msg.changes)

        val accepted = repository.push(msg.tripId, msg.changes).accepted
        send(session, ServerMessage.Pushed(msg.tripId, accepted))

        if (accepted.isEmpty()) return
        // 여기는 @Transactional 바깥 = 커밋 이후. 커밋 안 된 값을 퍼뜨리는 일은 없다.
        val broadcast = ServerMessage.Changes(msg.tripId, accepted, nextCursor = accepted.last().seq, hasMore = false)
        registry.subscribers(msg.tripId)
            .filter { it.id != session.id }
            .forEach { send(it, broadcast) }
    }

    /**
     * 시계가 틀린 기기 하나가 그 필드의 편집을 영원히 이기는 걸 막는다.
     * 서버 시각보다 [MAX_CLOCK_DRIFT_MS] 넘게 앞선 HLC는 거부. 앱은 HLC를 다시 찍어 보내면 된다.
     */
    private fun rejectFutureHlc(changes: List<FieldChange>) {
        val limit = System.currentTimeMillis() + MAX_CLOCK_DRIFT_MS
        val tooFar = changes.firstOrNull { it.hlc.wallTimeOrNull()?.let { t -> t > limit } == true } ?: return
        throw IllegalArgumentException(
            "hlc too far in the future: ${tooFar.hlc} (${tooFar.entityType}/${tooFar.entityId}/${tooFar.field})",
        )
    }

    /** Hlc.encode()의 앞 15자리. 형식이 틀리면 null → DB 제약(hlc 도메인)이 걸러낸다. */
    private fun String.wallTimeOrNull(): Long? = substringBefore(':').takeIf { it.length == 15 }?.toLongOrNull()

    private fun send(session: WebSocketSession, msg: ServerMessage) {
        if (!session.isOpen) return
        try {
            session.sendMessage(TextMessage(objectMapper.writeValueAsString(msg)))
        } catch (e: Exception) {
            log.warn("send failed to {}: {}", session.id, e.toString())
        }
    }

    private fun Exception.toUserMessage(): String = when (this) {
        is DataIntegrityViolationException -> "rejected: unknown field, malformed hlc, or invalid value"
        is IllegalArgumentException -> message ?: "invalid request"
        else -> "invalid message: ${javaClass.simpleName}"
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session.id)?.let { registry.unsubscribeAll(it) }
        log.info("closed: {} ({})", session.id, status)
    }

    companion object {
        const val MAX_CLOCK_DRIFT_MS = 60_000L
        private const val SEND_TIMEOUT_MS = 5_000
        private const val SEND_BUFFER_BYTES = 512 * 1024
    }
}
