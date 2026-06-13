# 008 - Async/Await & Scheduling

## Overview

Spript bridges Java's `CompletableFuture` to JavaScript Promises, enabling natural `async/await` syntax. It also provides scheduler functions for delayed/repeating tasks.

## Async/Await with HTTP and SQL

HTTP and SQL methods return Promises automatically:

```typescript
async function fetchData() {
    try {
        const response = await http.get("https://api.example.com/data");
        const data = JSON.parse(response.body());
        return data;
    } catch (error) {
        console.log("Error:", error);
        return null;
    }
}
```

```typescript
async function getPlayerScore(uuid: string) {
    const rows = await sql.query(pool,
        "SELECT score FROM players WHERE uuid = ?",
        [uuid]
    );
    return rows.length > 0 ? rows[0].score : 0;
}
```

## Scheduling

### setTimeout — Run Once After Delay

```typescript
// Run after 20 ticks (1 second)
setTimeout(() => {
    msg(player, "<green>1 second has passed!</green>");
}, 20);

// Run after 100 ticks (5 seconds)
setTimeout(() => {
    msg(player, "<yellow>5 seconds later...</yellow>");
}, 100);
```

### setInterval — Run Repeatedly

```typescript
// Announce every 100 ticks (5 seconds)
const intervalId = setInterval(() => {
    msg(Bukkit.getServer(), "<gold>This is a periodic announcement</gold>");
}, 100);
```

**Note:** `setTimeout` and `setInterval` use **ticks** (20 ticks = 1 second), not milliseconds.

## Thread Management

### runSync — Execute on Main Thread

```typescript
// From an async context, ensure main-thread safety
runSync(() => {
    player.sendMessage("This runs on the main thread");
});
```

### runAsync — Execute Off Main Thread

```typescript
// Heavy computation off the main thread
runAsync(() => {
    // This runs asynchronously
    // Do NOT call Bukkit APIs here (not thread-safe)
    const result = performHeavyComputation();
    
    // Switch back to main thread for Bukkit API calls
    runSync(() => {
        msg(player, `<green>Result: ${result}</green>`);
    });
});
```

## Working with Java CompletableFuture

Use `awaitFuture()` to convert any Java `CompletableFuture` to a JS Promise:

```typescript
// Example: Using a Java API that returns CompletableFuture
const future = someJavaObject.someAsyncMethod();
const result = await awaitFuture(future);
```

This is what `http` and `sql` use internally.

## Complete Example: Async Data Pipeline

```typescript
import { CommandSender } from 'org.bukkit.command';
import { PlayerJoinEvent } from 'org.bukkit.event.player';

command("syncprofile", async (sender: CommandSender, label: string, args: string[]) => {
    const target = args.length > 0 ? args[0] : sender.getName();
    
    msg(sender, `<yellow>Fetching profile for ${target}...</yellow>`);
    
    try {
        // 1. HTTP request (async, virtual thread)
        const response = await http.get(`https://api.mojang.com/users/profiles/minecraft/${target}`);
        const mojangData = JSON.parse(response.body());
        
        // 2. Database query (async, virtual thread)
        let playerData = await sql.query(pool,
            "SELECT * FROM player_data WHERE uuid = ?",
            [mojangData.id]
        );
        
        // 3. Process on main thread if needed
        runSync(() => {
            if (playerData.length === 0) {
                msg(sender, "<yellow>New player! Creating profile...</yellow>");
            } else {
                msg(sender, `<green>Found ${playerData.length} record(s)</green>`);
            }
        });
        
        // 4. Schedule a follow-up task
        setTimeout(() => {
            msg(sender, "<gray>Profile sync completed 1 second ago</gray>");
        }, 20);
        
    } catch (error) {
        msg(sender, `<red>Error: ${error.message || error}</red>`);
    }
});
```

## Thread Safety Rules

| API | Thread-Safe? | Notes |
|-----|-------------|-------|
| `msg()` | Yes | Safe from any thread |
| `cache.*` | Yes | Uses `ConcurrentHashMap` |
| `http.*` | Yes | Runs on virtual threads |
| `sql.*` | Yes | Runs on virtual threads |
| Bukkit APIs | **No** | Must run on main thread via `runSync()` |
| Player/World/Entity | **No** | Must run on main thread |

## Unloading

All scheduled tasks are automatically cancelled on `/jsreload` or server shutdown. You don't need to manually clean up timers or intervals.

## Next Steps

Review any topic:
- [002 - Events](002-events.md)
- [003 - Commands](003-commands.md)
- [005 - Cache](005-cache.md)
- [006 - HTTP](006-http.md)
- [007 - SQL](007-sql.md)
