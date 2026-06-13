# 001 - Getting Started

## What is Spript?

Spript is a Spigot 1.21 plugin that runs JavaScript/TypeScript inside your Minecraft server. Write server logic in TypeScript, compile with Babel, and reload without restarting.

## Prerequisites

- Spigot 1.21 server
- Java 21 or later
- Node.js (for the TypeScript project scaffold)

## Installing the Plugin

1. Place the `Spript.jar` in your server's `plugins/` folder
2. Start/restart the server
3. Spript creates `plugins/Spript/scripts/` with a bundled `stdlib.js`

## Creating a TypeScript Project

```bash
npx create-spript-project my-scripts
cd my-scripts
npm run build
```

This scaffolds:

- `src/index.ts` — your main script
- `tsconfig.json` — TypeScript config
- `babel.config.js` — Babel with TypeScript + Java import transform
- `babel-plugin-java-imports.js` — custom Babel plugin that converts Java imports to `Java.type()` calls
- `spript-env.d.ts` — type definitions for Spript's global API
- `package.json` — build scripts

## Manual Setup

If you prefer vanilla JavaScript, just create `.js` files directly in `plugins/Spript/scripts/`:

```js
// plugins/Spript/scripts/my-script.js
listen("org.bukkit.event.player.PlayerJoinEvent", function(event) {
    var player = event.getPlayer();
    msg(player, "<green>Welcome to the server!");
});
```

## Building & Deploying

```bash
npm run build    # Compile src/ → dist/
npm run dev      # Watch mode, auto-compile on save
```

Copy `dist/` contents to your server's `plugins/Spript/scripts/` folder.

## Reloading Scripts

After copying new compiled files to the server, run in-game or console:

```
/jsreload
```

This unloads all listeners/commands/timers, clears ephemeral cache, and reloads every script.

## Project Structure

```
plugins/Spript/
├── scripts/
│   ├── stdlib.js          # Injected automatically (do not edit)
│   ├── my-script.js       # Your compiled TypeScript/JS
│   └── helpers.js         # Module loaded via require()
└── config.yml             # (future)
```

## TypeScript vs JavaScript

Spript supports both:

| Feature | Vanilla JS | TypeScript + Babel |
|---------|-----------|-------------------|
| Type checking | No | Yes (tsc --noEmit) |
| Java imports | Full class string | `import { X } from 'org.bukkit.*'` |
| Async/await | Yes (via regenerator) | Yes |
| Required setup | Just create .js files | npm install + build |

## Next Steps

- [002 - Events](002-events.md) — Listen to Bukkit events
- [003 - Commands](003-commands.md) — Register commands with tab completion
- [004 - Chat](004-chat.md) — Send messages with MiniMessage formatting
