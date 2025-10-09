# MochaMix Multi-Project Troubleshooting Guide

## Common Issues and Solutions

### Build Issues

#### Problem: "Package does not exist" errors during compilation
**Symptoms:**
```
error: package rm_javazoom.jl.player.advanced does not exist
error: package org.rm_yaml.snakeyaml does not exist
```

**Solutions:**
1. **Check source directory structure**: Common libraries should be in `projects/[project]/common/src/main/java/` (standard Java structure)
2. **Verify project selection**: Run `.\switch-project.bat` to see current settings
3. **Regenerate IDE context**: `.\gradlew -b ide-context.gradle generateContext`
4. **Check source paths**: Ensure files exist in expected locations

#### Problem: Build fails with "Project directory not found"
**Symptoms:**
```
Project directory 'projects/myproject' not found
```

**Solutions:**
1. **Create missing directories**: Follow [ADDING_PROJECTS.md](ADDING_PROJECTS.md) structure guide
2. **Check project name**: Verify spelling matches directory name exactly
3. **Update validation lists**: Add project to `switch-project.bat` and `ide-context.gradle`

#### Problem: Merged source conflicts in v1_21_5
**Symptoms:**
```
duplicate class: SomeClass
```

**Solutions:**
1. **Check file precedence**: v1_21_5 > common > v1_21_1 (highest to lowest)
2. **Remove duplicate files**: Delete lower-priority duplicates
3. **Clean build directory**: `.\gradlew clean` then rebuild

### IDE Issues

#### Problem: IDE shows missing imports but build works
**Symptoms:**
- Red underlines in IDE but `gradlew build` succeeds
- Autocompletion not working for common classes

**Solutions:**
1. **Switch project context**: `.\switch-project.bat [project] [adapter]`
2. **Refresh IDE project**: Reload/refresh your IDE project files
3. **Clear IDE cache**: Clear IntelliJ/Eclipse caches and restart
4. **Check IDE Java version**: Ensure IDE uses Java 21

#### Problem: IDE context shows wrong source paths
**Symptoms:**
```
✗ C:\...\projects\myproject\common\src\main (missing)
```

**Solutions:**
1. **Create missing directories**: `mkdir projects\myproject\common\src\main\java`
2. **Check project structure**: Verify against [ARCHITECTURE.md](ARCHITECTURE.md)
3. **Regenerate context**: `.\gradlew -b ide-context.gradle generateContext`

### Tooling Issues

#### Problem: switch-project.bat shows "Invalid project"
**Symptoms:**
```
Invalid project: myproject
Valid options: mochamix
```

**Solutions:**
1. **Update script validation**: Add project to `switch-project.bat` validation list:
   ```batch
   if "%1"=="myproject" goto :checkAdapter
   ```
2. **Update help text**: Add project to available projects list
3. **Verify directory exists**: Check `projects\myproject` exists

#### Problem: Gradle properties not updating
**Symptoms:**
- Script runs but `gradle.properties` unchanged
- Wrong project still building

**Solutions:**
1. **Check file permissions**: Ensure `gradle.properties` is not read-only
2. **Manual update**: Edit `gradle.properties` directly:
   ```properties
   mochamix.project=myproject
   mochamix.adapter=v1_21_1
   ```
3. **PowerShell execution policy**: May need to enable script execution

### Version/Adapter Issues

#### Problem: "Invalid adapter" error
**Symptoms:**
```
Invalid adapter: 1_21_1
Valid options: v1_21_1, v1_21_5
```

**Solutions:**
1. **Use correct naming**: New format uses `v1_21_1` not `1_21_1`
2. **Update old scripts**: Remove old `switch-adapter.bat` if present
3. **Check validation lists**: Ensure all scripts use consistent naming

#### Problem: Build uses wrong Minecraft version
**Symptoms:**
- Expected 1.21.5 but builds for 1.21.1
- Wrong dependencies in classpath

**Solutions:**
1. **Check current adapter**: `.\switch-project.bat` shows current settings
2. **Switch adapter**: `.\switch-project.bat [project] v1_21_5`
3. **Verify configuration**: Check `minecraftTargets` in `build.gradle`

## Debug Commands

### Check Current State
```bash
# Show current project/adapter settings
.\switch-project.bat

# Show detailed source paths and dependency versions  
.\gradlew -b ide-context.gradle generateContext

# List all projects
dir projects

# Check build configuration
.\gradlew projects
```

### Verify File Structure
```bash
# Check project structure
tree projects /f

# Verify specific directories exist
dir projects\[project]\common\src\main\java
dir projects\[project]\v1_21_1\src\main\java

# Check for build files
dir projects\[project]\v1_21_1\build.gradle
```

### Clean Build Debug
```bash
# Complete clean rebuild with verbose output
.\gradlew clean build --info

# Check generated source merge (for v1_21_5)
dir projects\[project]\v1_21_5\build\merged-sources

# Verify JAR output
dir projects\[project]\[adapter]\build\libs
```

## Getting Help

### Log Collection
When reporting issues, include:

1. **Current configuration**:
   ```bash
   .\switch-project.bat
   ```

2. **IDE context output**:
   ```bash
   .\gradlew -b ide-context.gradle generateContext
   ```

3. **Build output**:
   ```bash
   .\gradlew clean build --info
   ```

4. **Directory structure**:
   ```bash
   tree projects /f
   ```

### Common File Locations

- **Main configuration**: `gradle.properties`
- **Project validation**: `settings.gradle` 
- **Build logic**: `build.gradle`
- **Tooling scripts**: `switch-project.bat`, `ide-context.gradle`
- **Project sources**: `projects/[project]/[adapter]/src/`
- **Build outputs**: `projects/[project]/[adapter]/build/libs/`
- **Merge cache**: `projects/[project]/[adapter]/build/merged-sources/` (v1_21_5 only)

## Emergency Reset

If everything breaks and you need to start fresh:

```bash
# 1. Reset to known good state
.\switch-project.bat mochamix v1_21_1

# 2. Clean everything
.\gradlew clean

# 3. Regenerate IDE context
.\gradlew -b ide-context.gradle generateContext

# 4. Test build
.\gradlew build

# 5. Refresh IDE project
# (Reload/refresh in your IDE)
```