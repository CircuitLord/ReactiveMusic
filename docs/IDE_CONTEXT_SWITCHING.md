# ReactiveMusic IDE Context Switching

This document explains how to switch the dependency context of the `common` module for better IDE support when developing for different Minecraft versions.

## Problem

The `common` module contains shared code that needs to work across multiple Minecraft versions. However, IDEs and Language Server Protocol (LSP) clients need to know which specific version's APIs are available for proper autocompletion and error checking.

## Solution

A standalone IDE context generator (`ide-context.gradle`) creates the proper dependency context based on the active adapter configuration, giving you full IDE support for the version you're currently working on without causing build configuration loops.

## Usage

### Method 1: Using the PowerShell Script (Recommended)

```powershell
# Switch to Minecraft 1.20.4 context
.\switch.ps1 1_20_4

# Switch to Minecraft 1.21.1 context
.\switch.ps1 1_21_1

# Switch to latest version context
.\switch.ps1 latest

# Show current adapter setting
.\switch.ps1
```

### Method 2: Manual Gradle Properties

Edit `gradle.properties` and change the line:
```properties
reactivemusic.adapter=1_20_4    # For MC 1.20.4
# or
reactivemusic.adapter=1_21_1    # For MC 1.21-1.21.1  
# or
reactivemusic.adapter=latest    # For latest version
```

### Method 3: Command Line (One-time)

```powershell
# Directly edit the property file
(Get-Content gradle.properties) -replace '^reactivemusic\.adapter=.*', 'reactivemusic.adapter=1_21_1' | Set-Content gradle.properties
```

## Available Adapters

| Adapter | Minecraft Version | Description |
|---------|------------------|-------------|
| `1_20_4` | 1.20.4 | Legacy version support |
| `1_21_1` | 1.21 - 1.21.1 | Stable release branch |
| `latest` | 1.21.6+ | Current development version |

## After Switching Context

1. **Generate IDE context**: Run `./gradlew -b ide-context.gradle generateContext`
2. **Refresh your IDE project**:
   - **IntelliJ IDEA**: File → Reload Gradle Project
   - **VS Code**: Ctrl+Shift+P → "Java: Refresh Projects"
   - **Eclipse**: Right-click project → Gradle → Refresh Gradle Project
3. **Test autocompletion** on version-specific APIs in the common module

## Technical Details

- The `ide-context.gradle` file is a standalone build script that runs independently of the main project
- It reads the `reactivemusic.adapter` property from `gradle.properties` 
- Uses `compileOnly` dependencies that match the selected adapter for IDE support
- Points to the common module source code via `sourceSets` configuration
- Completely avoids configuration loops by not depending on the main project's `subprojects` block
- The common source code is still included in adapter modules via their own `sourceSets` configuration

## Complete Workflow Example

```powershell
# 1. Switch to 1.20.4 for legacy support work
.\switch.ps1 1_20_4

# 2. Generate the IDE context for 1.20.4 
./gradlew -b ide-context.gradle generateContext

# 3. Refresh your IDE (IntelliJ: File → Reload Gradle Project)

# 4. Now work on common code with 1.20.4 APIs
# Your IDE will show 1.20.4 autocompletion and error checking
```

```java
// In common module - this will have proper autocompletion based on active adapter
public class MyMinecraftHelper {
    public void useVersionSpecificAPI() {
        // When adapter=1_20_4: Shows 1.20.4 APIs
        // When adapter=1_21_1: Shows 1.21.1 APIs  
        // When adapter=latest: Shows latest APIs
        MinecraftClient.getInstance().player.sendMessage(/* ... */);
    }
}
```

The IDE will now provide accurate autocompletion and error checking based on which adapter context you've selected!