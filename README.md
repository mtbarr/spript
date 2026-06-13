<div align="center">
  <h1>Spript</h1>
  <p><b>A fast, ergonomic, and modern JavaScript Engine for Spigot 1.21</b></p>
  <p>Write Minecraft plugins in TypeScript/JavaScript with hot-reloading, autocomplete, and a rich standard library.</p>
</div>

---

## Features

- **TypeScript or JavaScript**: Write in TypeScript with full autocomplete, or use vanilla JS — your choice.
- **Hot-Reloading**: Run `/jsreload` in-game to see changes instantly. Or reload a single file with `/jsreload my-script.js`.
- **Rich Standard Library**: Event listeners, commands, chat formatting (MiniMessage), HTTP requests, SQL databases, caching, scheduling, and item creation — all built-in.
- **Async/Await**: HTTP and SQL operations return Promises resolved on the main thread. Full `async/await` support.
- **Dynamic Commands**: Register commands and tab completers at runtime — no `plugin.yml` entries needed.
- **Native Kyori Adventure**: Format messages with MiniMessage (`<gradient:red:blue>`, `<#FF5555>`, etc.).
- **Modular**: Split your code across files with a built-in `require()` module system.
- **No Java Required**: Write fully functional Spigot plugins using only JavaScript/TypeScript.

---

## Installation (Server Owners)

1. Download the latest `Spript.jar` from [Releases](https://github.com/mtbarr/spript/releases).
2. Drop the `.jar` into your server's `plugins/` folder.
3. Start the server. The plugin auto-generates `plugins/Spript/scripts/` with the standard library.
4. Drop your `.js` scripts into `plugins/Spript/scripts/` and run `/jsreload`.

---

## Development Quick Start

### One-Line Project Scaffold

```bash
npx create-spript-project my-script
cd my-script
npm run build
```

This gives you a complete TypeScript workspace with Babel, Java import transforms, and VS Code autocomplete for Bukkit/Paper/NMS.

### Write Your Script

```typescript
import { PlayerJoinEvent } from 'org.bukkit.event.player';
import { CommandSender } from 'org.bukkit.command';
import { BlockBreakEvent } from 'org.bukkit.event.block';

listen(PlayerJoinEvent, (event) => {
    msg(event.getPlayer(), "<gradient:gold:yellow>Welcome to the server!</gradient>");
});

command("hello", (sender: CommandSender, label: string, args: string[]) => {
    msg(sender, "<green>Hello, " + sender.getName() + "!</green>");
});

command("broadcast", async (sender, label, args) => {
    const response = await http.get("https://api.example.com/data");
    const data = JSON.parse(response.body());
    msg(Bukkit.getServer(), "<gold>" + data.message + "</gold>");
}, "Broadcast a message from an API");
```

### Deploy

```bash
npm run build                    # Compile src/ → dist/
cp dist/* /server/plugins/Spript/scripts/
```

In-game or console:
```
/jsreload
```

To reload a single file and its dependencies:
```
/jsreload my-script.js
```

---

## Standard Library

| Function | Description |
|----------|-------------|
| `listen(event, callback, priority?)` | Listen to Bukkit events |
| `command(name, callback, desc?, usage?, aliases?, tabCompleter?)` | Register a dynamic command |
| `msg(target, text)` | Send a MiniMessage-formatted message |
| `component(text)` | Parse MiniMessage into a Component |
| `setTimeout(callback, ticks)` | Run once after a delay (20 ticks = 1 sec) |
| `setInterval(callback, ticks)` | Run repeatedly every N ticks |
| `runSync(callback)` | Execute on the main thread |
| `runAsync(callback)` | Execute off the main thread |
| `createItem(material, amount?, name?, lore?)` | Create an ItemStack with MiniMessage display name/lore |
| `cache.set(key, value, persistent?)` | Store a value in memory |
| `cache.get(key)` | Retrieve a value |
| `cache.getOrDefault(key, default)` | Retrieve with fallback |
| `cache.delete(key)` | Remove a value |
| `http.get(url, headers?)` | Make an async GET request |
| `http.post(url, body, headers?)` | Make an async POST request |
| `sql.createPool(config)` | Create a HikariCP connection pool |
| `sql.query(pool, sql, params?)` | Execute a SELECT query (async) |
| `sql.execute(pool, sql, params?)` | Execute an INSERT/UPDATE/DELETE (async) |

---

## API Overview

### Events

```typescript
import { PlayerJoinEvent } from 'org.bukkit.event.player';
import { EventPriority } from 'org.bukkit.event';

listen(PlayerJoinEvent, (event) => {
    msg(event.getPlayer(), "<green>Welcome!</green>");
}, EventPriority.NORMAL);
```

### Commands

```typescript
command("heal", (sender, label, args) => {
    const player = sender as Player;
    player.setHealth(20);
    msg(player, "<green>Healed!</green>");
}, "Heal yourself", "/heal", ["health"], (sender, alias, args) => {
    return ["@p", "@a"].filter(s => s.startsWith(args[0]));
});
```

### Chat (MiniMessage)

```typescript
msg(player, "<red><b>Warning!</b></red> <gray>Read the rules.</gray>");
msg(player, "<gradient:#FF5555:#55FF55>Gradient text</gradient>");
```

### Caching

```typescript
cache.set("playerCount", 42);                           // ephemeral (cleared on /jsreload)
cache.set("serverId", "my-server", true);                // persistent (survives reload)
const count = cache.get<number>("playerCount");          // 42
const val = cache.getOrDefault("missing", "default");    // "default"
cache.delete("playerCount");
```

### HTTP

```typescript
const response = await http.get("https://api.github.com/repos/anomalyco/opencode");
const data = JSON.parse(response.body());

const res = await http.post("https://api.example.com/data", { key: "value" }, {
    "Authorization": "Bearer token"
});
```

### SQL

```typescript
const pool = sql.createPool({
    url: "jdbc:postgresql://localhost:5432/minecraft",
    user: "admin",
    password: "secret"
});

const rows = await sql.query(pool, "SELECT * FROM players WHERE score > ?", [100]);
await sql.execute(pool, "UPDATE players SET score = ? WHERE uuid = ?", [500, uuid]);
```

### Async / Scheduling

```typescript
setTimeout(() => msg(player, "<green>5 seconds later!</green>"), 100);
setInterval(() => msg(server, "<gold>Announcement</gold>"), 200);

runAsync(() => {
    const result = heavyComputation();
    runSync(() => msg(player, "Result: " + result));
});
```

---

## Project Structure

```
plugins/Spript/
├── scripts/
│   ├── stdlib.js       # Injected automatically
│   ├── my-script.js    # Your scripts
│   └── helpers.js      # Loaded via require()
└── Spript.jar          # The plugin
```

---

## Tutorials

See the full tutorial series in [`docs/`](docs/):

| # | Guide | Topic |
|---|-------|-------|
| 001 | [Getting Started](docs/001-getting-started.md) | Setup, scaffold, reload |
| 002 | [Events](docs/002-events.md) | listen(), priority, cancellation |
| 003 | [Commands](docs/003-commands.md) | command(), args, tab completion |
| 004 | [Chat](docs/004-chat.md) | msg(), component(), MiniMessage |
| 005 | [Cache](docs/005-cache.md) | In-memory key-value storage |
| 006 | [HTTP](docs/006-http.md) | GET/POST requests |
| 007 | [SQL](docs/007-sql.md) | Connection pools, queries |
| 008 | [Async](docs/008-async.md) | async/await, scheduling |

---

## Building from Source

Requirements: Java 21+

```bash
git clone https://github.com/mtbarr/spript.git
cd spript
./gradlew build
```

The compiled jar is in `build/libs/`.

---

## License

MIT
