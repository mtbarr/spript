# 002 - Events

## Basic Usage

Use the global `listen()` function to listen to Bukkit events:

```typescript
import { PlayerJoinEvent } from 'org.bukkit.event.player';
import { EventPriority } from 'org.bukkit.event';

listen(PlayerJoinEvent, (event: PlayerJoinEvent) => {
    const player = event.getPlayer();
    msg(player, "<green>Welcome to the server!");
});
```

In vanilla JS:

```js
listen("org.bukkit.event.player.PlayerJoinEvent", function(event) {
    var player = event.getPlayer();
    msg(player, "<green>Welcome!");
});
```

## Signature

```
listen(eventClass, callback, priority?)
```

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `eventClass` | `Class<Event>` \| `string` | — | Bukkit event class or full class name string |
| `callback` | `(event) => void` | — | Function called when event fires |
| `priority` | `EventPriority` \| `string` | `"NORMAL"` | Event priority (see below) |

## Event Priority

Pass an `EventPriority` or string:

```typescript
import { EventPriority } from 'org.bukkit.event';

listen(PlayerJoinEvent, handler, EventPriority.LOWEST);
listen(PlayerJoinEvent, handler, "HIGH");
listen(PlayerJoinEvent, handler, "MONITOR");
```

Valid values: `LOWEST`, `LOW`, `NORMAL`, `HIGH`, `HIGHEST`, `MONITOR`

## Common Events

```typescript
import { PlayerJoinEvent } from 'org.bukkit.event.player';
import { PlayerChatEvent } from 'org.bukkit.event.player';
import { AsyncPlayerChatEvent } from 'org.bukkit.event.player';
import { PlayerInteractEvent } from 'org.bukkit.event.player';
import { BlockBreakEvent } from 'org.bukkit.event.block';
import { EntityDamageEvent } from 'org.bukkit.event.entity';
import { InventoryClickEvent } from 'org.bukkit.event.inventory';
```

## Cancelling Events

```typescript
import { BlockBreakEvent } from 'org.bukkit.event.block';

listen(BlockBreakEvent, (event: BlockBreakEvent) => {
    const player = event.getPlayer();
    if (!player.hasPermission("myplugin.break")) {
        msg(player, "<red>You don't have permission to break blocks!");
        event.setCancelled(true);
    }
}, "HIGH");
```

## Accessing Event Data

```typescript
import { PlayerInteractEvent } from 'org.bukkit.event.player';
import { Action } from 'org.bukkit.event.block';

listen(PlayerInteractEvent, (event: PlayerInteractEvent) => {
    if (event.getAction() === Action.RIGHT_CLICK_BLOCK) {
        const block = event.getClickedBlock();
        msg(event.getPlayer(), `<gold>You clicked: ${block.getType()}`);
    }
});
```

## Object-Oriented Style

Instead of a bare function, pass an object with a `handleEvent` method:

```js
var myListener = {
    handleEvent: function(event) {
        var player = event.getPlayer();
        msg(player, "<green>Object-style listener works too!");
    }
};

listen("org.bukkit.event.player.PlayerJoinEvent", myListener);
```

## Notes

- All listeners are automatically unregistered on `/jsreload` or server shutdown
- Event callbacks run on the Bukkit **main thread** by default
- For database/HTTP work inside events, use `runAsync()` (see [008-async](008-async.md))

## Next Steps

- [003 - Commands](003-commands.md) — Register commands with tab completion
- [004 - Chat](004-chat.md) — MiniMessage formatting and messaging
