# Real-Time Chat - Sistema de Chat em Tempo Real com Microsserviços

Este é um sistema de chat em tempo real robusto e escalável construído com uma arquitetura de microsserviços usando Spring Boot e tecnologias modernas.

## 📝 Descrição

O projeto implementa um sistema de chat em tempo real com suporte a salas de chat públicas e mensagens privadas. A arquitetura de microsserviços permite alta disponibilidade, escalabilidade horizontal e manutenção independente dos componentes.

## Funcionalidades Principais

- Autenticação e autorização com JWT
- Criação e gerenciamento de salas de chat
- Mensagens em tempo real usando WebSocket
- Suporte a mensagens privadas entre usuários
- Histórico de mensagens
- Lista de usuários online em cada sala
- Escalabilidade horizontal dos serviços de chat

## Tecnologias Utilizadas

### Backend
- **Spring Boot** - Framework base para os microsserviços
- **Spring Cloud** - Conjunto de ferramentas para microsserviços
  - Netflix Eureka (Service Discovery)
  - Spring Cloud Gateway
  - Spring Cloud Config
- **Spring Security** - Segurança e autenticação
- **Spring WebSocket** - Comunicação em tempo real
- **MongoDB** - Armazenamento de mensagens e salas
- **PostgreSQL** - Armazenamento de dados de usuários
- **Redis** - Cache e gerenciamento de sessões
- **RabbitMQ** - Message broker para comunicação entre serviços

## Frontend
- Em breve

### DevOps
- **Docker** - Containerização
- **Docker Compose** - Orquestração de containers

## Arquitetura

O sistema é composto pelos seguintes serviços:

### 1. API Gateway (Porta: 8080)
- Ponto de entrada único para clientes
- Roteamento de requisições REST e WebSocket
- Validação de JWT e segurança

### 2. Auth Service (Porta: 8083)
- Gerenciamento de usuários
- Autenticação e autorização
- Emissão e validação de JWT

### 3. Chat Service (Porta: 8082)
- Gerenciamento de salas de chat
- Comunicação WebSocket
- Persistência de mensagens
- Gerenciamento de presença de usuários

### 4. Service Discovery - Eureka (Porta: 8084)
- Registro e descoberta de serviços
- Balanceamento de carga

### 5. Config Server (Porta: 8085)
- Configurações centralizadas
- Gestão de ambientes (dev/prod)

## Guia de Instalação e Execução

### Pré-requisitos
- Docker e Docker Compose
- Java 17+
- Maven

### Passos para Execução

1. Clone o repositório:
```bash
git clone https://github.com/AlexReisC/real-time-chat.git
cd real-time-chat
```

2. Inicie os serviços de infraestrutura com Docker Compose:
```bash
docker-compose up -d
```

3. Execute os serviços Spring Boot:

Para o Auth Service:
```bash
cd auth-service
mvn spring-boot:run
```

Para o Chat Service:
```bash
cd chat-service
mvn spring-boot:run
```

## Endpoints Principais

### Auth Service
| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/auth/register` | Registrar novo usuário |
| POST | `/auth/login` | Autenticar usuário e gerar JWT |
| GET | `/auth/validate` | Validar token JWT |
| GET | `/auth/me` | Retornar informações do usuário |

### Chat Service
| Tipo | Endpoint | Descrição |
|------|----------|-----------|
| WS | `/ws/chat` | Conectar ao WebSocket |
| GET | `/chat/messages/{roomId}` | Buscar histórico de mensagens |
| POST | `/chat/rooms` | Criar nova sala |
| GET | `/chat/rooms` | Listar salas disponíveis |
| GET | `/chat/rooms/{roomId}/users` | Listar usuários da sala |

## Contribuição

Sinta-se à vontade para contribuir com o projeto. Abra uma issue ou envie um pull request com suas sugestões de melhorias.

## Licença

Este projeto está licenciado sob a licença MIT - veja o arquivo [LICENSE](LICENSE) para mais detalhes.