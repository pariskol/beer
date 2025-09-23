<p align="center">
<img src="beer-logo.png" alt="Logo" width="300"/>
</p>

**Beer** is a lightweight Java library that makes it simple to build RESTful web applications using Jetty and Servlets — without the boilerplate. Just pour your routes, filters, static files, and you’re ready to serve!

---

## ✨ Features

- ✅ **Simple route handling** — define `GET`, `POST`, `PUT`, `DELETE` handlers with ease.
- ✅ **WebSocket handling** — define WebSocket handlers just as easily.
- ✅ **Wildcard routes** — match dynamic paths effortlessly.
- ✅ **Path parameters support** — e.g. `/myPath/:pathParam`.
- ✅ **Filters & CORS** — add pre-processing, logging, or CORS in one line.
- ✅ **Serve static files** — host static content alongside your API.
- ✅ **HTTPS support** — configure SSL out of the box.
- ✅ **Centralized exception handling** — return clear JSON error responses.
- ✅ **Runs on Jetty** — robust, proven, and production-ready.

---

## 🚀 Quick Start

```java
import gr.kgdev.beer.core.Beer;
import gr.kgdev.beer.utils.BeerUtils;
import gr.kgdev.beer.model.SimpleMessage;

public class App {
    public static void main(String[] args) throws Exception {
        var beer = new Beer();

        var config = 
            new BeerConfig()
                .setIp("0.0.0.0")
                .setPort(8080)
                .setLoggerName("beer")
                .setJettyMinThreads(4)
                .setJettyMaxThreads(32)
                .setJettyIdleTimeout(60000);
                // .setKeystorePath("path/to/keystore"); // For HTTPS
                // .setKeystorePass("yourPassword");

        beer.init(config);

        // serve static files from classpath /public (in jar folder)
        beer.staticFiles("/", "/public");

        // common filters provided by beer
        beer.corsAllFilter();
        beer.loggingFilter();
        beer.exceptionFilter();
        // user defined filter
        beer.filter("/*", (req, res) -> System.out.println("Request filtered!"));

        // beer automatically converts returned object to json
        beer.get("/hello", (req, res) -> Map.of("message", "Hello Beer!"));
        beer.get("/hello-class", (req, res) -> new SimpleMessage("Hello Beer!"));
        beer.post("/echo", (req, res) -> {
            var body = BeerUtils.parseReqBody(req);
            return Map.of("you_sent", body);
        });
        // do not forget to call start!
        beer.start();
    }
}
```

Beer is designed for primarily creating restful services with json responses. But it is possible to override the default behavior and return any other kind of response. Html response example:

```java
    beer.get("/reset-password", (req, res) -> {
        // add content type header in order to return html response
        res.setHeader("Content-Type", "text/html; charset=utf-8");
        return FileUtils.readResourceFile("reset-password.html");
    });
```


## 🏗️ Core Concept

The core class is **Beer**.

This is where the main capabilities live:

- `init()` — Configures your server with a `BeerConfig`.
- `filter()` — Attaches custom servlet filters.
- `get()`, `post()`, `put()`, `delete()` — Registers route handlers.
- `socket()` — Registers WebSocket handlers.
- `broadcast()` — Sends data to WebSocket connections.
- `exceptionFilter()` — Handles uncaught exceptions globally.
- `corsAllFilter()` — Enables permissive CORS for all routes.
- `loggingFilter()` — Logs all incoming requests.
- `staticFiles()` — Serves static files from your classpath.
- `start()` — Starts the Jetty server.

## 🛠️ Utilities

Beer is a thin layer built on top of Servlets, so it includes helper classes to simplify request/response handling.  
The most important one is **BeerUtils**.

### BeerUtils Overview

- `parseReqBody()` — Reads the request body as a raw string.
- `parseBody()` — Converts the JSON request body into a Java object of the given class.
- `parseQueryParams()` — Maps query parameters into a Java object.
- `getPathParam()` — Extracts a named path parameter (e.g. `:id` in `/users/:id`).
- `parseBasicAuthCredentials()` — Parses HTTP Basic Authentication headers and returns a `Credentials` object.
- `getPathSegment()` — Gets a specific segment from the request URI by index.
- `redirect()` — Sends an HTTP redirect to the given URL.
- `json()` — Converts a Java object, collection, map, or string into a proper JSON string.


## 💡 Inspiration
Beer is inspired by the simplicity of Spark and Express — but built directly on top of Jetty and Servlets to give you control, clarity, and lightweight performance with minimal setup.
