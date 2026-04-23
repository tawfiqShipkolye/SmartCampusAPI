# Smart Campus Sensor & Room Management API

A RESTful API built with JAX-RS (Jersey) and Grizzly HTTP server for managing campus rooms and IoT sensors.

---

## How to Build and Run

### Prerequisites
- Java 11
- Apache Maven
- Apache NetBeans (recommended)

### Steps
1. Clone the repository:
   git clone https://github.com/tawfiqShipkolye/SmartCampusAPI.git

2. Open the project in NetBeans as a Maven project

3. Right-click Main.java and select Run File

4. The server will start at:
   http://localhost:8080/api/v1/

---

## Sample curl Commands

### 1. Discovery Endpoint
curl -X GET http://localhost:8080/api/v1/

### 2. Get All Rooms
curl -X GET http://localhost:8080/api/v1/rooms

### 3. Create a Room
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-101","name":"Main Hall","capacity":100}'

### 4. Get All Sensors
curl -X GET http://localhost:8080/api/v1/sensors

### 5. Get Sensors by Type
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"

### 6. Create a Sensor
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":400.0,"roomId":"LAB-101"}'

### 7. Post a Sensor Reading
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":24.5}'

### 8. Delete a Room
curl -X DELETE http://localhost:8080/api/v1/rooms/EMPTY-001

---

## API Design Overview

This API follows RESTful principles and is structured around three core resources:

- **Rooms** — `/api/v1/rooms` — manage campus rooms
- **Sensors** — `/api/v1/sensors` — manage IoT sensors linked to rooms
- **Sensor Readings** — `/api/v1/sensors/{sensorId}/readings` — historical data per sensor

All data is stored in-memory using ConcurrentHashMap. The API uses JAX-RS with Jersey and Grizzly as the embedded HTTP server.

---

## Report —

### Part 1.1 — JAX-RS Resource Lifecycle

By default, JAX-RS creates a new instance of each resource class for every incoming HTTP request. This is known as the per-request lifecycle. The runtime instantiates the resource, handles the request, and then discards the instance — it is not a singleton.

This has a critical implication for in-memory data management. Because each resource instance is thrown away after every request, you cannot store shared data inside the resource class itself — any data stored in instance fields would be lost immediately after the request completes. To maintain state across requests, shared data must be stored in a separate, application-scoped object that persists for the lifetime of the server. In this project, the DataStore singleton (using the static getInstance() pattern) serves this purpose. All resource instances share the same DataStore object regardless of how many times they are instantiated.

Since multiple requests can arrive simultaneously and each creates its own resource instance, all of which access the shared DataStore concurrently, thread safety is essential. This is why ConcurrentHashMap is used instead of a regular HashMap — it allows concurrent reads and writes without data corruption or race conditions.

---

### Part 1.2 — HATEOAS and Hypermedia

HATEOAS (Hypermedia as the Engine of Application State) means that API responses include links to related resources and available actions, rather than just raw data. For example, the discovery endpoint at GET /api/v1 returns not just metadata but also the URLs for rooms and sensors, guiding the client on what it can do next.

This benefits client developers significantly compared to static documentation. With static docs, the client must hardcode every URL and remember to update them if the API changes. With HATEOAS, the client can discover available resources dynamically at runtime by following the links provided in responses. This decouples the client from the server's URL structure, making the API more resilient to change and easier to navigate. It also reduces the chance of integration errors caused by outdated documentation.

---

### Part 2.1 — Returning Full Objects vs IDs Only

Returning only IDs in a list response reduces the size of the payload, which saves bandwidth. However, it forces the client to make a separate GET /{roomId} request for every room it wants details about — this is known as the N+1 problem and can severely degrade performance when there are many rooms.

Returning full room objects in the list means the client receives everything it needs in a single request, reducing round trips and improving responsiveness. The trade-off is a larger initial payload, but for most real-world use cases this is preferable. In this implementation, full room objects are returned from GET /api/v1/rooms to minimise client-side processing and network round trips.

---

### Part 2.2 — Idempotency of DELETE

Yes, the DELETE operation is idempotent in this implementation. Idempotency means that making the same request multiple times produces the same result as making it once.

In this API, the first DELETE /api/v1/rooms/{roomId} request successfully removes the room and returns 204 No Content. If the client accidentally sends the same request again, the room no longer exists in the DataStore, so the API returns 404 Not Found. The server-side state is identical after both calls — the room is gone. No additional side effects occur. Therefore, regardless of how many times the DELETE request is sent, the outcome is consistent: the room is removed and the data remains in a valid state.

---

### Part 3.1 — @Consumes and Media Type Mismatch

The @Consumes(MediaType.APPLICATION_JSON) annotation tells JAX-RS that the POST endpoint only accepts requests with a Content-Type: application/json header. If a client sends data with a different format such as text/plain or application/xml, JAX-RS will automatically reject the request before it even reaches the resource method. It returns an HTTP 415 Unsupported Media Type response, protecting the endpoint from malformed or unexpected input. The developer does not need to write any manual validation for this — JAX-RS handles the mismatch entirely through the annotation, making the API more robust and reducing boilerplate code.

---

### Part 3.2 — @QueryParam vs Path Parameter for Filtering

Using @QueryParam for filtering (e.g., GET /api/v1/sensors?type=CO2) is superior to embedding the filter in the path (e.g., /api/v1/sensors/type/CO2) for several reasons.

Query parameters are optional by nature, meaning the same endpoint handles both the filtered and unfiltered case cleanly without needing separate routes. Path parameters imply that the value is a required part of the resource identity, which is semantically incorrect for a filter — type/CO2 suggests CO2 is a specific resource, not a filter criterion. Query parameters also allow multiple filters to be combined easily (e.g., ?type=CO2&status=ACTIVE) without restructuring the URL. Additionally, query parameters are the industry-standard convention for search and filtering, making the API more intuitive and consistent with RESTful best practices.

---

### Part 4.1 — Sub-Resource Locator Pattern

The Sub-Resource Locator pattern delegates the handling of a nested path to a separate dedicated class. In this project, SensorResource contains a locator method for /{sensorId}/readings that returns an instance of SensorReadingResource, which handles all reading-related operations.

The architectural benefit is separation of concerns. If all endpoints were defined in one massive resource class, the file would become extremely long, difficult to read, and hard to maintain. By splitting logic into dedicated classes, each class has a single responsibility, making the codebase modular and easier to test, extend, and debug. In large APIs with dozens of nested resources, this pattern is essential for keeping the codebase manageable. It also allows the sub-resource class to be reused or replaced independently without affecting the parent resource.

---

### Part 5.2 — HTTP 422 vs 404 for Missing Reference

HTTP 404 Not Found is semantically meant to indicate that the requested URL itself does not exist. When a client POSTs a new sensor with a roomId that does not exist, the URL /api/v1/sensors is perfectly valid — the problem is inside the request body, not the URL.

HTTP 422 Unprocessable Entity is more accurate in this case because it signals that the server understood the request, parsed the JSON successfully, but could not process it because the content is logically invalid — specifically, it references a roomId that does not exist in the system. This gives the client a much clearer signal: the request was well-formed but semantically incorrect, and they need to fix the data, not the URL. Using 422 leads to better error handling on the client side and more expressive, self-documenting API behaviour.

---

### Part 5.4 — Risks of Exposing Stack Traces

Exposing raw Java stack traces to external API consumers is a serious security risk. A stack trace reveals the internal structure of the application, including package names, class names, method names, and line numbers. An attacker can use this information to identify the exact technology stack and libraries in use, which helps them look up known vulnerabilities (CVEs) for those specific versions. Method names and class hierarchies can reveal business logic, making it easier to craft targeted attacks. Line numbers can help an attacker understand code flow and identify error-prone areas to exploit. The GenericThrowableMapper in this project prevents this by catching all unhandled exceptions and returning only a generic 500 Internal Server Error message with no internal details.

---

### Part 5.5 — JAX-RS Filters vs Manual Logging

Using JAX-RS filters for cross-cutting concerns like logging is far superior to inserting Logger.info() statements manually into every resource method for several reasons.

Filters are applied automatically to every request and response without modifying any resource code. If logging needs to be changed or extended, you only update one class. Manual logging in every method means if you have 20 endpoints, you must add, maintain, and potentially fix logging in 20 places — this violates the DRY (Don't Repeat Yourself) principle. Filters also have guaranteed access to request and response context objects, making it easy to log consistent information such as HTTP method, URI, and status code in a structured way. Furthermore, filters can be added or removed without touching any business logic, keeping resource classes clean and focused solely on their intended purpose.
