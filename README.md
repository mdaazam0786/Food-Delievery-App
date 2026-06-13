# Foodzie - Food Delivery Platform

A comprehensive food delivery platform built with microservices architecture. Foodzie enables customers to browse restaurants, place orders, track deliveries in real-time, and manage their food delivery experience. Restaurant owners can manage menus and track orders, while delivery partners can view and accept delivery opportunities with live tracking.

---

## 📋 Project Overview

Foodzie is a full-stack, event-driven microservices application designed for scalability and real-time operations. The platform handles multiple concurrent workflows:

- **Customer Journey**: Browse restaurants → Add to cart → Checkout → Track delivery
- **Restaurant Operations**: Manage menu items → Accept/process orders → Track fulfillment
- **Delivery Operations**: Register drivers → View nearby orders → Accept deliveries → Real-time tracking

**Architecture Pattern**: Microservices with event-driven communication via Kafka, real-time WebSocket/SSE streams, and distributed geospatial caching.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    React Native Frontend                     │
│              (Customer, Restaurant, Driver Apps)             │
└──────────────────────┬──────────────────────────────────────┘
                       │ HTTPS
┌──────────────────────▼──────────────────────────────────────┐
│              API Gateway (Port 8080)                         │
│      • JWT Validation & Role-Based Routing                  │
│      • 20+ Route Rules with AuthenticationFilter            │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┬──────────────┐
        │              │              │              │
   ┌────▼───┐  ┌──────▼──┐  ┌───────▼──┐  ┌────────▼─────┐
   │ Auth   │  │ User    │  │ Cart     │  │ Restaurant   │
   │Service │  │Service  │  │Service   │  │Service       │
   │(8082)  │  │(8084)   │  │(8086)    │  │(8087)        │
   └────────┘  └─────────┘  └──────────┘  └──────────────┘
        │              │              │              │
   ┌────▼───┐  ┌──────▼──┐  ┌───────▼──┐  ┌────────▼─────┐
   │ Order  │  │Delivery │  │Delivery  │  │ Payment      │
   │Service │  │Service  │  │Acceptance│  │Service       │
   │(8083)  │  │(8090)   │  │(8095)    │  │(8088)        │
   └────────┘  └─────────┘  └──────────┘  └──────────────┘
        │              │              │              │
        └──────────────┼──────────────┼──────────────┘
                       │
        ┌──────────────┴──────────────┐
        │                             │
   ┌────▼──────────┐  ┌──────────────▼────┐
   │  Kafka Event  │  │ Eureka Service     │
   │  Streaming    │  │ Discovery (8761)   │
   └───────────────┘  └────────────────────┘
        │                      │
   ┌────▼──────────┐  ┌────────▼──────┐
   │ MySQL         │  │ MongoDB       │
   │ (Auth, Users, │  │ (Orders,      │
   │  Drivers)     │  │  Restaurants) │
   └───────────────┘  └───────────────┘
        │                      │
   ┌────▼──────────┐  ┌────────▼──────┐
   │ Redis (Geo)   │  │ Elasticsearch │
   │ (Real-time    │  │ (Search)      │
   │  Locations)   │  │               │
   └───────────────┘  └───────────────┘
```

---

## 🛠️ Tech Stack

### Backend
| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 17+ |
| **Framework** | Spring Boot | 3.x |
| **API Gateway** | Spring Cloud Gateway | 4.x |
| **Service Discovery** | Netflix Eureka | 4.x |
| **Event Streaming** | Apache Kafka | Latest |
| **Authentication** | Spring Security + JWT | 6.x |
| **OAuth2** | Google, GitHub | — |

### Databases
| Database | Purpose | Why Chosen |
|----------|---------|-----------|
| **MySQL** | Auth, user accounts, driver profiles | ACID compliance, normalized data |
| **MongoDB** | Orders, restaurants, menus, carts | Flexible schema, horizontal scaling |
| **Redis** | Real-time driver locations, caching | Sub-millisecond latency, geo queries |
| **Elasticsearch** | Restaurant/menu search indexing | Full-text search, faceted discovery |

### External Services
- **Cloudinary**: Image hosting (restaurant banners, menu photos)
- **Razorpay**: Payment processing (webhooks)
- **Nominatim (OpenStreetMap)**: Reverse geocoding
- **Google Maps / Google OAuth**: Maps, authentication

### Frontend
| Component | Technology |
|-----------|-----------|
| **Framework** | React Native / React.js |
| **State Management** | Context API |
| **Build Tool** | Vite |
| **Real-Time** | WebSocket, Server-Sent Events (SSE) |

---

## 🎯 Microservices

### 1. **API Gateway** (Port 8080)
**Responsibility**: Single entry point; routes requests to backend services; validates JWT tokens.

**Key Features**:
- 20+ route rules with context-aware filtering
- JWT authentication filter
- CORS handling with duplicate header deduplication
- OAuth2 redirect management
- WebSocket upgrade support

**Dependencies**: Eureka, Spring Cloud Gateway

---

### 2. **Auth Service** (Port 8082)
**Responsibility**: User authentication, session management, OAuth2 social login, MFA, password reset.

**Database**: MySQL

**Key Features**:
- Email/password registration & login
- OAuth2 integration (Google, GitHub)
- Multi-factor authentication (TOTP via Google Authenticator)
- JWT token lifecycle (access + refresh tokens)
- Session invalidation & logout
- Password reset via email tokens

**Endpoints**:
```
POST   /api/v1/auth/register              — Register new user
POST   /api/v1/auth/login                 — Login with credentials
POST   /api/v1/auth/mfa/verify            — Verify MFA challenge
POST   /api/v1/auth/refresh               — Refresh access token
POST   /api/v1/auth/logout                — Logout current session
POST   /api/v1/auth/logout-all            — Logout all sessions
POST   /api/v1/auth/password-reset        — Initiate password reset
POST   /api/v1/auth/password-reset/confirm — Complete password reset
GET    /oauth2/authorization/{provider}   — OAuth2 flow initiation
GET    /login/oauth2/code/{provider}      — OAuth2 callback handler
```

---

### 3. **User Service** (Port 8084)
**Responsibility**: User profiles, delivery addresses, preferences.

**Database**: MySQL

**Key Features**:
- Profile management
- Multiple delivery addresses
- User preferences & notifications settings
- Event listener for new user registration

---

### 4. **Cart Service** (Port 8086)
**Responsibility**: Shopping cart management with single-restaurant enforcement.

**Database**: MongoDB

**Key Features**:
- Add/remove items
- Single-restaurant constraint (cart cleared if switching restaurants)
- Quantity management
- Cart persistence across sessions
- Post-checkout cart clearing

**Endpoints**:
```
GET    /api/cart                 — Get user's current cart
POST   /api/cart/items           — Add item to cart
PATCH  /api/cart/items/{itemId}  — Update item quantity
DELETE /api/cart/items/{itemId}  — Remove specific item
DELETE /api/cart                 — Clear entire cart
```

---

### 5. **Restaurant Service** (Port 8087)
**Responsibility**: Restaurant catalog, menu management, restaurant admin dashboard.

**Databases**: MongoDB (data), Elasticsearch (search index)

**Key Features**:
- Restaurant provisioning & onboarding
- Menu management (CRUD items)
- Image upload via Cloudinary
- Open/close restaurant status
- Rating & discount management
- Analytics (today's earnings, order count)
- Geocoding via Nominatim

**Endpoints**:

**Admin Endpoints** (`/api/admin/restaurants`, requires ROLE_ADMIN or ROLE_RESTAURANT):
```
GET    /api/admin/restaurants/mine              — Get owned restaurant
POST   /api/admin/restaurants                   — Provision new restaurant
PUT    /api/admin/restaurants/{restaurantId}/status    — Toggle open/closed
POST   /api/admin/restaurants/{restaurantId}/menu      — Add menu item
PATCH  /api/admin/restaurants/{restaurantId}/menu/{itemId} — Update menu item
POST   /api/admin/restaurants/{restaurantId}/image     — Upload banner image
POST   /api/admin/menu-items/{itemId}/image           — Upload item image
POST   /api/admin/restaurants/upload-image             — Upload image (provisioning)
POST   /api/admin/menu-items/{itemId}/upload-image     — Upload menu item image
DELETE /api/admin/media/{publicId}                     — Delete image from Cloudinary
PATCH  /api/admin/restaurants/{restaurantId}/rating    — Update rating/discount
GET    /api/admin/analytics/todays-earnings            — Get daily earnings
```

**Public Endpoints** (`/api/restaurants`):
```
GET    /api/restaurants/{restaurantId}        — Get restaurant details
GET    /api/restaurants/{restaurantId}/menu   — Get available menu items
GET    /api/geocode?lat={lat}&lng={lng}       — Reverse-geocode coordinates
```

---

### 6. **Order Service** (Port 8083)
**Responsibility**: Order lifecycle management, order history, payment integration.

**Database**: MongoDB

**Key Features**:
- Order creation with validation
- Multi-item orders from single restaurant
- Order status tracking (PENDING → CONFIRMED → PREPARING → READY → PICKED → DELIVERED)
- Payment integration via Kafka events
- Order history with pagination
- Restaurant-specific order view

**Event Producers**: `order-events` Kafka topic
**Event Consumers**: Listens to `payment-events` from Payment Service

**Endpoints**:
```
POST   /api/orders                              — Create new order
GET    /api/orders/me?page=0&size=10            — Get user's order history
GET    /api/orders/{orderId}                    — Get specific order (owner only)
PATCH  /api/orders/{orderId}/status             — Update order status
GET    /api/orders/restaurant/{restaurantId}    — Get restaurant's orders
```

---

### 7. **Delivery Service** (Port 8090)
**Responsibility**: Driver registration, profile management, location tracking, earnings calculation.

**Databases**: MySQL (driver profiles, source of truth), Redis (live location cache)

**Key Features**:
- Driver registration & KYC status tracking
- Real-time location tracking via Redis GEOADD
- Status lifecycle (OFFLINE → IDLE → DELIVERING)
- Earnings summary & history
- Nearby driver discovery with expandable search radius
- Ghost driver cleanup (heartbeat monitoring)

**Redis Config**:
- Geo set key: `drivers:active:{cityZone}`
- Heartbeat TTL: 15 seconds
- Cleanup interval: 10 seconds

**Endpoints**:
```
POST   /api/drivers                             — Register new driver
GET    /api/drivers/{driverId}                  — Get driver profile
GET    /api/drivers/profile                     — Get profile by X-User-Email header
PATCH  /api/drivers/{driverId}/status           — Update driver status
GET    /api/drivers/{driverId}/earnings         — Get earnings summary
GET    /api/drivers/{driverId}/deliveries       — Get delivery history (paginated)
GET    /api/drivers/{driverId}/earnings-history — Get earnings history (paginated)
POST   /api/drivers/location/ping               — GPS ping (Redis only)
GET    /api/drivers/nearby?lat=X&lon=Y&cityZone=Z — Find nearby active drivers
```

---

### 8. **Delivery Matching Service** (Port 8092)
**Responsibility**: Event-driven order-to-driver matching algorithm.

**Databases**: MySQL (driver profiles), Redis (geospatial queries)

**Key Features**:
- Listens to OrderCreatedEvent from Kafka
- Performs radius-based geospatial search (initial 3km, expansion up to 10km)
- Selects top 5 nearby drivers
- Publishes delivery offers to Kafka `delivery-events` topic
- Matching algorithm with configurable parameters

**Algorithm**:
```
1. Order placed → OrderCreatedEvent published
2. Matching Service receives event
3. Search radius: 3 km
4. Find N nearby idle drivers via Redis GEORADIUS
5. If found: Select top 5 by distance, create DeliveryOfferEvents
6. If not found: Expand radius (3 → 5 → 8 → 10 km), retry
7. Publish offers to delivery-acceptance-service via SSE
```

**No REST API**: Event-driven only. Listens to Kafka topics.

---

### 9. **Delivery Acceptance Service** (Port 8095)
**Responsibility**: Driver offer delivery via SSE, distributed acceptance lock.

**Databases**: MySQL (driver profiles), Redis (distributed locks)

**Key Features**:
- Server-Sent Events (SSE) stream for driver apps
- Real-time push of delivery offers
- Distributed lock (Redis SET NX EX) for race condition handling
- Offer TTL: 30 seconds before re-widening search radius
- Accept/decline functionality

**Endpoints**:
```
GET    /api/delivery/drivers/{driverId}/offers/stream  — SSE stream (persistent)
POST   /api/delivery/offers/accept                     — Accept delivery (distributed lock)
POST   /api/delivery/offers/{orderId}/decline          — Decline delivery offer
```

**Response Codes**:
- `200 OK + assigned=true` → Driver won the race, delivery assigned
- `409 Conflict + assigned=false` → Another driver accepted faster

---

### 10. **Payment Service** (Port 8088)
**Responsibility**: Payment processing via Razorpay, webhook handling.

**Key Features**:
- Razorpay payment gateway integration
- Webhook endpoint for payment callbacks
- Payment status tracking
- Event publishing on payment completion

**Endpoints**:
```
POST   /api/payments/create              — Initiate payment order
GET    /api/payments/{paymentId}         — Get payment status
POST   /api/payments/webhook             — Razorpay webhook (public)
```

---

### 11. **Search Service** (Port 8089)
**Responsibility**: Elasticsearch-backed restaurant and menu search.

**Key Features**:
- Full-text search across restaurant names, descriptions
- Menu item search (name, description, tags)
- Faceted filtering (cuisine, rating, delivery time)
- Autocomplete suggestions

**Endpoints**:
```
GET    /api/search?q=pizza&cuisine=italian — Full-text search
GET    /api/search/autocomplete?q=piz       — Autocomplete suggestions
```

---

### 12. **Restaurant Acceptance Service** (Port 8091)
**Responsibility**: SSE stream for restaurant tablet, order acceptance/rejection.

**Key Features**:
- Server-Sent Events for restaurant order notifications
- Real-time order push to kitchen displays
- Accept/prepare/ready transitions
- Rejection with reason tracking

**Endpoints**:
```
GET    /api/restaurants/{restaurantId}/orders/stream  — SSE stream (persistent)
POST   /api/restaurants/orders/{orderId}/accept        — Accept order
POST   /api/restaurants/orders/{orderId}/reject        — Reject order
POST   /api/restaurants/orders/{orderId}/ready         — Mark ready for pickup
```

---

### 13. **Order Fulfillment Service** (Port 8093)
**Responsibility**: Orchestrates multi-step order lifecycle via event chain.

**Key Features**:
- Listens to order events (creation, status changes)
- Coordinates restaurant + driver transitions
- Publishes fulfillment status updates
- Handles order cancellations

**No REST API**: Event-driven only.

---

### 14. **WebSocket Manager** (Port 8097)
**Responsibility**: Live driver tracking for customers via WebSocket.

**Key Features**:
- Persistent WebSocket connection (`/ws/track/{orderId}`)
- Real-time driver location updates
- Order status streaming to customer app
- Automatic reconnection on client drop

**WebSocket Endpoint**:
```
WS     /ws/track/{orderId}?token={jwtToken}  — Live driver tracking stream
```

---

### 15. **Notification Service** (Port 8096)
**Responsibility**: SMS & push notifications.

**Key Features**:
- Order status notifications (to customer)
- Driver assignment notification
- Delivery arrival notification
- In-app push and SMS fallback

---

## 🔄 Event Flow & Kafka Topics

### Kafka Topics

| Topic | Producer | Consumers | Events |
|-------|----------|-----------|--------|
| `order-events` | Order Service | Order Fulfillment, Delivery Matching | OrderCreatedEvent, OrderStatusUpdatedEvent |
| `payment-events` | Payment Service | Order Service | PaymentCompletedEvent, PaymentFailedEvent |
| `delivery-events` | Delivery Matching | Delivery Acceptance | DeliveryOfferEvent, DeliveryAcceptedEvent |
| `user.registered` | Auth Service | Restaurant Service | NewUserRegisteredEvent |
| `restaurant-events` | Restaurant Service | Search Service | RestaurantUpdatedEvent, MenuItemUpdatedEvent |

### Event Flow Example: Order Placement to Delivery

```
1. Customer places order
   └─> OrderController.createOrder() → Order Service
   └─> Order saved to MongoDB
   └─> OrderCreatedEvent published to Kafka `order-events`

2. Delivery Matching Service receives event
   └─> Queries Redis GEORADIUS for nearby drivers
   └─> Selects top 5 candidates
   └─> Creates DeliveryOfferEvent for each driver
   └─> Publishes to `delivery-events`

3. Delivery Acceptance Service receives offer events
   └─> Registers drivers in SSE registry
   └─> Pushes "delivery_offer" event to driver's SSE stream
   └─> Driver's mobile app receives notification

4. Driver accepts offer
   └─> POST /api/delivery/offers/accept
   └─> Distributed lock attempt (Redis SET NX EX)
   └─> If lock acquired: AcceptOfferResponse(assigned=true)
   └─> Event published: DeliveryAcceptedEvent to `delivery-events`

5. Order Fulfillment Service coordinates transitions
   └─> Driver status updates to DELIVERING
   └─> Restaurant notified via SSE
   └─> Customer notified via WebSocket (live tracking)
```

---

## 🔐 Security & Authentication

### JWT Token Structure
```json
{
  "sub": "user@example.com",
  "iat": 1672531200,
  "exp": 1672617600,
  "roles": ["ROLE_CUSTOMER"]
}
```

### Roles
| Role | Services | Permissions |
|------|----------|-------------|
| `ROLE_CUSTOMER` | Frontend (orders, cart) | Browse restaurants, place orders, track delivery |
| `ROLE_RESTAURANT` | Admin panel | Manage own restaurant, view own orders |
| `ROLE_DRIVER` | Mobile app | View offers, accept deliveries, update location |
| `ROLE_ADMIN` | Admin panel | Full system access |

### API Authentication
1. **Public endpoints** (register, login, restaurant browse): No token required
2. **Protected endpoints** (orders, cart, profile): Token in `Authorization: Bearer {token}` header
3. **WebSocket clients**: Token passed as query parameter `?token={jwtToken}`
4. **API Gateway**: Validates token → extracts headers (`X-User-Id`, `X-User-Email`, `X-User-Role`)

### OAuth2 Flow
```
1. User clicks "Login with Google/GitHub"
2. Browser redirected to /oauth2/authorization/{provider}
3. OAuth provider → user consents
4. Callback to /login/oauth2/code/{provider}
5. Auth Service exchanges code for tokens
6. JWT generated & returned to frontend
7. Frontend stores JWT, uses for subsequent API calls
```

---

## 📊 Data Models

### Key Entities

**User**
```json
{
  "id": "userId",
  "email": "user@example.com",
  "name": "John Doe",
  "phone": "+91XXXXXXXXXX",
  "addresses": [
    {
      "id": "addrId",
      "label": "Home",
      "address": "123 Main St",
      "latitude": 28.5672578,
      "longitude": 77.2893970,
      "isDefault": true
    }
  ],
  "createdAt": "2024-01-01T00:00:00Z",
  "roles": ["ROLE_CUSTOMER"]
}
```

**Restaurant**
```json
{
  "id": "restaurantId",
  "name": "Pizza Palace",
  "ownerEmail": "owner@example.com",
  "address": "456 Food Street",
  "latitude": 28.5672578,
  "longitude": 77.2893970,
  "imageUrl": "https://cloudinary.com/.../pizza-palace.jpg",
  "status": "OPEN",
  "rating": 4.5,
  "totalRatings": 250,
  "discount": 10,
  "cuisines": ["Italian", "Pizza"],
  "deliveryTime": 30,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**MenuItem**
```json
{
  "id": "itemId",
  "restaurantId": "restaurantId",
  "name": "Margherita Pizza",
  "description": "Classic pizza with cheese and tomato",
  "price": 250,
  "imageUrl": "https://cloudinary.com/.../margherita.jpg",
  "category": "Pizza",
  "available": true,
  "prepTime": 20,
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**Order**
```json
{
  "id": "orderId",
  "userId": "userId",
  "restaurantId": "restaurantId",
  "items": [
    {
      "menuItemId": "itemId",
      "name": "Margherita Pizza",
      "price": 250,
      "quantity": 2
    }
  ],
  "deliveryAddress": {
    "address": "123 Main St",
    "latitude": 28.5672578,
    "longitude": 77.2893970
  },
  "totalPrice": 550,
  "status": "PREPARING",
  "paymentStatus": "COMPLETED",
  "driverId": "driverId",
  "createdAt": "2024-01-01T00:00:00Z",
  "deliveredAt": "2024-01-01T01:30:00Z"
}
```

**Driver**
```json
{
  "id": "driverId",
  "email": "driver@example.com",
  "name": "Raj Kumar",
  "phone": "+91XXXXXXXXXX",
  "kycStatus": "VERIFIED",
  "status": "IDLE",
  "latitude": 28.5672578,
  "longitude": 77.2893970,
  "cityZone": "delhi",
  "totalEarnings": 12500,
  "totalDeliveries": 150,
  "rating": 4.8,
  "createdAt": "2024-01-01T00:00:00Z",
  "lastLocationUpdate": "2024-01-15T10:30:00Z"
}
```

---

## 🌐 Real-Time Communication

### Server-Sent Events (SSE)

**Restaurant Order Stream** (`/api/restaurants/{restaurantId}/orders/stream`)
```javascript
// Server sends
event: order_created
data: {
  "orderId": "12345",
  "items": [...],
  "totalPrice": 550,
  "deliveryAddress": "123 Main St"
}
```

**Driver Offer Stream** (`/api/delivery/drivers/{driverId}/offers/stream`)
```javascript
// Server sends
event: delivery_offer
data: {
  "offerId": "offer123",
  "orderId": "12345",
  "restaurantName": "Pizza Palace",
  "pickupAddress": "456 Food Street",
  "deliveryAddress": "123 Main St",
  "estimatedPickupTime": 20,
  "estimatedDeliveryTime": 25,
  "expectedPay": 50,
  "expiresIn": 30
}
```

### WebSocket

**Live Driver Tracking** (`/ws/track/{orderId}?token={jwtToken}`)
```javascript
// Server sends (every 5 seconds during delivery)
{
  "type": "driver_location_update",
  "driverId": "driverId",
  "latitude": 28.5672578,
  "longitude": 77.2893970,
  "status": "DELIVERING",
  "estimatedArrival": "2024-01-15T10:35:00Z"
}

// Server sends on status change
{
  "type": "order_status_changed",
  "orderId": "12345",
  "status": "DELIVERED",
  "deliveredAt": "2024-01-15T10:38:00Z"
}
```

---

## 🎮 API Gateway Routing

**Route Configuration** (Spring Cloud Gateway):

| ID | Service | Pattern | Auth Filter | Purpose |
|----|---------|---------|-------------|---------|
| 0 | Auth | `/oauth2/**`, `/login/oauth2/**` | ❌ | OAuth2 social login |
| 1 | Auth | `/api/v1/auth/register`, `/api/v1/auth/login`, ... | ❌ | Public auth endpoints |
| 2 | Auth | `/api/v1/auth/**` | ✅ | Protected auth endpoints |
| 3-4 | User | `/api/v1/users/**`, `/api/v1/address/**` | ✅ | User management |
| 5 | Order | `/api/orders/**` | ✅ | Order management |
| 6-7 | Payment | `/api/payments/**` | ⚠️ | Payment (webhook is public) |
| 8 | Cart | `/api/cart/**` | ✅ | Cart management |
| 9 | Restaurant | `/api/geocode` | ❌ | Geocoding (public) |
| 10 | Restaurant | `/api/restaurants/**` | ❌ | Public catalog |
| 11 | Restaurant | `/api/admin/**` | ✅ | Admin management |
| 12-13 | Restaurant Accept | `/api/restaurants/*/orders/stream`, `/api/restaurants/orders/**` | ✅ | Restaurant operations |
| 14 | Search | `/api/search**` | ❌ | Search (public) |
| 15 | Delivery | `/api/drivers/**` | ✅ | Driver management |
| 16-17 | Delivery Accept | `/api/delivery/drivers/*/offers/stream`, `/api/delivery/offers/**` | ✅ | Driver acceptance |
| 18 | Fulfillment | `/api/fulfillment/**` | ✅ | Order fulfillment |
| 19 | WebSocket | `/ws/track/**` | ✅ | Live tracking (WebSocket) |

---

## 🗄️ Database Schema Overview

### MySQL (Auth, Users, Drivers)
```sql
-- Users
CREATE TABLE users (
  id BIGINT PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255),
  name VARCHAR(255),
  phone VARCHAR(20),
  roles VARCHAR(100),
  created_at TIMESTAMP,
  INDEX idx_email (email)
);

-- Drivers
CREATE TABLE drivers (
  id VARCHAR(50) PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  name VARCHAR(255),
  phone VARCHAR(20),
  kyc_status VARCHAR(50),
  status VARCHAR(50),
  total_earnings DECIMAL(10, 2),
  total_deliveries INT,
  rating DECIMAL(3, 2),
  created_at TIMESTAMP,
  last_location_update TIMESTAMP,
  INDEX idx_email (email),
  INDEX idx_status (status)
);
```

### MongoDB (Orders, Restaurants, Menus, Carts)
```javascript
// Collections: restaurants, menu_items, orders, carts

db.restaurants.insertOne({
  _id: ObjectId(),
  name: "Pizza Palace",
  ownerEmail: "owner@example.com",
  address: "456 Food Street",
  location: { type: "Point", coordinates: [77.2893970, 28.5672578] },
  imageUrl: "...",
  status: "OPEN",
  rating: 4.5,
  cuisines: ["Italian", "Pizza"],
  createdAt: ISODate()
});

db.orders.insertOne({
  _id: ObjectId(),
  userId: "userId",
  restaurantId: "restaurantId",
  items: [...],
  totalPrice: 550,
  status: "PREPARING",
  createdAt: ISODate()
});
```

### Redis (Geospatial, Caching)
```
# Driver locations (geospatial set)
GEOADD drivers:active:delhi 77.2893970 28.5672578 driver123
GEOADD drivers:active:delhi 77.2894000 28.5673000 driver456

# Heartbeat tracking
SET driver:heartbeat:driver123 1 EX 15
SET driver:heartbeat:driver456 1 EX 15

# Offer locks (distributed)
SET offer:lock:order123:driver456 1 NX EX 5
# Returns: (integer) 1 if acquired, nil if someone else has it
```

### Elasticsearch (Search Index)
```json
{
  "mappings": {
    "properties": {
      "name": { "type": "text", "analyzer": "standard" },
      "description": { "type": "text" },
      "cuisine": { "type": "keyword" },
      "rating": { "type": "double" },
      "location": { "type": "geo_point" }
    }
  }
}
```

---

## 📡 Deployment Architecture

### Infrastructure Components

| Component | Port | Purpose |
|-----------|------|---------|
| API Gateway | 8080 | Request routing |
| Auth Service | 8082 | Authentication |
| Order Service | 8083 | Order processing |
| User Service | 8084 | User management |
| Cart Service | 8086 | Cart management |
| Restaurant Service | 8087 | Restaurant catalog |
| Payment Service | 8088 | Payment processing |
| Search Service | 8089 | Search indexing |
| Delivery Service | 8090 | Driver management |
| Restaurant Acceptance Service | 8091 | Order acceptance |
| Delivery Matching Service | 8092 | Order matching |
| Order Fulfillment Service | 8093 | Fulfillment orchestration |
| Location Update Service | 8094 | Location streaming |
| Delivery Acceptance Service | 8095 | Offer acceptance |
| Notification Service | 8096 | SMS/Push notifications |
| WebSocket Manager | 8097 | Live tracking |
| Eureka Server | 8761 | Service discovery |

### Databases
- **MySQL**: `localhost:3306/foodzie`
- **MongoDB**: `mongodb+srv://mdaazam0786:...@cluster0.sczcxrl.mongodb.net/foodzie`
- **Redis**: `localhost:6379`
- **Elasticsearch**: `http://localhost:9200`
- **Kafka**: `localhost:9092`

---

## 🔧 Configuration

### Shared Environment Variables

```env
# JWT
JWT_SECRET=ZmVlZGllLXVzZXItc2VydmljZS1zZWNyZXQta2V5LTI1Ni1iaXRzLWxvbmch

# Eureka
EUREKA_URL=http://localhost:8761/eureka/

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# MongoDB
MONGODB_URI=mongodb+srv://mdaazam0786:aazam@786@cluster0.sczcxrl.mongodb.net/foodzie

# MySQL
MYSQL_HOST=localhost
MYSQL_USER=root
MYSQL_PASSWORD=admin@123

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Elasticsearch
ELASTICSEARCH_URI=http://localhost:9200

# Cloudinary
CLOUDINARY_CLOUD_NAME=dryku5yoq
CLOUDINARY_API_KEY=169252649454779
CLOUDINARY_API_SECRET=MQJC4PFLaQD6SHCt20-SNMJNb_Y

# OAuth2
GOOGLE_CLIENT_ID=464819426022-5b51s4qeg9ukikd7mheeid1h2vv6vus7.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=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
GITHUB_CLIENT_ID=Ov23liK5WGnfqHHPoaFw
GITHUB_CLIENT_SECRET=c00a3a291b3767d8c6222ff12b3971bccc7a5941

# CORS
CORS_ORIGINS=http://localhost:3000,http://localhost:3001,http://localhost:4200
```

### Service-Specific Configs

**Auth Service** (`auth-service/application.properties`):
- JWT expiration: 7 days
- TOTP issuer: "Foodzie"
- MFA OTP expiry: 5 minutes

**Delivery Service** (`delivery-service/application.properties`):
- Default search radius: 5 km
- Heartbeat TTL: 15 seconds
- Cleanup interval: 10 seconds

**Delivery Matching Service** (`delivery-matching-service/application.properties`):
- Initial radius: 3 km
- Expansion steps: 3 → 5 → 8 → 10 km
- Max candidates: 5 drivers

**Delivery Acceptance Service** (`delivery-acceptance-service/application.properties`):
- Offer TTL: 30 seconds

---

## 🎯 Use Cases & Workflows

### 1. Customer Order Flow

```
1. Register/Login
   └─> POST /api/v1/auth/register
   └─> Receive JWT token

2. Browse restaurants
   └─> GET /api/restaurants (public catalog)
   └─> GET /api/search?q=pizza (search)
   └─> GET /api/restaurants/{id}/menu (view menu)

3. Add to cart
   └─> GET /api/cart (fetch existing)
   └─> POST /api/cart/items (add item)
   └─> Enforced: Single restaurant per cart

4. Checkout
   └─> POST /api/payments/create (initiate payment)
   └─> Post payment: POST /api/orders (create order)
   └─> DELETE /api/cart (clear cart)

5. Track delivery
   └─> WS /ws/track/{orderId} (open WebSocket)
   └─> Receive real-time driver location + status updates
   └─> Driver arrives, mark delivered

6. Order history
   └─> GET /api/orders/me (view past orders)
   └─> Rate restaurant (if implemented)
```

### 2. Restaurant Admin Workflow

```
1. Register restaurant
   └─> POST /api/admin/restaurants (provision)
   └─> Set name, address, cuisines

2. Add menu items
   └─> POST /api/admin/restaurants/{id}/menu (add item)
   └─> POST /api/admin/restaurants/upload-image (upload photo)

3. Accept orders
   └─> SSE /api/restaurants/{id}/orders/stream (listen)
   └─> Receive real-time order notifications
   └─> POST /api/restaurants/orders/{id}/accept (confirm)
   └─> POST /api/restaurants/orders/{id}/ready (mark ready)

4. Monitor analytics
   └─> GET /api/admin/analytics/todays-earnings (dashboard)
   └─> View orders, earnings, ratings

5. Manage menu
   └─> PATCH /api/admin/restaurants/{id}/menu/{itemId} (edit)
   └─> Set available=false for out-of-stock items
```

### 3. Driver Workflow

```
1. Register as driver
   └─> POST /api/drivers (onboard)
   └─> KYC documents → status PENDING → VERIFIED

2. Clock in
   └─> PATCH /api/drivers/{id}/status (IDLE)
   └─> POST /api/drivers/location/ping (send GPS)
   └─> Added to Redis geo set

3. Receive offers
   └─> SSE /api/delivery/drivers/{id}/offers/stream (listen)
   └─> Real-time delivery offer push
   └─> Accept/decline within 30 seconds

4. Accept delivery
   └─> POST /api/delivery/offers/accept (with distributed lock)
   └─> Response: assigned=true (won) or assigned=false (lost race)

5. Deliver order
   └─> POST /api/drivers/location/ping (continuous updates)
   └─> Customer sees live driver tracking
   └─> Mark delivered (status → DELIVERED)

6. View earnings
   └─> GET /api/drivers/{id}/earnings (summary)
   └─> GET /api/drivers/{id}/earnings-history (detailed)
```

---

## 🧪 Testing & Test Cards

### Payment Gateway - Razorpay Test Mode

The payment gateway is currently configured in **test mode**. Use the following test card details to simulate payment transactions during development and testing.

#### Test Card Numbers

All test cards accept any future expiry date and a random CVV (e.g., 123).

| Card Type | Card Number | Usage | CVV | Expiry |
|-----------|-------------|-------|-----|--------|
| **Visa** | `4100 2800 0000 1007` | Standard payment | Any 3 digits | Any future date |
| **Mastercard** | `5500 6700 0000 1002` | Standard payment | Any 3 digits | Any future date |
| **RuPay** | `6527 6589 0000 1005` | Indian domestic | Any 3 digits | Any future date |
| **Diners** | `3608 280009 1007` | Premium card | Any 3 digits | Any future date |
| **Amex** | `3402 560004 01007` | American Express | Any 4 digits | Any future date |

#### Example Test Checkout

```json
// Sample request body for POST /api/payments/create
{
  "orderId": "order123",
  "amount": 550,
  "currency": "INR",
  "customerEmail": "customer@example.com",
  "customerPhone": "+91XXXXXXXXXX"
}

// Card details to enter in checkout form:
Card Number: 4100 2800 0000 1007
Expiry: 12/25
CVV: 123
Name: Test Customer
```

#### Payment Flow in Test Mode

1. Customer places order (creates order in system)
2. Clicks "Proceed to Payment"
3. Redirected to Razorpay payment form (test environment)
4. Enters test card details above
5. Payment processes instantly (test mode)
6. Success callback received at `/api/payments/webhook`
7. Order status updates to `CONFIRMED`
8. Restaurant receives order notification
9. Driver matching begins

#### Testing Scenarios

| Scenario | Card to Use | Result |
|----------|-------------|--------|
| Successful payment | Any test card above | Order placed successfully |
| Test failed payment | Use card number `4111 1111 1111 1111` | Payment failure, order cancelled |
| International card | Visa/Mastercard/Amex | Works in test mode |
| Local card | RuPay/Diners | Works in test mode |

#### Important Notes

- **Test Mode Only**: These cards only work in Razorpay's test environment
- **No Real Charges**: No money will be deducted from any account
- **No Fraud Detection**: All transactions are instantly approved
- **Webhook Simulation**: Payment confirmations are sent to your webhook endpoint
- **Production Switch**: Remove test mode flag and use live credentials before going live

#### Payment API Endpoints for Testing

```
# Initiate payment
POST /api/payments/create
{
  "orderId": "order123",
  "amount": 550,
  "currency": "INR"
}

# Webhook (called by Razorpay)
POST /api/payments/webhook
{
  "event": "payment.authorized",
  "payload": { ... }
}

# Check payment status
GET /api/payments/{paymentId}

# Refund (for testing cancellations)
POST /api/payments/{paymentId}/refund
```

---

## 🐛 Error Handling & Status Codes

| Status | Scenario | Example |
|--------|----------|---------|
| `200 OK` | Successful request | GET /api/restaurants/{id} |
| `201 Created` | Resource created | POST /api/orders (order placed) |
| `400 Bad Request` | Invalid input | Missing required field |
| `401 Unauthorized` | Missing/invalid token | JWT expired |
| `403 Forbidden` | Permission denied | User accessing another's order |
| `404 Not Found` | Resource not found | GET /api/restaurants/invalid-id |
| `409 Conflict` | Race condition lost | POST /api/delivery/offers/accept (another driver faster) |
| `500 Internal Server Error` | Server error | Unexpected exception |

---

## 📞 Support Services

### Cloudinary Integration
- **Purpose**: Image hosting for restaurant banners and menu photos
- **API Usage**: Upload, delete, fetch CDN URLs
- **Folder structure**: `foodzie/restaurants/`, `foodzie/menu-items/`

### Nominatim (OpenStreetMap)
- **Purpose**: Reverse geocoding (lat/lng → address)
- **Endpoint**: `https://nominatim.openstreetmap.org/reverse`
- **Usage**: Converting delivery coordinates to human-readable addresses

### Razorpay
- **Purpose**: Payment processing
- **Webhook**: `/api/payments/webhook` (called by Razorpay)
- **Flow**: Create order → customer pays → webhook confirms → Order Service updates

### Google Maps & Google OAuth
- **Maps**: Frontend location display, route optimization
- **OAuth**: Alternative login method

---

## 🎓 Key Architectural Patterns

| Pattern | Implementation | Benefit |
|---------|---|---|
| **Microservices** | 15+ independent services | Scalability, independent deployment |
| **API Gateway** | Spring Cloud Gateway | Single entry point, JWT validation, routing |
| **Service Discovery** | Netflix Eureka | Dynamic service location, load balancing |
| **Event Streaming** | Apache Kafka | Asynchronous communication, loose coupling |
| **Distributed Locks** | Redis SET NX EX | Race condition prevention (driver acceptance) |
| **Geospatial Caching** | Redis GEOADD | Sub-millisecond nearby driver queries |
| **Real-Time Streaming** | SSE + WebSocket | Live order/offer/tracking updates |
| **Polyglot Persistence** | MySQL + MongoDB + Redis | Optimized data stores per use case |
| **Full-Text Search** | Elasticsearch | Fast restaurant/menu discovery |
| **ACID Compliance** | MySQL transactions | Auth & payment data integrity |

---

## 📦 Frontend Integration

The React/React Native frontend connects to this backend via:

1. **REST API** (`http://localhost:8080/api/...`)
   - Orders, cart, restaurants, auth

2. **SSE Streams** (via EventSource)
   - Restaurant order notifications
   - Driver delivery offers

3. **WebSocket** (via native WS)
   - Live driver tracking
   - Order status updates

4. **OAuth2 Browser Flow**
   - Social login (Google, GitHub)

---

## 🚀 Scalability Considerations

### Horizontal Scaling
- **Stateless services**: All microservices are stateless (except WebSocket Manager)
- **Load balancing**: Eureka + Spring Cloud Load Balancer
- **Distributed caching**: Redis handles session/geospatial data

### Database Scaling
- **MongoDB**: Sharding for restaurants/orders collections
- **MySQL**: Read replicas for frequently queried user/driver data
- **Redis**: Cluster mode for geo data
- **Elasticsearch**: Index sharding for search

### Rate Limiting
- Implement per-service rate limits (location pings, offer acceptance)
- Queue depth monitoring for Kafka topics

### Monitoring & Observability
- **Logging**: Centralized logs (recommended: ELK stack)
- **Metrics**: Prometheus + Grafana for service-level metrics
- **Tracing**: Distributed tracing (recommended: Jaeger)
- **Health checks**: Actuator endpoints (`/actuator/health`)

---

## 📝 Summary

Foodzie is a comprehensive, production-grade food delivery platform built on modern cloud-native principles:

- **Microservices architecture** for independent scalability
- **Event-driven orchestration** via Kafka for loose coupling
- **Real-time communication** via SSE and WebSocket
- **Geospatial optimization** for efficient driver matching
- **Distributed transactions** via event choreography
- **Security-first** with JWT + OAuth2

The system handles complex workflows (order placement → matching → acceptance → delivery → completion) while maintaining high availability, responsiveness, and scalability.

---

## 📄 License

[Add your license here]

---

## 👥 Contributors

- **Architecture & Backend**: Foodzie Team
- **Frontend**: React/React Native Team

---

**Last Updated**: June 2024
**Version**: 1.0.0
