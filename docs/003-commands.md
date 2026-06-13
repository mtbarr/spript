# 003 - Commands

## Basic Command

Use the global `command()` function to register a new command:

```typescript
import { CommandSender } from 'org.bukkit.command';

command("hello", (sender: CommandSender, label: string, args: string[]) => {
    msg(sender, "<green>Hello, " + sender.getName() + "!");
});
```

In vanilla JS:

```js
command("hello", function(sender, label, args) {
    msg(sender, "§aHello, " + sender.getName() + "!");
});
```

Players can now run `/hello` in-game.

## Full Signature

```
command(name, callback, description?, usage?, aliases?, tabCompleter?)
```

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `name` | `string` | — | Command name (the part after `/`) |
| `callback` | `(sender, label, args) => void` | — | Executor function |
| `description` | `string` | `""` | Description shown in `/help` |
| `usage` | `string` | `"/<name>"` | Usage syntax |
| `aliases` | `string[]` | `[]` | Alternative command names |
| `tabCompleter` | `(sender, alias, args) => string[]` | `null` | Tab completion function |

## Complete Example

```typescript
import { CommandSender } from 'org.bukkit.command';

command("broadcast",
    (sender: CommandSender, label: string, args: string[]) => {
        if (args.length === 0) {
            msg(sender, "<red>Usage: /broadcast <message>");
            return;
        }
        const message = args.join(" ");
        const senderName = sender.getName();
        // Broadcast to all players
        msg(Bukkit.getServer(), `<gold>[${senderName}]</gold> ${message}`);
    },
    "Broadcast a message to the server",
    "/broadcast <message>",
    ["bc", "shout"],
    (sender: CommandSender, alias: string, args: string[]) => {
        if (args.length >= 1) {
            return ["hello", "world", "test"].filter(s =>
                s.toLowerCase().startsWith(args[0].toLowerCase())
            );
        }
        return [];
    }
);
```

## Permission Check

```typescript
import { CommandSender } from 'org.bukkit.command';

command("admin", (sender: CommandSender, label: string, args: string[]) => {
    if (!sender.hasPermission("myplugin.admin")) {
        msg(sender, "<red>You don't have permission!");
        return;
    }
    msg(sender, "<green>Admin command executed!");
});
```

## Async Commands

Commands can be `async` for HTTP requests or database queries:

```typescript
import { CommandSender } from 'org.bukkit.command';

command("uuid", async (sender: CommandSender, label: string, args: string[]) => {
    if (args.length === 0) {
        msg(sender, "<red>Usage: /uuid <player>");
        return;
    }
    
    msg(sender, "<yellow>Fetching UUID...");
    
    const response = await http.get(
        `https://api.mojang.com/users/profiles/minecraft/${args[0]}`
    );
    const data = JSON.parse(response.body());
    msg(sender, `<green>UUID: ${data.id}`);
});
```

## Tab Completion

The 6th parameter is a function that returns suggestions:

```typescript
import { CommandSender } from 'org.bukkit.command';

command("giveitem",
    (sender, label, args) => {
        // handle command
    },
    "Give yourself an item",
    "/giveitem <player> <item>",
    [],
    (sender: CommandSender, alias: string, args: string[]) => {
        if (args.length === 1) {
            // Suggest online players
            return Bukkit.getOnlinePlayers().stream()
                .map((p: any) => p.getName())
                .filter((name: string) => name.toLowerCase().startsWith(args[0].toLowerCase()))
                .toArray();
        }
        if (args.length === 2) {
            // Suggest materials
            return ["DIAMOND", "IRON_INGOT", "GOLD_INGOT"].filter(m =>
                m.toLowerCase().startsWith(args[1].toLowerCase())
            );
        }
        return [];
    }
);
```

## Notes

- Commands are registered dynamically (no `plugin.yml` entries needed)
- All commands are automatically unregistered on `/jsreload`
- Tab completer receives the raw `args` array — filter against `args[args.length - 1]` for prefix matching

## Next Steps

- [004 - Chat](004-chat.md) — MiniMessage formatting and messaging
- [008 - Async](008-async.md) — Async/await patterns
