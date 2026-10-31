# Environment Variables - Foodzie Backend Services

This document provides a comprehensive list of all environment variables used across all backend services' `application.properties` files, organized by service.

## Format
- `${VAR_NAME}` - Environment variable required (no default)
- `${VAR_NAME:defaultValue}` - Environment variable with default fallback

---

## 1. API Gateway

**Service:** `api-gateway` | **Port:** `8080` | **Config Path:** `backend/api-gateway/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `PORT` | `${PORT:8080}` | `8080` | Server port |
| `API_GATEWAY_HOST` | `${API_GATEWAY_HOST:...}` | `food-delievery-app-api-gateway.onrender.com` | Eureka instance hostname |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared JWT secret)

---

## 2. Auth Service

**Service:** `auth-service` | **Port:** `8082` | **Config Path:** `backend/auth-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `PORT` | `${PORT:8082}` | `8082` | Server port |
| `AUTH_SERVICE_HOST` | `${AUTH_SERVICE_HOST:...}` | `food-delievery-app-auth-service.onrender.com` | Eureka instance hostname |
| `GOOGLE_CLIENT_ID` | `${GOOGLE_CLIENT_ID:...}` | `464819426022-5b51s4qeg9ukikd7mheeid1h2vv6vus7.apps.googleusercontent.com` | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | `${GOOGLE_CLIENT_SECRET:...}` | `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` | Google OAuth2 client secret |
| `GITHUB_CLIENT_ID` | `${GITHUB_CLIENT_ID:...}` | `Ov23liK5WGnfqHHPoaFw` | GitHub OAuth2 client ID |
| `GITHUB_CLIENT_SECRET` | `${GITHUB_CLIENT_SECRET:...}` | `c00a3a291b3767d8c6222ff12b3971bccc7a5941` | GitHub OAuth2 client secret |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.jwt.access-token-expiration-ms` = `604800000`
- `app.jwt.refresh-token-expiration-ms` = `604800000`
- `app.jwt.issuer` = `foodzie-auth-service`
- `app.mfa.totp-issuer` = `Foodzie`
- `app.mfa.otp-expiry-minutes` = `5`
- `app.frontend.url` = `https://food-delievery-app-b6b3.vercel.app`
- `app.oauth.redirect-base` = `https://food-delievery-app-b6b3.vercel.app`
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,http://localhost:4200,https://food-delievery-app-b6b3.vercel.app`
- `app.kafka.topics.user-registered` = `user.registered`
- `app.audit.enabled` = `true`

**Note:** MongoDB credentials are hardcoded (see security concerns)

---

## 3. User Service

**Service:** `user-service` | **Port:** `8081` | **Config Path:** `backend/user-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `PORT` | `${PORT:8081}` | `8081` | Server port |
| `USER_SERVICE_HOST` | `${USER_SERVICE_HOST:...}` | `food-delievery-app-user-service.onrender.com` | Eureka instance hostname |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |
| `CLOUDINARY_CLOUD_NAME` | `${CLOUDINARY_CLOUD_NAME:...}` | `dryku5yoq` | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | `${CLOUDINARY_API_KEY:...}` | `397213128872371` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | `${CLOUDINARY_API_SECRET:...}` | `4GJqrpFfH5lBAJBxGQqdkddIXf8` | Cloudinary API secret |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,http://localhost:4200,https://food-delievery-app-b6b3.vercel.app`
- `app.kafka.topics.user-registered` = `user.registered`
- `spring.servlet.multipart.max-file-size` = `5MB`
- `spring.servlet.multipart.max-request-size` = `6MB`

**Note:** MongoDB credentials and Cloudinary credentials are hardcoded

---

## 4. Order Service

**Service:** `order-service` | **Port:** `8083` | **Config Path:** `backend/order-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `PORT` | `${PORT:8083}` | `8083` | Server port |
| `ORDER_SERVICE_HOST` | `${ORDER_SERVICE_HOST:...}` | `food-delievery-app-order-service.onrender.com` | Eureka instance hostname |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,http://localhost:4200,https://food-delievery-app-b6b3.vercel.app`
- `app.kafka.topics.order-events` = `order-events`
- `app.kafka.topics.payment-events` = `payment-events`
- `app.kafka.topics.delivery-events` = `delivery-events`

**Note:** MongoDB credentials are hardcoded

---

## 5. Payment Service

**Service:** `payment-service` | **Port:** `8084` | **Config Path:** `backend/payment-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `PORT` | `${PORT:8084}` | `8084` | Server port |
| `PAYMENT_SERVICE_HOST` | `${PAYMENT_SERVICE_HOST:...}` | `food-delievery-app-payment-service.onrender.com` | Eureka instance hostname |
| `RAZORPAY_KEY_ID` | `${RAZORPAY_KEY_ID:...}` | `rzp_test_SzcUUHfexIykCz` | Razorpay test key ID |
| `RAZORPAY_KEY_SECRET` | `${RAZORPAY_KEY_SECRET:...}` | `6d9YmKmQQf6F7wlY3ykYHSEN` | Razorpay test key secret |
| `RAZORPAY_WEBHOOK_SECRET` | `${RAZORPAY_WEBHOOK_SECRET:...}` | `Zm9vZHppZS13ZWJob29rLXNlY3JldC0yMDI2` | Razorpay webhook secret |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,http://localhost:4200,https://food-delievery-app-b6b3.vercel.app`
- `app.kafka.topics.order-events` = `order-events`
- `app.kafka.topics.payment-events` = `payment-events`
- `spring.kafka.listener.auto-startup` = `false`

**Note:** MongoDB credentials and Razorpay test keys are hardcoded

---

## 6. Cart Service

**Service:** `cart-service` | **Port:** `8086` | **Config Path:** `backend/cart-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `PORT` | `${PORT:8086}` | `8086` | Server port |
| `CART_SERVICE_HOST` | `${CART_SERVICE_HOST:...}` | `food-delievery-app-cart-service.onrender.com` | Eureka instance hostname |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,http://localhost:4200,https://food-delievery-app-b6b3.vercel.app`

**Note:** MongoDB credentials are hardcoded

---

## 7. Notification Service

**Service:** `notification-service` | **Port:** `8085` | **Config Path:** `backend/notification-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `NOTIFICATION_SERVICE_HOST` | `${NOTIFICATION_SERVICE_HOST:...}` | `food-delievery-app-notification-service.onrender.com` | Eureka instance hostname |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |
| `MAIL_USERNAME` | `${MAIL_USERNAME:...}` | `your@gmail.com` | Email account username (Gmail) |
| `MAIL_PASSWORD` | `${MAIL_PASSWORD:...}` | `your-app-password` | Email account password (Gmail app password) |
| `FIREBASE_CREDENTIALS_PATH` | `${FIREBASE_CREDENTIALS_PATH:...}` | `classpath:firebase-service-account.json` | Path to Firebase service account key |

**Static Values:**
- `app.mail.from` = `${MAIL_USERNAME}` (defaults to Gmail username)
- `app.kafka.topics.order-events` = `order-events`
- `app.kafka.topics.payment-events` = `payment-events`
- `spring.mail.host` = `smtp.gmail.com`
- `spring.mail.port` = `587`
- `spring.mail.properties.mail.smtp.auth` = `true`
- `spring.mail.properties.mail.smtp.starttls.enable` = `true`

**Note:** MongoDB credentials are hardcoded

---

## 8. Restaurant Service

**Service:** `restaurant-service` | **Port:** `8087` | **Config Path:** `backend/restaurant-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `PORT` | `${PORT:8087}` | `8087` | Server port |
| `RESTAURANT_SERVICE_HOST` | `${RESTAURANT_SERVICE_HOST:...}` | `food-delievery-app-restaurant-service.onrender.com` | Eureka instance hostname |
| `MONGODB_USERNAME` | `${MONGODB_USERNAME:...}` | `mdaazam0786` | MongoDB username |
| `MONGODB_PASSWORD` | `${MONGODB_PASSWORD:...}` | `aazam%40786` | MongoDB password |
| `MONGODB_HOST` | `${MONGODB_HOST:...}` | `cluster0.sczcxrl.mongodb.net` | MongoDB host |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |
| `CLOUDINARY_CLOUD_NAME` | `${CLOUDINARY_CLOUD_NAME:...}` | `dryku5yoq` | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | `${CLOUDINARY_API_KEY:...}` | `169252649454779` | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | `${CLOUDINARY_API_SECRET:...}` | `MQJC4PFLaQD6SHCt20-SNMJNb_Y` | Cloudinary API secret |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,http://localhost:4200,https://food-delievery-app-b6b3.vercel.app`
- `app.kafka.topics.restaurant-events` = `restaurant-events`
- `app.kafka.topics.user-registered` = `user.registered`
- `spring.servlet.multipart.max-file-size` = `10MB`
- `spring.servlet.multipart.max-request-size` = `10MB`

---

## 9. Search Service

**Service:** `search-service` | **Port:** `8089` | **Config Path:** `backend/search-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `PORT` | `${PORT:8089}` | `8089` | Server port |
| `SEARCH_SERVICE_HOST` | `${SEARCH_SERVICE_HOST:...}` | `food-delievery-app-search-service.onrender.com` | Eureka instance hostname |
| `MONGODB_USERNAME` | `${MONGODB_USERNAME:...}` | `mdaazam0786` | MongoDB username |
| `MONGODB_PASSWORD` | `${MONGODB_PASSWORD:...}` | `aazam%40786` | MongoDB password |
| `MONGODB_HOST` | `${MONGODB_HOST:...}` | `cluster0.sczcxrl.mongodb.net` | MongoDB host |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,http://localhost:4200,https://food-delievery-app-b6b3.vercel.app`
- `app.search.home-feed-radius-km` = `5.0`
- `app.search.text-search-radius-km` = `10.0`
- `app.search.max-results` = `50`
- `app.search.default-page-size` = `20`
- `app.search.geo-decay.scale-km` = `2.0`
- `app.search.geo-decay.offset-km` = `0.5`
- `app.search.geo-decay.decay` = `0.5`

---

## 10. Delivery Service

**Service:** `delivery-service` | **Port:** `8090` | **Config Path:** `backend/delivery-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `PORT` | `${PORT:8090}` | `8090` | Server port |
| `DELIVERY_SERVICE_HOST` | `${DELIVERY_SERVICE_HOST:...}` | `food-delievery-app-delivery-service.onrender.com` | Eureka instance hostname |
| `SPRING_DATA_REDIS_HOST` | `${SPRING_DATA_REDIS_HOST}` | *(required)* | Redis host |
| `SPRING_DATA_REDIS_PORT` | `${SPRING_DATA_REDIS_PORT}` | *(required)* | Redis port |
| `SPRING_DATA_REDIS_USERNAME` | `${SPRING_DATA_REDIS_USERNAME}` | *(required)* | Redis username |
| `SPRING_DATA_REDIS_PASSWORD` | `${SPRING_DATA_REDIS_PASSWORD}` | *(required)* | Redis password |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,http://localhost:4200,https://food-delievery-app-b6b3.vercel.app`
- `app.driver.geo-key-prefix` = `drivers:active`
- `app.driver.heartbeat-ttl-seconds` = `15`
- `app.driver.cleanup-interval-ms` = `10000`
- `app.driver.default-search-radius-km` = `5.0`
- `app.kafka.topics.order-events` = `order-events`
- `app.kafka.topics.delivery-events` = `delivery-events`

**Note:** MongoDB credentials are hardcoded

---

## 11. Location Update Service

**Service:** `location-update-service` | **Port:** `8091` | **Config Path:** `backend/location-update-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `LOCATION_UPDATE_SERVICE_HOST` | `${LOCATION_UPDATE_SERVICE_HOST:...}` | `food-delievery-app-location-update-service.onrender.com` | Eureka instance hostname |
| `SPRING_DATA_REDIS_HOST` | `${SPRING_DATA_REDIS_HOST}` | *(required)* | Redis host |
| `SPRING_DATA_REDIS_PORT` | `${SPRING_DATA_REDIS_PORT}` | *(required)* | Redis port |
| `SPRING_DATA_REDIS_USERNAME` | `${SPRING_DATA_REDIS_USERNAME}` | *(required)* | Redis username |
| `SPRING_DATA_REDIS_PASSWORD` | `${SPRING_DATA_REDIS_PASSWORD}` | *(required)* | Redis password |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |

**Static Values:**
- `app.kafka.topics.location-pings` = `driver-location-pings`

---

## 12. Delivery Matching Service

**Service:** `delivery-matching-service` | **Port:** `8092` | **Config Path:** `backend/delivery-matching-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `DELIVERY_MATCHING_SERVICE_HOST` | `${DELIVERY_MATCHING_SERVICE_HOST:...}` | `food-delievery-app-delivery-matching-service.onrender.com` | Eureka instance hostname |
| `MONGODB_USERNAME` | `${MONGODB_USERNAME:...}` | `mdaazam0786` | MongoDB username |
| `MONGODB_PASSWORD` | `${MONGODB_PASSWORD:...}` | `aazam%40786` | MongoDB password |
| `MONGODB_HOST` | `${MONGODB_HOST:...}` | `cluster0.sczcxrl.mongodb.net` | MongoDB host |
| `SPRING_DATA_REDIS_HOST` | `${SPRING_DATA_REDIS_HOST}` | *(required)* | Redis host |
| `SPRING_DATA_REDIS_PORT` | `${SPRING_DATA_REDIS_PORT}` | *(required)* | Redis port |
| `SPRING_DATA_REDIS_USERNAME` | `${SPRING_DATA_REDIS_USERNAME}` | *(required)* | Redis username |
| `SPRING_DATA_REDIS_PASSWORD` | `${SPRING_DATA_REDIS_PASSWORD}` | *(required)* | Redis password |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,http://localhost:4200,https://food-delievery-app-b6b3.vercel.app`
- `app.matching.initial-radius-km` = `3.0`
- `app.matching.radius-steps` = `3.0,5.0,8.0,10.0`
- `app.matching.max-candidates` = `5`
- `app.matching.heartbeat-key-prefix` = `driver:heartbeat:`
- `app.matching.geo-key-prefix` = `drivers:active:`
- `app.kafka.topics.order-events` = `order-events`
- `app.kafka.topics.delivery-events` = `delivery-events`

---

## 13. Order Fulfillment Service

**Service:** `order-fulfillment-service` | **Port:** `8093` | **Config Path:** `backend/order-fulfillment-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `PORT` | `${PORT:8093}` | `8093` | Server port |
| `ORDER_FULFILLMENT_SERVICE_HOST` | `${ORDER_FULFILLMENT_SERVICE_HOST:...}` | `food-delievery-app-order-fulfillment-service.onrender.com` | Eureka instance hostname |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,https://food-delievery-app-b6b3.vercel.app`
- `app.kafka.topics.order-events` = `order-events`

**Note:** MongoDB credentials are hardcoded

---

## 14. Delivery Acceptance Service

**Service:** `delivery-acceptance-service` | **Port:** `8095` | **Config Path:** `backend/delivery-acceptance-service/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `DELIVERY_ACCEPTANCE_SERVICE_HOST` | `${DELIVERY_ACCEPTANCE_SERVICE_HOST:...}` | `food-delievery-app-delivery-acceptance-service.onrender.com` | Eureka instance hostname |
| `MONGODB_USERNAME` | `${MONGODB_USERNAME:...}` | `mdaazam0786` | MongoDB username |
| `MONGODB_PASSWORD` | `${MONGODB_PASSWORD:...}` | `aazam%40786` | MongoDB password |
| `MONGODB_HOST` | `${MONGODB_HOST:...}` | `cluster0.sczcxrl.mongodb.net` | MongoDB host |
| `SPRING_DATA_REDIS_HOST` | `${SPRING_DATA_REDIS_HOST}` | *(required)* | Redis host |
| `SPRING_DATA_REDIS_PORT` | `${SPRING_DATA_REDIS_PORT}` | *(required)* | Redis port |
| `SPRING_DATA_REDIS_USERNAME` | `${SPRING_DATA_REDIS_USERNAME}` | *(required)* | Redis username |
| `SPRING_DATA_REDIS_PASSWORD` | `${SPRING_DATA_REDIS_PASSWORD}` | *(required)* | Redis password |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,https://food-delievery-app-b6b3.vercel.app`
- `app.kafka.topics.delivery-events` = `delivery-events`
- `app.kafka.topics.order-events` = `order-events`
- `app.acceptance.offer-ttl-seconds` = `30`

---

## 15. WebSocket Manager

**Service:** `websocket-manager` | **Port:** `8094` | **Config Path:** `backend/websocket-manager/src/main/resources/application.properties`

| Variable | Pattern | Default Value | Purpose |
|----------|---------|---------------|---------|
| `WEBSOCKET_MANAGER_HOST` | `${WEBSOCKET_MANAGER_HOST:...}` | `food-delievery-app-websocket-manager.onrender.com` | Eureka instance hostname |
| `SPRING_DATA_REDIS_HOST` | `${SPRING_DATA_REDIS_HOST}` | *(required)* | Redis host |
| `SPRING_DATA_REDIS_PORT` | `${SPRING_DATA_REDIS_PORT}` | *(required)* | Redis port |
| `SPRING_DATA_REDIS_USERNAME` | `${SPRING_DATA_REDIS_USERNAME}` | *(required)* | Redis username |
| `SPRING_DATA_REDIS_PASSWORD` | `${SPRING_DATA_REDIS_PASSWORD}` | *(required)* | Redis password |
| `KAFKA_BOOTSTRAP_SERVERS` | `${KAFKA_BOOTSTRAP_SERVERS:...}` | `tokaido.proxy.rlwy.net:48268` | Kafka broker connection |

**Static Values:**
- `app.jwt.secret` = `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch` (shared)
- `app.cors.allowed-origins` = `http://localhost:3000,http://localhost:3001,https://food-delievery-app-b6b3.vercel.app`
- `app.kafka.topics.location-pings` = `driver-location-pings`
- `app.kafka.topics.order-events` = `order-events`

**Note:** MongoDB credentials are hardcoded

---

## 16. Eureka Server

**Service:** `eureka-server` | **Port:** `8761` | **Config Path:** `backend/eureka-server/src/main/resources/application.properties`

**Note:** This service uses only static configuration, no environment variables.

---

## 17. Recommendation Service

**Service:** `recommendation-service` | **Config Path:** `backend/recommendation-service/src/main/resources/application.properties`

**Note:** This service has minimal configuration with no environment variables.

---

## Summary Statistics

### Total Environment Variables: 31

#### By Frequency:
- **KAFKA_BOOTSTRAP_SERVERS**: 11 services (most common)
- **MONGODB_USERNAME**: 5 services
- **MONGODB_PASSWORD**: 5 services
- **MONGODB_HOST**: 5 services
- **SPRING_DATA_REDIS_***: 5 services (4 variants: HOST, PORT, USERNAME, PASSWORD)
- **CLOUDINARY_***: 2 services (3 variants: CLOUD_NAME, API_KEY, API_SECRET)
- **SERVICE_HOST**: 13 services (each unique)
- **PORT**: 8 services
- **OAuth2 Credentials**: 4 variables (GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, GITHUB_CLIENT_ID, GITHUB_CLIENT_SECRET)
- **Razorpay Credentials**: 3 variables (RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET, RAZORPAY_WEBHOOK_SECRET)
- **Email Configuration**: 2 variables (MAIL_USERNAME, MAIL_PASSWORD)
- **Firebase**: 1 variable (FIREBASE_CREDENTIALS_PATH)

#### By Category:
- **Infrastructure**: PORT, SERVICE_HOST, KAFKA_BOOTSTRAP_SERVERS, SPRING_DATA_REDIS_*
- **Database**: MONGODB_USERNAME, MONGODB_PASSWORD, MONGODB_HOST
- **Authentication**: OAuth2 credentials, RAZORPAY_*
- **Third-Party Services**: CLOUDINARY_*, MAIL_*, FIREBASE_CREDENTIALS_PATH
- **Application**: App-specific configurations (see Static Values sections)

---

## Security Considerations

### 🔴 Critical Issues:
1. **Hardcoded MongoDB Credentials**: All services have MongoDB credentials hardcoded in the properties files:
   - Username: `mdaazam0786`
   - Password: `aazam%40786`
   - Host: `cluster0.sczcxrl.mongodb.net`

2. **Hardcoded OAuth2 Credentials**: Auth service contains hardcoded Google and GitHub OAuth credentials in plaintext

3. **Hardcoded Razorpay Test Keys**: Payment service contains test credentials hardcoded (though marked as "test")

4. **Hardcoded Cloudinary Credentials**: Two services have Cloudinary API credentials hardcoded

5. **Hardcoded JWT Secret**: Shared across all services: `ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch`

6. **Hardcoded Frontend URLs**: Production URLs hardcoded for OAuth callbacks and CORS

### 🟡 Warnings:
- Redis credentials required but no defaults provided for some services
- Firebase service account path expected but not provided
- Email configuration relies on environment variables but has development defaults

### Recommendations:
1. Move all credentials to environment variables with NO hardcoded defaults
2. Use a secrets management system (AWS Secrets Manager, HashiCorp Vault, etc.)
3. Rotate all exposed credentials immediately
4. Use different credentials for dev/staging/production
5. Implement secrets rotation policies
6. Add credential validation on application startup
7. Never commit sensitive data to version control
