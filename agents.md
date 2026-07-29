# ReactiveMusic Agent Notes

ReactiveMusic is a cross-loader Minecraft mod that replaces vanilla music with dynamic, event-driven music and bundled/user songpacks.

## Layout

- `src/main/java/` and `src/main/resources/` are shared/common code and resources.
- `fabric/`, `forge/`, and `neoforge/` contain loader-specific setup.
- `versions/<mc version>/gradle.properties` contains per-version dependency values.
- `stonecutter.gradle.kts` controls the active Minecraft version and helper tasks.
- `run/<mc version>/<loader>/` contains isolated development instances.

## Stonecutter

This repo uses Stonecutter comments (`//? if ...`) for Minecraft-version differences.

Do not manually flip Stonecutter comment blocks to switch versions. Use the Stonecutter/Gradle tasks.

Common commands:

- Refresh active source state: `./gradlew "Refresh active project"`
- Run active Fabric client: `./gradlew runActiveClientFabric`
- Run active NeoForge client: `./gradlew runActiveClientNeoforge`
- Run NeoForge 1.21.1 directly: `./gradlew :neoforge:1.21.1:runClient`
- Build all supported artifacts: `./gradlew chiseledBuild`

## Committing

1. Run `./gradlew chiseledBuild --no-build-cache`.
2. Before staging, run `./gradlew "Reset active project"` to normalize sources to `1.19.2`.
3. Review `git status` and `git diff`, stage explicit paths, then review `git diff --cached` before committing.

## Runtime smoke testing

Before releases, run `powershell -ExecutionPolicy Bypass -File scripts/release-smoke-test.ps1`. It builds production artifacts, then uses cached HeadlessMC installations to run every supported client in parallel. Each client creates a world, waits for a loaded chunk, verifies ReactiveMusic starts a song, and exits automatically.

- `1.19.2`: Fabric and Forge
- `1.20.1`: Fabric and Forge
- `1.21.1`: Fabric and NeoForge
- `1.21.11`: Fabric

Finish with `./gradlew "Reset active project"` and `./gradlew chiseledBuild --no-build-cache`.

Supported targets:

- Fabric: `1.19.2`, `1.20.1`, `1.21.1`, `1.21.11`
- Forge: `1.19.2`, `1.20.1`
- NeoForge: `1.21.1`
