# mbooks-quarkus

Movie catalog, seat booking, and payment API for the Cinemas booking platform. Runs on port **8080** under context path `/mbooks-1`.

## What it does

- **Movie catalog** — Browse all movies (`GET /rest/book/movies`), search by name or full-text, filter by category.
- **Venue & screening lookup** — `GET /rest/book/venues/{movieId}` returns venues, screens, and screening dates for a movie.
- **Seat selection** — `GET /rest/book/seats/{screenId}` returns the seat map with reservation status.
- **Booking & payment** — `POST /rest/book/payment/fullcheckout2` reserves seats with pessimistic locking, processes payment via Braintree (sandbox), records the purchase.
- **Purchase history** — `GET /rest/book/purchases` lists all purchases for a user; `/purchases/tickets?purchaseId=` returns ticket details.
- **Ticket management** — Cancel tickets or delete purchases via `/purchases/manage` and `/purchases/delete`.
- **Realtime** — Kafka producer publishes to `ios-movies-notifications2`; Kafka consumer broadcasts via WebSocket at `/ws`.

## Architecture

```
dalogin / iOS / Web
  │
  ▼
mbooks (:8080, /mbooks-1)
  ├── BookController (1171 lines) ── movies, venues, seats, checkout, purchases
  ├── TicketService ── pessimistic-lock seat reservation + rollback
  ├── PaymentService ── Braintree gateway (sandbox) integration
  ├── PurchaseDAO ── purchase/ticket CRUD
  ├── BookingHandlerImpl ── orchestration layer
  ├── DAO (singleton) ── Hibernate session management via HibernateUtil
  ├── KafkaMessageProducer ── publishes movie events
  └── KafkaListener → WebSocketServer.broadcastMessage()
           ▼
       MySQL book
```

## REST endpoints

| Path | Method | Description |
|------|--------|-------------|
| `/rest/book/hello` | GET | Health check |
| `/rest/book/movies` | GET | All movies (paginated, filterable by category) |
| `/rest/book/movies/{name}/{order}` | GET | Search movies by name |
| `/rest/book/movies/search?match=&category=` | GET | Full-text search |
| `/rest/book/movie/{movieId}` | GET | Single movie details |
| `/rest/book/venues/{movieId}` | GET | Venues + screenings for a movie |
| `/rest/book/seats/{screenId}` | GET | Seat map for a screen |
| `/rest/book/payment/clientToken` | GET | Generate Braintree client token |
| `/rest/book/payment/fullcheckout2` | POST | Reserve seats + process Braintree payment |
| `/rest/book/payment/webcheckout` | POST | Web-only checkout |
| `/rest/book/purchases` | GET | User's purchase history (uuid header required) |
| `/rest/book/purchases/tickets?purchaseId=` | GET | Tickets for a purchase |
| `/rest/book/purchases/manage` | POST | Cancel tickets |
| `/rest/book/purchases/delete` | DELETE | Delete a purchase |

## Database

Uses MySQL schema **`book`** via singleton Hibernate session (`DAO.instance()` + `HibernateUtil`).

| Entity | Table | Key relationships |
|--------|-------|-------------------|
| `Movie` | Movie | 1:N → Screen |
| `Screen` | Screen | N:1 → Movie, 1:1 → ScreeningDates, 1:N → Seats |
| `ScreeningDates` | ScreeningDates | 1:1 ← Screen, N:1 → Venues |
| `Venues` | Venues | 1:1 → Screen, N:1 → Location |
| `Location` | location | Referenced by Venues |
| `Seats` | Seats | N:1 → Screen |
| `Ticket` | Ticket | 1:1 → Screen, N:1 → Seats, N:1 → Purchase |
| `Purchase` | Purchase | 1:N → Ticket (uuid-based user link) |

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL_BOOKS` | `jdbc:mysql://localhost:3306/book` | JDBC URL for the book database |
| `BOOTSTRAP_URL` | `localhost:9092` | Kafka bootstrap server |

## Build & Run

```bash
./mvnw quarkus:dev                    # dev mode on port 8080
./mvnw package -DskipTests            # package for container
podman build -t mbooks-quarkus:local .
```

## Part of the Cinemas platform

| Service | Repo | Role |
|---------|------|------|
| dalogin-quarkus | [igeorge0902/dalogin-quarkus](https://github.com/igeorge0902/dalogin-quarkus) | Auth gateway |
| mbook-quarkus | [igeorge0902/mbook-quarkus](https://github.com/igeorge0902/mbook-quarkus) | User/device API |
| **mbooks-quarkus** | this repo | Movie/booking/payment API |
| simple-service-webapp-quarkus | [igeorge0902/simple-service-webapp-quarkus](https://github.com/igeorge0902/simple-service-webapp-quarkus) | Image server |
| k8infra | [igeorge0902/k8infra](https://github.com/igeorge0902/k8infra) | Kubernetes manifests, SQL fixes, deploy runbook |
