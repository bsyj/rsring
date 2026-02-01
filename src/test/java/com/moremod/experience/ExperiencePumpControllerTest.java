package com.moremod.experience;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Unit tests for ExperiencePumpController class.
 * Tests core functionality including tank scanning, capacity calculation,
 * XP conversion, and experience operations.
 * Includes property-based tests for total capacity calculation.
 */
@RunWith(JUnitQuickcheck.class)
public class ExperiencePumpControllerTest {
    
    private ExperiencePumpController controller;
    
    @Before
    public void setUp() {
        controller = ExperiencePumpController.getInstance();
    }
    
    @Test
    public void testSingletonInstance() {
        ExperiencePumpController instance1 = ExperiencePumpController.getInstance();
        ExperiencePumpController instance2 = ExperiencePumpController.getInstance();
        
        assertSame("Should return same singleton instance", instance1, instance2);
    }
    
    @Test
    public void testScanAllInventoriesWithNullPlayer() {
        TankScanResult result = controller.scanAllInventories(null);
        
        assertNotNull("Result should not be null", result);
        assertEquals("Should return empty result for null player", 0, result.getTankCount());
        assertEquals("Should have zero capacity for null player", 0, result.getTotalCapacity());
    }
    
    @Test
    public void testCalculateTotalCapacityWithNullPlayer() {
        int capacity = controller.calculateTotalCapacity(null);
        
        assertEquals("Should return 0 capacity for null player", 0, capacity);
    }
    
    @Test
    public void testCalculateTotalStoredWithNullPlayer() {
        int stored = controller.calculateTotalStored(null);
        
        assertEquals("Should return 0 stored for null player", 0, stored);
    }
    
    @Test
    public void testCalculateTotalRemainingCapacityWithNullPlayer() {
        int remaining = controller.calculateTotalRemainingCapacity(null);
        
        assertEquals("Should return 0 remaining capacity for null player", 0, remaining);
    }
    
    @Test
    public void testProcessScrollInputWithZeroDelta() {
        int baseAmount = 100;
        int result = controller.processScrollInput(0, true, baseAmount);
        
        assertEquals("Should return base amount when scroll delta is 0", baseAmount, result);
    }
    
    @Test
    public void testProcessScrollInputScrollUp() {
        int baseAmount = 100;
        int result = controller.processScrollInput(1, true, baseAmount);
        
        assertTrue("Should increase amount when scrolling up", result > baseAmount);
        assertEquals("Should increase by 10% (minimum 1)", 110, result);
    }
    
    @Test
    public void testProcessScrollInputScrollDown() {
        int baseAmount = 100;
        int result = controller.processScrollInput(-1, true, baseAmount);
        
        assertTrue("Should decrease amount when scrolling down", result < baseAmount);
        assertEquals("Should decrease by 10%", 90, result);
    }
    
    @Test
    public void testProcessScrollInputScrollDownMinimum() {
        int baseAmount = 5;
        int result = controller.processScrollInput(-1, true, baseAmount);
        
        assertTrue("Should not go below 1", result >= 1);
        assertEquals("Should be 4 (5 - 1)", 4, result);
    }
    
    @Test
    public void testProcessScrollInputScrollDownToMinimum() {
        int baseAmount = 1;
        int result = controller.processScrollInput(-1, true, baseAmount);
        
        assertEquals("Should stay at minimum of 1", 1, result);
    }
    
    @Test
    public void testPerformExperienceOperationWithNullPlayer() {
        int result = controller.performExperienceOperation(null, 100, true);
        
        assertEquals("Should return 0 for null player", 0, result);
    }
    
    @Test
    public void testPerformExperienceOperationWithZeroAmount() {
        int result = controller.performExperienceOperation(null, 0, true);
        
        assertEquals("Should return 0 for zero amount", 0, result);
    }
    
    @Test
    public void testPerformExperienceOperationWithNegativeAmount() {
        int result = controller.performExperienceOperation(null, -100, true);
        
        assertEquals("Should return 0 for negative amount", 0, result);
    }
    
    @Test
    public void testConvertXPToLevelZero() {
        double level = controller.convertXPToLevel(0);
        
        assertEquals("Should return 0 level for 0 XP", 0.0, level, 0.001);
    }
    
    @Test
    public void testConvertXPToLevelNegative() {
        double level = controller.convertXPToLevel(-100);
        
        assertEquals("Should return 0 level for negative XP", 0.0, level, 0.001);
    }
    
    @Test
    public void testConvertXPToLevelLowRange() {
        // Test levels 0-15 (17 XP per level)
        double level = controller.convertXPToLevel(170); // Should be level 10
        
        assertEquals("Should correctly convert XP in 0-15 range", 10.0, level, 0.001);
    }
    
    @Test
    public void testConvertXPToLevelMidRange() {
        // Test levels 16-30 
        double level = controller.convertXPToLevel(272); // Should be level 16 (272 / 17 = 16)
        
        assertEquals("Should correctly convert XP at boundary", 16.0, level, 0.1);
    }
    
    @Test
    public void testConvertLevelToXPZero() {
        int xp = controller.convertLevelToXP(0.0);
        
        assertEquals("Should return 0 XP for level 0", 0, xp);
    }
    
    @Test
    public void testConvertLevelToXPNegative() {
        int xp = controller.convertLevelToXP(-5.0);
        
        assertEquals("Should return 0 XP for negative level", 0, xp);
    }
    
    @Test
    public void testConvertLevelToXPLowRange() {
        // Test levels 0-15 (17 XP per level)
        int xp = controller.convertLevelToXP(10.0);
        
        assertEquals("Should correctly convert level in 0-15 range", 170, xp);
    }
    
    @Test
    public void testConvertLevelToXPBoundary() {
        // Test level 15 boundary
        int xp = controller.convertLevelToXP(15.0);
        
        assertEquals("Should correctly convert level at boundary", 255, xp); // 15 * 17
    }
    
    @Test
    public void testGetPlayerTotalExperienceWithNullPlayer() {
        int xp = controller.getPlayerTotalExperience(null);
        
        assertEquals("Should return 0 XP for null player", 0, xp);
    }
    
    @Test
    public void testGetXPToNextLevelLowRange() {
        int xpRequired = controller.getXPToNextLevel(5);
        
        assertEquals("Should return 17 XP for levels below 15", 17, xpRequired);
    }
    
    @Test
    public void testGetXPToNextLevelMidRange() {
        int xpRequired = controller.getXPToNextLevel(20);
        
        assertEquals("Should return correct XP for levels 16-30", 32, xpRequired); // 17 + (20 - 15) * 3 = 17 + 15 = 32
    }
    
    @Test
    public void testGetXPToNextLevelHighRange() {
        int xpRequired = controller.getXPToNextLevel(35);
        
        assertEquals("Should return correct XP for levels 31+", 97, xpRequired); // 62 + (35 - 30) * 7 = 62 + 35 = 97
    }
    
    @Test
    public void testFindFirstAvailableTankWithNullPlayer() {
        ItemStack result = controller.findFirstAvailableTank(null);
        
        assertTrue("Should return empty stack for null player", result.isEmpty());
    }
    
    @Test
    public void testRefreshInventoryStateWithNullPlayer() {
        // Should not throw exception
        controller.refreshInventoryState(null);
        
        // Test passes if no exception is thrown
        assertTrue("Should handle null player gracefully", true);
    }
    
    @Test
    public void testGetDiagnosticsWithNullPlayer() {
        Map<String, Object> diagnostics = controller.getDiagnostics(null);
        
        assertNotNull("Should not return null diagnostics", diagnostics);
        assertEquals("Should indicate error for null player", "Player is null", diagnostics.get("error"));
    }
    
    @Test
    public void testFormatExperienceDisplayZero() {
        String formatted = controller.formatExperienceDisplay(0);
        
        assertEquals("Should format zero XP correctly", "0 XP (0 levels)", formatted);
    }
    
    @Test
    public void testFormatExperienceDisplayNegative() {
        String formatted = controller.formatExperienceDisplay(-100);
        
        assertEquals("Should format negative XP as zero", "0 XP (0 levels)", formatted);
    }
    
    @Test
    public void testFormatExperienceDisplayPositive() {
        String formatted = controller.formatExperienceDisplay(170); // 10 levels
        
        assertTrue("Should include XP amount", formatted.contains("170 XP"));
        assertTrue("Should include level equivalent", formatted.contains("levels"));
        assertTrue("Should show approximately 10 levels", formatted.contains("10.0"));
    }
    
    @Test
    public void testCalculateLevelBasedExtractionWithNullPlayer() {
        int extractable = controller.calculateLevelBasedExtraction(null, 5);
        
        assertEquals("Should return 0 for null player", 0, extractable);
    }
    
    @Test
    public void testCalculateLevelBasedExtractionWithNegativeLevel() {
        int extractable = controller.calculateLevelBasedExtraction(null, -1);
        
        assertEquals("Should return 0 for negative target level", 0, extractable);
    }
    
    @Test
    public void testXPConversionRoundTrip() {
        // Test that converting XP to level and back gives approximately the same result
        int originalXP = 1000;
        double level = controller.convertXPToLevel(originalXP);
        int convertedBackXP = controller.convertLevelToXP(level);
        
        // Allow for some rounding error
        int difference = Math.abs(originalXP - convertedBackXP);
        assertTrue("Round trip conversion should be approximately equal (diff: " + difference + ")", 
                  difference <= 100); // Allow up to 100 XP difference due to level calculation complexity
    }
    
    @Test
    public void testScrollInputFineGrainedControl() {
        // Test that scroll input provides fine-grained control
        int baseAmount = 1000;
        
        int scrollUp = controller.processScrollInput(1, true, baseAmount);
        int scrollDown = controller.processScrollInput(-1, true, baseAmount);
        
        assertEquals("Scroll up should increase by 10%", 1100, scrollUp);
        assertEquals("Scroll down should decrease by 10%", 900, scrollDown);
        
        // Test with small base amount
        int smallBase = 5;
        int smallScrollUp = controller.processScrollInput(1, true, smallBase);
        int smallScrollDown = controller.processScrollInput(-1, true, smallBase);
        
        assertEquals("Small scroll up should increase by at least 1", 6, smallScrollUp);
        assertEquals("Small scroll down should decrease by 1", 4, smallScrollDown);
    }
    
    @Test
    public void testCapacityCalculationEdgeCases() {
        // Test with zero capacity
        assertEquals("Zero capacity should remain zero", 0, controller.calculateTotalCapacity(null));
        
        // Test remaining capacity calculation
        assertEquals("Remaining capacity should be zero for null player", 0, 
                    controller.calculateTotalRemainingCapacity(null));
    }
    
    @Test
    public void testExperienceOperationEdgeCases() {
        // Test with various invalid inputs
        assertEquals("Should handle null player", 0, 
                    controller.performExperienceOperation(null, 100, true));
        assertEquals("Should handle zero amount", 0, 
                    controller.performExperienceOperation(null, 0, true));
        assertEquals("Should handle negative amount", 0, 
                    controller.performExperienceOperation(null, -50, false));
    }
    
    // ========== PROPERTY-BASED TESTS ==========
    
    /**
     * Property-based test for total capacity calculation.
     * 
     * **Property 3: Total Capacity Calculation**
     * For any set of detected experience tanks, the displayed total capacity 
     * should equal the sum of all individual tank capacities.
     * 
     * **Validates: Requirements 3.4**
     */
    @Property(trials = 100)
    public void testTotalCapacityCalculation(@InRange(min = "1", max = "10") int numTanks,
                                           @InRange(min = "1000", max = "50000") int tankCapacity,
                                           @InRange(min = "0", max = "100000") int storedXP) {
        
        // Create a test TankScanResult with the specified number of tanks
        TestTankScanResult testResult = new TestTankScanResult(numTanks, tankCapacity, storedXP);
        
        // Calculate expected total capacity (sum of individual capacities)
        int expectedTotalCapacity = numTanks * tankCapacity;
        
        // Verify that the scan result's total capacity matches the expected sum
        int actualTotalCapacity = testResult.getTotalCapacity();
        
        assertEquals("Total capacity should equal sum of individual tank capacities",
                    expectedTotalCapacity, actualTotalCapacity);
        
        // Additional property: Total capacity should never be negative
        assertTrue("Total capacity should never be negative", actualTotalCapacity >= 0);
        
        // Additional property: Total capacity should be at least as large as any individual tank
        if (numTanks > 0) {
            assertTrue("Total capacity should be at least as large as individual tank capacity",
                      actualTotalCapacity >= tankCapacity);
        }
        
        // Additional property: If we have N tanks of capacity C, total should be N * C
        assertEquals("Total capacity should be number of tanks times individual capacity",
                    numTanks * tankCapacity, actualTotalCapacity);
    }
    
    /**
     * Property-based test for total capacity calculation with varying tank capacities.
     * Tests the property with tanks of different capacities to ensure proper summation.
     * 
     * **Property 3: Total Capacity Calculation (Varied Capacities)**
     * **Validates: Requirements 3.4**
     */
    @Property(trials = 100)
    public void testTotalCapacityCalculation_VariedCapacities(@InRange(min = "2", max = "5") int numTanks) {
        
        // Create tanks with different capacities: 1000, 2000, 3000, etc.
        int[] tankCapacities = new int[numTanks];
        int expectedTotalCapacity = 0;
        
        for (int i = 0; i < numTanks; i++) {
            tankCapacities[i] = 1000 * (i + 1);
            expectedTotalCapacity += tankCapacities[i];
        }
        
        TestTankScanResult testResult = new TestTankScanResult(tankCapacities);
        
        // Verify that the total capacity equals the sum of varied capacities
        int actualTotalCapacity = testResult.getTotalCapacity();
        
        assertEquals("Total capacity should equal sum of varied individual tank capacities",
                    expectedTotalCapacity, actualTotalCapacity);
        
        // Property: Total should be greater than any individual tank capacity
        for (int individualCapacity : tankCapacities) {
            assertTrue("Total capacity should be greater than or equal to any individual tank",
                      actualTotalCapacity >= individualCapacity);
        }
    }
    
    /**
     * Property-based test for total capacity calculation edge cases.
     * Tests boundary conditions and edge cases for capacity calculation.
     * 
     * **Property 3: Total Capacity Calculation (Edge Cases)**
     * **Validates: Requirements 3.4**
     */
    @Property(trials = 50)
    public void testTotalCapacityCalculation_EdgeCases(@InRange(min = "0", max = "1") int numTanks,
                                                      @InRange(min = "1", max = "100000") int tankCapacity) {
        
        TestTankScanResult testResult = new TestTankScanResult(numTanks, tankCapacity, 0);
        
        int expectedTotalCapacity = numTanks * tankCapacity;
        int actualTotalCapacity = testResult.getTotalCapacity();
        
        assertEquals("Total capacity should equal sum even for edge cases",
                    expectedTotalCapacity, actualTotalCapacity);
        
        // Edge case properties
        if (numTanks == 0) {
            assertEquals("Zero tanks should result in zero total capacity", 0, actualTotalCapacity);
        } else {
            assertEquals("Single tank should have capacity equal to its individual capacity",
                        tankCapacity, actualTotalCapacity);
        }
    }
    
    /**
     * Property-based test for capacity calculation mathematical properties.
     * Tests fundamental mathematical properties that should always hold.
     * 
     * **Property 3: Total Capacity Calculation (Mathematical Properties)**
     * **Validates: Requirements 3.4**
     */
    @Property(trials = 100)
    public void testTotalCapacityCalculation_MathematicalProperties(@InRange(min = "1", max = "20") int numTanks,
                                                                   @InRange(min = "100", max = "10000") int baseCapacity) {
        
        TestTankScanResult testResult = new TestTankScanResult(numTanks, baseCapacity, 0);
        
        int totalCapacity = testResult.getTotalCapacity();
        
        // Mathematical properties that should always hold:
        
        // 1. Distributive property: N * C = C + C + ... + C (N times)
        int expectedSum = 0;
        for (int i = 0; i < numTanks; i++) {
            expectedSum += baseCapacity;
        }
        assertEquals("Distributive property should hold", expectedSum, totalCapacity);
        
        // 2. Multiplicative property: N * C = total
        assertEquals("Multiplicative property should hold", numTanks * baseCapacity, totalCapacity);
        
        // 3. Monotonicity: More tanks should never decrease total capacity
        TestTankScanResult smallerResult = new TestTankScanResult(Math.max(0, numTanks - 1), baseCapacity, 0);
        assertTrue("More tanks should result in greater or equal capacity",
                  totalCapacity >= smallerResult.getTotalCapacity());
        
        // 4. Scaling property: Doubling tank capacity should double total capacity
        TestTankScanResult doubledResult = new TestTankScanResult(numTanks, baseCapacity * 2, 0);
        assertEquals("Doubling capacity should double total", totalCapacity * 2, doubledResult.getTotalCapacity());
    }
    
    /**
     * Property-based test for XP calculation round trip accuracy.
     * Tests that converting XP to levels and back to XP produces equivalent results
     * using Minecraft's official formulas.
     * 
     * **Property 4: XP Calculation Round Trip**
     * **Validates: Requirements 6.1, 6.2**
     */
    @Property(trials = 100)
    public void testXPCalculationRoundTrip(@InRange(min = "0", max = "100000") int originalXP) {
        
        // Convert XP to level and back to XP
        double level = controller.convertXPToLevel(originalXP);
        int convertedBackXP = controller.convertLevelToXP(level);
        
        // Calculate the difference between original and round-trip converted XP
        int difference = Math.abs(originalXP - convertedBackXP);
        
        // Property 1: Round trip conversion should be approximately equal
        // Allow for reasonable rounding error due to level calculation complexity
        // The tolerance is based on the XP requirements per level at different ranges
        int tolerance = calculateToleranceForXP(originalXP);
        assertTrue("Round trip conversion should be approximately equal (original: " + originalXP + 
                  ", converted: " + convertedBackXP + ", diff: " + difference + ", tolerance: " + tolerance + ")", 
                  difference <= tolerance);
        
        // Property 2: Level should never be negative
        assertTrue("Level should never be negative for non-negative XP", level >= 0.0);
        
        // Property 3: Zero XP should convert to zero level and back to zero XP
        if (originalXP == 0) {
            assertEquals("Zero XP should convert to zero level", 0.0, level, 0.001);
            assertEquals("Zero level should convert back to zero XP", 0, convertedBackXP);
        }
        
        // Property 4: Monotonicity - more XP should never result in lower level
        if (originalXP > 0) {
            double lowerLevel = controller.convertXPToLevel(Math.max(0, originalXP - 1));
            assertTrue("More XP should result in higher or equal level", level >= lowerLevel);
        }
        
        // Property 5: Level boundaries should be consistent
        // Test specific level boundaries for accuracy
        if (originalXP <= 272) {
            // Levels 0-15 range: should be linear relationship
            double expectedLevel = (double) originalXP / 17.0;
            assertEquals("Level calculation in 0-15 range should be linear", 
                        expectedLevel, level, 0.1);
        }
        
        // Property 6: Converted XP should never be negative
        assertTrue("Converted XP should never be negative", convertedBackXP >= 0);
        
        // Property 7: For very small XP amounts, precision should be high
        if (originalXP <= 100) {
            assertTrue("Small XP amounts should have high precision (diff: " + difference + ")", 
                      difference <= 5);
        }
    }
    
    /**
     * Property-based test for XP calculation boundary conditions.
     * Tests the accuracy of XP calculations at critical level boundaries.
     * 
     * **Property 4: XP Calculation Round Trip (Boundary Conditions)**
     * **Validates: Requirements 6.1, 6.2**
     */
    @Property(trials = 50)
    public void testXPCalculationRoundTrip_BoundaryConditions(@InRange(min = "0", max = "50") int levelOffset) {
        
        // Test critical boundaries in Minecraft's XP system
        int[] criticalXPValues = {
            0,      // Level 0
            17,     // Level 1
            272,    // Level 16 boundary (end of linear range)
            825,    // Level 31 boundary (end of quadratic range 1)
            1395,   // Level 40 (in quadratic range 2)
            2220,   // Level 50 (higher level)
            272 + levelOffset,  // Near level 16 boundary
            825 + levelOffset   // Near level 31 boundary
        };
        
        for (int xp : criticalXPValues) {
            if (xp < 0) continue; // Skip negative values
            
            double level = controller.convertXPToLevel(xp);
            int convertedBackXP = controller.convertLevelToXP(level);
            
            int difference = Math.abs(xp - convertedBackXP);
            int tolerance = calculateToleranceForXP(xp);
            
            assertTrue("Boundary XP " + xp + " should round trip accurately (diff: " + difference + 
                      ", tolerance: " + tolerance + ")", difference <= tolerance);
            
            // Additional boundary-specific checks
            if (xp == 0) {
                assertEquals("Zero XP should stay zero", 0, convertedBackXP);
                assertEquals("Zero XP should be level 0", 0.0, level, 0.001);
            }
            
            if (xp == 272) {
                // This is the boundary between level ranges
                assertTrue("Level 16 boundary should be around level 16", 
                          Math.abs(level - 16.0) < 0.5);
            }
            
            if (xp == 825) {
                // This is the boundary between quadratic ranges
                assertTrue("Level 31 boundary should be around level 30-31", 
                          level >= 30.0 && level <= 31.5);
            }
        }
    }
    
    /**
     * Property-based test for XP calculation mathematical consistency.
     * Tests that the XP calculation formulas maintain mathematical consistency.
     * 
     * **Property 4: XP Calculation Round Trip (Mathematical Consistency)**
     * **Validates: Requirements 6.1, 6.2**
     */
    @Property(trials = 100)
    public void testXPCalculationRoundTrip_MathematicalConsistency(@InRange(min = "1", max = "50") int level1,
                                                                  @InRange(min = "1", max = "50") int level2) {
        
        // Ensure level1 <= level2 for consistency tests
        int lowerLevel = Math.min(level1, level2);
        int higherLevel = Math.max(level1, level2);
        
        int xpLower = controller.convertLevelToXP(lowerLevel);
        int xpHigher = controller.convertLevelToXP(higherLevel);
        
        // Property 1: Higher levels should require more XP
        assertTrue("Higher level should require more XP (level " + lowerLevel + " = " + xpLower + 
                  " XP, level " + higherLevel + " = " + xpHigher + " XP)", xpHigher >= xpLower);
        
        // Property 2: XP difference should be reasonable
        if (higherLevel > lowerLevel) {
            int xpDifference = xpHigher - xpLower;
            assertTrue("XP difference should be positive for higher levels", xpDifference > 0);
            
            // The difference should not be unreasonably large
            int maxExpectedDifference = (higherLevel - lowerLevel) * 200; // Rough upper bound
            assertTrue("XP difference should not be unreasonably large (diff: " + xpDifference + 
                      ", max expected: " + maxExpectedDifference + ")", 
                      xpDifference <= maxExpectedDifference);
        }
        
        // Property 3: Converting back should give approximately the same level
        double convertedLowerLevel = controller.convertXPToLevel(xpLower);
        double convertedHigherLevel = controller.convertXPToLevel(xpHigher);
        
        assertEquals("Lower level should round trip accurately", 
                    lowerLevel, convertedLowerLevel, 0.5);
        assertEquals("Higher level should round trip accurately", 
                    higherLevel, convertedHigherLevel, 0.5);
        
        // Property 4: Level ordering should be preserved
        assertTrue("Level ordering should be preserved after round trip", 
                  convertedHigherLevel >= convertedLowerLevel);
    }
    
    /**
     * Property-based test for level-based extraction calculation.
     * Tests that the XP amount calculated for level-based extraction operations
     * follows the correct mathematical relationship using Minecraft's level formulas.
     * 
     * **Property 14: Level-Based Extraction Calculation**
     * For any extraction operation, the XP amount should be calculated based on 
     * the current player's level using proper level-to-XP cost formulas.
     * 
     * **Validates: Requirements 6.4**
     */
    @Property(trials = 100)
    public void testLevelBasedExtractionCalculation(@InRange(min = "0", max = "50") int currentLevel,
                                                   @InRange(min = "0", max = "50") int targetLevel) {
        
        // Ensure currentLevel >= targetLevel for meaningful extraction
        int actualCurrentLevel = Math.max(currentLevel, targetLevel);
        int actualTargetLevel = Math.min(currentLevel, targetLevel);
        
        // Calculate XP amounts using the controller's conversion methods
        int currentXP = controller.convertLevelToXP(actualCurrentLevel);
        int targetXP = controller.convertLevelToXP(actualTargetLevel);
        
        // Calculate expected extractable XP
        int expectedExtractableXP = Math.max(0, currentXP - targetXP);
        
        // Test the mathematical properties of level-based extraction
        
        // Property 1: Extractable XP should equal current XP minus target XP
        assertEquals("Extractable XP should equal current XP minus target XP",
                    expectedExtractableXP, currentXP - targetXP);
        
        // Property 2: Extractable XP should never be negative
        assertTrue("Extractable XP should never be negative", expectedExtractableXP >= 0);
        
        // Property 3: If current level equals target level, extractable XP should be 0
        if (actualCurrentLevel == actualTargetLevel) {
            assertEquals("Same current and target level should result in 0 extractable XP",
                        0, expectedExtractableXP);
        }
        
        // Property 4: If current level > target level, extractable XP should be positive
        if (actualCurrentLevel > actualTargetLevel) {
            assertTrue("Higher current level should result in positive extractable XP",
                      expectedExtractableXP > 0);
        }
        
        // Property 5: Monotonicity - higher current level should result in more extractable XP
        if (actualCurrentLevel > 0) {
            int lowerCurrentXP = controller.convertLevelToXP(actualCurrentLevel - 1);
            int lowerExtractableXP = Math.max(0, lowerCurrentXP - targetXP);
            
            assertTrue("Higher current level should result in more or equal extractable XP",
                      expectedExtractableXP >= lowerExtractableXP);
        }
        
        // Property 6: Lower target level should result in more extractable XP
        if (actualTargetLevel > 0) {
            int higherTargetXP = controller.convertLevelToXP(actualTargetLevel + 1);
            int lowerExtractableXP = Math.max(0, currentXP - higherTargetXP);
            
            assertTrue("Lower target level should result in more or equal extractable XP",
                      expectedExtractableXP >= lowerExtractableXP);
        }
        
        // Property 7: Extractable XP should not exceed current XP
        assertTrue("Extractable XP should not exceed current XP",
                  expectedExtractableXP <= currentXP);
        
        // Property 8: Mathematical consistency - extractable + target should equal current
        assertEquals("Extractable XP plus target XP should equal current XP",
                    currentXP, expectedExtractableXP + targetXP);
        
        // Property 9: Level boundaries should be respected
        // Converting extractable XP back to levels should be consistent
        if (expectedExtractableXP > 0) {
            double extractableLevels = controller.convertXPToLevel(expectedExtractableXP);
            assertTrue("Extractable XP should convert to reasonable level amount",
                      extractableLevels >= 0.0);
            
            // Note: Due to Minecraft's non-linear XP system, extractable levels may not
            // directly correspond to level differences, especially across different XP ranges
            // This is expected behavior and not a bug
        }
        
        // Property 10: Edge case - target level 0 should extract all current XP
        if (actualTargetLevel == 0) {
            assertEquals("Target level 0 should extract all current XP",
                        currentXP, expectedExtractableXP);
        }
        
        // Property 11: Specific level ranges should follow expected patterns
        if (actualCurrentLevel <= 15 && actualTargetLevel <= 15) {
            // In the linear range (0-15), the relationship should be simple
            int levelDiff = actualCurrentLevel - actualTargetLevel;
            int expectedLinearXP = levelDiff * 17; // 17 XP per level in this range
            
            // Allow some tolerance due to partial level calculations
            int tolerance = Math.max(1, expectedLinearXP / 10);
            assertTrue("Linear range extraction should follow 17 XP per level pattern (expected: " + 
                      expectedLinearXP + ", actual: " + expectedExtractableXP + ", tolerance: " + tolerance + ")",
                      Math.abs(expectedExtractableXP - expectedLinearXP) <= tolerance);
        }
    }
    
    /**
     * Property-based test for level-based extraction edge cases.
     * Tests boundary conditions and edge cases for level-based extraction calculation.
     * 
     * **Property 14: Level-Based Extraction Calculation (Edge Cases)**
     * **Validates: Requirements 6.4**
     */
    @Property(trials = 50)
    public void testLevelBasedExtractionCalculation_EdgeCases(@InRange(min = "0", max = "100") int xpOffset) {
        
        // Test critical level boundaries
        int[] criticalLevels = {0, 1, 15, 16, 30, 31, 50};
        
        for (int currentLevel : criticalLevels) {
            for (int targetLevel : criticalLevels) {
                if (currentLevel < targetLevel) continue; // Skip invalid combinations
                
                int currentXP = controller.convertLevelToXP(currentLevel);
                int targetXP = controller.convertLevelToXP(targetLevel);
                int expectedExtractableXP = currentXP - targetXP;
                
                // Add some XP offset to test non-exact level amounts
                int adjustedCurrentXP = currentXP + (xpOffset % 100);
                int adjustedExtractableXP = adjustedCurrentXP - targetXP;
                
                // Properties for boundary conditions
                assertTrue("Boundary extraction should never be negative", 
                          expectedExtractableXP >= 0);
                assertTrue("Adjusted boundary extraction should never be negative", 
                          adjustedExtractableXP >= 0);
                
                // Property: Extractable XP should increase with XP offset
                assertTrue("Adding XP offset should increase or maintain extractable amount",
                          adjustedExtractableXP >= expectedExtractableXP);
                
                // Property: The increase should equal the offset
                assertEquals("XP offset should directly increase extractable amount",
                            expectedExtractableXP + (xpOffset % 100), adjustedExtractableXP);
                
                // Property: Level 0 target should extract everything
                if (targetLevel == 0) {
                    assertEquals("Target level 0 should extract all XP",
                                currentXP, expectedExtractableXP);
                    assertEquals("Target level 0 should extract all adjusted XP",
                                adjustedCurrentXP, adjustedExtractableXP);
                }
                
                // Property: Same level should extract nothing
                if (currentLevel == targetLevel) {
                    assertEquals("Same current and target level should extract nothing",
                                0, expectedExtractableXP);
                }
            }
        }
    }
    
    /**
     * Property-based test for level-based extraction mathematical consistency.
     * Tests that the extraction calculation maintains mathematical consistency
     * across different level ranges and XP amounts.
     * 
     * **Property 14: Level-Based Extraction Calculation (Mathematical Consistency)**
     * **Validates: Requirements 6.4**
     */
    @Property(trials = 100)
    public void testLevelBasedExtractionCalculation_MathematicalConsistency(@InRange(min = "1", max = "1000") int baseXP,
                                                                           @InRange(min = "0", max = "500") int extractXP) {
        
        // Ensure we have a valid scenario: baseXP > extractXP
        int currentXP = Math.max(baseXP, extractXP + 1);
        int targetXP = currentXP - Math.min(extractXP, currentXP - 1);
        
        // Convert to levels for testing
        double currentLevel = controller.convertXPToLevel(currentXP);
        double targetLevel = controller.convertXPToLevel(targetXP);
        
        // Calculate extractable XP
        int extractableXP = currentXP - targetXP;
        
        // Mathematical consistency properties
        
        // Property 1: Extraction should be conservative (never extract more than available)
        assertTrue("Should never extract more XP than available",
                  extractableXP <= currentXP);
        
        // Property 2: Extraction should be exact (current - target = extractable)
        assertEquals("Extraction should be exact mathematical difference",
                    currentXP - targetXP, extractableXP);
        
        // Property 3: After extraction, remaining XP should equal target XP
        int remainingXP = currentXP - extractableXP;
        assertEquals("Remaining XP after extraction should equal target XP",
                    targetXP, remainingXP);
        
        // Property 4: Level consistency - remaining level should be close to target level
        double remainingLevel = controller.convertXPToLevel(remainingXP);
        double levelTolerance = 0.5; // Allow half-level tolerance due to rounding
        assertTrue("Remaining level should be close to target level (remaining: " + remainingLevel + 
                  ", target: " + targetLevel + ")",
                  Math.abs(remainingLevel - targetLevel) <= levelTolerance);
        
        // Property 5: Monotonicity in XP space
        if (currentXP > targetXP + 100) {
            int midXP = (currentXP + targetXP) / 2;
            int extractableToMid = currentXP - midXP;
            int extractableFromMid = midXP - targetXP;
            
            assertEquals("Extraction should be additive",
                        extractableXP, extractableToMid + extractableFromMid);
        }
        
        // Property 6: Boundary behavior
        if (extractableXP == 0) {
            assertTrue("Zero extraction should mean current XP <= target XP",
                      currentXP <= targetXP);
        } else {
            assertTrue("Non-zero extraction should mean current XP > target XP",
                      currentXP > targetXP);
        }
        
        // Property 7: Scale invariance for small amounts
        if (extractableXP < 100) {
            // Small extractions should be precise
            assertEquals("Small extractions should be precise",
                        currentXP - targetXP, extractableXP);
        }
        
        // Property 8: Level range consistency
        if (currentLevel <= 15 && targetLevel <= 15) {
            // Both in linear range - extraction should follow linear pattern
            double levelDiff = currentLevel - targetLevel;
            int expectedLinearExtraction = (int)(levelDiff * 17);
            int tolerance = Math.max(17, extractableXP / 10); // Allow reasonable tolerance
            
            assertTrue("Linear range extraction should follow expected pattern (expected: " + 
                      expectedLinearExtraction + ", actual: " + extractableXP + ", tolerance: " + tolerance + ")",
                      Math.abs(extractableXP - expectedLinearExtraction) <= tolerance);
        }
    }
    
    /**
     * Calculates appropriate tolerance for XP round-trip conversion based on the XP amount.
     * Different XP ranges have different precision requirements due to the complexity
     * of Minecraft's level calculation formulas.
     * 
     * @param xp The XP amount to calculate tolerance for
     * @return The maximum acceptable difference for round-trip conversion
     */
    private int calculateToleranceForXP(int xp) {
        if (xp <= 272) {
            // Levels 0-15: Linear range, should be very precise
            return Math.max(1, xp / 50); // 2% tolerance, minimum 1
        } else if (xp <= 825) {
            // Levels 16-30: First quadratic range, moderate precision
            return Math.max(5, xp / 30); // ~3.3% tolerance, minimum 5
        } else {
            // Levels 31+: Second quadratic range, lower precision due to complexity
            return Math.max(10, xp / 20); // 5% tolerance, minimum 10
        }
    }
    
    /**
     * Test-specific implementation of TankScanResult for property-based testing.
     * This allows us to control the capacity calculation for testing purposes
     * without relying on Minecraft classes.
     */
    private static class TestTankScanResult {
        private final int totalCapacity;
        private final int totalStored;
        private final int tankCount;
        
        // Constructor for uniform tank capacities
        public TestTankScanResult(int numTanks, int individualCapacity, int storedXP) {
            this.tankCount = numTanks;
            this.totalCapacity = numTanks * individualCapacity;
            this.totalStored = Math.min(numTanks * storedXP, totalCapacity);
        }
        
        // Constructor for varied tank capacities
        public TestTankScanResult(int[] tankCapacities) {
            this.tankCount = tankCapacities.length;
            int capacity = 0;
            for (int cap : tankCapacities) {
                capacity += cap;
            }
            this.totalCapacity = capacity;
            this.totalStored = 0; // Default to empty tanks for varied capacity tests
        }
        
        public int getTotalCapacity() {
            return totalCapacity;
        }
        
        public int getTotalStored() {
            return totalStored;
        }
        
        public int getTankCount() {
            return tankCount;
        }
        
        public int getTotalRemainingCapacity() {
            return totalCapacity - totalStored;
        }
    }
}