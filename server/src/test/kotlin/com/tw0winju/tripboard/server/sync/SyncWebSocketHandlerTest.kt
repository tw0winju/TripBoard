package com.tw0winju.tripboard.server.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URI
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 진짜 WebSocket 클라이언트 두 개를 붙여서 push → 브로드캐스트까지 끝까지 확인한다. Colima 설정은 SyncRepositoryTest 참고. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SyncWebSocketHandlerTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var objectMapper: ObjectMapper

    private val tripId = UUID.randomUUID().toString()
    private val clients = mutableListOf<Client>()

    /** 받은 메시지를 큐에 쌓아 두는 테스트용 클라이언트 */
    inner class Client {
        private val inbox = LinkedBlockingQueue<String>()
        val session: WebSocketSession = StandardWebSocketClient()
            .execute(
                object : TextWebSocketHandler() {
                    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
                        inbox.add(message.payload)
                    }
                },
                null,
                URI("ws://localhost:$port/ws/sync"),
            )
            .get(5, TimeUnit.SECONDS)

        fun send(json: String) = session.sendMessage(TextMessage(json))
        fun send(msg: ClientMessage) = send(objectMapper.writeValueAsString(msg))

        fun receive(): ServerMessage =
            objectMapper.readValue(checkNotNull(inbox.poll(5, TimeUnit.SECONDS)) { "no message within 5s" })

        fun receiveNothing(): String? = inbox.poll(500, TimeUnit.MILLISECONDS)
    }

    private fun client() = Client().also { clients += it }

    @AfterTest
    fun closeAll() = clients.forEach { it.session.close() }

    private fun hlc(wallTime: Long = System.currentTimeMillis(), node: String = "A") =
        "${wallTime.toString().padStart(15, '0')}:00000:$node"

    @Test
    fun `pull한 세션은 다른 기기의 push를 실시간으로 받고, push한 세션은 ack만 받는다`() {
        val watcher = client()
        watcher.send(ClientMessage.Pull(tripId))
        val snapshot = assertIs<ServerMessage.Changes>(watcher.receive())
        assertTrue(snapshot.changes.isEmpty())
        assertEquals(0, snapshot.nextCursor)

        val editor = client()
        val change = FieldChange("trip_item", "i1", "memo", "9시 출발", hlc())
        editor.send(ClientMessage.Push(tripId, listOf(change)))

        val ack = assertIs<ServerMessage.Pushed>(editor.receive())
        assertEquals(listOf("9시 출발"), ack.accepted.map { it.value })

        val live = assertIs<ServerMessage.Changes>(watcher.receive())
        assertEquals(tripId, live.tripId)
        assertEquals(listOf("9시 출발"), live.changes.map { it.value })
        assertEquals(ack.accepted.single().seq, live.nextCursor)

        assertNull(editor.receiveNothing(), "push한 세션은 자기 변경을 브로드캐스트로 다시 받지 않는다")
    }

    @Test
    fun `LWW에서 진 push는 ack가 비어 있고 브로드캐스트도 없다`() {
        val watcher = client()
        watcher.send(ClientMessage.Pull(tripId))
        watcher.receive()

        val editor = client()
        val now = System.currentTimeMillis()
        editor.send(ClientMessage.Push(tripId, listOf(FieldChange("trip_item", "i1", "memo", "새것", hlc(now)))))
        editor.receive(); watcher.receive()

        editor.send(ClientMessage.Push(tripId, listOf(FieldChange("trip_item", "i1", "memo", "오래된 것", hlc(now - 1_000)))))
        val ack = assertIs<ServerMessage.Pushed>(editor.receive())
        assertTrue(ack.accepted.isEmpty())
        assertNull(watcher.receiveNothing())
    }

    @Test
    fun `다른 여행의 push는 받지 않는다`() {
        val watcher = client()
        watcher.send(ClientMessage.Pull(tripId))
        watcher.receive()

        val editor = client()
        editor.send(ClientMessage.Push(UUID.randomUUID().toString(), listOf(FieldChange("trip_item", "i1", "memo", "x", hlc()))))
        editor.receive()
        assertNull(watcher.receiveNothing())
    }

    @Test
    fun `서버 시각보다 1분 넘게 앞선 HLC는 거부된다`() {
        val editor = client()
        val future = System.currentTimeMillis() + SyncWebSocketHandler.MAX_CLOCK_DRIFT_MS + 10_000
        editor.send(ClientMessage.Push(tripId, listOf(FieldChange("trip_item", "i1", "memo", "x", hlc(future)))))
        val error = assertIs<ServerMessage.Error>(editor.receive())
        assertTrue("future" in error.message, error.message)
    }

    @Test
    fun `모르는 필드와 깨진 JSON은 error로 답하고 연결은 유지된다`() {
        val editor = client()
        editor.send(ClientMessage.Push(tripId, listOf(FieldChange("trip_item", "i1", "color", "red", hlc()))))
        assertIs<ServerMessage.Error>(editor.receive())

        editor.send("""{"type":"dance"}""")
        assertIs<ServerMessage.Error>(editor.receive())

        editor.send(ClientMessage.Pull(tripId))
        assertIs<ServerMessage.Changes>(editor.receive())
    }
}
