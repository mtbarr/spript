<div align="center">
  <h1>Spript</h1>
  <p><em>JavaScript, meet Minecraft.</em></p>
  <p>A lightweight scripting engine for Spigot that lets you write server logic in TypeScript — no Java, no restarts, no ceremony.</p>
</div>

<br>

Spript is a plugin that embeds a modern JavaScript runtime directly into your Spigot server. It enables you to listen to events, register commands, query databases, make HTTP requests, format chat with MiniMessage, cache data in memory, and schedule tasks — using only TypeScript or JavaScript.

Scripts run inside Nashorn 15.7 with full ES6 support, transpiled through Babel and backed by a custom standard library that wraps Bukkit's API into clean, idiomatic function calls. The plugin loads your code from a folder, watches your changes through `/jsreload`, and cleans up after itself on every reload.

A companion CLI, `create-spript-project`, scaffolds a complete TypeScript workspace with Babel, type definitions for the entire Paper and Java standard libraries, and a custom Babel plugin that transforms Java import statements into `Java.type()` calls at compile time — giving you autocomplete for everything from `Player` to `ChunkSnapshot`.

## Create a Spript project

Download and run only the project generator:

```bash
bash install.sh my-spript-project
```

Or download the setup script remotely with curl:

```bash
curl -fsSL https://raw.githubusercontent.com/mtbarr/spript/main/install.sh -o install.sh
bash install.sh my-spript-project
```

Under the hood, Spript manages listener registration, dynamic command mapping, executor service isolation for HTTP and SQL, and two-tier caching — all scoped per script file, so partial reloads only touch what they need to.

MIT licensed.
