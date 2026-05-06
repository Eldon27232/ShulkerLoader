# Shulker Loader

Shulker Loader redirects item pickups into a shulker box held in the player's offhand.

## Minecraft 1.21.10 Fabric Port

This port targets Minecraft 1.21.10 on Fabric.

Supported versions:

- Minecraft 1.21.10
- Fabric Loader 0.17.3 or newer
- Fabric API for Minecraft 1.21.10
- Java 21

## Installation

Install the mod jar on both the client and the server. For singleplayer, install it in the client's `mods` folder.

The 1.21.10 port is not documented as server-only because the current port should be present on both sides when joining a Fabric server.

## Behavior

When the player holds exactly one shulker box in the offhand, picked-up non-shulker items are inserted into that offhand shulker box before normal inventory pickup handling.

Limits:

- Shulker boxes are not inserted into other shulker boxes.
- The offhand shulker box stack must have `count == 1`.
- When the offhand shulker box is full, or the picked-up item cannot fit, the remaining items continue through the normal vanilla inventory pickup flow.

## Upgrade Notes

Minecraft 1.21.10 changed the `PlayerInventory` structure used by older builds. Direct access to the old offhand field is no longer compatible and can crash with `NoSuchFieldError` for `field_7544`.

This port fixes the 1.21.10 pickup crash by using the public offhand stack API instead of accessing the removed inventory field.

## Test Steps

1. Install the jar on both the client and the server.
2. Start Minecraft 1.21.10 with Fabric.
3. Put an empty shulker box in the player's offhand.
4. Drop and pick up a stack of normal stackable items.
5. Confirm the items go into the offhand shulker box and the game does not crash.
