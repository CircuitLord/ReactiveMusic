# ReactiveMusic Agent Notes

ReactiveMusic is a cross-loader Minecraft mod that replaces vanilla music with dynamic, event-driven music and bundled/user songpacks.

## Layout

- `src/main/java/` and `src/main/resources/` are shared/common code and resources.
- `fabric/`, `forge/`, and `neoforge/` contain loader-specific setup.
- `versions/<mc version>/gradle.properties` contains per-version dependency values.
- `stonecutter.gradle.kts` controls the active Minecraft version and helper tasks.
- `run/` is the shared dev run directory.

## Stonecutter

This repo uses Stonecutter comments (`//? if ...`) for Minecraft-version differences.

Do not manually flip Stonecutter comment blocks to switch versions. Use the Stonecutter/Gradle tasks.

Common commands:

- Refresh active source state: `./gradlew "Refresh active project"`
- Run active Fabric client: `./gradlew runActiveClientFabric`
- Run active NeoForge client: `./gradlew runActiveClientNeoForge`
- Run NeoForge 1.21.1 directly: `./gradlew :neoforge:1.21.1:runClient`
- Build all supported artifacts: `./gradlew chiseledBuild`

Supported targets:

- Fabric: `1.19.2`, `1.20.1`, `1.21.1`, `1.21.11`
- Forge: `1.19.2`, `1.20.1`
- NeoForge: `1.21.1`
