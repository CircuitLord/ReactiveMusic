# MochaMix Multi-Project Architecture Reference

## Current Project Structure

```
MochaMix/
├── projects/
│   └── mochamix/                    # Main music mod project
│       ├── common/                  # Shared code (rm_javazoom, org.* packages)
│       │   └── src/main/java/      # Standard Java structure
│       │       ├── rm_javazoom/    # JavaZoom MP3 library
│       │       └── org/            # YAML and other libraries  
│       ├── v1_21_1/                # Base version (Minecraft 1.21.1)
│       │   └── src/main/java/      # Standard Java structure
│       │       └── rocamocha/
│       │           ├── reactivemusic/
│       │           └── mochamix/
│       └── v1_21_5/                # Override version (Minecraft 1.21.5)
│           └── src/main/java/      # Override files only
│
├── run/                            # Shared runtime environments
│   ├── client/                     # Client-side testing
│   │   ├── v1_21_1/               # Minecraft 1.21.1 client world/config
│   │   └── v1_21_5/               # Minecraft 1.21.5 client world/config
│   └── server/                     # Server-side testing  
│       ├── v1_21_1/               # Minecraft 1.21.1 server world/config
│       └── v1_21_5/               # Minecraft 1.21.5 server world/config
│
├── build.gradle                    # Root build configuration
├── settings.gradle                 # Project selection and validation
├── gradle.properties              # Project settings and versions
├── switch-project.bat             # Project/adapter switcher tool  
└── ide-context.gradle             # IDE context generator
```

## Key Configuration Files

### gradle.properties
```properties
# Project selection
mochamix.project=mochamix           # Active project
mochamix.adapter=v1_21_1           # Active adapter (version)
```

### Source Merging Strategy

**Base Version (v1_21_1):**
- `projects/mochamix/v1_21_1/src/main/java/` (highest priority)
- `projects/mochamix/common/src/main/java/` (lower priority)

**Override Version (v1_21_5):**
- `projects/mochamix/v1_21_5/src/main/java/` (highest priority) 
- `projects/mochamix/common/src/main/java/` (middle priority)
- `projects/mochamix/v1_21_1/src/main/java/` (lowest priority)

## Tooling Commands

```bash
# Show current settings
.\switch-project.bat

# Switch project/adapter
.\switch-project.bat mochamix v1_21_1
.\switch-project.bat mochamix v1_21_5

# Generate IDE context
.\gradlew -b ide-context.gradle generateContext

# Build current project
.\gradlew clean build
```

## Adding New Projects

See [ADDING_PROJECTS.md](ADDING_PROJECTS.md) for detailed instructions on extending this architecture with new projects.

## Version Support Matrix

| Adapter | Minecraft | Yarn | Fabric Loader | Fabric API |
|---------|-----------|------|---------------|------------|
| v1_21_1 | 1.21.1    | 1.21.1+build.3 | 0.16.14 | 0.114.0+1.21.1 |
| v1_21_5 | 1.21.5    | 1.21.5+build.1 | 0.17.1  | 0.127.0+1.21.5 |

## Architecture Benefits

✅ **Multi-Version Support**: Clean separation of version-specific code  
✅ **Code Reuse**: Common libraries shared across versions  
✅ **Project Isolation**: Independent projects in same repository  
✅ **IDE Integration**: Smart context switching with proper classpath  
✅ **Build Efficiency**: Only active project builds, faster iteration  
✅ **Merge Strategy**: Sophisticated source override system for version differences  
✅ **Shared Run Directories**: Version-based shared runtime environments across projects

## Run Directory Strategy

The build system uses **organized nested run directories**:

```
run/
├── client/
│   ├── v1_21_1/    # All v1_21_1 projects share this client environment
│   └── v1_21_5/    # All v1_21_5 projects share this client environment  
└── server/
    ├── v1_21_1/    # All v1_21_1 projects share this server environment
    └── v1_21_5/    # All v1_21_5 projects share this server environment
```

This allows you to:
- Test multiple mods together on the same Minecraft version
- Preserve world saves and configs when switching projects
- Maintain version-specific mod compatibility testing
- Clearly separate client vs server testing environments