# Distributed API Rate Limiter & Resilience System

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7.0-red.svg)](https://redis.io/)
[![Resilience4j](https://img.shields.io/badge/Resilience4j-2.2.0-blue.svg)](https://resilience4j.readme.io/)
[![Docker](https://img.shields.io/badge/Docker-Supported-blue.svg)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A high-throughput, distributed API rate limiter engineered with **Spring Boot**, **Redis**, and custom **Lua Scripts** using the **Sliding Window Log Algorithm**. The system is designed to protect backend microservices against traffic spikes and DDoS attacks while maintaining system availability through **Resilience4j Circuit Breakers**.

---

## Key Features

* **Distributed Rate Limiting:** Global request tracking across multi-instance microservice deployments via centralized Redis memory storage.
* **Sliding Window Log Algorithm:** Precise timestamp tracking down to the millisecond using Redis Sorted Sets (`ZSET`), eliminating time-boundary request burst bugs common in fixed-window algorithms.
* **Thread-Safe & Race Condition Free:** Enforces atomic operations inside Redis via native Lua scripts, guaranteeing sub-millisecond execution without application-level locking overhead.
* **Fault Tolerance & Graceful Degradation:** Integrates Resilience4j Circuit Breaker patterns to fall back safely during Redis service outages or network failures.
* **Multi-Tenant Key Isolation:** Restricts traffic independently on a per-user or per-API-key basis (`X-USER-ID` header routing).
* **Container Ready:** Includes orchestration files (`docker-compose.yml`) for seamless deployment across local and production environments.

---

## Architecture Overview

### System Workflow

```mermaid
graph TD
    A[Incoming Request<br/>Header: X-USER-ID] --> B[Spring Boot Application]
    B --> C[Execute Lua Script]
    C --> D[Redis Engine<br/>ZSET Sliding Window]
    
    D -->|Count < Limit| E[Allowed]
    D -->|Count >= Limit| F[HTTP 429<br/>Too Many Requests]
    
    E --> G[Resilience4j Circuit Breaker]
    G -->|Redis Healthy| H[Target API Resource]
    G -->|Redis Down| I[HTTP 503<br/>Service Fallback]

Sliding Window Execution Mechanics
When a request is evaluated:

Prune: Removes obsolete timestamp entries older than currentTime - windowSize (ZREMRANGEBYSCORE).

Count: Evaluates the active request volume remaining within the sliding frame (ZCARD).

Decide:

If count < max_limit: Registers request timestamp (ZADD), resets TTL (EXPIRE), and yields HTTP 200 OK.

If count >= max_limit: Denies execution and yields HTTP 429 Too Many Requests.

Tech Stack
Language & Framework: Java 17, Spring Boot 3.2.3 (Spring Web, Spring Data Redis, Spring AOP)

Data Store: Redis 7 (In-Memory Data Structure Store)

Scripting: Lua (Atomic Execution inside Redis)

Resilience: Resilience4j (Circuit Breaker & Fallback Handler)

Containerization: Docker & Docker Compose

Build Tool: Apache Maven
Directory Structure

distributed-api-rate-limiter/
├── src/
│   ├── main/
│   │   ├── java/com/example/rate_limiter/
│   │   │   ├── config/
│   │   │   │   └── RedisConfig.java          # Redis Script Loader Beans
│   │   │   ├── controller/
│   │   │   │   └── ApiController.java        # REST Controller & Circuit Breaker
│   │   │   ├── service/
│   │   │   │   └── RateLimiterService.java   # Core Business Logic Execution
│   │   │   └── RateLimiterApplication.java   # Spring Boot Main Entry Point
│   │   └── resources/
│   │       ├── application.properties        # Server & Application Configs
│   │       └── scripts/
│   │           └── sliding_window.lua        # Atomic Sliding Window Script
│   └── test/                                 # Unit & Integration Tests
├── Dockerfile                                # Application Container Blueprint
├── docker-compose.yml                        # Docker Multi-Container Configuration
├── mvnw                                      # Maven Wrapper
└── pom.xml                                   # Dependency Management File
Getting Started
Prerequisites
Ensure the following tools are installed on your environment:

Java Development Kit (JDK 17+)

Git

Docker Desktop or Homebrew (for running Redis locally)

Local Setup & Installation
Clone the Repository:git clone [https://github.com/](https://github.com/)<your-username>/distributed-api-rate-limiter.git
cd distributed-api-rate-limiter

Start Redis Container / Service:

Via Docker Compose:docker compose up -d
Via Homebrew (macOS alternative):brew install redis
brew services start redis
Build and Launch the Spring Boot Server:./mvnw clean spring-boot:run
The server will start running at http://localhost:8080.

Verification & API Usage
Default Rate Limit Rule: Maximum 3 requests per 10-second window per User ID.

1. Verification under Permitted Threshold
Fire 3 rapid requests using curl:curl -i -H "X-USER-ID: testuser" http://localhost:8080/api/test
Response:HTTP/1.1 200 OK
Content-Type: text/plain;charset=UTF-8

Request successful for user: testuser
2. Rate Limit Rejection Check
Fire a 4th request within the 10-second window:curl -i -H "X-USER-ID: testuser" http://localhost:8080/api/test
Response:HTTP/1.1 429 Too Many Requests
Content-Type: text/plain;charset=UTF-8

Rate limit exceeded! Maximum 3 requests per 10 seconds allowed.
3. Circuit Breaker Fallback Check
Stop Redis to trigger system resilience handling:brew services stop redis  # or: docker stop redis_ratelimiter
Send a request to the endpoint:curl -i -H "X-USER-ID: testuser" http://localhost:8080/api/test
Response:HTTP/1.1 503 Service Unavailable
Content-Type: text/plain;charset=UTF-8

Redis or Downstream issue detected. Request handled by Resilience4j Fallback.
License
Distributed under the MIT License. See LICENSE for details.