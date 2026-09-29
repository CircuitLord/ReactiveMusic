plugins {
    id("dev.architectury.loom-no-remap")
    id("architectury-plugin")
}

val minecraft = stonecutter.current.version

version = "${mod.version}+$minecraft"
group = mod.group
base {
    archivesName.set("${mod.id}-common")
}

architectury.common(stonecutter.tree.branches.mapNotNull {
    if (stonecutter.current.project !in it) null
    else it.project.prop("loom.platform")
})

repositories {
    maven("https://maven.terraformersmc.com/releases/") { name = "Terraformers" }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    implementation("net.fabricmc:fabric-loader:${mod.dep("fabric_loader")}")
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.build {
    group = "versioned"
    description = "Must run through 'chiseledBuild'"
}
