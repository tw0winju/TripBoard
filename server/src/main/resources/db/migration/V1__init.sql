-- V1: 초기 스키마
--
-- 서버 역할은 "필드 레지스터 저장소 + 중계"다.
-- 앱의 RemoteChange(entityType, entityId, field, value, hlc) 한 건이 lww_fields의 한 행에 대응한다.
--
-- 1. 필드 단위 LWW → (엔티티, 필드)당 한 행인 레지스터 테이블
--    - 필드마다 컬럼 쌍(title, title_hlc, ...)을 두면 필드마다 upsert 문이 달라지고,
--      행 단위 seq로는 pull에서 "바뀐 필드만" 보낼 수 없다.
--    - 모든 변경을 쌓는 op log는 LWW에서 진 값까지 영원히 남는다.
--    → 필드마다 최신 값 하나만 남기는 "압축된 op log". cursor=0으로 pull하면 그게 곧 스냅샷이다.
--
-- 2. pull 커서 → 여행(trip)별 서버 순번 (trips.last_seq)
--    - 전역 bigserial은 번호 발급 순서와 커밋 순서가 다르다.
--      11번이 먼저 커밋돼 커서가 11로 넘어간 뒤 10번이 커밋되면, 10번은 영영 못 받는다.
--    - trips 행을 UPDATE해서 순번을 받으면 그 행 잠금이 커밋까지 유지된다.
--      그래서 같은 여행 안에서는 발급 순서와 커밋 순서가 같다. (여행 단위 직렬화. 동시 편집자가 몇 명뿐이라 충분)
--
-- 3. 삭제 → 'deleted' 필드도 LWW 레지스터. 툼스톤 행은 영구 보관
--    - 툼스톤을 지우면 커서가 오래된 기기는 삭제 소식을 못 받는다.
--      늦게 도착한 편집이 항목을 되살리기도 한다(좀비).
--    - 엔티티당 한 행이라 비용이 작다. 정리가 필요해지면 삭제된 엔티티의 "나머지 필드"만 지운다.
--
-- 4. 분수 인덱스와 HLC → COLLATE "C"
--    - 기본 콜레이션(en_US.utf8)은 언어 규칙으로 비교한다. 예: 'B' < 'a'가 거짓.
--      Kotlin String.compareTo, SQLite BINARY(=바이트 순)와 결과가 다르면 서버와 앱이 서로 다른 값을 고른다.

-- Hlc.encode() 형식: 15자리 wallTime : 5자리 counter : nodeId
-- 자리수를 고정해야 문자열 순서와 시간 순서가 같다. counter가 99999를 넘는 버그도 여기서 막힌다.
CREATE DOMAIN hlc AS text COLLATE "C"
    CHECK (VALUE ~ '^[0-9]{15}:[0-9]{5}:.+$');

-- 동기화 범위(scope) 단위. 여행의 제목 같은 필드 값은 lww_fields에 있다.
CREATE TABLE trips (
    id         text        PRIMARY KEY,
    last_seq   bigint      NOT NULL DEFAULT 0,   -- 이 여행에서 마지막으로 발급한 순번 = pull 커서의 상한
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE lww_fields (
    trip_id     text        NOT NULL REFERENCES trips (id),
    entity_type text        NOT NULL,
    entity_id   text        NOT NULL,
    field       text        NOT NULL,
    value       text        COLLATE "C" NOT NULL,   -- position(분수 인덱스)도 여기 들어온다
    hlc         hlc         NOT NULL,
    seq         bigint      NOT NULL,               -- 이 값이 받아들여질 때 발급한 순번. 갱신되면 새 번호로 바뀐다
    updated_at  timestamptz NOT NULL DEFAULT now(), -- 서버가 받은 시각. 툼스톤 정리 판단용 (기기 시계는 믿지 않는다)

    -- trip_id를 키에 넣어 여행마다 네임스페이스를 나눈다.
    -- 다른 여행에서 같은 entity_id로 먼저 써서 자리를 차지하는 일을 막는다.
    PRIMARY KEY (trip_id, entity_type, entity_id, field),

    -- pull: WHERE trip_id = ? AND seq > ? ORDER BY seq
    CONSTRAINT lww_fields_trip_seq_key UNIQUE (trip_id, seq),

    -- EAV라 테이블만 봐서는 어떤 필드가 있는지 안 보인다. 허용 목록을 여기 적어 둔다.
    -- 필드를 추가할 때는 이 제약을 바꾸는 마이그레이션을 쓴다.
    CONSTRAINT lww_fields_known_field CHECK (
        (entity_type = 'trip'      AND field IN ('title', 'deleted')) OR
        (entity_type = 'trip_item' AND field IN ('title', 'memo', 'position', 'deleted'))
    ),
    CONSTRAINT lww_fields_deleted_bool CHECK (field <> 'deleted' OR value IN ('true', 'false')),

    -- 여행 자신의 필드는 자기 스트림에만 들어간다
    CONSTRAINT lww_fields_trip_self CHECK (entity_type <> 'trip' OR entity_id = trip_id)
);
