# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

VeloCloud-SyncProxy is a Velocity proxy server plugin written in Java 21. It leverages the Velocity API to extend proxy server functionality. The plugin uses an event-driven architecture with dependency injection.

## Build & Development Commands

### Building
```bash
gradle build          # Full build with compilation and artifact generation
gradle clean build    # Clean build from scratch
gradle check         # Verify compilation without artifacts
```

### Running
```bash
gradle runVelocity   # Run the Velocity server with the plugin loaded for testing
```

### Cleaning
```bash
gradle clean         # Remove build artifacts
```

### Build Configuration
- **Java Version**: 21 (toolchain configured in build.gradle.kts)
- **Build Optimizations**: Configuration cache, parallel compilation, and build caching are enabled
- **Repositories**: Maven Central and PaperMC public repository

## Architecture & Code Structure

### Entry Point
- **SyncProxy.java** (`src/main/java/de/Snenjih/SyncProxy.java`): Main plugin class marked with `@Plugin` annotation

### Design Patterns

1. **Plugin System**: Uses Velocity's plugin framework. The main plugin class is registered via the `@Plugin` annotation with metadata (id, name, version, authors, URL).

2. **Event-Driven Architecture**: The plugin responds to proxy events via the `@Subscribe` annotation on handler methods. Currently listens to `ProxyInitializeEvent` for initialization logic.

3. **Dependency Injection**: Uses Google Inject for dependency management. The Logger is injected via `@Inject` annotation.

### Build System

- **Template Expansion**: BuildConstants.java uses Gradle's template expansion to inject the project version at compile time. The `${version}` placeholder is replaced during the build process.
- **Gradle Plugins**:
  - `run-velocity`: Orchestrates running a Velocity server for local testing
  - `idea-ext` & `eclipse`: IDE integration for automatic template generation during project sync

### Package Structure
```
src/main/
├── java/de/Snenjih/
│   └── SyncProxy.java          # Main plugin class
└── templates/de/Snenjih/
    └── BuildConstants.java      # Version constants (generated)
```

## Development Notes

- **Velocity API Version**: Currently using 3.5.0-SNAPSHOT from PaperMC repository
- **Plugin ID**: "syncproxy" (used in configurations and commands)
- **Gradle Features**: The project enables parallel builds and build caching for faster compilation
- **IDE Setup**: Both IntelliJ IDEA and Eclipse are configured to auto-generate templates on project sync
