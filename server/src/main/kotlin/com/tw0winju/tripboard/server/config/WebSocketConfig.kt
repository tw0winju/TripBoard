package com.tw0winju.tripboard.server.config

import com.tw0winju.tripboard.server.sync.SyncWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

/**
 * 순수 WebSocket으로 시작. STOMP(메시지 브로커)를 쓸지는 나중에 결정.
 *  - 순수 WS: 프로토콜을 직접 설계 → 동기화 메시지 포맷을 완전히 통제
 *  - STOMP: 구독/브로드캐스트가 공짜, 대신 클라이언트(OkHttp)에 STOMP 프레이밍이 필요
 */
@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val syncWebSocketHandler: SyncWebSocketHandler,
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(syncWebSocketHandler, "/ws/sync")
            .setAllowedOriginPatterns("*") // TODO: 인증 붙일 때 같이 정리
    }
}
