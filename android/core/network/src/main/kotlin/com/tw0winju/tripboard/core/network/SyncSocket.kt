package com.tw0winju.tripboard.core.network

/**
 * TODO: OkHttp WebSocket을 Flow로 감싸기 (callbackFlow + awaitClose).
 *  - 연결 상태(Connecting/Connected/Disconnected)를 StateFlow로 노출할지
 *  - 재연결 백오프는 누가 책임질지 (이 클래스 vs 상위 SyncManager)
 *  - 예전 방식(RxJava Observable.create)과 비교해 보기
 */
interface SyncSocket
