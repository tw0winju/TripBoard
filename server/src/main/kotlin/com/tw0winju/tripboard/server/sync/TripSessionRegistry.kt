package com.tw0winju.tripboard.server.sync

import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import java.util.concurrent.ConcurrentHashMap

/** 어떤 세션이 어떤 여행을 보고 있는지. 브로드캐스트 대상 찾기용. 단일 서버 인스턴스 전제. */
@Component
class TripSessionRegistry {

    private val sessionsByTrip = ConcurrentHashMap<String, MutableSet<WebSocketSession>>()

    fun subscribe(tripId: String, session: WebSocketSession) {
        sessionsByTrip.computeIfAbsent(tripId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun unsubscribeAll(session: WebSocketSession) {
        sessionsByTrip.values.forEach { it.remove(session) }
        sessionsByTrip.entries.removeIf { it.value.isEmpty() }
    }

    fun subscribers(tripId: String): Set<WebSocketSession> =
        sessionsByTrip[tripId]?.toSet() ?: emptySet()
}
