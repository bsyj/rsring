# Project Structure

## Directory Layout

```
rsring/
├── src/main/java/com/moremod/
│   ├── capability/          # Forge capability implementations
│   │   ├── IRsRingCapability.java
│   │   ├── RsRingCapability.java
│   │   ├── IExperiencePumpCapability.java
│   │   └── ExperiencePumpCapability.java
│   ├── client/              # Client-side GUI classes
│   │   ├── GuiRingFilterContainer.java
│   │   ├── ContainerRingFilter.java
│   │   ├── GuiExperiencePumpController.java
│   │   └── GuiRsRingConfig.java
│   ├── config/              # Configuration classes
│   │   ├── RsRingConfig.java
│   │   └── ExperiencePumpConfig.java
│   ├── event/               # Event handlers
│   │   ├── CommonEventHandler.java
│   │   ├── ClientInputEvents.java
│   │   └── RecipeRegistryHandler.java
│   ├── experience/          # Experience system logic
│   │   ├── ExperiencePumpController.java
│   │   ├── ExperienceTankManager.java
│   │   ├── BlacklistWhitelistManager.java
│   │   └── InventoryIntegrationLayer.java
│   ├── item/                # Item implementations
│   │   ├── ItemChestRing.java
│   │   ├── ItemAbsorbRing.java
│   │   ├── ItemExperiencePump.java
│   │   └── ItemExperiencePumpController.java
│   ├── network/             # Network packet handlers
│   │   ├── PacketSyncRingFilter.java
│   │   └── PacketPumpAction.java
│   ├── proxy/               # Client/Server proxies
│   │   ├── CommonProxy.java
│   │   └── ClientProxy.java
│   ├── rsring/              # Main mod class
│   │   └── RsRingMod.java
│   └── util/                # Utility classes
│       ├── XpHelper.java
│       └── ItemLocationTracker.java
├── src/main/resources/
│   └── assets/rsring/
│       ├── lang/            # Localization files
│       │   ├── en_us.lang
│       │   └── zh_cn.lang
│       ├── models/item/     # Item models (JSON)
│       ├── recipes/         # Crafting recipes (JSON)
│       └── textures/        # Texture files (PNG)
│           └── gui/         # GUI textures (Cyclic style)
├── src/test/java/           # Unit and property-based tests
├── docs/                    # Technical documentation
├── .kiro/
│   ├── specs/               # Feature specifications
│   └── steering/            # AI assistant steering rules
└── build.gradle             # Build configuration
```

## Package Organization

**capability**: Forge capability interfaces and implementations for persistent data storage

**client**: All GUI-related classes (GuiContainer, Container, rendering)

**config**: Configuration file handlers using Forge config system

**event**: Forge event subscribers for game events

**experience**: Business logic for experience collection, storage, and transfer

**item**: Item class implementations extending Forge Item classes

**network**: SimpleNetworkWrapper packet handlers for client-server sync

**proxy**: Side-specific initialization (client vs server)

**util**: Shared utility classes and helper methods

## Key Architectural Patterns

**Capability System**: Used for attaching data to items (ring bindings, energy, tank capacity)

**Proxy Pattern**: CommonProxy/ClientProxy for side-specific code

**Event-Driven**: Forge event bus for game hooks (tick events, player events, etc.)

**Network Packets**: Custom packets for synchronizing state between client and server

**GUI Architecture**: GuiContainer + Container pattern following Cyclic conventions

## Naming Conventions

- Classes: PascalCase (e.g., `ItemChestRing`, `ExperienceTankManager`)
- Packages: lowercase (e.g., `capability`, `experience`)
- Constants: UPPER_SNAKE_CASE
- Variables/Methods: camelCase
- GUI textures: lowercase with underscores (e.g., `slot_background.png`)

## Documentation Locations

**Specs**: `.kiro/specs/{feature-name}/` - Requirements, design, and tasks
**Technical Docs**: `docs/` - Implementation details and fix summaries
**User Docs**: `README.md`, `changelog.txt` - User-facing documentation
