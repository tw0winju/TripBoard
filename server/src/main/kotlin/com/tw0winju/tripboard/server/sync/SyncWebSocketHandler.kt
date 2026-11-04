package com.tw0winju.tripboard.server.sync

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * TODO
 *  - 메시지 포맷 정의 (push: 변경 목록 / pull: 커서 이후 변경 / ack)
 *  - 같은 여행(trip)을 보고 있는 세션들에게 브로드캐스트 (세션 레지스트리)
 *  - 서버가 병합의 권위자(authority)인지, 단순 중계 + 저장소인지 결정
 */
@Component
class SyncWebSocketHandler : TextWebSocketHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun afterConnectionEstablished(session: WebSocketSession) {
        log.info("connected: {}", session.id)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // 일단 에코. 연결 확인용
        session.sendMessage(TextMessage("echo: ${message.payload}"))
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        log.info("closed: {} ({})", session.id, status)
    }
}
