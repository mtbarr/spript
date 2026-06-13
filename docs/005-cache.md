# 005 - Cache

## Overview

Spript provides an in-memory key-value cache with two tiers:

- **Ephemeral** (default) — cleared on `/jsreload` or server restart
- **Persistent** — survives reloads, cleared only on server restart or explicit deletion

## Basic Usage

```typescript
// Set a value (ephemeral by default)
cache.set("playerCount", 42);

// Set a persistent value (survives /jsreload)
cache.set("serverId", "my-server", true);

// Get a value
const count = cache.get<number>("playerCount");
console.log(count); // 42

// Get with default
const name = cache.getOrDefault<string>("unknownKey", "default");
console.log(name); // "default"

// Delete a value
cache.delete("playerCount");
```

## API Reference

### `cache.set<T>(key, value, persistent?)`

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `key` | `string` | — | Unique key |
| `value` | `T` | — | Any serializable value |
| `persistent` | `boolean` | `false` | Whether to survive reloads |

### `cache.get<T>(key)`

Returns `T | undefined`. Returns `undefined` if key doesn't exist.

### `cache.getOrDefault<T>(key, defaultValue)`

Returns `T`. Returns `defaultValue` if key doesn't exist.

### `cache.delete(key)`

Removes the key from both caches.

## Ephemeral vs Persistent

```typescript
// Ephemeral — cleared on /jsreload
cache.set("tempData", someValue);

// Persistent — survives /jsreload
cache.set("configData", someValue, true);

// If you set the same key with different persistence,
// the old entry is removed from the other cache
cache.set("key", "ephemeral");
cache.set("key", "persistent", true); // moves to persistent
```

## Practical Examples

### Player Data Cache

```typescript
import { PlayerJoinEvent } from 'org.bukkit.event.player';
import { PlayerQuitEvent } from 'org.bukkit.event.player';

listen(PlayerJoinEvent, (event) => {
    const player = event.getPlayer();
    // Track join time
    cache.set(`joinTime:${player.getUniqueId()}`, Date.now(), false);
    msg(player, "<green>Welcome back!</green>");
});

listen(PlayerQuitEvent, (event) => {
    const player = event.getPlayer();
    const joinTime = cache.get<number>(`joinTime:${player.getUniqueId()}`);
    if (joinTime) {
        const sessionLength = Date.now() - joinTime;
        console.log(`${player.getName()} played for ${sessionLength}ms`);
        cache.delete(`joinTime:${player.getUniqueId()}`);
    }
});
```

### Cooldown System

```typescript
import { PlayerInteractEvent } from 'org.bukkit.event.player';

listen(PlayerInteractEvent, (event) => {
    const player = event.getPlayer();
    const cooldownKey = `cooldown:${player.getUniqueId()}`;
    const lastUse = cache.get<number>(cooldownKey);
    const now = Date.now();
    
    if (lastUse && (now - lastUse) < 5000) {
        msg(player, "<red>Please wait before using this again.</red>");
        event.setCancelled(true);
        return;
    }
    
    cache.set(cooldownKey, now);
    msg(player, "<green>Action performed!</green>");
});
```

### Server Configuration

```typescript
// Persistent config that survives reloads
if (!cache.get("config:loaded")) {
    cache.set("motd", "<gold>Welcome to my server!</gold>", true);
    cache.set("maxPlayers", 100, true);
    cache.set("config:loaded", true, true);
}
```

## Notes

- Cache is **in-memory only** — data is lost on server restart
- For persistent storage across restarts, use [SQL](007-sql.md)
- Keys are case-sensitive strings
- Both caches use `ConcurrentHashMap` internally — thread-safe

## Next Steps

- [006 - HTTP](006-http.md) — Making HTTP requests
- [007 - SQL](007-sql.md) — Database queries
- [008 - Async](008-async.md) — Async/await patterns
