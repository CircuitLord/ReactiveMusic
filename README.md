## 📁 Multi-Project Architecture

This repository uses a sophisticated **multi-project build system** that supports:
- **Multiple independent projects** in a single repository  
- **Multi-version Minecraft adapters** with source inheritance
- **Smart IDE context switching** for development
- **Automated source merging** for version-specific overrides

### Quick Start
```bash
# Switch between projects and Minecraft versions  
.\switch-project.bat mochamix v1_21_1    # Minecraft 1.21.1
.\switch-project.bat mochamix v1_21_5    # Minecraft 1.21.5

# Build current project
.\gradlew clean build

# Add new projects
# See docs/ADDING_PROJECTS.md for detailed guide
```

### Documentation
- **[Architecture Overview](docs/ARCHITECTURE.md)** - System design and structure
- **[Adding Projects Guide](docs/ADDING_PROJECTS.md)** - Step-by-step project creation
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and solutions

