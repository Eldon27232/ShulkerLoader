# Changelog

## 1.0.12+mc1.21.10

- Port Fabric build to Minecraft 1.21.10.
- Update mappings and Fabric API for Minecraft 1.21.10.
- Fix the 1.21.10 offhand pickup crash caused by the changed `PlayerInventory` offhand field, which could appear as `field_7544` / `NoSuchFieldError`.
- Lower Fabric Loader metadata requirement to 0.17.3 or newer.
- Document that this 1.21.10 port should be installed on both client and server.
