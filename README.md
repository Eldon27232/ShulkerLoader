# Shulker Loader 1.21.10 Fabric Port

This branch is a Minecraft 1.21.10 Fabric port of Shulker Loader. Shulker Loader redirects item pickups into a shulker box held in the player's offhand.

Supported versions:

- Minecraft 1.21.10
- Fabric Loader 0.17.3 or newer
- Fabric API for Minecraft 1.21.10
- Java 21

## Build

Use the Gradle wrapper:

```powershell
.\gradlew.bat clean build
```

The remapped mod jar is generated in `build/libs/`. Do not use the `-sources.jar` file as the gameplay mod.

## Installation

Install the mod jar on both the client and the server by placing it in each side's `mods` folder. For singleplayer, install it in the client's `mods` folder.

The 1.21.10 port is not documented as server-only because the current port should be present on both sides when joining a Fabric server.

## Behavior

When the player holds exactly one shulker box in the offhand, picked-up non-shulker items are inserted into that offhand shulker box before normal inventory pickup handling.

Limits:

- Shulker boxes are not inserted into other shulker boxes.
- The offhand shulker box stack must have `count == 1`.
- When the offhand shulker box is full, or the picked-up item cannot fit, the remaining items continue through the normal vanilla inventory pickup flow.
- Only the offhand shulker box is handled.
- Conflicts with other inventory or pickup mods may need separate compatibility testing.

## Fixes In This Port

Minecraft 1.21.10 changed the `PlayerInventory` structure used by older builds. Direct access to the old offhand field is no longer compatible and can crash with `NoSuchFieldError` for `field_7544`.

This port fixes the 1.21.10 pickup crash by using the public offhand stack API instead of accessing the removed inventory field.

The Fabric Loader metadata is compatible with Fabric Loader 0.17.3 or newer.

## Test Steps

1. Install the jar on both the client and the server.
2. Start Minecraft 1.21.10 with Fabric.
3. Put an empty shulker box in the player's offhand.
4. Drop and pick up a stack of normal stackable items, such as dirt or clay balls.
5. Confirm the items go into the offhand shulker box and the game does not crash.
