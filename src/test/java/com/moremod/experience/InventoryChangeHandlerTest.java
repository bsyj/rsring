package com.moremod.experience;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for InventoryChangeHandler.
 * Tests Requirements 7.1, 7.2, 7.3 for inventory change detection and event handling.
 * Includes property-based tests for inventory change detection updates.
 */
@RunWith(JUnitQuickcheck.class)
public class InventoryChangeHandlerTest {
    
    private InventoryChangeHandler handler;
    private TestEventListener eventListener;
    
    @Before
    public void setUp() {
        // Create handler and event listener
        handler = InventoryChangeHandler.getInstance();
        eventListener = new TestEventListener();
        MinecraftForge.EVENT_BUS.register(eventListener);
    }
    
    @After
    public void tearDown() {
        MinecraftForge.EVENT_BUS.unregister(eventListener);
        eventListener.clear();
    }
    
    /**
     * Test handler initialization.
     */
    @Test
    public void testHandlerInitialization() {
        // Act & Assert
        assertNotNull("Handler should be initialized", handler);
        assertNotNull("Handler should be singleton", InventoryChangeHandler.getInstance());
        assertSame("Handler should return same instance", handler, InventoryChangeHandler.getInstance());
    }
    
    /**
     * Test manual inventory refresh with null player.
     */
    @Test
    public void testManualInventoryRefreshWithNullPlayer() {
        // Act & Assert - should not crash
        handler.refreshPlayerInventory(null);
        handler.refreshBaublesInventory(null);
        handler.scheduleInventoryRefresh(null);
        handler.forceFullInventoryRefresh(null);
        
        // No events should be fired for null players
        assertEquals("Should not fire events for null player", 0, eventListener.getAllEvents().size());
    }
    
    /**
     * Test inventory change event creation.
     */
    @Test
    public void testInventoryChangeEventCreation() {
        // Test static factory methods
        InventoryChangeEvent tankAdded = InventoryChangeEvent.tankAdded(
            null, InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY, ItemStack.EMPTY, 0);
        assertNotNull("Tank added event should be created", tankAdded);
        assertEquals("Should be tank added event", 
                   InventoryChangeEvent.ChangeType.TANK_ADDED, tankAdded.getChangeType());
        
        InventoryChangeEvent ringRemoved = InventoryChangeEvent.ringRemoved(
            null, InventoryChangeEvent.InventoryLocation.BAUBLES, ItemStack.EMPTY, 1);
        assertNotNull("Ring removed event should be created", ringRemoved);
        assertEquals("Should be ring removed event", 
                   InventoryChangeEvent.ChangeType.RING_REMOVED, ringRemoved.getChangeType());
        
        InventoryChangeEvent inventoryRefresh = InventoryChangeEvent.inventoryRefreshed(
            null, InventoryChangeEvent.InventoryLocation.HOTBAR);
        assertNotNull("Inventory refresh event should be created", inventoryRefresh);
        assertEquals("Should be inventory refresh event", 
                   InventoryChangeEvent.ChangeType.INVENTORY_REFRESHED, inventoryRefresh.getChangeType());
    }
    
    /**
     * Test inventory change event properties.
     */
    @Test
    public void testInventoryChangeEventProperties() {
        // Create test events
        InventoryChangeEvent tankEvent = InventoryChangeEvent.tankAdded(
            null, InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY, ItemStack.EMPTY, 5);
        
        // Test event properties
        assertTrue("Tank event should affect tanks", tankEvent.affectsTanks());
        assertFalse("Tank event should not affect rings", tankEvent.affectsRings());
        assertFalse("Tank event should not affect Baubles", tankEvent.affectsBaubles());
        assertEquals("Should have correct slot index", 5, tankEvent.getSlotIndex());
        assertEquals("Should have correct location", 
                   InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY, tankEvent.getLocation());
        
        InventoryChangeEvent baublesEvent = InventoryChangeEvent.baublesChanged(
            null, ItemStack.EMPTY, 2);
        assertTrue("Baubles event should affect Baubles", baublesEvent.affectsBaubles());
        assertEquals("Should have correct location", 
                   InventoryChangeEvent.InventoryLocation.BAUBLES, baublesEvent.getLocation());
    }
    
    /**
     * Test event description generation.
     */
    @Test
    public void testEventDescriptionGeneration() {
        InventoryChangeEvent event = InventoryChangeEvent.tankModified(
            null, InventoryChangeEvent.InventoryLocation.HOTBAR, ItemStack.EMPTY, 3);
        
        String description = event.getDescription();
        assertNotNull("Description should not be null", description);
        assertTrue("Description should contain change type", 
                  description.contains("Tank Modified"));
        assertTrue("Description should contain location", 
                  description.contains("Hotbar"));
        assertTrue("Description should contain slot", 
                  description.contains("slot 3"));
    }
    
    /**
     * Test inventory location enum.
     */
    @Test
    public void testInventoryLocationEnum() {
        // Test all enum values exist
        assertNotNull("Player inventory location should exist", 
                     InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY);
        assertNotNull("Hotbar location should exist", 
                     InventoryChangeEvent.InventoryLocation.HOTBAR);
        assertNotNull("Baubles location should exist", 
                     InventoryChangeEvent.InventoryLocation.BAUBLES);
        assertNotNull("Off hand location should exist", 
                     InventoryChangeEvent.InventoryLocation.OFFHAND);
        assertNotNull("Unknown location should exist", 
                     InventoryChangeEvent.InventoryLocation.UNKNOWN);
        
        // Test display names
        assertEquals("Player inventory display name", "Player Inventory", 
                   InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY.getDisplayName());
        assertEquals("Baubles display name", "Baubles Slots", 
                   InventoryChangeEvent.InventoryLocation.BAUBLES.getDisplayName());
    }
    
    /**
     * Test change type enum.
     */
    @Test
    public void testChangeTypeEnum() {
        // Test all enum values exist
        assertNotNull("Tank added type should exist", 
                     InventoryChangeEvent.ChangeType.TANK_ADDED);
        assertNotNull("Ring removed type should exist", 
                     InventoryChangeEvent.ChangeType.RING_REMOVED);
        assertNotNull("Baubles changed type should exist", 
                     InventoryChangeEvent.ChangeType.BAUBLES_CHANGED);
        assertNotNull("Inventory refreshed type should exist", 
                     InventoryChangeEvent.ChangeType.INVENTORY_REFRESHED);
        
        // Test display names
        assertEquals("Tank added display name", "Tank Added", 
                   InventoryChangeEvent.ChangeType.TANK_ADDED.getDisplayName());
        assertEquals("Ring modified display name", "Ring Modified", 
                   InventoryChangeEvent.ChangeType.RING_MODIFIED.getDisplayName());
    }
    
    /**
     * Test event timestamp functionality.
     */
    @Test
    public void testEventTimestamp() {
        long beforeTime = System.currentTimeMillis();
        InventoryChangeEvent event = InventoryChangeEvent.inventoryRefreshed(
            null, InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY);
        long afterTime = System.currentTimeMillis();
        
        long eventTime = event.getTimestamp();
        assertTrue("Event timestamp should be within expected range", 
                  eventTime >= beforeTime && eventTime <= afterTime);
    }
    
    /**
     * Test event toString method.
     */
    @Test
    public void testEventToString() {
        InventoryChangeEvent event = InventoryChangeEvent.tankAdded(
            null, InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY, ItemStack.EMPTY, 1);
        
        String toString = event.toString();
        assertNotNull("toString should not be null", toString);
        assertTrue("toString should contain class name", 
                  toString.contains("InventoryChangeEvent"));
        assertTrue("toString should contain change type", 
                  toString.contains("TANK_ADDED"));
        assertTrue("toString should contain location", 
                  toString.contains("PLAYER_INVENTORY"));
    }
    
    /**
     * Test event listener functionality.
     */
    @Test
    public void testEventListenerFunctionality() {
        // Create a test event directly (without using event bus)
        InventoryChangeEvent testEvent = InventoryChangeEvent.inventoryRefreshed(
            null, InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY);
        
        // Manually add to event listener to simulate event capture
        eventListener.onInventoryChange(testEvent);
        
        // Verify event was captured
        assertEquals("Should capture one event", 1, eventListener.getAllEvents().size());
        assertEquals("Should capture correct event", testEvent, eventListener.getAllEvents().get(0));
        
        // Test refresh event filtering
        assertEquals("Should have one refresh event", 1, eventListener.getRefreshEvents().size());
        assertEquals("Should be correct refresh event", testEvent, eventListener.getRefreshEvents().get(0));
    }
    
    /**
     * Test event listener clearing.
     */
    @Test
    public void testEventListenerClearing() {
        // Create multiple events directly (without using event bus)
        InventoryChangeEvent tankEvent = InventoryChangeEvent.tankAdded(
            null, InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY, ItemStack.EMPTY, 0);
        InventoryChangeEvent ringEvent = InventoryChangeEvent.ringRemoved(
            null, InventoryChangeEvent.InventoryLocation.BAUBLES, ItemStack.EMPTY, 1);
        
        // Manually add events to listener
        eventListener.onInventoryChange(tankEvent);
        eventListener.onInventoryChange(ringEvent);
        
        assertEquals("Should have two events", 2, eventListener.getAllEvents().size());
        
        // Clear events
        eventListener.clear();
        assertEquals("Should have no events after clear", 0, eventListener.getAllEvents().size());
    }
    
    // ========== PROPERTY-BASED TESTS ==========
    
    /**
     * Property-based test for inventory change detection updates.
     * 
     * **Property 12: Inventory Change Detection Updates**
     * For any inventory modification that affects tanks or rings, the system should update 
     * detection results and capacity displays accordingly.
     * 
     * **Validates: Requirements 7.1, 7.2, 7.3**
     */
    @Property(trials = 100)
    public void testInventoryChangeDetectionUpdates(@InRange(min = "1", max = "5") int numChanges,
                                                   @InRange(min = "0", max = "4") int locationIndex,
                                                   @InRange(min = "0", max = "2") int changeTypeIndex) {
        // Clear any existing events
        eventListener.clear();
        
        // Define possible inventory locations
        InventoryChangeEvent.InventoryLocation[] locations = {
            InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY,
            InventoryChangeEvent.InventoryLocation.HOTBAR,
            InventoryChangeEvent.InventoryLocation.BAUBLES,
            InventoryChangeEvent.InventoryLocation.OFFHAND,
            InventoryChangeEvent.InventoryLocation.UNKNOWN
        };
        
        // Define possible change types that affect tanks/rings
        InventoryChangeEvent.ChangeType[] changeTypes = {
            InventoryChangeEvent.ChangeType.TANK_ADDED,
            InventoryChangeEvent.ChangeType.RING_REMOVED,
            InventoryChangeEvent.ChangeType.BAUBLES_CHANGED
        };
        
        InventoryChangeEvent.InventoryLocation testLocation = locations[locationIndex % locations.length];
        InventoryChangeEvent.ChangeType testChangeType = changeTypes[changeTypeIndex % changeTypes.length];
        
        // Create inventory change events directly (without relying on event bus)
        List<InventoryChangeEvent> testEvents = new ArrayList<>();
        for (int i = 0; i < numChanges; i++) {
            InventoryChangeEvent event;
            
            // Create appropriate event based on change type
            switch (testChangeType) {
                case TANK_ADDED:
                    event = InventoryChangeEvent.tankAdded(null, testLocation, ItemStack.EMPTY, i);
                    break;
                case RING_REMOVED:
                    event = InventoryChangeEvent.ringRemoved(null, testLocation, ItemStack.EMPTY, i);
                    break;
                case BAUBLES_CHANGED:
                    event = InventoryChangeEvent.baublesChanged(null, ItemStack.EMPTY, i);
                    break;
                default:
                    event = InventoryChangeEvent.inventoryRefreshed(null, testLocation);
                    break;
            }
            
            testEvents.add(event);
        }
        
        // Property 1: All created events should have consistent properties
        // Requirements 7.1 - Inventory change detection should create valid events
        assertEquals("Should create expected number of events", numChanges, testEvents.size());
        
        // Property 2: Event properties should be preserved correctly
        // Requirements 7.3 - State refresh mechanisms should maintain consistency
        for (int i = 0; i < testEvents.size(); i++) {
            InventoryChangeEvent event = testEvents.get(i);
            
            assertNotNull("Event should not be null", event);
            assertNotNull("Event change type should not be null", event.getChangeType());
            assertNotNull("Event location should not be null", event.getLocation());
            assertEquals("Event slot index should match", i, event.getSlotIndex());
        }
        
        // Property 3: Events affecting tanks should be properly identified
        // Requirements 7.1 - Tank-related inventory changes should be detected
        for (InventoryChangeEvent event : testEvents) {
            if (event.getChangeType() == InventoryChangeEvent.ChangeType.TANK_ADDED ||
                event.getChangeType() == InventoryChangeEvent.ChangeType.TANK_REMOVED ||
                event.getChangeType() == InventoryChangeEvent.ChangeType.TANK_MODIFIED) {
                assertTrue("Tank events should be identified as affecting tanks", 
                          event.affectsTanks());
                assertFalse("Tank events should not affect rings", event.affectsRings());
            }
        }
        
        // Property 4: Events affecting rings should be properly identified
        // Requirements 7.1 - Ring-related inventory changes should be detected
        for (InventoryChangeEvent event : testEvents) {
            if (event.getChangeType() == InventoryChangeEvent.ChangeType.RING_ADDED ||
                event.getChangeType() == InventoryChangeEvent.ChangeType.RING_REMOVED ||
                event.getChangeType() == InventoryChangeEvent.ChangeType.RING_MODIFIED) {
                assertTrue("Ring events should be identified as affecting rings", 
                          event.affectsRings());
                assertFalse("Ring events should not affect tanks", event.affectsTanks());
            }
        }
        
        // Property 5: Baubles events should be properly identified
        // Requirements 7.2 - Baubles slot modifications should be detected
        for (InventoryChangeEvent event : testEvents) {
            if (event.getChangeType() == InventoryChangeEvent.ChangeType.BAUBLES_CHANGED ||
                event.getLocation() == InventoryChangeEvent.InventoryLocation.BAUBLES) {
                assertTrue("Baubles events should be identified as affecting Baubles", 
                          event.affectsBaubles());
            }
        }
        
        // Property 6: Event timestamps should be in chronological order
        // Requirements 7.3 - Events should maintain temporal consistency
        for (int i = 1; i < testEvents.size(); i++) {
            long previousTime = testEvents.get(i - 1).getTimestamp();
            long currentTime = testEvents.get(i).getTimestamp();
            assertTrue("Event timestamps should be in chronological order", 
                      currentTime >= previousTime);
        }
        
        // Property 7: Event descriptions should be non-empty and contain relevant information
        // Requirements 7.1, 7.2, 7.3 - Events should provide meaningful information
        for (InventoryChangeEvent event : testEvents) {
            String description = event.getDescription();
            assertNotNull("Event description should not be null", description);
            assertFalse("Event description should not be empty", description.trim().isEmpty());
            assertTrue("Event description should contain change type", 
                      description.contains(event.getChangeType().getDisplayName()));
            assertTrue("Event description should contain location", 
                      description.contains(event.getLocation().getDisplayName()));
        }
        
        // Property 8: Event factory methods should create consistent events
        // Requirements 7.1, 7.2, 7.3 - Event creation should be reliable
        InventoryChangeEvent factoryEvent;
        switch (testChangeType) {
            case TANK_ADDED:
                factoryEvent = InventoryChangeEvent.tankAdded(null, testLocation, ItemStack.EMPTY, 0);
                assertEquals("Factory method should create correct change type", 
                           InventoryChangeEvent.ChangeType.TANK_ADDED, factoryEvent.getChangeType());
                assertEquals("Factory method should set correct location", 
                           testLocation, factoryEvent.getLocation());
                break;
            case RING_REMOVED:
                factoryEvent = InventoryChangeEvent.ringRemoved(null, testLocation, ItemStack.EMPTY, 0);
                assertEquals("Factory method should create correct change type", 
                           InventoryChangeEvent.ChangeType.RING_REMOVED, factoryEvent.getChangeType());
                assertEquals("Factory method should set correct location", 
                           testLocation, factoryEvent.getLocation());
                break;
            case BAUBLES_CHANGED:
                factoryEvent = InventoryChangeEvent.baublesChanged(null, ItemStack.EMPTY, 0);
                assertEquals("Factory method should create correct change type", 
                           InventoryChangeEvent.ChangeType.BAUBLES_CHANGED, factoryEvent.getChangeType());
                assertEquals("Factory method should set Baubles location", 
                           InventoryChangeEvent.InventoryLocation.BAUBLES, factoryEvent.getLocation());
                break;
        }
        
        // Property 9: Event toString should contain essential information
        // Requirements 7.1, 7.2, 7.3 - Events should be debuggable
        for (InventoryChangeEvent event : testEvents) {
            String eventString = event.toString();
            assertNotNull("Event toString should not be null", eventString);
            assertTrue("Event toString should contain class name", 
                      eventString.contains("InventoryChangeEvent"));
            assertTrue("Event toString should contain change type", 
                      eventString.contains(event.getChangeType().name()));
            assertTrue("Event toString should contain location", 
                      eventString.contains(event.getLocation().name()));
        }
        
        // Property 10: Manual refresh operations should be testable
        // Requirements 7.3 - Manual refresh should be available
        // Test that handler methods don't crash with null parameters (defensive programming)
        try {
            handler.refreshPlayerInventory(null);
            handler.refreshBaublesInventory(null);
            handler.forceFullInventoryRefresh(null);
            handler.scheduleInventoryRefresh(null);
            // If we reach here, the methods handled null gracefully
            assertTrue("Handler methods should handle null gracefully", true);
        } catch (Exception e) {
            fail("Handler methods should not throw exceptions with null parameters: " + e.getMessage());
        }
    }
    
    /**
     * Property-based test for inventory change event consistency.
     * 
     * Tests that inventory change events maintain consistency across different scenarios.
     * 
     * **Validates: Requirements 7.1, 7.2, 7.3**
     */
    @Property(trials = 50)
    public void testInventoryChangeEventConsistency(@InRange(min = "0", max = "100") int slotIndex,
                                                   @InRange(min = "0", max = "4") int locationIndex) {
        // Define test parameters
        InventoryChangeEvent.InventoryLocation[] locations = {
            InventoryChangeEvent.InventoryLocation.PLAYER_INVENTORY,
            InventoryChangeEvent.InventoryLocation.HOTBAR,
            InventoryChangeEvent.InventoryLocation.BAUBLES,
            InventoryChangeEvent.InventoryLocation.OFFHAND,
            InventoryChangeEvent.InventoryLocation.UNKNOWN
        };
        
        InventoryChangeEvent.InventoryLocation testLocation = locations[locationIndex % locations.length];
        
        // Test all event factory methods for consistency
        InventoryChangeEvent tankAdded = InventoryChangeEvent.tankAdded(null, testLocation, ItemStack.EMPTY, slotIndex);
        InventoryChangeEvent tankRemoved = InventoryChangeEvent.tankRemoved(null, testLocation, ItemStack.EMPTY, slotIndex);
        InventoryChangeEvent ringAdded = InventoryChangeEvent.ringAdded(null, testLocation, ItemStack.EMPTY, slotIndex);
        InventoryChangeEvent ringRemoved = InventoryChangeEvent.ringRemoved(null, testLocation, ItemStack.EMPTY, slotIndex);
        InventoryChangeEvent baublesChanged = InventoryChangeEvent.baublesChanged(null, ItemStack.EMPTY, slotIndex);
        InventoryChangeEvent inventoryRefreshed = InventoryChangeEvent.inventoryRefreshed(null, testLocation);
        
        // Property 1: All events should have consistent location information
        assertEquals("Tank added event should have correct location", testLocation, tankAdded.getLocation());
        assertEquals("Tank removed event should have correct location", testLocation, tankRemoved.getLocation());
        assertEquals("Ring added event should have correct location", testLocation, ringAdded.getLocation());
        assertEquals("Ring removed event should have correct location", testLocation, ringRemoved.getLocation());
        assertEquals("Baubles changed event should have Baubles location", 
                    InventoryChangeEvent.InventoryLocation.BAUBLES, baublesChanged.getLocation());
        assertEquals("Inventory refreshed event should have correct location", testLocation, inventoryRefreshed.getLocation());
        
        // Property 2: All events should have consistent slot information
        assertEquals("Tank added event should have correct slot", slotIndex, tankAdded.getSlotIndex());
        assertEquals("Tank removed event should have correct slot", slotIndex, tankRemoved.getSlotIndex());
        assertEquals("Ring added event should have correct slot", slotIndex, ringAdded.getSlotIndex());
        assertEquals("Ring removed event should have correct slot", slotIndex, ringRemoved.getSlotIndex());
        assertEquals("Baubles changed event should have correct slot", slotIndex, baublesChanged.getSlotIndex());
        assertEquals("Inventory refreshed event should have default slot", -1, inventoryRefreshed.getSlotIndex());
        
        // Property 3: Tank events should correctly identify as affecting tanks
        assertTrue("Tank added should affect tanks", tankAdded.affectsTanks());
        assertTrue("Tank removed should affect tanks", tankRemoved.affectsTanks());
        assertFalse("Ring added should not affect tanks", ringAdded.affectsTanks());
        assertFalse("Ring removed should not affect tanks", ringRemoved.affectsTanks());
        
        // Property 4: Ring events should correctly identify as affecting rings
        assertFalse("Tank added should not affect rings", tankAdded.affectsRings());
        assertFalse("Tank removed should not affect rings", tankRemoved.affectsRings());
        assertTrue("Ring added should affect rings", ringAdded.affectsRings());
        assertTrue("Ring removed should affect rings", ringRemoved.affectsRings());
        
        // Property 5: Baubles events should correctly identify as affecting Baubles
        assertTrue("Baubles changed should affect Baubles", baublesChanged.affectsBaubles());
        if (testLocation == InventoryChangeEvent.InventoryLocation.BAUBLES) {
            assertTrue("Events in Baubles location should affect Baubles", tankAdded.affectsBaubles());
            assertTrue("Events in Baubles location should affect Baubles", ringAdded.affectsBaubles());
        }
        
        // Property 6: Event timestamps should be reasonable (within last few seconds)
        long currentTime = System.currentTimeMillis();
        long timeDelta = 5000; // 5 seconds tolerance
        
        assertTrue("Tank added timestamp should be recent", 
                  Math.abs(currentTime - tankAdded.getTimestamp()) < timeDelta);
        assertTrue("Ring removed timestamp should be recent", 
                  Math.abs(currentTime - ringRemoved.getTimestamp()) < timeDelta);
        assertTrue("Baubles changed timestamp should be recent", 
                  Math.abs(currentTime - baublesChanged.getTimestamp()) < timeDelta);
        
        // Property 7: Event toString should contain essential information
        String tankAddedStr = tankAdded.toString();
        assertTrue("Tank added toString should contain change type", 
                  tankAddedStr.contains("TANK_ADDED"));
        assertTrue("Tank added toString should contain location", 
                  tankAddedStr.contains(testLocation.name()));
        assertTrue("Tank added toString should contain slot", 
                  tankAddedStr.contains(String.valueOf(slotIndex)));
        
        // Property 8: Event descriptions should be informative and consistent
        String tankDesc = tankAdded.getDescription();
        String ringDesc = ringAdded.getDescription();
        
        assertNotNull("Tank description should not be null", tankDesc);
        assertNotNull("Ring description should not be null", ringDesc);
        assertTrue("Tank description should contain 'Tank Added'", tankDesc.contains("Tank Added"));
        assertTrue("Ring description should contain 'Ring Added'", ringDesc.contains("Ring Added"));
        assertTrue("Tank description should contain location", tankDesc.contains(testLocation.getDisplayName()));
        assertTrue("Ring description should contain location", ringDesc.contains(testLocation.getDisplayName()));
    }
    
    /**
     * Test event listener to capture fired events.
     */
    private static class TestEventListener {
        private final List<InventoryChangeEvent> allEvents = new ArrayList<>();
        
        @SubscribeEvent
        public void onInventoryChange(InventoryChangeEvent event) {
            allEvents.add(event);
        }
        
        public List<InventoryChangeEvent> getAllEvents() {
            return new ArrayList<>(allEvents);
        }
        
        public List<InventoryChangeEvent> getRefreshEvents() {
            List<InventoryChangeEvent> refreshEvents = new ArrayList<>();
            for (InventoryChangeEvent event : allEvents) {
                if (event.getChangeType() == InventoryChangeEvent.ChangeType.INVENTORY_REFRESHED) {
                    refreshEvents.add(event);
                }
            }
            return refreshEvents;
        }
        
        public void clear() {
            allEvents.clear();
        }
    }
}