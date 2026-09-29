# ReactiveMusic Agent Notes

ReactiveMusic is a Minecraft mod that replaces vanilla music with dynamic, event-driven music and bundled/user songpacks.

## Layout

- `src/main/java/` and `src/main/resources/` are shared/common code and resources.
- `fabric/` contains Fabric-loader-specific setup (currently the only loader).
- `versions/<mc version>/gradle.properties` contains per-version dependency values.
- `stonecutter.gradle.kts` controls the active Minecraft version and helper tasks.
- `run/<mc version>/<loader>/` contains isolated development instances.

## Minecraft 26.x and mapping names

Starting with Minecraft 26.1 (Mojang's year.release versioning), the game ships unobfuscated:
there's no Yarn/intermediary layer anymore, and all Minecraft-facing code is written against
Mojang's own official (mojmap) class/method names directly (e.g. `net.minecraft.client.Minecraft`,
`net.minecraft.world.entity.Entity`, not `MinecraftClient`/`Entity` from Yarn). The build uses
`dev.architectury.loom-no-remap` (not the classic `dev.architectury.loom`) for this reason — no
`mappings(...)` dependency, no `mod`-prefixed dependency configurations, and `shadowJar` instead of
`remapJar` as the real build output (see `fabric/build.gradle.kts`).

Only `26.1.2` is currently built (see `settings.gradle.kts`). `26.2` needs further porting beyond
the mapping switch — Mojang changed real API shape between the two (e.g. `Minecraft.screen` was
removed in favor of `setScreenAndShow(Screen)`, `Gui.getBossOverlay()` was relocated). When
resuming that work, don't guess at symbol names — extract the real class from
`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/<version>/` and
inspect it with `javap -p`, since even close-by versions can rename things.

## Stonecutter

This repo uses Stonecutter comments (`//? if ...`) for Minecraft-version differences.

Do not manually flip Stonecutter comment blocks to switch versions. Use the Stonecutter/Gradle tasks.

Common commands:

- Refresh active source state: `./gradlew "Refresh active project"`
- Run active Fabric client: `./gradlew runActiveClientFabric`
- Build all supported artifacts: `./gradlew chiseledBuild`

## Committing

1. Run `./gradlew chiseledBuild --no-build-cache`.
2. Before staging, run `./gradlew "Reset active project"` to normalize sources to `26.1.2`.
3. Review `git status` and `git diff`, stage explicit paths, then review `git diff --cached` before committing.

## Runtime smoke testing

Before releases, run `powershell -ExecutionPolicy Bypass -File scripts/release-smoke-test.ps1`. It builds production artifacts, then uses cached HeadlessMC installations to run every supported client in parallel. Each client creates a world, waits for a loaded chunk, verifies ReactiveMusic starts a song, and exits automatically.

- `26.1.2`: Fabric only, needs Java 25. This target is untested against HeadlessMC — verify a
  manual run works before trusting it in CI (see the note at the top of the script).

Finish with `./gradlew "Reset active project"` and `./gradlew chiseledBuild --no-build-cache`.

Supported targets:

- Fabric: `26.1.2`
