# 숙련 주차 프로젝트

숙련 주차 프로젝트(Lv 1 ~ Lv 20) 기록을 정리한 문서입니다.
각 항목을 클릭하면 상세 내용이 펼쳐집니다.

---

<details>
<summary><b>Lv 1. Docker로 MySQL과 Redis 설정</b></summary>

**체크리스트**
- [x] Docker로 MySQL과 Redis를 실행합니다.
- [x] Spring 애플리케이션의 환경 변수를 설정합니다.
  - `spring.jpa.hibernate.ddl-auto`는 `update`로 두고 진행합니다.

```properties
# MySQL 연결 설정
spring.datasource.url=jdbc:mysql://localhost:3306/{이름}
spring.datasource.username={사용자명}
spring.datasource.password={비밀번호}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Redis 연결 설정
spring.data.redis.host={사용자명}
spring.data.redis.port=6379

# JPA 설정
spring.jpa.hibernate.ddl-auto=update
```

</details>

<details>
<summary><b>Lv 2. SQL을 JPA 인덱스로 표현하기</b></summary>

**체크리스트**
- [x] SQL을 직접 실행하는 대신 `@Table`과 `@Index`로 인덱스를 선언합니다. 테이블 이름, 인덱스 이름과 컬럼 순서는 제공된 SQL과 같아야 합니다.
  ```sql
  CREATE INDEX idx_chat_world_created_at ON chat_messages(world_id, created_at);
  ```
- [x] **확인:** 서버를 실행하고 인덱스가 실제 DB에 생성됐는지 확인합니다.
- [x] **확인:** 인덱스가 없으면 시작 검사에서 서버 실행을 중단합니다. `CHAT_HISTORY_INDEX_MISSING` 오류가 사라지고 서버가 정상 실행되어 `http://localhost:8080`에서 첫 화면이 열립니다.

```java
// TODO Lv 2: 제공된 SQL과 같은 인덱스를 선언합니다.
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_world_created_at", columnList = "world_id, created_at")
})
```

</details>

<details>
<summary><b>Lv 3. 요청 검증과 DTO: 플레이어 등록</b></summary>

**체크리스트 / API 명세 → 플레이어 등록**
- [x] API 명세에 맞게 플레이어 등록 Controller, 요청 DTO와 서비스를 구현합니다. 닉네임은 비어 있지 않은 2~12글자이며, 영문 대소문자와 숫자, 밑줄만 허용합니다.
  - 정규식: `^[a-zA-Z0-9_]+$`
- [x] 이미 등록된 닉네임이면 `ConflictException`으로 `DUPLICATE_NICKNAME` 에러를 던집니다.
- [x] Controller의 요청 매핑, JSON 본문 바인딩, DTO 검증과 성공 응답을 명세대로 구현합니다.
- [x] 중복이 아니면 제공된 `savePlayer(new Player(request.getNickname()))`로 저장합니다. 동시 등록의 제약 위반 처리는 이 함수에서 제공합니다. 성공 응답은 명세대로 본문 없는 `201`입니다.
- [x] **테스트 확인:** `PlayerRegistrationTest.java`와 `PlayerApiTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 게임 화면에서 닉네임을 적용할 수 있습니다. 잘못된 닉네임이나 이미 등록된 닉네임은 새로 저장되지 않습니다.

```java
// CreatePlayerRequest
// TODO Lv 3: 2~12글자의 영문 대소문자, 숫자와 밑줄을 허용하는 검증을 적용합니다.
@NotBlank
@Size(min = 2, max = 12)
@Pattern(regexp = "^[a-zA-Z0-9_]+$")
private final String nickname;
```

```java
// PlayerController
// TODO Lv 3: API 명세에 맞게 요청을 매핑하고, 검증한 요청으로 등록 서비스를 호출한 뒤 성공 응답을 반환합니다.
@PostMapping("/players")
public ResponseEntity<Void> create(@Valid @RequestBody CreatePlayerRequest request) {
    playerService.createPlayer(request);
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
```

```java
// PlayerService
@Transactional
public void createPlayer(CreatePlayerRequest request) {
    // TODO Lv 3: 닉네임 중복을 확인하고 플레이어를 저장합니다.
    if (!playerRepository.existsByNickname(request.getNickname())) {
        savePlayer(new Player(request.getNickname()));
    } else {
        throw new ConflictException("DUPLICATE_NICKNAME");
    }
}
```

</details>

<details>
<summary><b>Lv 4. 월드 생성</b></summary>

**체크리스트 / API 명세 → 월드 목록, 월드 생성**
- [x] `worldOperations.duringCreation()`에 람다를 전달하고 그 결과를 반환합니다. 이 함수는 동시에 들어온 생성 요청이 순서대로 처리되도록 합니다.
- [x] 람다 안에서 `worldRepository.countRootWorlds()`가 `MAX_WORLDS` 이상이면 `ConflictException`으로 `WORLD_LIMIT_REACHED` 에러를 던집니다.
- [x] 제한을 넘지 않으면 `createPreparedWorld(request)`의 결과를 반환합니다.
- [x] **테스트 확인:** `WorldCreationTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 새 월드가 목록에 표시되고 서버를 재시작해도 남아 있습니다.

```java
// WorldService → createWorld
@Transactional
public CommittedWorldCreation createWorld(CreateWorldRequest request) {
    if (!baselineReadiness.isReady()) {
        throw new ServiceUnavailableException("WORLD_BASELINE_INITIALIZING");
    }
    // TODO Lv 4: duringCreation() 안에서 기본 월드 3개 제한을 검사하고 createPreparedWorld(request)를 호출합니다.
    return worldOperations.duringCreation(() -> {
        if (worldRepository.countRootWorlds() >= MAX_WORLDS) {
            throw new ConflictException("WORLD_LIMIT_REACHED");
        } else {
            return createPreparedWorld(request);
        }
    });
}
```

</details>

<details>
<summary><b>Lv 5. 채팅 저장과 내역 조회</b></summary>

**체크리스트**
- [x] 제공된 채팅 핸들러에서 호출할 저장 서비스를 구현합니다. 보낸 사람은 메시지 본문이 아니라 연결에 저장된 사용자 정보로 결정합니다. 저장 결과는 제공된 `savedResponse(worldId, saved)`로 반환합니다. 이 함수는 응답 생성과 저장 완료 이벤트 발행을 담당합니다.
- [x] 채팅 저장에는 `chatMessageRepository.save()`를 사용합니다.
- [x] 최근 채팅을 조회하는 코드는 아래와 같습니다.
  ```java
  List<ChatMessage> recent = chatMessageRepository.findByWorldIdOrderByCreatedAtDescIdDesc(worldId, PageRequest.of(0, capped));
  ```
- [x] **테스트 확인:** `ChatServiceTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 저장된 내용과 반환된 목록의 건수 및 순서를 확인합니다. 이 검사는 게임 서버나 REST API 실행 없이 수행합니다.

```java
// ChatService → saveMessage
@Transactional
public ChatMessageResponse saveMessage(Long worldId, String sender, String content) {
    // TODO Lv 5: 채팅을 저장하고 savedResponse(worldId, saved)의 결과를 반환합니다.
    World world = worldRepository.findById(worldId).orElseThrow(
            () -> new NotFoundException("WORLD_NOT_FOUND")
    );

    ChatMessage save = chatMessageRepository.save(new ChatMessage(world, sender, content));
    return new ChatMessageResponse(sender, content, save.getCreatedAt());
}
```

```java
// ChatService → getRecentMessages
@Transactional(readOnly = true)
public List<ChatMessageResponse> getRecentMessages(Long worldId, int limit) {
    if (!worldRepository.existsById(worldId)) {
        throw new NotFoundException("WORLD_NOT_FOUND");
    }

    int capped = Math.min(Math.max(limit, 1), MAX_LIMIT);

    List<ChatMessage> recent = chatMessageRepository
            .findByWorldIdOrderByCreatedAtDescIdDesc(worldId, PageRequest.of(0, capped));

    // TODO Lv 5: recent를 오래된 순서로 바꾸고 응답 DTO 목록으로 반환합니다.
    Collections.reverse(recent);
    List<ChatMessageResponse> chatMessageResponses = new ArrayList<>();
    for (ChatMessage saved : recent) {
        chatMessageResponses.add(savedResponse(worldId, saved));
    }
    return chatMessageResponses;
}
```

</details>

<details>
<summary><b>Lv 6. 최근 채팅 조회 API 구현</b></summary>

**체크리스트 / API 명세 → 최근 채팅 조회**
- [x] 요청 경로, HTTP 메서드, 경로 변수와 선택 파라미터의 기본값을 명세에 맞게 구현합니다.
- [x] 제공된 `RecentChatQueryService.getRecentMessages()`의 결과를 `ResponseEntity`에 담아 반환합니다.
- [x] **테스트 확인:** `RecentChatApiTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** API를 호출하여 성공 상태 코드와 응답을 확인합니다. 아직 채팅을 저장하지 않았다면 빈 배열이 정상입니다. 저장된 채팅이 있다면 응답 필드와 순서도 확인합니다.

```java
// WorldChatController : API에 맞는 요청 매핑과 응답 구현
// TODO Lv 6: API 명세에 맞는 요청 매핑과 응답을 구현합니다.
@GetMapping("/worlds/{worldId}/chats")
public ResponseEntity<List<ChatMessageResponse>> chats(
        @PathVariable Long worldId,
        @RequestParam(defaultValue = "50") int limit
) {
    return ResponseEntity.ok(chatService.getRecentMessages(worldId, limit));
}
```

</details>

<details>
<summary><b>Lv 7. WebSocket 연결과 사용자 식별</b></summary>

**체크리스트 / API 명세 → WebSocket 연결**
- [x] `playerRepository.findByNickname(nickname)`으로 플레이어를 조회해 `player`에 대입합니다. 조회 결과가 없으면 `null`을 사용합니다.
- [x] `worldRepository.findById(worldId)`로 월드를 조회해 `world`에 대입합니다. 조회 결과가 없으면 `null`을 사용합니다.
- [x] `attributes`에 `ATTR_NICKNAME`을 키로 `nickname`을, `ATTR_WORLD_ID`를 키로 `worldId`를 저장합니다. 이 값은 연결 이후 `WebSocketSession.getAttributes()`에서 사용할 수 있습니다.
- [x] **테스트 확인:** `NicknameHandshakeInterceptorTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 제공 테스트에서 정상 요청의 닉네임과 월드 ID가 세션 속성에 저장되는지 확인합니다.

```java
// 닉네임으로 플레이어 조회. 없으면 null
// TODO Lv 7: 닉네임으로 플레이어를 조회합니다. 없으면 null을 사용합니다.
Player player = playerRepository.findByNickname(nickname).orElse(null);
```

```java
// worldId로 월드를 조회. 없으면 null
// TODO Lv 7: worldId로 월드를 조회합니다. 없으면 null을 사용합니다.
World world = worldRepository.findById(worldId).orElse(null);
```

```java
// attributes에 저장
// TODO Lv 7: nickname과 worldId를 ATTR_NICKNAME, ATTR_WORLD_ID 키로 attributes에 저장합니다.
attributes.put(ATTR_NICKNAME, nickname);
attributes.put(ATTR_WORLD_ID, worldId);
```

</details>

<details>
<summary><b>Lv 8. HandshakeInterceptor 등록</b></summary>

**체크리스트 / API 명세 → WebSocket 연결**
- [x] `WebSocketConfig`에서 `/ws/worlds/{worldId}` 경로에 `NicknameHandshakeInterceptor`를 등록합니다. 인터셉터를 새로 만들거나 사용자 식별 로직을 핸들러로 옮기지 않습니다.
- [x] **테스트 확인:** `WebSocketConfigTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** Postman으로 연결을 요청하고 로그 또는 디버거로 `beforeHandshake()` 실행과 월드 ID 및 닉네임의 세션 속성 저장을 확인합니다.

```java
// NicknameHandshakeInterceptor 등록
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // TODO Lv 8: 제공된 인터셉터를 핸들러 등록에 연결합니다.
    registry.addHandler(gameWebSocketHandler, "/ws/worlds/{worldId}")
            .addInterceptors(nicknameInterceptor)
            .setAllowedOriginPatterns(properties.wsAllowedOrigins().toArray(String[]::new));
}
```

</details>

<details>
<summary><b>Lv 9. 월드별 WebSocket 세션 관리</b></summary>

**체크리스트**
- [x] `register()`에서 `sessions.putIfAbsent(nicknameKey, candidate)`로 연결을 등록합니다. 반환값이 `null`이면 새로 등록한 것이므로 `added`를 `true`로 설정합니다. 이미 등록된 연결이 있으면 덮어쓰지 않습니다.
- [x] `get()`에서 `sessions.get(key(nickname))`으로 연결을 조회해 반환합니다.
- [x] **테스트 확인:** `WorldSessionRegistryTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 등록한 연결을 월드와 닉네임으로 조회할 수 있고, 같은 월드의 중복 닉네임은 기존 연결을 덮어쓰지 않습니다.

```java
// WorldSessionRegistry → register()
// TODO Lv 9: putIfAbsent()로 candidate를 등록하고, 새로 등록했으면 added를 true로 설정합니다.
boolean added = false;
SessionRegistry.Entry entry = sessions.putIfAbsent(nicknameKey, candidate);
if (entry == null) {
    added = true;
}
```

```java
// WorldSessionRegistry → get()
public SessionRegistry.Entry get(Long worldId, String nickname) {
    ConcurrentHashMap<String, SessionRegistry.Entry> sessions = worlds.get(worldId);
    if (sessions == null) {
        return null;
    }
    // TODO Lv 9: sessions에서 key(nickname)에 해당하는 연결을 반환합니다.
    return sessions.get(key(nickname));
}
```

</details>

<details>
<summary><b>Lv 10. Redis 접속 상태 관리</b></summary>

**체크리스트**
- [x] `join()`에서 `redisTemplate.opsForZSet().add(key, connectionId, expiresAt())`로 접속 정보를 저장합니다.
- [x] `leave()`에서 `redisTemplate.opsForZSet().remove(key(worldId), connectionId)`로 종료된 연결을 삭제합니다.
- [x] 연결별 만료는 90초, 키 전체 정리용 TTL은 180초입니다.
- [x] **테스트 확인:** Docker를 실행하고 `PresenceServiceTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 연결하면 Redis에 연결 ID와 만료 시각이 저장되고, 정상 종료하면 해당 원소가 제거됩니다. `ZRANGE 키 0 -1 WITHSCORES`로 확인하세요.

```java
public void join(Long worldId, String connectionId) {
    String key = key(worldId);
    // TODO Lv 10: ZSet에 connectionId를 member로, expiresAt()을 score로 저장합니다.
    redisTemplate.opsForZSet().add(key, connectionId, expiresAt());
    redisTemplate.expire(key, KEY_TTL);
}
```

```java
public void leave(Long worldId, String connectionId) {
    // TODO Lv 10: key(worldId)의 ZSet에서 connectionId를 제거합니다.
    redisTemplate.opsForZSet().remove(key(worldId), connectionId);
}
```

</details>

<details>
<summary><b>Lv 11. 메시지 라우팅과 Ping/Pong</b></summary>

**체크리스트 / API 명세 → 메시지 공통 형식, ping 요청, pong 응답**
- [x] `MessageRouter.route()`에서 찾아 둔 `handler`의 `handle(context, message)`를 호출합니다.
- [x] `PingWsHandler`에서 `presenceService.heartbeat(context.worldId(), connection.connectionId())`를 호출해 현재 연결의 Redis 접속 상태를 갱신합니다.
- [x] `broadcaster.sendTo(context.session(), new PongResponse())`로 ping을 보낸 연결에 pong을 응답합니다.
- [x] **테스트 확인:** `MessageRouterTest.java`와 `PingWsHandlerTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 게임에 입장한 상태에서 `GET /worlds`의 접속 인원이 90초 후에도 유지되는지 확인합니다. ping에 대한 pong 응답과 Redis 갱신 호출은 제공 테스트로 확인합니다.

```java
// MessageRouter.route()
try {
    // TODO Lv 11: handler에 context와 message를 전달해 handle()을 호출합니다.
    handler.handle(context, message);
} catch (...) { ... }
```

```java
// PingWsHandler
@Override
public void handle(WsMessageContext context, JsonNode message) {
    WorldSessionRegistry.Entry connection = registry.get(context.worldId(), context.nickname());
    if (connection == null || connection.session() != context.session()) {
        return;
    }
    // TODO Lv 11: presenceService.heartbeat()에 월드 ID와 현재 연결 ID를 전달합니다.
    presenceService.heartbeat(context.worldId(), connection.connectionId());
    // TODO Lv 11: broadcaster.sendTo()로 현재 세션에 PongResponse를 보냅니다.
    broadcaster.sendTo(context.session(), new PongResponse());
}
```

</details>

<details>
<summary><b>Lv 12. 플레이어 이동 요청 처리</b></summary>

**체크리스트 / API 명세 → 플레이어 이동 요청**
- [x] 제공된 `WsFields.finiteNumber()`, `finiteFloat()`, `booleanValue()`로 요청 값을 읽습니다. 각 메서드는 메시지와 필드명을 받습니다.
- [x] `PlayerAction.Move`에 현재 연결의 닉네임, 위치, 시선과 이동 상태를 전달하고 `engineManager.enqueue(월드 ID, 이동 요청)`를 호출합니다. 월드와 닉네임은 요청 본문에서 받지 않습니다.
- [x] `PlayerAction.Move`의 생성자 순서는 닉네임, x, y, z, yaw, pitch, crouching, gliding, 내부 식별자입니다. 내부 식별자 조회는 제공 코드 그대로 사용합니다.
- [x] **테스트 확인:** `MoveWsHandlerTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 게임에서 이동 키를 눌러 자신의 캐릭터가 이동하는지 확인합니다.

읽는 값과 자료형:

| 인자 | 자료형 | 읽는 방법 |
| --- | --- | --- |
| x, y, z | `double` | `WsFields.finiteNumber(message, 필드명)` |
| yaw, pitch | `float` | `WsFields.finiteFloat(message, 필드명)` |
| crouching, gliding | `boolean` | `WsFields.booleanValue(message, 필드명)` |

```java
// MoveWsHandler
@Override
public void handle(WsMessageContext context, JsonNode message) {
    String finalSceneActionId = WsFields.optionalFinalSceneActionId(message);
    // TODO Lv 12: 명세의 이동 값을 읽어 현재 사용자의 이동 요청을 엔진에 전달합니다.
    double x = WsFields.finiteNumber(message, "x");
    double y = WsFields.finiteNumber(message, "y");
    double z = WsFields.finiteNumber(message, "z");
    float yaw = WsFields.finiteFloat(message, "yaw");
    float pitch = WsFields.finiteFloat(message, "pitch");
    boolean crouching = WsFields.booleanValue(message, "crouching");
    boolean gliding = WsFields.booleanValue(message, "gliding");
    PlayerAction action = new PlayerAction.Move(
            context.nickname(),
            x, y, z,
            yaw, pitch,
            crouching, gliding,
            finalSceneActionId
            );
    engineManager.enqueue(context.worldId(), action);
}
```

**확인**
- 前: 작성 전에는 캐릭터가 움직여도 다른 몹들이 반응하지 않았다. 몸체만 움직이고 실체는 가만히 있는 상태였다.
- 後: 작성 후에는 몹 주변을 가면 반응을 한다.

</details>

<details>
<summary><b>Lv 13. 채팅 요청 처리와 응답 구성</b></summary>

**체크리스트 / API 명세 → 채팅 요청, 채팅 응답**
- [x] `readContent()`에서 명세의 채팅 내용 필드를 읽어 반환합니다. 문자열은 `WsFields.text(메시지, 필드명)`으로 읽을 수 있습니다.
- [x] 명세를 보고 `ChatResponse`의 필드와 생성자를 완성합니다. 생성자 매개변수는 `sender`, `content`, `timestamp` 3개지만, 명세의 응답에는 메시지 종류를 나타내는 `type` 필드(값 `"chat"`)도 있습니다. `type`은 매개변수로 받지 않고 항상 `"chat"`으로 채웁니다.
- [x] `createResponse()`에서 `chatService.saveMessage()`로 채팅을 저장합니다. 월드는 `context.worldId()`, 보낸 사람은 `context.nickname()`, 내용은 매개변수 `content`를 사용합니다. 저장 결과로 `ChatResponse`를 만들어 반환합니다.
- [x] **테스트 확인:** `ChatWsHandlerTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 게임에서 일반 채팅을 보내고 최근 채팅 조회 API와 DB에서 저장 결과를 확인합니다. 채팅은 아직 본인과 다른 참여자의 화면에 표시되지 않습니다.

```java
// ChatWsHandler.readContent()
private String readContent(JsonNode message) {
    // TODO Lv 13: API 명세의 채팅 내용을 읽습니다.
    return WsFields.text(message, "content");
}
```

```java
// ChatWsHandler.createResponse()
private ChatResponse createResponse(WsMessageContext context, String content) {
    // TODO Lv 13: 현재 연결의 사용자로 저장하고 명세에 맞는 응답을 만듭니다.
    ChatMessageResponse saved = chatService.saveMessage(context.worldId(), context.nickname(), content);
    return new ChatResponse(saved.getSender(), saved.getContent(), saved.getCreatedAt());
}
```

```java
// ChatResponse
@Getter
public class ChatResponse {
    // TODO Lv 13: API 명세에 맞게 응답 필드와 생성자를 완성합니다.
    private final String sender;
    private final String content;
    private final LocalDateTime timestamp;
    private final String type = "chat";

    public ChatResponse(String sender, String content, LocalDateTime timestamp) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
    }
}
```

</details>

<details>
<summary><b>Lv 14. 같은 월드의 참여자에게 채팅 전송</b></summary>

**체크리스트 / API 명세 → 채팅 전송 응답**
- [x] `WorldBroadcaster.broadcast(worldId, message)`로 같은 월드의 세션에 메시지를 전달합니다. 보낸 사람도 수신 대상에 포함합니다.
- [x] **테스트 확인:** `LocalChatSenderTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 같은 월드의 두 참여자가 보낸 사람, 내용과 시각을 포함한 채팅을 받습니다. 다른 월드의 참여자에게는 전달되지 않으며 DB에는 보낸 채팅 한 건만 저장됩니다.

```java
@Service
@RequiredArgsConstructor
public class LocalChatSender {
    private final WorldBroadcaster broadcaster;

    public void send(Long worldId, Object message) {
        // TODO Lv 14: 같은 월드의 참여자에게 메시지를 전송합니다.
        broadcaster.broadcast(worldId, message);
    }
}
```

</details>

<details>
<summary><b>Lv 15. 접속자 목록 조회</b></summary>

**체크리스트 / API 명세 → 접속자 목록 요청, 접속자 목록 응답**
- [x] `registry.entries(context.worldId())`로 현재 월드의 연결 목록을 조회합니다. 각 항목의 `session()`으로 세션을 꺼내고, `isOpen()`이 `true`인 세션만 선택합니다.
- [x] 각 세션의 `getAttributes()`에서 `NicknameHandshakeInterceptor.ATTR_NICKNAME`에 저장된 닉네임을 꺼냅니다.
- [x] 닉네임 목록을 명세의 기준대로 정렬해 `users`에 담고, 목록의 크기를 `count`에 담습니다. `broadcaster.sendTo()`로 요청한 연결에만 응답합니다.
- [x] **테스트 확인:** `OnlineUsersWsHandlerTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 게임 창 하나만 남기고 나머지 연결을 종료합니다. 게임에서 사용 중인 닉네임과 다른 닉네임을 등록해 Postman으로 같은 월드에 연결합니다. 명세의 요청을 보내 두 닉네임과 인원수 `2`가 응답에 포함되는지 확인합니다. 게임 연결을 종료한 뒤 다시 요청하면 Postman의 닉네임만 남아야 합니다.

```java
// OnlineUsersResponse
@Getter
public class OnlineUsersResponse {
    // TODO Lv 15: API 명세에 맞게 응답 필드와 생성자를 완성합니다.
    private final List<String> users;
    private final int count;
    private final String type = "onlineUsers";

    public OnlineUsersResponse(List<String> users, int count) {
        this.users = users;
        this.count = count;
    }
}
```

```java
// OnlineUsersWsHandler
@Component
@RequiredArgsConstructor
public class OnlineUsersWsHandler implements WsMessageHandler {
    private final WorldSessionRegistry registry;
    private final WorldBroadcaster broadcaster;

    @Override
    public String type() {
        return "onlineUsers";
    }

    @Override
    public void handle(WsMessageContext context, JsonNode message) {
        // TODO Lv 15: 현재 월드의 열린 연결에서 닉네임을 조회하고 요청자에게 응답합니다.
        Collection<SessionRegistry.Entry> entries = registry.entries(context.worldId());
        List<String> users = new ArrayList<>();
        for (SessionRegistry.Entry entry : entries) {
            WebSocketSession session = entry.session();
            if (session.isOpen()) {
                users.add(session.getAttributes().get(NicknameHandshakeInterceptor.ATTR_NICKNAME).toString());
            }
        }
        users.sort(String::compareTo);
        OnlineUsersResponse onlineUsers = new OnlineUsersResponse(users, users.size());
        broadcaster.sendTo(context.session(), onlineUsers);
    }
}
```

</details>

<details>
<summary><b>Lv 16. 낙관적 락</b></summary>

**체크리스트**
- [x] `WorldTrialSite`의 `revision` 필드를 JPA가 관리하는 버전 필드로 설정합니다. 버전 값을 직접 증가시키는 코드는 작성하지 않습니다.
- [x] **테스트 확인:** `OptimisticLockTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 같은 버전을 읽은 두 저장 중 하나만 커밋되고, 충돌한 트랜잭션의 다른 행 변경도 함께 롤백되어야 합니다. 서로 다른 월드의 독립된 저장은 모두 성공해야 합니다.

```java
@Version private long revision;
```

</details>

<details>
<summary><b>Lv 17. 커서 페이지 조회</b></summary>

**체크리스트**
- [x] 다음 페이지가 있는지 알아보기 위해 요청한 개수보다 한 건 더 조회하는 코드는 제공되어 있습니다. 예를 들어 2건씩 요청했을 때 3건이 조회되면, 앞의 2건을 반환하고 **반환한 두 번째 채팅**을 다음 조회의 기준으로 사용합니다.
- [x] 조회 결과는 생성 시각 내림차순이며, 시각이 같으면 ID 내림차순입니다. `hasNext`가 `true`이면 반환 목록 `items`의 마지막 항목을 `last`에 대입하고, `false`이면 `null`을 대입합니다.
- [x] 첫 요청은 커서 없이 호출합니다. 다음 페이지는 응답의 `nextCreatedAt`과 `nextId`를 각각 `beforeCreatedAt`, `beforeId`로 함께 전달해 조회합니다.
- [x] **테스트 확인:** `ChatHistoryTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 제공 테스트에서 다음 커서가 반환한 목록의 마지막 항목인지 확인합니다. 마지막 페이지와 빈 결과에서는 다음 커서가 `null`이어야 합니다.

```java
// ChatHistoryService.getHistory()
@Transactional(readOnly = true)
public ChatHistoryPage getHistory(Long worldId, LocalDateTime beforeCreatedAt, Long beforeId, int limit) {
    if ((beforeCreatedAt == null) != (beforeId == null) || limit < 1 || limit > 100) {
        throw new InvalidRequestException("VALIDATION_FAILED");
    }
    if (!worlds.existsById(worldId)) {
        throw new NotFoundException("WORLD_NOT_FOUND");
    }
    List<ChatMessage> found = repository.findHistory(
            worldId, beforeCreatedAt, beforeId, PageRequest.of(0, limit + 1));
    boolean hasNext = found.size() > limit;
    List<ChatHistoryEntry> items = found.stream().limit(limit)
            .map(message -> new ChatHistoryEntry(
                    message.getId(),
                    message.getSenderNickname(),
                    message.getContent(),
                    message.getCreatedAt()
            )).toList();
    // TODO Lv 17: 다음 페이지가 있으면 반환한 마지막 항목을, 없으면 null을 선택합니다.
    ChatHistoryEntry last;
    if (hasNext) {
        last = items.get(limit - 1);
    } else {
        last = null;
    }
    return new ChatHistoryPage(items, hasNext,
            last == null ? null : last.getCreatedAt(),
            last == null ? null : last.getId());
}
```

</details>

<details>
<summary><b>Lv 18. Redis 최근 채팅 캐시</b></summary>

**체크리스트**
- [x] `read()`에서 `key(worldId, limit)`에 저장된 JSON 문자열을 조회해 `json`에 대입합니다.
- [x] `write()`에서 제공된 `json`을 `key(worldId, limit)`에 저장하고 **5초의 TTL을 설정**합니다. 빈 목록도 저장합니다.
- [x] `invalidate()`에서 제공된 `keys` 목록에 해당하는 Redis 데이터를 삭제합니다.
- [x] **테스트 확인:** Docker를 실행하고 `RecentChatCacheTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 저장한 채팅과 빈 목록을 다시 읽을 수 있고, 키의 TTL이 5초 이내인지 확인합니다. 한 월드의 캐시를 삭제해도 다른 월드의 캐시는 남아 있어야 합니다.

```java
// RecentChatCache.read()
public List<ChatMessageResponse> read(Long worldId, int limit) {
    try {
        // TODO Lv 18: 해당 키의 JSON 문자열을 Redis에서 조회합니다.
        String json = redis.opsForValue().get(key(worldId, limit));
        return json == null ? null : Arrays.asList(mapper.readValue(json, ChatMessageResponse[].class));
    } catch (RuntimeException unavailable) {
        return null;
    }
}
```

```java
// RecentChatCache.write()
public void write(Long worldId, int limit, List<ChatMessageResponse> messages) {
    try {
        String json = mapper.writeValueAsString(messages);
        // TODO Lv 18: json을 Redis에 저장하고 5초의 TTL을 설정합니다.
        Duration TTL = Duration.ofSeconds(5);
        redis.opsForValue().set(key(worldId, limit), json, TTL);
    } catch (RuntimeException unavailable) {
        // 캐시는 보조 저장소이므로 DB 조회 결과를 그대로 응답합니다.
    }
}
```

```java
// RecentChatCache.invalidate()
public void invalidate(Long worldId) {
    List<String> keys = IntStream.rangeClosed(1, 100)
            .mapToObj(limit -> key(worldId, limit)).toList();
    try {
        // TODO Lv 18: keys에 담긴 캐시를 Redis에서 삭제합니다.
        redis.delete(keys);
    } catch (RuntimeException unavailable) {
        // 무효화에 실패한 캐시는 최대 5초 뒤 만료됩니다.
    }
}
```

</details>

<details>
<summary><b>Lv 19. Redis Lua로 채팅 전송 횟수 제한</b></summary>

**체크리스트**
- [x] 수정 전에 제공 테스트를 실행해, 동시 요청이 5건을 초과해 통과하는 실패를 확인합니다.
- [x] `allow(playerId)`에서 Lua Script를 실행합니다. 현재 횟수가 5 미만이면 횟수를 증가시키고, 5 이상이면 거절합니다. 메서드는 허용 시 `true`, 거절 시 `false`를 반환합니다.
- [x] 처음 허용할 때만 Redis 키에 10초 만료 시간을 설정합니다. 이후 요청에서는 만료 시간을 연장하지 않습니다.
- [x] 기존 플레이어별 Redis 키를 그대로 사용합니다. 같은 플레이어는 월드를 바꾸거나 재접속해도 남은 횟수를 공유하며, 서버별로 키를 나누지 않습니다.
- [x] **테스트 확인:** Docker를 실행하고 `ChatRateLimitTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 제공 테스트에서 동시 요청을 5건까지만 허용하는지, 후속 요청이 제한 시간을 연장하지 않는지 확인합니다. 만료 후에는 다시 허용하고, 다른 플레이어의 한도에는 영향을 주지 않아야 합니다.

```java
// ChatRateLimitService → Lua Script 작성
@Service
@RequiredArgsConstructor
public class ChatRateLimitService {
    private static final DefaultRedisScript<Long> CHATLIMIT_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            if current >= 5 then
                return 0
            else
                redis.call('INCR', KEYS[1])
                if current == 0 then
                    redis.call('EXPIRE', KEYS[1], 10)
                end
                return 1
            end
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean allow(Long playerId) {
        String key = "chat:limit:" + playerId;
        // TODO Lv 19: 횟수 확인부터 최초 만료 설정까지 원자적으로 실행합니다.
        Long result = redisTemplate.execute(CHATLIMIT_SCRIPT, List.of(key));
        return result == 1L;
    }
}
```

</details>

<details>
<summary><b>Lv 20. 멀티 서버</b></summary>

**체크리스트**
- [x] 서버 A의 참여자가 보낸 채팅을 서버 B의 같은 월드 참여자도 볼 수 있도록 구현하세요. 서버가 채팅을 Redis 채널에 보내면, 채널을 구독한 각 서버가 받아 자신에게 연결된 참여자에게 전달합니다. 게임 동기화는 엔진이 담당합니다.

**실행 준비**
- [x] `build.gradle`의 `webcraft-engine` 버전을 `2.2.6`으로 변경합니다.
- [x] Docker Compose로 앱 서버 2개, MySQL 1개, Redis 1개를 실행하도록 구성합니다. 앱의 외부 포트는 서로 다르게 지정하고, 두 앱이 같은 MySQL과 Redis에 연결되도록 설정합니다.
  - MySQL이 준비되기 전에 앱이 시작되면 실행이 실패하므로 `depends_on`과 `healthcheck`로 순서를 맞추세요.
- [x] `application.properties`에 `webcraft.chat.pubsub-enabled=true`를 추가합니다. 이 설정을 켜면 제공된 코드가 채팅 전송 시 `ChatRelay.publish()`를 호출하고, Redis 구독 기능을 활성화합니다.

**채팅 발행 — `ChatRelay.publish()`**

`worldId`에는 채팅을 보낼 월드 ID가, `message`에는 이미 만들어진 채팅 응답 객체가 전달됩니다.

- [x] 두 값을 아래 예시 형태처럼 JSON 문자열로 만들어 `ChatRelay.CHANNEL`에 발행합니다. JSON 변환에는 주입된 `objectMapper`를, 발행에는 `redisTemplate.convertAndSend()`를 사용합니다.

```json
{
  "worldId": 1,
  "message": {
    "type": "chat",
    "sender": "Alice",
    "content": "안녕하세요",
    "timestamp": "2026-09-17T12:00:00"
  }
}
```

**채팅 구독과 수신**
- [x] `ChatSubscriptionConfig.chatSubscription()`에서 `container`에 `relay`를 리스너로 등록합니다. 구독할 채널은 `ChatRelay.CHANNEL`이며, `ChannelTopic`으로 지정합니다.
- [x] `ChatRelay.onMessage()`에서 `message.getBody()`로 Redis 메시지의 본문을 읽고 JSON으로 변환합니다. 본문의 `worldId`는 월드 ID로, `message`는 `JsonNode`로 꺼내 `localChatSender.send()`에 전달합니다. `message`를 `ChatResponse`나 `Map`으로 다시 변환하지 않고 꺼낸 `JsonNode`를 그대로 전달합니다. 발행한 서버도 자기 채널을 구독하므로, 수신 처리에서는 전송만 하고 다시 저장하거나 발행하지 않습니다.
- [x] **테스트 확인:** Docker를 실행하고 `ChatRelayTest.java`의 주석을 해제한 뒤 실행합니다.
- [x] **확인:** 일반 창에서는 서버 A에 `Alice`로, 시크릿 창에서는 서버 B에 `Bob`으로 접속하고 같은 월드에 입장합니다. 어느 쪽에서 보내든 채팅이 양쪽 화면에 한 번씩 표시되어야 합니다. 한쪽이 다른 월드에 입장하면 서로의 채팅이 표시되지 않아야 합니다.

```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8.4
    container_name: mysql-compose
    environment:
      MYSQL_DATABASE: WebCraft
      MYSQL_ROOT_PASSWORD: 1234
    ports:
      - "3307:3306"
    volumes:
      - mysql_compose_data:/var/lib/mysql
    healthcheck:
      test: [ "CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-p1234" ]
      interval: 5s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7
    container_name: redis-compose
    ports:
      - "6380:6379"
    volumes:
      - redis_compose_data:/data
    healthcheck:
      test: [ "CMD", "redis-cli", "ping" ]
      interval: 5s
      timeout: 5s
      retries: 10

  app1:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: app1
    ports:
      - "8081:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/WebCraft
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 1234
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy

  app2:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: app2
    ports:
      - "8082:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/WebCraft
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 1234
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy

volumes:
  mysql_compose_data:
  redis_compose_data:
```

```dockerfile
# Dockerfile
# ---- 1단계: 빌드 ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Gradle wrapper와 설정 파일 먼저 복사 (의존성 캐싱 활용)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성만 먼저 받아서 레이어 캐싱
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 후 빌드
COPY src src
RUN ./gradlew clean build -x test --no-daemon

# ---- 2단계: 실행 ----
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```java
// ChatRelay.publish()
public void publish(Long worldId, Object message) {
    // TODO Lv 20: worldId와 message를 JSON으로 묶어 채팅 채널에 발행합니다.
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("worldId", worldId);
    map.put("message", message);
    String messageMapping = objectMapper.writeValueAsString(map);
    redisTemplate.convertAndSend(CHANNEL, messageMapping);
}
```

```java
// ChatRelay.onMessage()
@Override
public void onMessage(Message message, byte[] pattern) {
    // TODO Lv 20: JSON에서 worldId와 message를 읽어 localChatSender.send()로 전달합니다.
    String body = new String(message.getBody(), StandardCharsets.UTF_8);
    JsonNode node = objectMapper.readTree(body);
    Long worldId = node.get("worldId").asLong();
    JsonNode messageNode = node.get("message");
    localChatSender.send(worldId, messageNode);
}
```

```java
// ChatSubscriptionConfig
@Configuration
@ConditionalOnProperty(name = "webcraft.chat.pubsub-enabled", havingValue = "true")
public class ChatSubscriptionConfig {

    @Bean
    public RedisMessageListenerContainer chatSubscription(
            RedisConnectionFactory connectionFactory,
            ChatRelay relay
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // TODO Lv 20: 제공된 relay를 채팅 채널의 수신 리스너로 등록합니다.
        ChannelTopic channelTopic = new ChannelTopic(ChatRelay.CHANNEL);
        container.addMessageListener(relay, channelTopic);
        return container;
    }
}
```

</details>
