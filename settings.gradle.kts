pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.6"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true
    create(rootProject) {
        versions("26.1.2")
        // 26.2 needs more porting work beyond the mapping-scheme switch: Minecraft.screen was
        // removed in favor of setScreenAndShow(Screen) with no public getter, and Gui.getBossOverlay()
        // was relocated. Re-add "26.2" here once ModConfig/SongPicker are updated for those.
        branch("fabric")
    }
}

rootProject.name = "ReactiveMusic"
