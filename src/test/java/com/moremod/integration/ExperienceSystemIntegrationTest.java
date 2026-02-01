package com.moremod.integration;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.moremod.experience.*;
import com.moremod.service.RingDetectionSystem;

/**
 * Integration tests for cross-component functionality in the experience system.
 * Tests complete workflows that span multiple components.
 * 
 * Tests Requirements: All requirements for integrated system behavior
 */
public class ExperienceSystemIntegrationTest {
    
    private ExperiencePumpController pumpController;
    private ExperienceTankManager tankManager;
    private InventoryIntegrationLayer inventoryLayer;
    private RingDetectionSystem ringSystem;
    
    @Before
    public void setUp() {
        pumpController = ExperiencePumpController.getInstance();
        tankManager = ExperienceTankManager.getInstance();
        inventoryLayer = InventoryIntegrationLayer.getInstance();
        ringSystem = RingDetectionSystem.getInstance();
    }
    
    /**
     * Test that all singleton instances are properly initialized.
     */
    @Test
    public void testSingletonInitialization() {
        assertNotNull("ExperiencePumpController should be initialized", pumpController);
        assertNotNull("ExperienceTankManager should be initialized", tankManager);
        assertNotNull("InventoryIntegrationLayer should be initialized", inventoryLayer);
        assertNotNull("RingDetectionSystem should be initialized", ringSystem);
        
        // Verify singleton pattern
        assertSame("ExperiencePumpController should return same instance", 
                  pumpController, ExperiencePumpController.getInstance());
        assertSame("ExperienceTankManager should return same instance", 
                  tankManager, ExperienceTankManager.getInstance());
        assertSame("InventoryIntegrationLayer should return same instance", 
                  inventoryLayer, InventoryIntegrationLayer.getInstance());
        assertSame("RingDetectionSystem should return same instance", 
                  ringSystem, RingDetectionSystem.getInstance());
    }
    
    /**
     * Test XP calculation round trip consistency.
     * Validates that converting XP to levels and back produces consistent results.
     */
    @Test
    public void testXPCalculationRoundTrip() {
        // Test various XP amounts
        int[] testXPValues = {0, 17, 100, 272, 500, 825, 1000, 5000, 10000};
        
        for (int xp : testXPValues) {
            double level = pumpController.convertXPToLevel(xp);
            int convertedBack = pumpController.convertLevelToXP(level);
            
            // Allow small rounding error (within 1%)
            double errorPercent = xp > 0 ? Math.abs((double)(convertedBack - xp) / xp) * 100 : 0;
            assertTrue("XP round trip should be accurate within 1% for " + xp + " XP (got " + convertedBack + ")", 
                      errorPercent < 1.0 || Math.abs(convertedBack - xp) <= 1);
        }
    }
    
    /**
     * Test XP calculation overflow protection.
     */
    @Test
    public void testXPCalculationOverflowProtection() {
        // Test near-maximum values
        int nearMaxXP = Integer.MAX_VALUE - 1000;
        double level = pumpController.convertXPToLevel(nearMaxXP);
        
        assertTrue("Level should be positive", level > 0);
        assertTrue("Level should be reasonable", level < 1000000);
        
        // Test very high level
        double veryHighLevel = 50000;
        int xp = pumpController.convertLevelToXP(veryHighLevel);
        
        assertTrue("XP should be capped at max int", xp <= Integer.MAX_VALUE);
        assertTrue("XP should be positive", xp > 0);
    }
    
    /**
     * Test level-based extraction calculation.
     */
    @Test
    public void testLevelBasedExtraction() {
        // Test extraction at different levels
        int[] testLevels = {0, 5, 10, 15, 20, 25, 30, 35, 40};
        
        for (int level : testLevels) {
            int xpToNextLevel = pumpController.getXPToNextLevel(level);
            
            assertTrue("XP to next level should be positive for level " + level, xpToNextLevel > 0);
            assertTrue("XP to next level should be reasonable for level " + level, xpToNextLevel < 10000);
            
            // Verify it matches Minecraft's formula
            if (level <= 14) {
                assertEquals("XP to next level should be 17 for levels 0-14", 17, xpToNextLevel);
            } else if (level <= 29) {
                int expected = 17 + (level - 15) * 3;
                assertEquals("XP to next level should match formula for levels 15-29", expected, xpToNextLevel);
            } else {
                int expected = 62 + (level - 30) * 7;
                assertEquals("XP to next level should match formula for levels 30+", expected, xpToNextLevel);
            }
        }
    }
    
    /**
     * Test experience display format.
     */
    @Test
    public void testExperienceDisplayFormat() {
        // Test various XP amounts
        int[] testXPValues = {0, 17, 100, 500, 1000, 5000};
        
        for (int xp : testXPValues) {
            String display = pumpController.formatExperienceDisplay(xp);
            
            assertNotNull("Display format should not be null", display);
            assertFalse("Display format should not be empty", display.isEmpty());
            
            // Should contain both XP and level information
            assertTrue("Display should contain XP amount", display.contains(String.valueOf(xp)));
            
            // Should contain level information (L or level)
            assertTrue("Display should contain level indicator", 
                      display.toLowerCase().contains("l") || display.toLowerCase().contains("level"));
        }
    }
    
    /**
     * Test tank upgrade preservation workflow.
     */
    @Test
    public void testTankUpgradeWorkflow() {
        // Test that tank manager is ready for upgrade operations
        assertNotNull("Tank manager should be initialized", tankManager);
        
        // Test capacity validation
        int storedXP = 1000;
        int maxCapacity = 2000;
        
        int validated = tankManager.validateCapacity(storedXP, maxCapacity);
        assertEquals("Stored XP within capacity should not change", storedXP, validated);
        
        // Test overflow handling
        int overflowXP = 3000;
        int validated2 = tankManager.validateCapacity(overflowXP, maxCapacity);
        assertEquals("Stored XP exceeding capacity should be capped", maxCapacity, validated2);
    }
    
    /**
     * Test inventory integration layer null safety.
     */
    @Test
    public void testInventoryIntegrationNullSafety() {
        // Test that null player is handled gracefully
        TankScanResult result = inventoryLayer.scanAllInventories(null);
        
        assertNotNull("Scan result should not be null for null player", result);
        assertEquals("Scan result should be empty for null player", 0, result.getTankCount());
        assertEquals("Total capacity should be 0 for null player", 0, result.getTotalCapacity());
        assertEquals("Total stored should be 0 for null player", 0, result.getTotalStored());
    }
    
    /**
     * Test ring detection system null safety.
     */
    @Test
    public void testRingDetectionNullSafety() {
        // Test that null player is handled gracefully
        RingDetectionResult result = ringSystem.scanForRings(null);
        
        assertNotNull("Ring detection result should not be null for null player", result);
        assertFalse("Should not have rings for null player", result.hasRings());
        assertEquals("Ring count should be 0 for null player", 0, result.getRingCount());
    }
    
    /**
     * Test pump controller null safety.
     */
    @Test
    public void testPumpControllerNullSafety() {
        // Test that null player is handled gracefully
        int capacity = pumpController.calculateTotalCapacity(null);
        int stored = pumpController.calculateTotalStored(null);
        int remaining = pumpController.calculateTotalRemainingCapacity(null);
        
        assertEquals("Total capacity should be 0 for null player", 0, capacity);
        assertEquals("Total stored should be 0 for null player", 0, stored);
        assertEquals("Remaining capacity should be 0 for null player", 0, remaining);
    }
    
    /**
     * Test scroll wheel fine tuning.
     */
    @Test
    public void testScrollWheelFineTuning() {
        int baseAmount = 100;
        
        // Test scroll up
        int increased = pumpController.processScrollInput(1, true, baseAmount);
        assertTrue("Scroll up should increase amount", increased > baseAmount);
        
        // Test scroll down
        int decreased = pumpController.processScrollInput(-1, true, baseAmount);
        assertTrue("Scroll down should decrease amount", decreased < baseAmount);
        assertTrue("Scroll down should not go below 1", decreased >= 1);
        
        // Test no scroll
        int unchanged = pumpController.processScrollInput(0, true, baseAmount);
        assertEquals("No scroll should not change amount", baseAmount, unchanged);
        
        // Test multiple scrolls
        int amount = baseAmount;
        for (int i = 0; i < 5; i++) {
            amount = pumpController.processScrollInput(1, true, amount);
        }
        assertTrue("Multiple scroll ups should significantly increase amount", amount > baseAmount * 1.4);
    }
    
    /**
     * Test component interaction consistency.
     */
    @Test
    public void testComponentInteractionConsistency() {
        // Verify that all components use consistent data structures
        
        // Tank scan result should be compatible with pump controller
        TankScanResult scanResult = TankScanResult.empty();
        assertNotNull("Empty scan result should not be null", scanResult);
        assertEquals("Empty scan result should have 0 tanks", 0, scanResult.getTankCount());
        
        // Ring detection result should be compatible with ring system
        RingDetectionResult ringResult = RingDetectionResult.empty();
        assertNotNull("Empty ring result should not be null", ringResult);
        assertFalse("Empty ring result should have no rings", ringResult.hasRings());
        
        // Experience tank data should be compatible with tank manager
        ExperienceTankData tankData = new ExperienceTankData(0, 1000, 1);
        assertNotNull("Tank data should not be null", tankData);
        assertEquals("Tank data should have correct capacity", 1000, tankData.getMaxCapacity());
    }
    
    /**
     * Test error recovery and graceful degradation.
     */
    @Test
    public void testErrorRecoveryAndGracefulDegradation() {
        // Test that invalid inputs are handled gracefully
        
        // Negative XP
        double level = pumpController.convertXPToLevel(-100);
        assertEquals("Negative XP should convert to level 0", 0.0, level, 0.01);
        
        // Negative level
        int xp = pumpController.convertLevelToXP(-10);
        assertEquals("Negative level should convert to 0 XP", 0, xp);
        
        // Invalid capacity
        int validated = tankManager.validateCapacity(1000, -500);
        assertTrue("Invalid capacity should be handled gracefully", validated >= 0);
    }
}
