# Smart Campus API

A RESTful API built with JAX-RS (Jersey) and Grizzly for managing campus rooms and sensors.

---

## How to Run

1. Clone the repo
2. Open in NetBeans
3. Right-click `Main.java` → Run File
4. Server starts at: `http://localhost:8080/api/v1/`

---

## curl Commands

```bash
# Discovery
curl -X GET http://localhost:8080/api/v1/

# Get all rooms
curl -X GET http://localhost:8080/api/v1/rooms

# Create a room
curl -X POST http://localhost:8080/api/v1/rooms -H "Content-Type: application/json" -d '{"id":"HALL-101","name":"Main Hall","capacity":100}'

# Get all sensors
curl -X GET http://localhost:8080/api/v1/sensors

# Filter sensors by type
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"

# Create a sensor
curl -X POST http://localhost:8080/api/v1/sensors -H "Content-Type: application/json" -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":400.0,"roomId":"LAB-101"}'

# Post a reading
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings -H "Content-Type: application/json" -d '{"value":24.5}'
```

---

## Questions

### Part 1.1 — JAX-RS Lifecycle
JAX-RS creates a new resource instance per request, not a singleton. This means shared data cannot be stored in the resource class itself. A singleton DataStore using ConcurrentHashMap is used to safely share data across all requests and prevent race conditions.

### Part 1.2 — HATEOAS
HATEOAS means the API response includes links to other resources. This allows clients to navigate the API dynamically instead of hardcoding URLs. It makes the API easier to use and less likely to break when URLs change.

### Part 2.1 — Full Objects vs IDs
Returning only IDs forces the client to make extra requests for each room, causing performance issues. Returning full objects gives the client everything in one request, reducing network round trips.

### Part 2.2 — DELETE Idempotency
Yes, DELETE is idempotent. The first call removes the room and returns 204. Any repeat call returns 404 because the room is already gone. The server state remains consistent either way.

### Part 3.1 — @Consumes Mismatch
If a client sends data as text/plain or application/xml instead of application/json, JAX-RS automatically returns 415 Unsupported Media Type without the request reaching the method.

### Part 3.2 — @QueryParam vs Path
Query parameters are optional and allow multiple filters to be combined easily. Path parameters imply the value is a fixed part of the resource identity, which is wrong for filtering. Query parameters are the REST standard for searching and filtering.

### Part 4.1 — Sub-Resource Locator
Delegating nested paths to separate classes keeps each class small and focused. It avoids one massive controller with hundreds of methods, making the code easier to read, test and maintain.

### Part 5.2 — 422 vs 404
404 means the URL does not exist. 422 means the URL is valid but the data inside the request is wrong. Using 422 when a roomId reference is missing is more accurate because the endpoint exists — the problem is in the request body.

### Part 5.4 — Stack Trace Risks
Stack traces expose class names, method names, line numbers and library versions. Attackers can use this to find known vulnerabilities. The GenericThrowableMapper prevents this by returning a safe generic 500 error message instead.

### Part 5.5 — Filters vs Manual Logging
Filters apply logging automatically to every request in one place. Manual logging in every method means duplicating code across many classes. Filters follow the DRY principle and keep resource classes clean.
