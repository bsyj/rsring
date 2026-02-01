# Design Document: Baubles Ring Detection Fix

## Overview

This design addresses the critical bug where ring toggle functionality fails when rings are equipped in Baubles slots. The solution involves creating a unified, robust ring detection system that consolidates the currently duplicated logic between `PacketToggleRsRing.Handler` and `CommonEventHandler`, while improving error handling and Baubles API integration.

The core issue is that both classes implement similar but slightly different ring detection logic with poor error handling for Baubles API reflection calls. This leads to silent failures when rings are in Baubles slots, causing the "ring not found" error message.

## Architecture

The solution follows a centralized service pattern with the following components:

```
RingDetectionService (new)
├── findRing() - unified ring detection logic
├── BaublesIntegration - robust Baubles API handling
└── ErrorLogger - comprehensive error reporting

PacketToggleRsRing.Handler (modified)
└── uses RingDetectionService instead of local findRing()

CommonEventHandler (modified)
└── uses RingDetectionService instead of local findRing()
```

The architecture maintains backward compatibility while eliminating code duplication and improving reliability.

## Components and Interfaces

### RingDetectionService

A new utility class that provides centralized ring detection functionality:

```java
public class RingDetectionService {
    // Main entry point - finds any ring type
    public static ItemStack findAnyRing(EntityPlayer player)
    
    // Type-specific ring detection
    public static ItemStack findRing(EntityPlayer player, Class<? extends Item> ringClass)
    
    // Internal methods
    private static ItemStack findInHands(EntityPlayer player, Class<? extends Item> ringClass)
    private static ItemStack findInBaubles(EntityPlayer player, Class<? extends Item> ringClass)
    private static ItemStack findInInventory(EntityPlayer player, Class<? extends Item> ringClass)
    private static void logSearchResult(String location, boolean found, ItemStack result)
}
```

### BaublesIntegration

Enhanced Baubles API integration with proper error handling:

```java
private static class BaublesIntegration {
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static ItemStack findRingInBaubles(EntityPlayer player, Class<? extends Item> ringClass) {
        if (!Loader.isModLoaded("baubles")) {
            LOGGER.debug("Baubles mod not loaded, skipping Baubles search");
            return ItemStack.EMPTY;
        }
        
        try {
            // Robust reflection-based Baubles API access
            Class<?> apiClass = Class.forName("baubles.api.BaublesApi");
            Object handler = apiClass.getMethod("getBaublesHandler", EntityPlayer.class)
                                   .invoke(null, player);
            
            if (handler instanceof IInventory) {
                IInventory baubles = (IInventory) handler;
                return searchInventoryForRing(baubles, ringClass);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.error("Baubles API class not found - mod may be outdated", e);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Baubles API method not found - incompatible version", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOGGER.error("Failed to access Baubles API", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error accessing Baubles inventory", e);
        }
        
        return ItemStack.EMPTY;
    }
}
```

### Modified PacketToggleRsRing.Handler

The packet handler will be simplified to use the centralized service:

```java
public static class Handler implements IMessageHandler<PacketToggleRsRing, IMessage> {
    @Override
    public IMessage onMessage(PacketToggleRsRing message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        player.getServerWorld().addScheduledTask(() -> {
            ItemStack ringStack = RingDetectionService.findAnyRing(player);
            if (!ringStack.isEmpty()) {
                // Toggle logic remains the same
                toggleRingState(player, ringStack);
            } else {
                player.sendMessage(new TextComponentString(TextFormatting.RED + "未找到戒指"));
            }
        });
        return null;
    }
}
```

### Modified CommonEventHandler

The event handler will use the same centralized service:

```java
public class CommonEventHandler {
    // Remove duplicate findRing methods
    // Use RingDetectionService.findRing() in all locations
    
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Use RingDetectionService instead of local findRing
        ItemStack rsRingStack = RingDetectionService.findRing(player, ItemRsRing.class);
        ItemStack chestRingStack = RingDetectionService.findRing(player, ItemChestRing.class);
        // Rest of logic remains the same
    }
}
```

## Data Models

No new data models are required. The existing `ItemStack` and capability system remain unchanged. The service operates on the existing data structures:

- `ItemStack` - represents ring items
- `IRsRingCapability` - ring functionality and state
- `EntityPlayer` - player inventory access
- `IInventory` - Baubles inventory interface

## Error Handling

The design implements comprehensive error handling at multiple levels:

### Baubles API Error Handling
- **ClassNotFoundException**: Log error about missing Baubles API, continue with other locations
- **NoSuchMethodException**: Log error about incompatible Baubles version, continue gracefully  
- **IllegalAccessException/InvocationTargetException**: Log reflection access errors, continue operation
- **General Exception**: Catch-all for unexpected errors with full stack trace logging

### Search Process Error Handling
- **Debug Logging**: Log each search location and result for troubleshooting
- **Graceful Degradation**: If one search location fails, continue with remaining locations
- **User Feedback**: Provide clear "ring not found" message only after all locations searched

### Inventory Synchronization Error Handling
- **Baubles Dirty Marking**: Log failures but don't interrupt ring toggle operation
- **Capability Sync**: Ensure ring state changes persist even if some sync operations fail

## Testing Strategy

The testing approach combines unit tests for specific scenarios and property-based tests for comprehensive coverage:

### Unit Testing Focus
- **Specific Integration Points**: Test Baubles API reflection calls with mock objects
- **Error Conditions**: Test behavior when Baubles mod is missing or API calls fail
- **Edge Cases**: Test with empty inventories, multiple rings, invalid ring types
- **Regression Prevention**: Test the specific bug scenario (ring in Baubles slot)

### Property-Based Testing Focus  
- **Ring Detection Consistency**: Verify ring detection works across all inventory locations
- **Search Priority**: Ensure search order is maintained (hands → Baubles → inventory)
- **Error Recovery**: Test that failures in one location don't prevent searching others

### Testing Configuration
- **Framework**: JUnit 5 with Mockito for mocking Minecraft/Forge components
- **Property Testing**: Use jqwik for property-based testing with minimum 100 iterations
- **Test Environment**: Use Minecraft test framework for integration testing
- **Coverage Target**: 90%+ line coverage for new RingDetectionService class

Each property-based test will be tagged with format: **Feature: baubles-ring-detection-fix, Property {number}: {property_text}**

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Ring Detection Priority Order
*For any* player with rings in multiple inventory locations (hands, Baubles, inventory), the ring detection system should always return the ring from the highest priority location according to the search order: main hand → off hand → Baubles slots → inventory.
**Validates: Requirements 1.1, 1.3**

### Property 2: Ring Detection Success Across All Locations  
*For any* ring placed in any valid inventory location (main hand, off hand, Baubles slot, or inventory), the ring detection system should successfully find and return that ring.
**Validates: Requirements 1.2, 2.3**

### Property 3: Baubles Integration Robustness
*For any* Baubles mod state (loaded/not loaded) and any Baubles API condition (working/failing), the ring detection system should handle the situation gracefully without throwing exceptions and continue searching other locations.
**Validates: Requirements 2.1, 2.2, 2.4**

### Property 4: Comprehensive Error Logging
*For any* error condition during ring detection (Baubles API failures, reflection errors, missing rings), the system should log appropriate error messages with correct log levels and continue operation.
**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

### Property 5: Ring State Synchronization
*For any* ring state modification in any inventory location, the changes should be properly synchronized back to the ItemStack and any relevant inventory systems (including Baubles) should be marked as dirty.
**Validates: Requirements 5.1, 5.2, 5.3, 5.4**

### Property 6: Backward Compatibility Preservation
*For any* existing ring detection scenario that worked before the fix, the new system should produce identical results while maintaining the same external behavior.
**Validates: Requirements 4.4**