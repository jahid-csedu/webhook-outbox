# Webhook Outbox Worker

## Endpoints

### 1. Receiver Endpoint
**POST** `/receiver`

Simulates webhook delivery with different modes:

| Mode        | Description                                              |
|------------|----------------------------------------------------------|
| success    | Always returns 200 OK                                    |
| flaky      | Fails a few times before succeeding                     |
| rate-limit | Returns 429 with `Retry-After` on first attempt, then succeeds |
| fail-400   | Returns 400 Bad Request                                  |

**Headers:**
- `X-Mode` (optional) – Mode of webhook (`success`, `flaky`, `rate-limit`, `fail-400`)
- `X-Aggregate-Id` (optional) – Identifier for webhook grouping

**Body:** JSON payload of the webhook

---

### 2. Enqueue Webhook
**POST** `/webhooks/enqueue`

- Adds a new webhook to the outbox for delivery.
- **Body:** JSON containing `aggregateId`, `targetUrl`, and `payload`.

---

### 3. Replay Webhook
**POST** `/webhooks/outbox/{aggregateId}/replay`

- Retries all failed or pending webhooks for the given aggregate ID.

---

### 4. Outbox Endpoint
**GET** `/webhooks/outbox?status=PENDING&limit=50`

- Returns all webhooks in the outbox with their current status.

---

## Running the Project

Run the docker containers
```bash
docker-compose up
```

Run the spring boot project
```bash
./gradlew bootRun
```

## Running the Tests

```bash
./gradlew test
```