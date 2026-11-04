# 트립보드 (TripBoard)

여럿이 함께 편집하고 오프라인에서도 쓸 수 있는 여행 준비 앱.

```
TripBoard/
├── android/            # Kotlin · Jetpack Compose · Hilt · Room · WorkManager (멀티 모듈)
├── server/             # Spring Boot (Kotlin) · PostgreSQL · WebSocket
├── docs/               # 설계 문서
└── docker-compose.yml  # 로컬 PostgreSQL
```

`android/`와 `server/`는 **독립된 Gradle 빌드**입니다. 각각 따로 엽니다.

## 실행

### 서버
```bash
cd server
./gradlew bootRun          # docker-compose의 Postgres가 자동으로 뜸 (Docker 필요)
```
- 헬스체크: http://localhost:8080/actuator/health
- WebSocket: `ws://localhost:8080/ws/sync` (지금은 에코)

### 앱
Android Studio → **Open** → `TripBoard/android` 폴더 선택 → Gradle Sync → `app` 실행.
목록 화면의 + 버튼을 누르면 Room에 여행이 저장되고 목록이 Flow로 갱신됩니다.

## 요구 사항
- Android Studio (AGP 8.10 지원 버전 이상), JDK 17+ (Studio 내장 JBR 사용 가능)
- 서버: JDK 21 (없으면 Gradle toolchain이 자동 다운로드), Docker

## 다음 단계
1. `android/core/sync`의 `TODO("직접 구현")`을 테스트부터 채우기 (HLC → LWW → OR-Set → 분수 인덱스)
2. 서버 `V1__init.sql` 스키마 설계
3. `SyncSocket`(OkHttp WebSocket → Flow) + `SyncWorker` 연결

자세한 구조와 설계 선택지는 [docs/architecture.md](docs/architecture.md).
