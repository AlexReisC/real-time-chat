Chat em tempo real usando Websockets feito com Spring Boot e arquitetura de Microsserviços.
## Arquitetura

**Tech stack**: Java (21), Spring Boot, PostgreSQL, MongoDB, Redis, Docker, RabbitMQ.
### Serviços do sistema
- **Service Discovery**
	- **Dependências Spring Boot:** Spring Cloud Netflix Eureka, Spring Boot Actuator
- **Config Server**
	- **Dependências Spring Boot:** Spring Cloud Config Server, Spring Boot Actuator
- **API Gateway**
	- **Dependências Spring Boot:** Spring Cloud Config, Spring Cloud Netflix Eureka, Spring Cloud Gateway Server Webflux, Spring Boot Actuator
- **Auth Service**
	- **Dependências Spring Boot:** Spring Web, Spring Security, Spring Cloud Netflix Eureka, Spring Cloud Config, Spring Boot Validation, jsonwebtoken, Spring Data JPA, PostgreSQL, Flyway, Lombok
- **Chat Service**
	- **Dependências Spring Boot:** Spring WebSockets, Spring Web, Spring Security, Spring Data Mongo, Spring Boot Validation, jsonwebtoken,  Spring Cloud Netflix Eureka,  Spring Cloud  Config, Spring OAuth2 Resource Server, Spring Boot AMQP, Reactor Netty, Spring Data Redis
### Demais componentes do sistema
- **RabbitMQ (mensageria)**
	- Função: Roteia mensagens (exchanges/queues). Ex.: quando um usuário envia mensagem, Chat Service publica na exchange; instâncias interessadas consomem e entregam aos sockets locais.
- **Redis (cache & presença)**
	- Função: Cache de metadados (ultimas mensagens, lista de participantes), locks leves.
- **Docker Compose**: Orquestra todos os serviços.
### Responsabilidade de cada serviço
1. **API Gateway**
	- Função: Ponto de entrada único para clientes — roteia requisições REST e faz upgrade para WebSocket e configura o CORS.
2. **Service Discovery (Eureka)**
	- Função: Permite que serviços encontrem uns aos outros dinamicamente. O gateway e serviços se registram no Eureka.
3. **Config Server**
	- Função: Centraliza configurações (profiles, secrets leves) para todos os serviços.
4. **Auth Service**
	- Função: Cadastro/login, emissão de JWT, gerenciamento de perfis, validação de autorização. Persiste usuários em PostgreSQL.
5. **Chat Service**
	- Função: Lógica de salas, conexão WebSocket (STOMP ou protocolo simples), persistência de mensagens em MongoDB, publicação/assinatura via RabbitMQ para entregar mensagens entre instâncias. Mantém estado efêmero no Redis (por ex. cache de mensagens).

---
### Endpoints por serviço
> **Os endepoints do Auth Service e do CHat Service usam o mesmo prefixo `/api/v1/`**

#### Auth Service
| Método  | Endpoint              | Descrição                                   |
| ------- | --------------------- | ------------------------------------------- |
| `POST`  | `/auth/register`      | Registrar novo usuário                      |
| `POST`  | `/auth/login`         | Autenticar usuário e gerar JWT              |
| `POST`  | `/auth/refresh`       | Refresh do token                            |
| `GET`   | `/users/me`           | Retornar informações do usuário autenticado |
| `PATCH` | `/users/me`           | Atualizar nome de usuário (username)        |
| `PUT`   | `/users//me/password` | Atualizar a senha do usuário autenticado    |

#### **Chat Service**
| Tipo     | Endpoint                                   | Descrição                                                                |
| -------- | ------------------------------------------ | ------------------------------------------------------------------------ |
| `WS`     | `/ws/chat`                                 | Conectar-se ao chat (envia JWT no handshake)                             |
| `GET`    | `/chat/messages/{roomId}`                  | Buscar histórico de mensagens                                            |
| `POST`   | `/rooms`                                   | Criar nova sala de chat                                                  |
| `GET`    | `/rooms`                                   | Listar todas as salas disponíveis                                        |
| `GET`    | `/rooms/{roomId}/members`                  | Listar todos os usuários conectados em uma sala                          |
| `GET`    | `/messages/room/{roomId}/history`          | Listar todos as mensagens (páginadas) de uma sala                        |
| `GET`    | `/messages/private/{targetUserId}`/history | Listar todas as mensagens (páginadas) de uma conversa privada            |
| `GET`    | `messages/private/{targetUserId}`          | Carregamento das mensagens (50 últimas) do cache de uma conversa privada |
| `GET`    | `/messages/room/{roomId}`                  | Carregamento dedas mensagens (50 últimas) do cache de uma sala           |
| `GET`    | `/private/conversations`                   | Listar as conversas existentes do usuário                                |
| `DELETE` | `rooms/{roomId}`                           | Deletar uma sala                                                         |
| `DELETE` | `rooms/{roomId}/members`                   | Remove o usuário permanentemente de uma sala                             |

#### **API Gateway**
| Rota           | Encaminha para   | Observações                |
| -------------- | ---------------- | -------------------------- |
| `/auth/**`     | Auth Service     | Login, registro, validação |
| `/chat/**`     | Chat Service     | REST do chat               |
| `/ws/**`       | Chat Service     | Proxy de WebSocket         |
| `/actuator/**` | Todos (restrito) | Monitoramento              |
| `/users/**`    | Auth Service     | Gerenciamento de usuário   |

### Portas de cada serviço
| Serviço/Banco | Nº porta        |
| ------------- | --------------- |
| Gateway       | 8080            |
| Chat          | 8082            |
| Auth          | 8083            |
| Discovery     | 8761            |
| Config        | 8888            |
| PostgreSQL    | 5432 (default)  |
| Mongo         | 27017 (defautl) |
| Mongo-Express | 8081 (default)  |
| RabbitMQ      | 5672 (default)  |
| React         | 3000            |

---
## Estrutura do projeto completo
```markdown
real-time chat/
 ├─ .github/
 ├─ api-gateway/
 ├─ auth-service/
 ├─ chat-service/
 ├─ config-data/
 ├─ config-server/
 ├─ docs/
 ├─ eureka-server/
 ├─ frontend/
 ├─ .env
 ├─ .gitignore
 ├─ LICENSE
 ├─ README.md
 └─ docker-compose.yml
```
- **`.github/`**: diretório com os workflows para rodar testes automatizados do Auth Service e do Chat Service
- **`docs/`**: diretório contendo a documentação do projeto
- **`config-data`**: diretório contendo os arquivos de configuração `api-gateway.properties`, `auth-service.properties` e `chat-service.properties` do API Gateway, Auth Service e Chat Service, respectivamente. O Config Server indica que os arquivos de configuração estão aí (`spring.cloud.config.server.native.search-locations=file:/config-data`) e os serviços se conectam ao Config Server para acessam suas respectivas configurações (`spring.config.import=optional:configserver:http://config-server:8888/`).

---
## Estrutura de cada serviço
### Chat Service
```markdown
chat/
 ├─ config/
 │   └─ RedisConfig.java
 ├─ controller/
 │   └─ ChatController.java
 │   └─ MessageController.java
 │   └─ RoomController.java
 ├─ service/
 │   ├─ ChatService.java
 │   └─ MessageService.java
 ├─ dto/
 │   ├─ request/
 │   │   ├─ CreateRoomDTO.java
 │   │   ├─ PrivateMessageDTO.java
 │   │   ├─ PublicMessageDTO.java
 │   │   └─ UserNotificationDTO.java
 │   └─ response/
 │   	 ├─ UserNotificationResponseDTO.java
 │   	 ├─ PageResponse.java
 │   	 ├─ ResponseMessageDTO.java
 │   	 └─ ErrorResponse.java
 ├─ model/
 │   ├─ Message.java
 │   └─ Room.java
 │   └─ MessageType.java
 │   └─ NotificationType.java
 ├─ security/
 │   ├─ JwtService.java
 │   └─ SecurityConfig.java
 ├─ websocket/
 │   ├─ WebSocketConfig.java
 │   ├─ UserHandshakeInterceptor.java
 └─ repository/
     ├─ MessageRepository.java
     └─ RoomRepository.java
```
**Estrutura dde arquivos:**
- **`RedisConfig.java`**
	- Define o bean `RedisTemplate<String, Object>` com serialização JSON via `GenericJackson2JsonRedisSerializer`. Garante que objetos Java sejam convertidos para JSON ao serem armazenados no Redis e reconstruídos corretamente na leitura.
- **`ChatController.java`**: Controller STOMP — processa mensagens WebSocket recebidas via `@MessageMapping`. Trata os eventos:
	- `chat.addUser` — entrada de usuário em uma sala
	- `chat.removeUser` — saída de usuário de uma sala
	- `chat.sendPublic` — envio de mensagem pública em uma sala
	- `chat.sendPrivate` — envio de mensagem privada entre dois usuários
	Faz o dispatch das respostas via `SimpMessagingTemplate`.
- **`MessageController.java`**
	- Controller REST — expõe endpoints HTTP para consulta de histórico de mensagens. Fornece listagem paginada de mensagens por sala (`GET /room/{roomId}`), histórico paginado de mensagens privadas entre dois usuários e busca das mensagens recentes via cache Redis.
- **`RoomController.java`**
	- Controller REST — gerencia operações de sala via HTTP. Expõe criação de salas (`POST /rooms`), listagem paginada de salas (`GET /rooms`) e listagem paginada de membros por sala (`GET /rooms/{roomId}/members`).
- **`RoomService.java`**
	- Contém a lógica de negócio das salas. Usa `MongoTemplate` para operações atômicas: `addToSet` para adicionar membros sem race condition e `pull` para remover. Usa `RoomRepository` para consultas e verificações de existência.
- **`MessageService.java`**
	- Contém a lógica de negócio das mensagens. Persiste mensagens no MongoDB, mantém cache Redis das últimas 50 mensagens por sala (com TTL) e fornece fallback para o banco quando o cache está vazio ou indisponível.
- **`CreateRoomDTO.java`**
	- Carrega o título da nova sala enviado pelo cliente na criação via REST. Contém validação `@NotBlank`.
- **`PrivateMessageDTO.java`**
	- Carrega o conteúdo e o ID do destinatário de uma mensagem privada enviada via WebSocket. Validações de `@NotBlank` e `@Size`.
- **`PublicMessageDTO.java`**
	- Carrega o ID da sala e o conteúdo de uma mensagem pública enviada via WebSocket. Validações de `@NotBlank` e `@Size`.
- **`UserNotificationDTO.java`**
	- Carrega o ID da sala e o tipo de evento enviado pelo cliente ao entrar ou sair de uma sala via WebSocket. O tipo é validado pelo enum `NotificationType`
- **`ResponseMessageDTO.java`**
	- Representa uma mensagem serializada para o cliente. Campos: `id`, `type` (ROOM/PRIVATE), `roomId`, `senderId`, `recipientId`, `content` e `timestamp`.
- **`UserNotificationResponseDTO.java`**
	- Representa a notificação de presença em broadcast para os membros da sala quando um usuário entra ou sai. Campos: `type`, `userId`, `username`, `roomId`, `content` e `timestamp`.
- **`PageResponse.java`**
	- Wrapper genérico de paginação reutilizável para qualquer tipo de listagem. Encapsula: `content` (lista), `pageNumber`, `totalPages`, `totalElements` e `size`.
- **`ErrorResponse.java`**
	- Representa erros retornados ao cliente — tanto via REST (HTTP) quanto via WebSocket (fila `/queue/errors`). Campos: `message`, código de status HTTP e `timestamp`.
- **`Message.java`**
	- Documento MongoDB da coleção `messages`. Campos: `id`, `type`, `roomId`, `senderId`, `senderUsername`, `recipientId`, `content` e `timestamp`. Possui índices em `roomId`, `senderId`, `recipientId` e índice composto em `(roomId, timestamp)` para performance nas queries de histórico.
- **`Room.java`**
	- Documento MongoDB da coleção `rooms`. Campos: `id`, `title` (único, indexado) e `membersIds` (`Set<String>` de IDs de usuários). O índice único em `title` previne salas com nomes duplicados.
- **`MessageType.java`**
	- Enum que classifica o tipo de mensagem: `ROOM` (mensagem pública em sala) ou `PRIVATE` (mensagem direta entre dois usuários). Usado em `Message` para distinguir os dois fluxos de persistência e roteamento.
- **`NotificationType.java`**
	- Enum que classifica o tipo de notificação de presença: `JOIN` (usuário entrou na sala) ou `LEAVE` (usuário saiu da sala). Usado em `UserNotificationDTO` e `UserNotificationResponseDTO`.
- **`JwtService.java`**
	- Utilitário para parsing de tokens JWT emitidos pelo Auth Service. Extrai claims do token (`userId`, `username`), valida a assinatura usando a chave secreta compartilhada via Config Server e verifica a expiração.
- **`SecurityConfig.java`**
	- Configura o Spring Security: desabilita CSRF, define endpoints REST como autenticados via JWT (`oauth2ResourceServer`), e libera o endpoint WebSocket (`/ws/chat`) para que o `UserHandshakeInterceptor` trate a autenticação manualmente.
- **`WebSocketConfig.java`**
	- Configura o broker STOMP com relay externo no RabbitMQ (via plugin STOMP), os prefixos de destino (`/app` para controllers, `/topic` e `/queue` para subscriptions do cliente) e registra o endpoint de conexão `/ws/chat` com o `UserHandshakeInterceptor`.
- **`UserHandshakeInterceptor.java`**
	- Intercepta a requisição HTTP de upgrade para WebSocket. Extrai e valida o JWT do parametro da requisição (`serveltRequest.getParameter("token")`, e injeta `userId` e `username` nos atributos da sessão WebSocket. Esses atributos são consumidos pelos handlers STOMP no `ChatController`.
- **`MessageRepository.java`**
	- Interface Spring Data MongoDB para a coleção de mensagens. Define as queries: `findByRoomId` (paginado, para histórico de sala), `findPrivateConversation` (entre dois usuários, paginado) e `findTop50ByRoomIdOrderByTimestampDesc` (para popular o cache Redis).
- **`RoomRepository.java`**
	- Interface Spring Data MongoDB para a coleção de salas. Fornece: `existsByTitle` (verificação de duplicidade na criação), `findById`, `existsById` e `findAll` com `Pageable` para listagem paginada.

### **Auth Service**
```markdown
auth/
 ├─ config/
 │   ├─ ApplicationConfig.java
 │   ├─ SecurityConfig.java
 │   └─ JwtAuthFilter.java
 ├─ controller/
 │   └─ AuthController.java
 │   └─ UserController.java
 ├─ service/
 │   ├─ AuthService.java
 │   ├─ JwtService.java
 │   └─ UserDetailsServiceImpl.java
 ├─ dto/
 │   ├─ request/
 │   │   ├─ CreateUserDTO.java
 │   │   ├─ ChangePasswordRequest.java
 │   │   ├─ RefreshRequest.java
 │   │   ├─ UpdateProfileRequest.java
 │   │   └─ LoginUserDTO.java
 │   └─ response/
 │       ├─ ErrorApiResponse.java
 │       ├─ AuthTokenDTO.java
 │       └─ UserResponseDTO.java
 ├─ entity/
 │   └─ User.java
 │   └─ Role.java
 ├─ exception/
 │   ├─ EmailAlreadyExistsException.java
 │   └─ GlobalExceptionHandler.java
 ├─ repository/
 │   └─ UserRepository.java
 ├─ service/
 │   └─ AuthService.java
 │   └─ JwtService.java
 │   └─ UserDetailsServiceImpl.java
 ├─ AuthServiceApplication.java
 └─ resources/
	├─ application.properties
	└─ db
		└─ migration
			├─ V1__create_users_table.sql
			├─ V2__create_correct_users_table.sql
			├─ V3__rename_column.sql
```
**Estrutura de arquivos**
- **`ApplicationConfig.java`** Define os beans de infraestrutura de segurança reutilizados pelo Spring Security: `PasswordEncoder` (BCrypt com strength 12), `AuthenticationProvider` (DaoAuthenticationProvider configurado com `UserDetailsServiceImpl` e o encoder) e `AuthenticationManager` exposto como bean para uso no `AuthService`.
- **`SecurityConfig.java`** Configura o `SecurityFilterChain`: desabilita CSRF (API stateless), define os endpoints públicos via `permitAll`, aplica `SessionCreationPolicy.STATELESS`, registra o `AuthenticationProvider` e adiciona o `JwtAuthFilter` antes do filtro padrão do Spring. Também configura CORS e os handlers de resposta JSON para erros `401` e `403`.
- **`JwtAuthFilter.java`** Filtro executado uma vez por requisição (`OncePerRequestFilter`). Extrai o token do cabeçalho `Authorization: Bearer`, valida assinatura e expiração via `JwtService`, carrega o usuário e popula o `SecurityContextHolder`. Se o token for inválido ou ausente, interrompe o filtro com `401` — a requisição não continua.
- **`AuthController.java`** Controller REST — expõe os endpoints públicos de autenticação, todos sob `/api/v1/auth`:
	- `POST /register` — cria novo usuário e retorna par de tokens
	- `POST /login` — autentica credenciais e retorna par de tokens
	- `POST /refresh` — renova o access token a partir de um refresh token válido
- **`UserController.java`** Controller REST — expõe os endpoints protegidos de gerenciamento de usuário, sob `/api/v1/users`:
	- `GET /me` — retorna dados do usuário autenticado
	- `PATCH /me` — atualiza o perfil do usuário autenticado
	- `PUT /me/password` — altera a senha do usuário autenticado
- **`AuthService.java`**: Contém a lógica de autenticação e registro. Delega a verificação de credenciais ao `AuthenticationManager` do Spring Security (não verifica senha manualmente). No registro, constrói a entidade `User`, encoda a senha com BCrypt e persiste — capturando `DataIntegrityViolationException` como fallback para e-mail duplicado. Emite pares de tokens via método privado `issueTokens`, que delega ao `JwtService`.
- **`JwtService.java`**: Responsável por toda a lógica de tokens JWT. Gera access tokens e refresh tokens com claims distintas (`type: "access"` / `type: "refresh"`), valida assinatura e expiração, extrai claims individuais (`sub`, `userId`) e expõe `isRefreshToken()` para distinguir os dois tipos. A chave HMAC-SHA256 é validada em startup via `@PostConstruct` — a aplicação falha imediatamente se a chave tiver menos de 32 bytes.
- **`UserDetailsServiceImpl.java`**: Implementação do contrato `UserDetailsService` do Spring Security. Realiza apenas a busca de usuário por email via `UserRepository` para uso interno do `AuthenticationManager`. Não é injetado diretamente em controllers ou outros services.
- **`UserService.java`** Contém a lógica de negócio de perfil e administração de usuários. Fornece: busca por email e por ID, listagem paginada, atualização de perfil (`username`), troca de senha com verificação da senha atual e desativação de conta (soft delete via `enabled = false`).
- **`CreateUserDTO.java`** Carrega os dados de registro: `username`, `email` e `password`. Validações: `@NotBlank` em todos os campos, `@Email` no email e `@Size(min=8, max=72)` na senha.
- **`LoginUserDTO.java`** Carrega as credenciais de login: `email` e `password`. Validações: `@NotBlank` e `@Email` no email, `@NotBlank` na senha.
- **`RefreshRequest.java`** Carrega o refresh token para renovação. Validação: `@NotBlank`.
- **`UpdateProfileRequest.java`** Carrega o novo `username` para atualização de perfil. Validação: `@NotBlank`.
- **`ChangePasswordRequest.java`** Carrega `currentPassword` e `newPassword` para troca de senha. Validações: `@NotBlank` em ambos, `@Size(min=8, max=72)` na nova senha.
- **`AuthTokenDTO.java`** Retornado nos endpoints de autenticação e renovação. Campos: `accessToken`, `refreshToken` e `expiresIn` (tempo de expiração do access token em milissegundos). Construído via factory method `AuthTokenDTO.of(...)`.
- **`UserResponseDTO.java`** Representa os dados públicos de um usuário: `id` (UUID), `email` e `username`. Nunca expõe senha ou campos internos de controle de conta. Construído via factory method `UserResponseDTO.from(User)`.
- **`ErrorApiResponse.java`** Representa erros retornados ao cliente. Campos: `status` (código HTTP), `message` (descrição do erro), `errors` (lista de erros de validação por campo, quando aplicável) e `timestamp`. Construído via factory method `ErrorApiResponse.of(...)`.
- **`User.java`** Entidade JPA mapeada para a tabela `users` no PostgreSQL. Implementa `UserDetails` para integração com o Spring Security. Campos principais: `id` (UUID gerado), `username` (único), `email` (único), `password` (hash BCrypt), `role` (enum `Role`), `enabled`, campos de controle de conta e `createdAt` (auditoria imutável). O método `getAuthorities()` converte o `role` em `SimpleGrantedAuthority` com prefixo `ROLE_`.
- **`Role.java`** Enum que define os níveis de acesso: `USER` (padrão para novos registros) e `ADMIN` (acesso às operações administrativas). Persistido como `VARCHAR` no banco via `@Enumerated(EnumType.STRING)`.
- **`UserRepository.java`** Interface Spring Data JPA para a tabela `users`. Fornece: `findByEmail(String email)` retornando `Optional<User>` — usado tanto pela autenticação quanto pelas operações de perfil.
- **`GlobalExceptionHandler.java`** Intercepta todas as exceções via `@RestControllerAdvice` e serializa respostas de erro no formato `ErrorApiResponse`. Trata: exceções de domínio customizadas, erros de validação (`MethodArgumentNotValidException`), exceções do Spring Security (`BadCredentialsException`, `AccessDeniedException`) e exceções JWT. Possui handler genérico para `Exception` que loga internamente e retorna `500` sem expor detalhes ao cliente.

### API Gateway
```markdown
api-gateway/
 ├─ src/
 │   ├─ main/
 │   │   ├─ java/
 │   │   │   └─ chat/
 │   │   │       └─ api_gateway/
 │   │   │           └─ ApiGatewayApplication.java
 │   │   └─ resources/
 │   │       └─ application.properties
 ├─ pom.xml
```

### Config Server
```markdown
config-server/
 ├─ src/
 │   ├─ main/
 │   │   ├─ java/
 │   │   │   └─ chat/
 │   │   │       └─ config_server/
 │   │   │           └─ ConfigServerApplication.java
 │   │   └─ resources/
 │   │       └─ application.properties
 ├─ pom.xml
```

### Service Discovery
```markdown
eureka-server/
 ├─ src/
 │   ├─ main/
 │   │   ├─ java/
 │   │   │   └─ chat/
 │   │   │       └─ eureka_server/
 │   │   │           └─ EurekaServerApplication.java
 │   │   └─ resources/
 │   │       └─ application.properties
 ├─ pom.xml
```

---
## Fluxos principais
### Registro de Usuário
1. Cliente envia `POST /api/v1/auth/register` com `CreateUserDTO`.
2. `AuthController` valida o payload via `@Valid` e delega ao `AuthService`.
3. `AuthService` constrói a entidade `User`, encoda a senha com BCrypt e chama `userRepository.save()`.
4. Em caso de violação de constraint (email duplicado), captura `DataIntegrityViolationException` e lança `EmailAlreadyExistsException` → resposta `409`.
5. `AuthService` chama `issueTokens()`, que gera access token e refresh token via `JwtService`.
6. Retorna `AuthTokenDTO` com `201 Created`.

### Login
1. Cliente envia `POST /api/v1/auth/login` com `LoginUserDTO`.
2. `AuthController` valida o payload e delega ao `AuthService`.
3. `AuthService` constrói um `UsernamePasswordAuthenticationToken` e chama `authenticationManager.authenticate()`.
4. O Spring Security executa internamente: carrega o usuário via `UserDetailsServiceImpl.loadUserByUsername(email)`, verifica o hash da senha com BCrypt e valida o estado da conta.
5. Em caso de credenciais inválidas, o Spring lança `BadCredentialsException` → `GlobalExceptionHandler` retorna `401`.
6. `AuthService` extrai o `UserDetails` do resultado, gera os tokens e retorna `AuthTokenDTO` com `200 OK`.

### Renovação de Token
1. Cliente envia `POST /api/v1/auth/refresh` com `RefreshRequest`.
2. `AuthService` verifica via `JwtService.isRefreshToken()` se o token é do tipo correto.
3. Extrai o email do subject do token e busca o usuário no banco.
4. Valida assinatura e expiração via `JwtService.isTokenValid()`.
5. Emite novo par de tokens e retorna `AuthTokenDTO` com `200 OK`.

### Requisição Autenticada
1. Cliente envia qualquer request com cabeçalho `Authorization: Bearer {accessToken}`.
2. `JwtAuthFilter` extrai o token, valida assinatura e expiração via `JwtService`.
3. Carrega o usuário via `UserDetailsServiceImpl` e popula o `SecurityContextHolder`.
4. O Spring Security libera o acesso ao endpoint conforme as regras de autorização configuradas.
5. Controllers acessam o usuário autenticado via `Authentication authentication` injetado pelo Spring.

### Troca de Senha
1. Cliente envia `PUT /api/v1/users/me/password` com `ChangePasswordRequest` e Bearer token.
2. `UserController` extrai o email do `Authentication` e delega ao `UserService`.
3. `UserService` busca o usuário, verifica a senha atual com `passwordEncoder.matches()`.
4. Se incorreta, lança `InvalidPasswordException` → resposta `422`.
5. Encoda a nova senha e persiste com `repository.save()`.

### Mensagem Pública
1. Cliente envia frame STOMP para `/app/chat.sendPublic` com `PublicMessageDTO`.
2. `ChatController` valida o payload (`@Valid`) e extrai `userId`/`username` dos atributos da sessão.
3. `MessageService` persiste a mensagem no MongoDB com `type = ROOM`.
4. `MessageService` atualiza o cache Redis da sala (lista com trim para 50 itens + TTL).
5. `ChatController` faz broadcast via `SimpMessagingTemplate` para `/topic/rooms.{roomId}`.
6. Todas as instâncias do serviço recebem a mensagem via relay RabbitMQ e entregam aos seus clientes conectados.

### Mensagem Privada
1. Cliente envia frame STOMP para `/app/chat.sendPrivate` com `PrivateMessageDTO`.
2. `ChatController` extrai `senderId` da sessão e usa `messageDTO.recipientId()` como destinatário.
3. `MessageService` persiste a mensagem com `type = PRIVATE` e `roomId = null`.
4. `ChatController` entrega ao destinatário via `convertAndSendToUser(recipientId, "/queue/private", ...)`.
5. `ChatController` entrega uma cópia ao remetente para confirmação de envio.

### Entrada em Sala
1. Cliente envia frame STOMP para `/app/chat.addUser` com `UserNotificationDTO`.
2. `ChatController` extrai `userId` e `username` da sessão WebSocket.
3. `RoomService` executa `addToSet` atômico via `MongoTemplate` — idempotente, sem race condition.
4. `ChatController` faz broadcast de `UserNotificationResponseDTO` para `/topic/rooms.{roomId}`.

