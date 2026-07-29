plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.13-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}
stonecutter active "1.19.2" /* [SC] DO NOT EDIT */

// Builds every version into `build/libs/{mod.version}/{loader}`
stonecutter registerChiseled tasks.register("chiseledBuild", stonecutter.chiseled) {
    group = "project"
    ofTask("buildAndCollect")
}

// Builds loader-specific versions into `build/libs/{mod.version}/{loader}`
for (it in stonecutter.tree.branches) {
    if (it.id.isEmpty()) continue
    val loader = it.id.upperCaseFirst()
    stonecutter registerChiseled tasks.register("chiseledBuild$loader", stonecutter.chiseled) {
        group = "project"
        versions { branch, _ -> branch == it.id }
        ofTask("buildAndCollect")
    }
}

// Runs active versions for each loader
// For branches where the active version exists, use a simple dependsOn.
// For branches where it doesn't (e.g. Forge only has 1.20.1 but active is 1.21.1),
// use a chiseled task so Stonecutter properly sets up sources via chiseledSrc.
for (branch in stonecutter.tree.branches) {
    if (branch.id.isEmpty()) continue
    val loader = branch.id.upperCaseFirst()
    val types = listOf("Client", "Server")

    val activeNode = branch.nodes.find { it.metadata == stonecutter.current }
    if (activeNode != null) {
        // Active version exists in this branch — direct dependsOn works
        for (type in types) tasks.register("runActive$type$loader") {
            group = "project"
            dependsOn("${activeNode.hierarchy}:run$type")
        }
    } else {
        // No active version in this branch — use chiseled tasks to set up sources
        for (type in types) {
            stonecutter registerChiseled tasks.register("runActive$type$loader", stonecutter.chiseled) {
                group = "project"
                versions { branchId, _ -> branchId == branch.id }
                ofTask("run$type")
            }
        }
    }
}
