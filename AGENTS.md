# Spript - Agent Instructions

## Critical Constraints
* **NEVER COMPILE THE PROJECT**: Do not run `./gradlew build` or any compilation step autonomously unless the user explicitly asks you to. The user has explicitly forbidden unsolicited compilation (noted as a hard rule in `build.gradle.kts`).

## Repository Architecture
The repo contains two distinct applications:

1. **Spript Engine (Root directory)**
   * A Spigot 1.21-R0.1-SNAPSHOT plugin providing a JavaScript runtime.
   * **Language**: Pure Kotlin 2.0 (no `.java` files).
   * **JavaScript Engine**: Uses standalone `Nashorn 15.7` (since Nashorn was removed in Java 15). Configured to run ES6.
   * **Chat/Text**: Uses `Kyori Adventure` (`MiniMessage`) natively via `MiniMessageUtil.kt`. `CommandSender` interfaces are treated as `Audience`.
   * **Stdlib**: `src/main/resources/scripts/stdlib.js` contains global JS utilities (`listen`, `command`, `setTimeout`, etc.). It is injected into the engine before user scripts.
   * **Packaging**: Built using the standard Gradle plugin. Dependencies are loaded natively via Spigot `libraries` tag, generating a micro-jar.

2. **Spript CLI (`create-spript-project/`)**
   * A Node.js CLI tool to scaffold local TypeScript project templates for Spript developers.
   * Provides auto-complete in VS Code by pulling `@grakkit-types/paper` and `@grakkit-types/java` via NPM.
   * Generates `tsconfig.json` and a custom `spript-env.d.ts` defining the plugin's global API.

## Developer Commands
* **Build Plugin (Only if explicitly requested)**: `./gradlew build` (the runnable artifact is generated in `build/libs/`).
* **Test CLI Generator**: `cd create-spript-project && node run.js <test-project-name>`
