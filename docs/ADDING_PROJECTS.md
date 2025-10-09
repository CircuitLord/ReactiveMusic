# Adding New Projects to MochaMix Multi-Project Setup

This guide explains how to add new projects to the MochaMix multi-project build system while maintaining the sophisticated multi-version adapter architecture.

## Overview

The MochaMix project uses a **project-centric architecture** where each project can support multiple Minecraft versions through adapters. The structure follows this pattern:

```
projects/
├── [project-name]/
│   ├── common/           # Shared code across all versions
│   │   └── src/main/java/ # Standard Java structure
│   ├── v1_21_1/          # Base version adapter  
│   │   └── src/main/java/
│   ├── v1_21_5/          # Override version adapter
│   │   └── src/main/java/
│   └── [other-versions]/
```

## Step-by-Step Guide

### 1. Create Project Directory Structure

Create the basic directory structure for your new project:

```bash
# Example: Adding a new project called "logical-loadouts"
mkdir projects\logical-loadouts
mkdir projects\logical-loadouts\common
mkdir projects\logical-loadouts\common\src
mkdir projects\logical-loadouts\common\src\main
mkdir projects\logical-loadouts\common\src\main\java
mkdir projects\logical-loadouts\common\src\main\resources

# Create version adapters (start with base version)
mkdir projects\logical-loadouts\v1_21_1
mkdir projects\logical-loadouts\v1_21_1\src
mkdir projects\logical-loadouts\v1_21_1\src\main
mkdir projects\logical-loadouts\v1_21_1\src\main\java
mkdir projects\logical-loadouts\v1_21_1\src\main\resources
```

### 2. Create Build Files

Create `build.gradle` files for each adapter:

**projects/logical-loadouts/v1_21_1/build.gradle:**
```gradle
// This build.gradle file is automatically configured by the root build.gradle
// No additional configuration needed - all settings are inherited from root project
```

**projects/logical-loadouts/v1_21_5/build.gradle:** (if adding override version)
```gradle
// This build.gradle file is automatically configured by the root build.gradle
// No additional configuration needed - all settings are inherited from root project
```

### 3. Update Root Configuration Files

#### A. Update `settings.gradle`

Add your new project to the validation list:

```gradle
// Validate that the project and adapter directories exist
def projectDir = file("projects/${activeProject}")
if (!projectDir.exists()) {
    def availableProjects = fileTree(dir: 'projects', include: '*').files.collect { it.name }
    throw new GradleException("Project directory 'projects/${activeProject}' not found. Available projects: ${availableProjects}")
}
```

The validation will automatically discover your new project, but you may want to add it to documentation comments.

#### B. Update `switch-project.bat`

Add your project to the valid projects list:

```batch
REM Validate project
if "%1"=="mochamix" goto :checkAdapter
if "%1"=="logical-loadouts" goto :checkAdapter
echo Invalid project: %1
echo Valid options: mochamix, logical-loadouts
goto :eof
```

Also update the usage instructions:
```batch
echo Available projects: mochamix, logical-loadouts
```

#### C. Update `ide-context.gradle`

Add your project to the valid projects list:

```gradle
def getActiveProject() {
    def propsFile = file('gradle.properties')
    if (!propsFile.exists()) {
        return 'mochamix'
    }
    
    def props = new Properties()
    propsFile.withInputStream { props.load(it) }
    
    def project = props.getProperty('mochamix.project', 'mochamix')
    def validProjects = ['mochamix', 'logical-loadouts']  // Add your project here
    
    return validProjects.contains(project) ? project : 'mochamix'
}
```

### 4. Create Project Source Code

#### A. Common Code Structure

Place shared code under standard Java structure `projects/[project]/common/src/main/java/`:

```
projects/logical-loadouts/common/src/main/java/
└── com/
    └── mochamix/
        └── logicalloadouts/
            ├── LogicalLoadouts.java
            ├── LogicalLoadoutsClient.java
            └── api/

projects/logical-loadouts/common/src/main/resources/
├── fabric.mod.json
├── logicalloadouts.mixins.json
└── assets/
    └── logical-loadouts/
        ├── icon.png
        └── lang/
```

**Note**: Common code now follows standard Maven/Gradle directory structure with `src/main/java/` for consistency.

#### B. Version-Specific Code

Place version-specific code under `projects/[project]/v1_21_1/src/main/java/`:

```
projects/logical-loadouts/v1_21_1/src/main/
├── java/
│   └── com/
│       └── mochamix/
│           └── logicalloadouts/
│               ├── mixin/
│               └── compat/
└── resources/
    └── assets/
        └── logical-loadouts/
```

### 5. Create fabric.mod.json

Create the mod metadata file at `projects/[project]/common/src/main/resources/fabric.mod.json`:

```json
{
    "schemaVersion": 1,
    "id": "logical-loadouts",
    "version": "1.0.0",
    "name": "Logical Loadouts",
    "description": "Equipment loadout management system for Minecraft",
    "authors": ["MochaMix"],
    "contact": {},
    "license": "MIT",
    "environment": "*",
    "entrypoints": {
        "main": [
            "com.mochamix.logicalloadouts.LogicalLoadouts"
        ],
        "client": [
            "com.mochamix.logicalloadouts.LogicalLoadoutsClient"
        ]
    },
    "mixins": [
        "logicalloadouts.mixins.json"
    ],
    "depends": {
        "fabricloader": ">=0.16.0",
        "minecraft": ">=1.21.0",
        "java": ">=21"
    },
    "suggests": {
        "fabric-api": "*"
    }
}
```

### 6. Update Gradle Properties (Optional)

You can add project-specific properties to `gradle.properties`:

```properties
# Logical Loadouts project settings (example)
logical_loadouts_version=1.0.0-alpha.1
logical_loadouts_archives_base_name=logical-loadouts
```

### 7. Switch to Your New Project

Use the tooling to switch to your new project:

```bash
# Switch to your new project with v1_21_1 adapter
.\switch-project.bat logical-loadouts v1_21_1

# This will:
# 1. Update gradle.properties with mochamix.project=logical-loadouts
# 2. Generate IDE context for your project
# 3. Verify directory structure exists
```

### 8. Build and Test

Test your new project setup:

```bash
# Clean and build your new project
.\gradlew clean build

# The output JAR will be located at:
# projects\logical-loadouts\v1_21_1\build\libs\logical-loadouts-v1_21_1-[version].jar
```

## Adding Override Versions (Advanced)

To add override versions like `v1_21_5`:

### 1. Create Override Directory
```bash
mkdir projects\logical-loadouts\v1_21_5
mkdir projects\logical-loadouts\v1_21_5\src
mkdir projects\logical-loadouts\v1_21_5\src\main
mkdir projects\logical-loadouts\v1_21_5\src\main\java
mkdir projects\logical-loadouts\v1_21_5\src\main\resources
```

### 2. Add Override Files

Only add files that need to be different from the base version. The build system will automatically merge:
- `v1_21_5` files (highest priority)
- `common` files (middle priority)  
- `v1_21_1` base files (lowest priority)

### 3. Update Configuration

Add the new adapter to `minecraftTargets` in `build.gradle` if it's not already there, and update `switch-project.bat` and `ide-context.gradle` validation lists.

## Architecture Benefits

This project-centric architecture provides:

1. **Clean Separation**: Each project is completely independent
2. **Version Inheritance**: Override versions inherit from base versions automatically
3. **Shared Dependencies**: Common libraries can be placed in `common/`
4. **IDE Support**: Context switching works seamlessly across projects
5. **Build Isolation**: Each project builds independently with proper classpaths
6. **Scalability**: Easy to add new projects without affecting existing ones
7. **Shared Runtime**: Version-based run directories allow testing multiple mods together

## Testing Your New Project

### Run Directory Sharing

All projects using the same Minecraft version share organized run directories:
- **v1_21_1 client**: Projects use `run/client/v1_21_1/`
- **v1_21_1 server**: Projects use `run/server/v1_21_1/` 
- **v1_21_5 client**: Projects use `run/client/v1_21_5/`
- **v1_21_5 server**: Projects use `run/server/v1_21_5/`

This means:
```bash
# Both commands use the same world saves and configs
.\switch-project.bat mochamix v1_21_1
.\gradlew runClient      # Uses run/client/v1_21_1/

.\switch-project.bat logical-loadouts v1_21_1  
.\gradlew runClient      # Same world, with both mods loaded
```

## Troubleshooting

### Common Issues:

1. **Missing Source Directory**: Ensure `common/src/main/` exists (not `common/src/main/java/`)
2. **Build Failures**: Check that `build.gradle` files exist in adapter directories
3. **IDE Issues**: Run `.\gradlew -b ide-context.gradle generateContext` to refresh
4. **Switch Failures**: Verify project name is added to validation lists in tooling scripts

### Debug Commands:

```bash
# Check current project settings
.\switch-project.bat

# Verify source paths
.\gradlew -b ide-context.gradle generateContext

# List available projects
dir projects

# Check build output
.\gradlew build --info
```

## Example Project Template

For a complete example, see the existing `mochamix` project structure as a reference template for organizing your new projects.