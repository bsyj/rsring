# Technology Stack

## Build System

**Gradle with ForgeGradle 2.3**: Standard Minecraft 1.12.2 mod build system

**Java Version**: JDK 8 (source and target compatibility 1.8)

**Minecraft Version**: 1.12.2 with Forge 14.23.5.2847

**MCP Mappings**: stable_39

## Dependencies

**Required**:
- Minecraft Forge 14.23.5.2847+

**Optional (deobfCompile)**:
- Baubles 1.5.2+ (accessory slot support)
- JEI 4.16.1.1013+ (recipe display)

**Testing**:
- JUnit 4.12
- junit-quickcheck 0.9.1 (property-based testing)

## Common Commands

### Build
```bash
gradlew build
```
Output: `build/libs/rsring-1.0.jar`

### Clean Build
```bash
gradlew clean build
```

### Run Client (Development)
```bash
gradlew runClient
```

### Run Server (Development)
```bash
gradlew runServer
```

### Setup IDE
```bash
gradlew setupDecompWorkspace
gradlew idea  # or eclipse
```

## Key Libraries & Frameworks

**Forge Capabilities**: Used for persistent data storage (ring bindings, energy, XP tank data)

**Forge Energy API**: FE energy system integration

**Baubles API**: Reflection-based integration for accessory slots (optional dependency)

**Network System**: Custom packet handlers for client-server synchronization

**GUI System**: GuiContainer-based GUIs following Cyclic mod conventions

## Multi-Workspace Context

This workspace contains multiple Minecraft mods:
- **rsring**: Main project (RS Ring Mod)
- **Cyclic-trunk-1.12**: Reference mod for GUI patterns and utilities
- **Baubles-master**: Baubles API reference
- **SophisticatedBackpacks-1.16.x**: Reference for XP calculation logic
- **XRay-Mod-1.12.2**: Additional reference mod

When working on rsring, you may reference code patterns from Cyclic and Baubles for consistency.
