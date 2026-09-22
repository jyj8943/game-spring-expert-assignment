# 게임 서버 단기 심화 트랙 9기 - 숙련 주차 프로젝트: WebCraft

<img width="1263" height="905" alt="image" src="https://github.com/user-attachments/assets/e8e11d77-85fd-4e6c-8619-a44d58ee5ce9" />


팀스파르타 내일배움캠프 게임 서버 단기 심화 트랙 9기에서 진행한 숙련 주차 프로젝트입니다. Spring Boot와 JPA를 기반으로 WebSocket 실시간 통신과 Redis를 활용한 게임 서버 기능을 학습했습니다.

제공된 게임 클라이언트와 서버 뼈대를 바탕으로 Lv 1~19 과제를 진행했습니다. 플레이어 등록과 월드 생성부터 실시간 이동·채팅, 접속 상태 관리, 낙관적 락, 커서 페이지 조회, 최근 채팅 캐시, Lua Script 기반 채팅 횟수 제한까지 단계적으로 구현했습니다.

<br/>

## 프로젝트 배경

- **교육 과정:** 팀스파르타 내일배움캠프
- **프로젝트 구분:** 숙련 주차 프로젝트
- **개발 기간:** 2026.09.16 ~ 2026.09.22
- **진행 범위:** Lv 1 ~ 19
- **학습 목표:** WebSocket 기반 실시간 통신과 Redis 활용, 동시성 문제 이해
- **제공 구성:** 게임 클라이언트, 게임 엔진, 서버 뼈대, API 명세, 과제 가이드 및 테스트 코드
- **구현 범위:** 제공된 코드를 바탕으로 단계별 서버 기능 완성
- **미구현 범위:** Lv 20의 Redis Pub/Sub 기반 멀티 서버 채팅

<br/>

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 4.1.0, Spring MVC |
| 데이터 접근 | Spring Data JPA, Hibernate |
| 데이터베이스 | Docker - MySQL |
| 실시간 통신 | Spring WebSocket |
| 접속 상태·캐시·요청 제한 | Redis, Spring Data Redis, Lua Script |
| 요청 검증 | Jakarta Bean Validation |
| 개발 도구 | Gradle Wrapper, Lombok, IntelliJ IDEA |
| 테스트 | JUnit 5, Mockito, H2, Testcontainers |
| 동작 확인 | Postman, Redis CLI, 브라우저 |
| 제공 게임 엔진 | webcraft-engine 2.1.5 |

<br/>

## 주요 기능

- 닉네임 검증 및 중복 등록 방지
- 기본 월드 최대 3개 생성 제한
- 채팅 저장과 최근 채팅 조회 API
- WebSocket 연결 시 플레이어와 월드 식별
- 월드별 세션 관리와 중복 연결 등록 방지
- Redis를 이용한 접속 상태 저장과 갱신
- Ping/Pong을 통한 연결 확인
- 플레이어 이동 요청을 게임 엔진에 전달
- 같은 월드의 참여자에게 채팅 전송
- 현재 월드의 접속자 목록과 인원수 조회
- JPA 낙관적 락을 통한 이전 상태의 덮어쓰기 방지
- 생성 시각과 ID를 사용하는 커서 페이지 조회
- 최근 채팅 조회 결과를 5초 동안 캐시
- Lua Script를 이용한 플레이어별 채팅 횟수 제한

<br/>

## 구조와 설계

아래는 이번 과제에서 다룬 주요 패키지입니다.

```text
com.gameexpert
├── common              # 공통 예외와 처리
├── config              # WebSocket 등 설정
├── player
│   ├── controller      # 플레이어 등록 요청
│   ├── service         # 닉네임 중복 검사와 저장
│   ├── repository      # 플레이어 데이터 접근
│   ├── entity          # Player
│   └── dto             # 플레이어 등록 요청
├── world
│   ├── controller      # 월드 관련 HTTP 요청
│   ├── service         # 월드 생성과 조회
│   ├── repository      # 월드 데이터 접근
│   ├── entity          # World
│   └── dto             # 월드 요청·응답
├── chat
│   ├── controller      # 최근 채팅·과거 채팅 조회
│   ├── service         # 저장·조회·캐시·횟수 제한
│   ├── repository      # 채팅 조회와 저장
│   ├── entity          # ChatMessage
│   ├── dto             # 채팅 조회 응답
│   └── event           # 채팅 저장 이벤트와 캐시 무효화
├── presence            # Redis 접속 상태 관리
├── trial               # 시련 데이터와 낙관적 락
└── ws
    ├── handler         # 메시지 종류별 처리
    └── dto             # WebSocket 응답
```

HTTP 요청은 `Controller → Service → Repository → DB` 순서로 처리합니다. 요청 검증과 HTTP 응답은 컨트롤러에서 연결하고, 서비스는 처리 흐름과 트랜잭션을 담당합니다.

WebSocket 요청은 연결 시 닉네임과 월드를 확인한 뒤, 메시지의 `type`에 맞는 핸들러로 전달합니다.

```text
WebSocket 연결 요청
→ NicknameHandshakeInterceptor
→ 연결에 사용할 닉네임·월드 정보 저장
→ 월드별 세션 등록

WebSocket 메시지 수신
→ MessageRouter
→ type에 해당하는 핸들러
→ 서비스 또는 게임 엔진 호출
→ 특정 연결 또는 같은 월드의 참여자에게 응답
```

MySQL은 플레이어·월드·채팅 등의 저장 데이터를 관리하고, Redis는 접속 상태·최근 채팅 캐시·채팅 횟수 제한에 사용합니다.

<br/>

## Lv 1 ~ 19 학습 기록

### Lv 1. Docker로 MySQL과 Redis 설정

Docker로 MySQL과 Redis를 실행하고, `application.properties`에 연결 정보를 설정했습니다.

MySQL에는 데이터베이스 주소와 계정 정보를, Redis에는 호스트와 포트를 지정했습니다. JPA 스키마 설정은 과제 조건에 맞게 `ddl-auto=update`를 사용했습니다.

MySQL과 Redis가 서로 다른 역할을 담당하며, 애플리케이션에서 각각 연결 설정이 필요하다는 점을 학습했습니다.

### Lv 2. SQL을 JPA 인덱스로 표현하기

월드별 최근 채팅 조회에 사용할 복합 인덱스를 `ChatMessage`에 선언했습니다.

```java
@Table(
    name = "chat_messages",
    indexes = {
        @Index(
            name = "idx_chat_world_created_at",
            columnList = "world_id, created_at"
        )
    }
)
```

제공된 SQL의 인덱스 이름과 컬럼 순서를 JPA 매핑에 반영했습니다. 인덱스를 선언할 때 Java 필드명이 아닌 실제 DB 컬럼명을 사용한다는 점을 확인했습니다.

### Lv 3. 요청 검증과 DTO: 플레이어 등록

닉네임은 비어 있지 않은 2~12글자로 제한하고, 영문 대소문자·숫자·밑줄만 사용할 수 있도록 검증했습니다.

```java
@NotBlank
@Size(min = 2, max = 12)
@Pattern(regexp = "^[a-zA-Z0-9_]+$")
private final String nickname;
```

| 어노테이션 | 검증 내용 |
| --- | --- |
| `@NotBlank` | null, 빈 문자열, 공백만 있는 문자열 금지 |
| `@Size` | 닉네임 길이를 2~12자로 제한 |
| `@Pattern` | 허용할 문자 종류 제한 |
| `@Valid` | Controller에서 DTO 검증 실행 |

Controller에는 `@PostMapping("/players")`, `@RequestBody`, `@Valid`를 연결했습니다. 등록에 성공하면 본문 없는 `201 Created`를 반환합니다.

서비스에서는 `existsByNickname()`으로 중복 여부를 검사합니다. 이미 존재하면 `DUPLICATE_NICKNAME` 예외를 발생시키고, 중복이 아니면 제공된 `savePlayer()`를 호출합니다.

동시 등록 시 DB의 유일성 제약 위반을 처리하는 코드는 제공된 저장 메서드를 사용했습니다.

### Lv 4. 월드 생성

기본 월드는 최대 3개까지 생성할 수 있도록 구현했습니다.

```java
return worldOperations.duringCreation(() -> {
    if (worldRepository.countRootWorlds() >= MAX_WORLDS) {
        throw new ConflictException("WORLD_LIMIT_REACHED");
    }

    return createPreparedWorld(request);
});
```

개수 확인과 생성 작업을 제공된 `duringCreation()` 내부에 함께 배치했습니다.

화면에서 생성 버튼을 제한하는 것과 별개로 서버에서도 생성 가능 여부를 검사해야 한다는 점을 학습했습니다.

### Lv 5. 채팅 저장과 내역 조회

`saveMessage()`에서 월드를 조회하고, 없으면 `WORLD_NOT_FOUND` 예외를 발생시키도록 작성했습니다.

월드가 존재하면 닉네임과 내용으로 `ChatMessage`를 생성하고, 저장 결과를 제공된 `savedResponse()`에 전달했습니다.

최근 채팅은 생성 시각 내림차순, 같은 시각에서는 ID 내림차순으로 조회합니다.

```java
findByWorldIdOrderByCreatedAtDescIdDesc(
    worldId,
    PageRequest.of(0, capped)
)
```

최신 채팅을 제한 개수만큼 가져온 뒤, 응답에서는 오래된 채팅부터 읽을 수 있도록 목록을 뒤집고 DTO로 변환했습니다.

### Lv 6. 최근 채팅 조회 API 구현

`GET /worlds/{worldId}/chats` 요청을 처리하도록 Controller를 구현했습니다.

```java
@GetMapping("/worlds/{worldId}/chats")
public ResponseEntity<List<ChatMessageResponse>> chats(
        @PathVariable Long worldId,
        @RequestParam(defaultValue = "50") int limit
) {
    return ResponseEntity.ok(
            chatService.getRecentMessages(worldId, limit)
    );
}
```

`@PathVariable`로 월드 ID를 받고, `@RequestParam`으로 조회 개수를 받습니다. `limit`을 생략하면 기본값 50을 사용합니다.

Postman으로 실제 요청을 보내 채팅이 없는 경우 `200 OK`와 빈 배열 `[]`이 반환되는 것을 확인했습니다.

### Lv 7. WebSocket 연결과 사용자 식별

`NicknameHandshakeInterceptor`에서 닉네임으로 플레이어를, ID로 월드를 조회했습니다.

조회 결과는 `Optional`이므로 제공된 처리 흐름에 맞게 `orElse(null)`을 사용했습니다.

연결 이후에도 사용할 정보는 지정된 키로 저장했습니다.

```java
attributes.put(ATTR_NICKNAME, nickname);
attributes.put(ATTR_WORLD_ID, worldId);
```

처음에는 닉네임을 키로, 월드 ID를 값으로 저장하려고 했습니다. 이후 `put(키, 값)`의 의미를 다시 확인하고 두 정보를 각각 저장하도록 수정했습니다.

### Lv 8. HandshakeInterceptor 등록

구현한 인터셉터를 WebSocket 핸들러에 연결했습니다.

```java
registry.addHandler(gameWebSocketHandler, "/ws/worlds/{worldId}")
        .addInterceptors(nicknameInterceptor);
```

위 코드는 등록 흐름의 핵심 부분이며, 기존 Origin 설정은 유지했습니다.

Spring Bean으로 등록되어 있어도 WebSocket 연결 요청에 자동 적용되는 것은 아니라는 점을 학습했습니다.

Postman으로 연결을 요청하고 IntelliJ 디버거에서 `beforeHandshake()` 실행과 세션 속성 저장을 확인했습니다.

### Lv 9. 월드별 WebSocket 세션 관리

월드와 닉네임을 기준으로 연결을 등록하고 조회하도록 구현했습니다.

```java
boolean added =
        sessions.putIfAbsent(nicknameKey, candidate) == null;
```

`putIfAbsent()`는 키가 없을 때만 저장합니다. 반환값이 `null`이면 새로 등록한 것이고, 기존 값이 반환되면 중복 등록입니다.

이를 이용하여 기존 연결을 덮어쓰지 않도록 했습니다. 연결 조회에서도 등록과 같은 닉네임 키 변환 규칙을 사용했습니다.

### Lv 10. Redis 접속 상태 관리

접속 시 Redis Sorted Set에 연결 정보를 등록하고, 종료 시 해당 연결을 삭제했습니다.

| 항목 | 저장 내용 |
| --- | --- |
| key | `world:{worldId}:presence` |
| member | 연결을 식별하는 `connectionId` |
| score | 연결의 만료 시각 |

```java
redisTemplate.opsForZSet().add(
    key,
    connectionId,
    expiresAt()
);
```

```java
redisTemplate.opsForZSet().remove(
    key(worldId),
    connectionId
);
```

연결별 유효 시간은 90초이고, 키 전체 정리용 TTL은 180초입니다.

Sorted Set 원소의 만료 시각과 Redis 키 자체의 TTL은 서로 다른 개념이라는 점을 학습했습니다. 원소는 score에 시각을 저장한다고 자동 삭제되는 것이 아니라, 제공된 정리 로직에서 만료 여부를 판단합니다.

### Lv 11. 메시지 라우팅과 Ping/Pong

`MessageRouter`에서 찾은 핸들러의 `handle()`을 호출하도록 연결했습니다.

```java
handler.handle(context, message);
```

Ping 핸들러에서는 현재 연결의 접속 상태를 갱신하고, 요청한 연결에 Pong을 응답했습니다.

```java
presenceService.heartbeat(
    context.worldId(),
    connection.connectionId()
);

broadcaster.sendTo(
    context.session(),
    new PongResponse()
);
```

게임 접속을 유지한 채 약 4분 후 월드 목록을 다시 조회하여 접속 인원이 유지되는 것을 확인했습니다.

### Lv 12. 플레이어 이동 요청 처리

클라이언트가 보낸 위치·회전·이동 상태를 읽고, `PlayerAction.Move` 객체로 만들어 엔진에 전달했습니다.

| 값 | 읽는 메서드 |
| --- | --- |
| `x`, `y`, `z` | `WsFields.finiteNumber()` |
| `yaw`, `pitch` | `WsFields.finiteFloat()` |
| `crouching`, `gliding` | `WsFields.booleanValue()` |

닉네임과 월드 ID는 메시지에서 임의로 읽지 않고 현재 연결의 `context`에서 가져왔습니다.

```java
engineManager.enqueue(context.worldId(), move);
```

이 단계에서는 이동 요청을 구성하고 전달하는 부분을 구현했으며, 실제 게임 처리에는 제공된 엔진을 사용했습니다.

### Lv 13. 채팅 요청 처리와 응답 구성

`WsFields.text()`로 채팅 내용을 읽고, `ChatService.saveMessage()`를 호출하여 저장했습니다.

저장 결과의 발신자·내용·생성 시각으로 `ChatResponse`를 구성했습니다. 응답 시각은 새로 생성하지 않고 저장 결과의 시각을 사용했습니다.

응답 DTO의 `type`은 항상 `"chat"`으로 설정했습니다.

```text
채팅 내용 추출
→ 현재 월드와 닉네임으로 저장
→ 저장 결과를 사용해 응답 DTO 생성
```

### Lv 14. 같은 월드의 참여자에게 채팅 전송

`LocalChatSender`에서 제공된 `WorldBroadcaster`를 호출하도록 작성했습니다.

```java
public void send(Long worldId, Object message) {
    broadcaster.broadcast(worldId, message);
}
```

채팅을 보낸 사람도 수신 대상에 포함하며, 전달 대상은 같은 월드의 연결입니다.

특정 연결에 보내는 `sendTo()`와 월드 전체에 보내는 `broadcast()`의 역할을 구분했습니다.

### Lv 15. 접속자 목록 조회

현재 월드의 세션 중 연결이 열려 있는 세션만 선택하고, 세션 속성에서 닉네임을 추출했습니다.

```java
List<String> users = registry.entries(context.worldId()).stream()
        .map(entry -> entry.session())
        .filter(session -> session.isOpen())
        .map(session -> (String) session.getAttributes()
                .get(NicknameHandshakeInterceptor.ATTR_NICKNAME))
        .sorted()
        .toList();
```

세션 속성의 반환 타입은 `Object`이므로 `String`으로 형변환했습니다.

응답에는 닉네임 목록과 목록의 크기인 `count`, 메시지 종류인 `"onlineUsers"`를 담고 요청한 연결에만 전송했습니다.

게임과 Postman을 같은 월드에 연결했을 때 인원수 2를 확인하고, 게임 연결만 종료한 뒤에는 Postman의 닉네임만 남고 인원수가 1로 바뀌는 것을 확인했습니다.

### Lv 16. 낙관적 락

`WorldTrialSite`의 `revision`을 JPA가 관리하는 버전 필드로 지정했습니다.

```java
@Version
private long revision;
```

같은 버전을 읽은 두 트랜잭션이 데이터를 수정할 때, 먼저 반영된 변경을 이전 버전의 저장이 덮어쓰지 못하도록 합니다.

버전 증가 코드는 직접 작성하지 않고 JPA에 맡겼습니다.

제공된 `OptimisticLockTest`에서는 같은 데이터의 저장 충돌, 충돌한 트랜잭션의 다른 행 변경 롤백, 서로 다른 월드의 독립된 저장을 확인하도록 구성되어 있습니다.

### Lv 17. 커서 페이지 조회

채팅이 계속 추가되는 상황에서 과거 내역을 이어 읽을 수 있도록 다음 커서를 구성했습니다.

커서는 생성 시각과 ID를 함께 사용합니다. 시각이 같은 채팅도 ID로 순서를 구분할 수 있습니다.

제공된 조회 코드는 요청 개수보다 한 건 더 가져와 다음 페이지 존재 여부를 판단합니다.

```java
boolean hasNext = found.size() > limit;
```

다음 페이지가 있으면 실제 반환 목록의 마지막 항목을 선택하고, 없으면 `null`을 사용했습니다.

```java
ChatHistoryEntry last =
        hasNext ? items.get(items.size() - 1) : null;
```

예를 들어 2건을 요청하여 `[30, 20, 10]`이 조회되었다면, `[30, 20]`을 반환하고 ID 20인 채팅을 다음 커서의 기준으로 사용합니다.

추가 조회한 ID 10을 기준으로 삼으면 아직 반환하지 않은 채팅을 건너뛸 수 있다는 점을 학습했습니다.

### Lv 18. Redis 최근 채팅 캐시

최근 채팅 조회 결과를 JSON 문자열로 저장하고 5초의 TTL을 적용했습니다.

```java
String json = redis.opsForValue().get(key(worldId, limit));
```

```java
redis.opsForValue().set(
    key(worldId, limit),
    json,
    Duration.ofSeconds(5)
);
```

```java
redis.delete(keys);
```

캐시 키는 월드와 조회 개수를 모두 포함합니다.

```text
world:{worldId}:chat:recent:{limit}
```

같은 월드에서도 조회 개수가 다르면 다른 캐시를 사용합니다. 무효화 시에는 제공된 키 목록을 이용하여 해당 월드의 캐시를 삭제합니다.

캐시가 없는 `null`과 채팅이 없다는 조회 결과인 `[]`을 구분하고, 빈 목록도 캐시에 저장했습니다.

`RecentChatCacheTest`의 테스트 4개가 모두 통과한 것을 확인했습니다.

### Lv 19. Redis Lua로 채팅 전송 횟수 제한

플레이어별로 처음 채팅을 허용한 시점부터 10초 동안 최대 5건을 허용하도록 구현했습니다.

기존 코드에서는 횟수 조회와 증가가 별도 명령이어서, 여러 요청이 같은 횟수를 읽고 함께 통과할 수 있었습니다.

수정 전 제공 테스트에서 다음 실패를 확인했습니다.

```text
expected: 5L
but was: 12L
```

이를 해결하기 위해 조회·검사·증가·최초 만료 설정을 하나의 Lua Script로 묶었습니다.

```lua
local count = tonumber(redis.call('GET', KEYS[1]) or '0')

if count >= 5 then
    return 0
end

local updated = redis.call('INCR', KEYS[1])
if updated == 1 then
    redis.call('EXPIRE', KEYS[1], 10)
end

return 1
```

Java에서는 플레이어별 키를 스크립트에 전달하고, 반환값을 허용 여부로 변환했습니다.

```java
String key = "chat:limit:" + playerId;
Long result = redisTemplate.execute(LIMIT_SCRIPT, List.of(key));
return Long.valueOf(1L).equals(result);
```

처음 허용할 때만 TTL을 설정하므로 이후 요청이 제한 시간을 연장하지 않습니다.

수정 후에는 다음 네 테스트가 모두 통과했습니다.

- 5건 허용 후 6번째 요청 거절
- 후속 요청이 최초 만료 시간을 연장하지 않음
- 동시 요청에서도 최대 5건만 허용
- 만료 후 다시 요청 허용

<br/>

## 테스트 및 동작 확인

제공된 테스트의 `@Test` 주석을 해제하고 단계별로 실행했습니다. 실제 동작은 브라우저, Postman, IntelliJ 디버거, Redis CLI를 함께 사용하여 확인했습니다.

| 확인 대상 | 확인 방법 및 기록 |
| --- | --- |
| 최근 채팅 API | Postman에서 `200 OK`, 채팅이 없는 경우 `[]` 확인 |
| WebSocket 인터셉터 | 디버거에서 닉네임과 월드 ID의 속성 저장 확인 |
| Redis 접속 정보 | `ZRANGE`, `TTL`로 저장된 연결과 만료 정보 확인 |
| Ping/Pong | 게임 접속 약 4분 후에도 접속 인원 유지 확인 |
| 플레이어 이동 | 제공 테스트 및 실제 게임 이동 확인 |
| 접속자 목록 | 연결 종료 전후 인원수 2 → 1 변경 확인 |
| 최근 채팅 캐시 | 테스트 4개 통과 |
| Lua 횟수 제한 | 수정 전 동시 요청 실패 재현, 수정 후 테스트 4개 통과 |

<br/>

## 학습 내용 정리

### 1. Redis와 MySQL의 차이는 무엇인가요?

MySQL은 테이블과 관계를 기반으로 데이터를 관리하는 관계형 데이터베이스입니다. SQL을 사용하며, 트랜잭션과 제약조건으로 데이터의 일관성을 관리합니다.

Redis는 메모리 중심의 데이터 저장소이며, String·Hash·List·Set·Sorted Set과 같은 자료구조를 제공합니다. TTL을 이용해 일정 시간이 지난 데이터를 만료시킬 수 있어 캐시나 일시적인 상태 관리에 활용하기 좋습니다.

Redis도 영속화 설정을 지원하므로 단순히 “서버가 종료되면 항상 모든 데이터가 사라지는 저장소”로 구분하지는 않습니다.

프로젝트에서는 다음과 같이 역할을 나누었습니다.

| 저장소 | 사용한 데이터 | 사용 이유 |
| --- | --- | --- |
| MySQL | 플레이어, 월드, 채팅, 시련 데이터 | 저장 데이터를 관계와 트랜잭션으로 관리 |
| Redis | 접속 상태 | 잦은 갱신과 만료 처리 |
| Redis | 최근 채팅 캐시 | 일정 시간 동안 조회 결과 재사용 |
| Redis | 채팅 전송 횟수 | 플레이어별 횟수와 제한 시간 관리 |

### 2. 채팅 기능에 SSE보다는 WebSocket을 더 선호하는 이유는 무엇인가요?

SSE는 서버에서 클라이언트로 이벤트를 전달하는 단방향 통신입니다. 클라이언트가 서버로 메시지를 보내려면 별도의 HTTP 요청이 필요합니다.

WebSocket은 하나의 연결에서 클라이언트와 서버가 양방향으로 메시지를 주고받을 수 있습니다.

이 프로젝트에서는 채팅 전송뿐 아니라 플레이어 이동, Ping/Pong, 접속자 목록 요청도 실시간으로 처리합니다. 따라서 양쪽에서 빈번하게 메시지를 보내는 구조에 WebSocket이 적합합니다.

SSE도 HTTP 요청과 조합하면 채팅을 구현할 수 있지만, 이 프로젝트는 지속적인 양방향 통신이 필요하므로 WebSocket을 사용합니다.

### 3. Pub/Sub에서 Publisher와 Subscriber는 각각 어떤 역할을 하나요?

Publisher는 특정 채널에 메시지를 발행하는 역할입니다. Subscriber는 해당 채널을 구독하고, 메시지가 도착하면 이를 처리하는 역할입니다.

Publisher는 개별 수신자가 누구인지 알 필요 없이 채널에 메시지를 보냅니다.

멀티 서버 채팅에 적용하면 다음과 같은 흐름으로 구성할 수 있습니다.

```text
서버 A에서 채팅 수신
→ Redis 채널에 발행
→ 채널을 구독한 서버 A와 B가 수신
→ 각 서버가 자신에게 연결된 같은 월드 참여자에게 전달
```

Redis Pub/Sub은 기본적으로 메시지를 보관하여 나중에 재전달하는 방식이 아닙니다. 따라서 채팅 내역 저장은 DB 저장 로직과 별도로 고려해야 합니다.
