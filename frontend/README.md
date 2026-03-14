# Relay — Frontend

Interface de chat em tempo real construída com **React 18 + Vite**, parte da arquitetura de microsserviços Relay.

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| UI | React 18 |
| Build | Vite 5 |
| Estilo | CSS Modules + variáveis CSS |
| WebSocket | `@stomp/stompjs` (STOMP sobre WS) |
| HTTP | Fetch nativo |
| Fontes | Sora + DM Mono (Google Fonts) |

---

## Estrutura

```
src/
├── context/
│   └── AuthContext.jsx        # Estado global de autenticação (login, register, updateProfile)
├── hooks/
│   └── useWebSocket.js        # Hook STOMP — conecta ao Chat Service via API Gateway
├── services/
│   └── api.js                 # Cliente HTTP para Auth, Rooms e Messages
├── components/
│   ├── Modal.jsx / .module.css
│   ├── FormInput.jsx / .module.css
│   ├── Sidebar.jsx / .module.css     # Lista de salas + DMs + modais de sala e perfil
│   └── ChatView.jsx / .module.css    # Mensagens + painel de membros + input
├── pages/
│   ├── LoginPage.jsx / .module.css   # Login e cadastro
│   └── ChatPage.jsx / .module.css    # Orquestrador principal
├── styles/
│   └── globals.css            # Tokens de design (variáveis CSS) + reset
├── App.jsx                    # Roteamento: não autenticado → Login | autenticado → Chat
└── main.jsx                   # Entry point React
```

---

## Como rodar

```bash
# 1. Instalar dependências
npm install

# 2. Copiar variáveis de ambiente
cp .env.example .env

# 3. Iniciar em desenvolvimento (porta 3000)
npm run dev
```

O `vite.config.js` já configura proxy para o API Gateway em `localhost:8080`:
- `/auth/**` → `http://localhost:8080`
- `/chat/**` → `http://localhost:8080`
- `/ws`      → `ws://localhost:8080` (WebSocket upgrade)

---

## Integração com o backend

### 1. Autenticação (AuthContext.jsx)

Substitua os mocks pelos chamadas reais:

```js
// Login
const res = await fetch('/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password }),
});
const { token, user } = await res.json();
```

### 2. WebSocket / STOMP (useWebSocket.js)

Descomente o bloco `Client` do `@stomp/stompjs`:

```js
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'ws://localhost:3000/ws',   // proxy → Gateway → Chat Service
  connectHeaders: { Authorization: `Bearer ${token}` },
  reconnectDelay: 5000,
  onConnect: () => {
    client.subscribe('/topic/rooms.*', frame => onMessage(JSON.parse(frame.body)));
    client.subscribe('/user/queue/private', frame => onMessage(JSON.parse(frame.body)));
  },
});
client.activate();
```

### 3. REST (services/api.js)

Todos os endpoints já estão mapeados. Basta remover os mocks de `ChatPage.jsx` e chamar a API:

```js
// Carregar salas ao montar a página
useEffect(() => {
  roomsApi.list().then(setRooms);
}, []);

// Histórico ao abrir chat
messagesApi.roomHistory(roomId).then(msgs =>
  setMessages(prev => ({ ...prev, [`r${roomId}`]: msgs }))
);
```

---

## Build para produção

```bash
npm run build
# Artefatos em dist/ — sirva com nginx ou qualquer CDN estático
```
