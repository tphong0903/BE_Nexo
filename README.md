# Nexo Backend (Social Network Microservices)

**Nexo** là hệ thống Backend cho nền tảng mạng xã hội, được xây dựng dựa trên kiến trúc **Microservices** hiện đại, sử dụng **Spring Boot**, **gRPC** để giao tiếp giữa các service, và **Kafka** cho xử lý bất đồng bộ.

## 🚀 Tính năng nổi bật

* **Kiến trúc Microservices:** Tách biệt các nghiệp vụ thành các service độc lập, dễ dàng mở rộng.
* **Giao tiếp hiệu năng cao:** Sử dụng **gRPC** (Google Remote Procedure Call) cho giao tiếp nội bộ giữa các service thay vì REST truyền thống.
* **Video Streaming:** Hỗ trợ upload và phát video chuẩn **HLS (HTTP Live Streaming)** (trong `upload-file-service`).
* **Tìm kiếm thông minh:** Tích hợp **Meilisearch** để tìm kiếm người dùng và nội dung nhanh chóng.
* **Real-time Chat:** Nhắn tin thời gian thực qua **WebSocket** (trong `messaging-service` & `notification-service`).
* **Bảo mật & Định danh:** Sử dụng **Keycloak** (OIDC/OAuth2) để quản lý xác thực và phân quyền tập trung.
* **Event-Driven:** Sử dụng **Apache Kafka** để xử lý các sự kiện (Post created, User registered...) một cách bất đồng bộ.

## 🏗️ Kiến trúc hệ thống

Dự án bao gồm các module chính:

| Service | Mô tả | Port Mặc định |
| :--- | :--- | :--- |
| **eureka-server** | Service Discovery (đăng ký và khám phá dịch vụ). | `8761` |
| **api-gateway** | Cổng vào duy nhất (Entry point), định tuyến request, xác thực JWT. | `8080` (hoặc `8888`) |
| **auth-service** | Wrapper cho Keycloak, xử lý Login, Register, Forgot Password. | `808x` |
| **user-service** | Quản lý Profile, Follow, Block, Search User (Meilisearch). | `808x` |
| **post-service** | Quản lý Bài viết, Story, Reels, HashTags. | `808x` |
| **interaction-service** | Quản lý Like, Comment, thả tim. | `808x` |
| **feed-service** | Tổng hợp News Feed cho người dùng (Aggregator). | `808x` |
| **messaging-service** | Chat 1-1, Chat nhóm, Trạng thái online (Presence). | `808x` |
| **notification-service** | Thông báo đẩy (Push Notification) qua WebSocket/Email. | `808x` |
| **upload-file-service** | Upload ảnh/video (Cloudinary/MinIO), Transcode Video sang HLS. | `808x` |

## 🛠️ Công nghệ sử dụng

* **Core:** Java 17+, Spring Boot 3.x, Spring Cloud.
* **Database:** PostgreSQL (Dữ liệu chính), Redis (Cache & Pub/Sub).
* **Message Broker:** Apache Kafka.
* **Search Engine:** Meilisearch.
* **Identity Provider:** Keycloak.
* **Communication:** REST API (Client -> Gateway), gRPC (Service <-> Service).
* **Containerization:** Docker, Docker Compose.

## ⚙️ Cài đặt và Chạy ứng dụng

### 1. Yêu cầu (Prerequisites)
* Java JDK 17 trở lên.
* Maven 3.8+.
* Docker & Docker Compose.

### 2. Khởi chạy hạ tầng (Infrastructure)
Sử dụng Docker Compose để chạy các service nền tảng (PostgreSQL, Kafka, Redis, Keycloak, Meilisearch...):

```bash
docker-compose up -d
