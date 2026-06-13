# 004 - Chat & Messaging

## Sending Messages

Use `msg()` to send formatted messages to players or the console:

```typescript
import { CommandSender } from 'org.bukkit.command';

// To a single player
msg(player, "<green>Hello!</green>");

// To console
msg(Bukkit.getConsoleSender(), "<gold>[MyPlugin] Server started");
```

`msg()` accepts anything implementing `Audience` (players, console, `CommandSender`, etc.).

## MiniMessage Formatting

Spript uses **Kyori Adventure MiniMessage** for rich text formatting. No need for legacy `§` codes.

### Colors

```
<black>   <dark_blue>   <dark_green>   <dark_aqua>
<dark_red>  <dark_purple>  <gold>         <gray>
<blue>      <green>        <aqua>         <red>
<light_purple>  <yellow>     <white>
```

### Basic Styles

```
<b>bold</b>         <i>italic</i>       <u>underline</u>
<s>strikethrough</s>  <obf>obfuscated</obf>
```

### Hex Colors

```
<#FF5555>Red text</#FF5555>
<color:#55FF55>Green text</color>
```

### Gradients

```
<gradient:red:blue>Gradient text</gradient>
<gradient:#FF0000:#0000FF>Custom gradient</gradient>
```

### Examples

```typescript
msg(player, "<red><b>Warning!</b></red> <gray>This is important</gray>");
msg(player, "<gradient:gold:light_purple>Fancy title</gradient>");
msg(player, "<aqua><u>Click me</u></aqua>");
```

## Component Builder

Use `component()` to create reusable Adventure `Component` objects:

```typescript
const title = component("<gradient:gold:yellow><b>=== My Server ===</b></gradient>");
msg(player, title);

const warning = component("<red><b>!</b></red> <gold>Please read the rules</gold>");
msg(player, warning);
```

## Broadcasting

```typescript
// Broadcast to all players
Bukkit.getServer().broadcast(component("<gold><b>Server announcement!</b></gold>"));

// Or iterate
Bukkit.getOnlinePlayers().forEach((player: any) => {
    msg(player, "<green>Your personal message</green>");
});
```

## Creating Items with Custom Names

```typescript
const item = createItem("DIAMOND_SWORD", 1,
    "<gradient:gold:yellow>Legendary Sword</gradient>",
    [
        "<gray>A powerful weapon</gray>",
        "",
        "<green>+10 Damage</green>",
        "<dark_gray>Rare item</dark_gray>"
    ]
);

if (item) {
    player.getInventory().addItem(item);
}
```

`createItem(material, amount?, displayName?, loreLines?)` returns an `ItemStack` with MiniMessage-formatted display name and lore.

## Multiline Messages

Since manual line breaks work in MiniMessage:

```typescript
msg(player, [
    "<gold>=== Menu ===</gold>",
    "<green>/warp spawn</green>  <gray>Teleport to spawn</gray>",
    "<green>/warp pvp</green>    <gray>Teleport to PvP arena</gray>",
    "<gold>============</gold>"
].join("\n"));
```

## Next Steps

- [005 - Cache](005-cache.md) — In-memory key-value storage
- [006 - HTTP](006-http.md) — Making HTTP requests
- [007 - SQL](007-sql.md) — Database queries
