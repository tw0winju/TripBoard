# 아키텍처 메모

## 1. 저장소 구조: 모노레포 + 독립 빌드 2개

| 선택지 | 장점 | 단점 |
|---|---|---|
| **모노레포, 빌드 분리 (현재)** | 한 저장소에서 프로토콜 변경을 앱·서버 함께 커밋. 플러그인(AGP vs Spring) 충돌 없음 | 공통 코드 공유는 별도 작업 필요 |
| 모노레포, 단일 Gradle 빌드 | `core:sync`를 서버에서 바로 의존 가능 | AGP·Spring Boot 플러그인이 한 빌드에 섞여 설정이 복잡 |
| 저장소 분리 | 배포 단위가 명확 | 사이드 프로젝트에선 관리 부담만 큼 |

> 나중에 `core:sync`를 서버와 공유하고 싶어지면: composite build(`includeBuild`)로 붙이거나, KMP 모듈로 전환.

## 2. Android 모듈 그래프

```
app
 ├─ feature:trips ─┬─ core:data ─┬─ core:database (Room)
 │                 │             ├─ core:network (OkHttp WebSocket)
 │                 │             ├─ core:sync    (순수 Kotlin)
 │                 │             └─ core:model   (순수 Kotlin, api)
 │                 └─ core:designsystem (Compose 테마)
 └─ core:data, core:designsystem
```

규칙
- `feature`끼리는 서로 의존하지 않는다. 화면 이동은 `app`의 NavHost가 연결.
- `feature`는 `core:database`/`core:network`를 직접 모른다. `core:data`의 Repository 인터페이스만 본다.
- `core:model`, `core:sync`는 Android 의존성 0 → 단위 테스트가 빠르고, KMP 전환 시 그대로 `commonMain`으로.

### build-logic (컨벤션 플러그인)
모듈마다 반복되는 `compileSdk`, `minSdk`, Compose/Hilt 설정을 `tripboard.android.library` 같은 플러그인 하나로 묶었다.
예전 방식(`allprojects {}`/`subprojects {}` 또는 `apply from: 'common.gradle'`)과 비교하면:
- 모듈이 **필요한 것만 골라서** 적용 (Compose 안 쓰는 모듈은 Compose 플러그인을 안 붙임)
- 타입 안전한 Kotlin 코드라 IDE 지원·리팩터링이 됨
- Now in Android(구글 공식 샘플)와 같은 구조라 면접에서 설명하기 쉬움

## 3. 예전 방식 ↔ 지금 방식 대응표

| 예전 | 지금 (이 프로젝트) |
|---|---|
| XML 레이아웃 + findViewById/DataBinding | Jetpack Compose |
| MVP (Presenter ↔ View 인터페이스) | MVVM/UDF: ViewModel이 `StateFlow<UiState>` 하나를 노출 |
| AsyncTask / RxJava | Coroutines + Flow (`viewModelScope`, `stateIn`) |
| Dagger 2 수동 Component | Hilt (`@HiltViewModel`, `@AndroidEntryPoint`) |
| SQLiteOpenHelper / Room + LiveData | Room + Flow |
| Service / JobScheduler / AlarmManager | WorkManager (`CoroutineWorker`, `@HiltWorker`) |
| Fragment + Navigation XML | Navigation Compose (타입 세이프 `@Serializable` 라우트) |
| Groovy `build.gradle` + `ext {}` 버전 | Kotlin DSL + Version Catalog (`libs.versions.toml`) |

## 4. 아직 정하지 않은 것 (직접 결정할 것)

- **서버 영속성 계층**: JDBC(현재) / JPA / jOOQ / Exposed
  - 동기화 서버는 "변경분을 HLC 순으로 upsert"가 핵심이라 SQL을 직접 통제하는 쪽(JDBC·jOOQ)이 유리할 수 있음. JPA는 dirty checking·영속성 컨텍스트가 오히려 방해될 수 있음 → 면접 질문 거리.
- **WebSocket 프로토콜**: 순수 WS(현재) vs STOMP
- **로컬 변경 추적**: 엔티티에 필드별 HLC 컬럼 vs outbox(op log) 테이블
- **서버 역할**: 병합의 권위자 vs 저장·중계만 (CRDT라면 둘 다 수렴해야 함)

## 5. 버전

`android/gradle/libs.versions.toml`, `server/build.gradle.kts`에서 관리.
Gradle 8.14.3 / AGP 8.10.1 / Kotlin 2.1.21 / Compose BOM 2025.05.01 / Hilt 2.56.2 / Room 2.7.1 / Spring Boot 3.5.0.
안정적으로 호환이 확인된 조합으로 시작했으니, 첫 빌드가 된 뒤 Android Studio의 업데이트 제안에 따라 올리면 된다.

## 6. 결정 기록

| 날짜 | 결정 | 고려한 대안 | 이유 |
|---|---|---|---|
| 2026-11-06 | **KMP는 우선순위에서 제외** | KMP 확장을 전제로 설계 | Android 이직 준비가 목표. Android 전용 API를 자유롭게 쓴다 |
| 2026-11-06 | 로컬 DB: **Room** 유지 | SQLDelight | 한국 안드로이드 공고에서 요구 빈도가 높음. 동기화용 SQL은 `@Query`로 충분 |
| 2026-11-06 | 네트워크: **OkHttp** 유지 | Ktor Client | 채용 시장 표준. WebSocket 콜백을 `callbackFlow`로 감싸는 경험 자체가 학습 목표 |

`core:sync`는 KMP와 무관하게 순수 Kotlin으로 유지한다. 에뮬레이터 없이 JVM 단위 테스트로 빠르게 검증할 수 있기 때문.

### 아직 열려 있는 결정
- 화면 구조: 지금의 UDF(MVVM) vs MVI
- 로컬 변경 추적: 필드별 HLC 컬럼 vs outbox(op log) 테이블 vs 둘 다
- 4절의 서버 영속성 계층, WebSocket 프로토콜
