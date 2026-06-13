# 006 - HTTP Requests

## Overview

Spript provides a simple HTTP client via `http.get()` and `http.post()`. Both return Promises resolved on the Bukkit main thread.

> **Important:** HTTP requests are **asynchronous** — use `async/await` or `.then()` to handle responses.

## GET Requests

```typescript
try {
    const response = await http.get("https://api.github.com/repos/anomalyco/opencode");
    const data = JSON.parse(response.body());
    msg(sender, `<green>Stars: ${data.stargazers_count}</green>`);
} catch (error) {
    msg(sender, `<red>Request failed: ${error}</red>`);
}
```

### With Custom Headers

```typescript
const headers = {
    "Authorization": "Bearer your-token",
    "User-Agent": "Spript"
};

const response = await http.get("https://api.github.com/user", headers);
```

## POST Requests

```typescript
const body = {
    username: "player1",
    score: 100
};

try {
    const response = await http.post("https://api.example.com/scores", body);
    msg(sender, "<green>Score saved!</green>");
} catch (error) {
    msg(sender, `<red>Error: ${error}</red>`);
}
```

### With Headers

```typescript
const response = await http.post(
    "https://api.example.com/data",
    { key: "value" },
    { "X-API-Key": "secret123" }
);
```

## API Reference

### `http.get(url, headers?)`

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `url` | `string` | — | Full URL to fetch |
| `headers` | `Record<string, string>` | `null` | Optional HTTP headers |

Returns `Promise<HttpResponse>` where the response has `.body()`, `.statusCode()`, `.headers()`, etc. (Java `HttpResponse<String>`).

### `http.post(url, body, headers?)`

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `url` | `string` | — | Full URL |
| `body` | `any` | — | String or object (auto-stringified with JSON) |
| `headers` | `Record<string, string>` | `null` | Optional HTTP headers |

Returns `Promise<HttpResponse<String>>`.

If no `Content-Type` header is provided, defaults to `application/json`.

## Async Commands with HTTP

```typescript
import { CommandSender } from 'org.bukkit.command';

command("weather", async (sender: CommandSender, label: string, args: string[]) => {
    if (args.length === 0) {
        msg(sender, "<red>Usage: /weather <city>");
        return;
    }
    
    try {
        msg(sender, "<yellow>Fetching weather data...");
        const response = await http.get(
            `https://api.open-meteo.com/v1/forecast?latitude=-23.55&longitude=-46.63&current_weather=true`
        );
        const data = JSON.parse(response.body());
        msg(sender, `<green>Temperature: ${data.current_weather.temperature}°C`);
    } catch (e) {
        msg(sender, `<red>Failed to fetch weather`);
    }
});
```

## Error Handling

Always wrap HTTP calls in try/catch — network errors, timeouts, and invalid URLs are caught:

```typescript
try {
    const response = await http.get("https://invalid-url.example");
} catch (error) {
    msg(sender, `<red>Network error: ${error.message || error}</red>`);
}
```

## Notes

- Uses Java 11+ `HttpClient` under the hood with HTTP/2 support
- Requests run on **virtual threads** (Java 21+) — highly scalable
- Response body is always a **string** — parse with `JSON.parse()` for JSON APIs
- Connection timeout: 10 seconds

## Next Steps

- [007 - SQL](007-sql.md) — Database queries
- [008 - Async](008-async.md) — Async/await patterns
