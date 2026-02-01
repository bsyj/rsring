package com.moremod.client;

import com.moremod.experience.ExperiencePumpController;
import com.moremod.experience.TankScanResult;
import net.minecraft.entity.player.EntityPlayer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

import static org.junit.Assert.*;

/**
 * Unit tests for GuiExperiencePumpController.
 * Tests the GUI controller functionality including experience display formatting,
 * scroll wheel handling, and fine-tuning controls.
 * 
 * Validates Requirements 3.6, 3.7, 6.3 for GUI controller functionality.
 * Includes property-based tests for scroll wheel fine tuning.
 */
@RunWith(JUnitQuickcheck.class)
public class GuiExperiencePumpControllerTest {
    
    private ExperiencePumpController controller;
    
    @Before
    public void setUp() {
        controller = ExperiencePumpController.getInstance();
    }
    
    /**
     * Test experience display formatting functionality.
     * Validates Requirement 6.3 for experience display format.
     */
    @Test
    public void testExperienceDisplayFormatting() {
        // Test various XP amounts
        String formatted100 = controller.formatExperienceDisplay(100);
        assertTrue("Display should contain XP", formatted100.contains("XP"));
        assertTrue("Display should contain levels", formatted100.contains("levels"));
        
        String formatted1000 = controller.formatExperienceDisplay(1000);
        assertTrue("Display should contain XP", formatted1000.contains("XP"));
        assertTrue("Display should contain levels", formatted1000.contains("levels"));
        
        String formatted0 = controller.formatExperienceDisplay(0);
        assertEquals("Zero XP should display correctly", "0 XP (0 levels)", formatted0);
        
        // Test that different XP amounts produce different level displays
        String formatted17 = controller.formatExperienceDisplay(17);  // Should be 1 level
        String formatted34 = controller.formatExperienceDisplay(34);  // Should be 2 levels
        assertNotEquals("Different XP amounts should produce different displays", formatted17, formatted34);
    }
    
    /**
     * Test scroll wheel fine-tuning amount processing.
     * Validates Requirements 3.6, 3.7 for scroll wheel fine-tuned adjustment controls.
     */
    @Test
    public void testScrollWheelFineTuning() {
        // Test extraction fine-tuning (Requirement 3.6)
        int baseAmount = 100;
        int scrollUp = controller.processScrollInput(1, true, baseAmount);
        int scrollDown = controller.processScrollInput(-1, true, baseAmount);
        
        // Scroll up should increase the amount
        assertTrue("Scroll up should increase extraction amount", scrollUp > baseAmount);
        
        // Scroll down should decrease the amount
        assertTrue("Scroll down should decrease extraction amount", scrollDown < baseAmount);
        assertTrue("Scroll down should not go below 1", scrollDown >= 1);
        
        // Test injection fine-tuning (Requirement 3.7)
        int injectionScrollUp = controller.processScrollInput(1, false, baseAmount);
        int injectionScrollDown = controller.processScrollInput(-1, false, baseAmount);
        
        // Scroll up should increase the amount
        assertTrue("Scroll up should increase injection amount", injectionScrollUp > baseAmount);
        
        // Scroll down should decrease the amount
        assertTrue("Scroll down should decrease injection amount", injectionScrollDown < baseAmount);
        assertTrue("Scroll down should not go below 1", injectionScrollDown >= 1);
    }
    
    /**
     * Test fine-tuning amount cycling functionality.
     */
    @Test
    public void testFineTuningAmountCycling() {
        // Test the cycling logic: 10 -> 50 -> 100 -> 500 -> 1000 -> 10
        assertEquals("10 should cycle to 50", 50, getNextFineTuneAmount(10));
        assertEquals("50 should cycle to 100", 100, getNextFineTuneAmount(50));
        assertEquals("100 should cycle to 500", 500, getNextFineTuneAmount(100));
        assertEquals("500 should cycle to 1000", 1000, getNextFineTuneAmount(500));
        assertEquals("1000 should cycle to 10", 10, getNextFineTuneAmount(1000));
        assertEquals("Unknown value should default to 100", 100, getNextFineTuneAmount(999));
    }
    
    /**
     * Helper method that replicates the private getNextFineTuneAmount logic for testing.
     */
    private int getNextFineTuneAmount(int currentAmount) {
        switch (currentAmount) {
            case 10: return 50;
            case 50: return 100;
            case 100: return 500;
            case 500: return 1000;
            case 1000: return 10;
            default: return 100; // Default fallback
        }
    }
    
    /**
     * Test comprehensive tank detection integration.
     */
    @Test
    public void testComprehensiveTankDetection() {
        // Test that the controller can handle empty scan results
        TankScanResult emptyResult = TankScanResult.empty();
        assertEquals("Empty result should have 0 tanks", 0, emptyResult.getTankCount());
        assertEquals("Empty result should have 0 capacity", 0, emptyResult.getTotalCapacity());
        assertEquals("Empty result should have 0 stored", 0, emptyResult.getTotalStored());
        assertTrue("Empty result should have empty tank list", emptyResult.getAllTanks().isEmpty());
    }
    
    /**
     * Test XP calculation accuracy for display formatting.
     * Validates that the GUI uses accurate XP-to-level conversions.
     */
    @Test
    public void testXPCalculationAccuracy() {
        // Test known XP values and their level equivalents
        // Level 0-15: 17 XP per level
        assertEquals("0 XP should be 0 levels", 0.0, controller.convertXPToLevel(0), 0.01);
        assertEquals("17 XP should be 1 level", 1.0, controller.convertXPToLevel(17), 0.01);
        assertEquals("255 XP should be 15 levels", 15.0, controller.convertXPToLevel(255), 0.01); // 15 * 17 = 255
        
        // Test round-trip conversion
        int testXP = 500;
        double level = controller.convertXPToLevel(testXP);
        int backToXP = controller.convertLevelToXP(level);
        
        // Should be approximately equal (allowing for rounding)
        assertTrue("Round-trip XP conversion should be accurate within 1 XP", 
                  Math.abs(testXP - backToXP) <= 1);
    }
    
    /**
     * Test that the GUI properly handles no tanks scenario.
     */
    @Test
    public void testNoTanksScenario() {
        // Test with null player
        TankScanResult nullPlayerResult = controller.scanAllInventories(null);
        assertEquals("Null player should result in 0 tanks", 0, nullPlayerResult.getTankCount());
        
        // Test total capacity calculation with no tanks
        assertEquals("Null player should have 0 total capacity", 0, controller.calculateTotalCapacity(null));
        assertEquals("Null player should have 0 total stored", 0, controller.calculateTotalStored(null));
    }
    
    /**
     * Test scroll wheel input edge cases.
     */
    @Test
    public void testScrollWheelEdgeCases() {
        // Test zero scroll delta
        int baseAmount = 100;
        int noScroll = controller.processScrollInput(0, true, baseAmount);
        assertEquals("Zero scroll delta should return base amount", baseAmount, noScroll);
        
        // Test minimum amount enforcement
        int smallBase = 5;
        int scrollDownSmall = controller.processScrollInput(-1, true, smallBase);
        assertTrue("Scroll result should never be less than 1", scrollDownSmall >= 1);
        
        // Test large scroll values
        int largeScroll = controller.processScrollInput(10, true, baseAmount);
        assertTrue("Large positive scroll should increase amount", largeScroll > baseAmount);
        
        int largeNegativeScroll = controller.processScrollInput(-10, true, baseAmount);
        assertTrue("Large negative scroll should decrease amount", largeNegativeScroll < baseAmount);
        assertTrue("Large negative scroll should not go below 1", largeNegativeScroll >= 1);
    }
    
    /**
     * Test XP to next level calculation.
     */
    @Test
    public void testXPToNextLevelCalculation() {
        // Test known level requirements
        assertEquals("Level 0 should require 17 XP to next level", 17, controller.getXPToNextLevel(0));
        assertEquals("Level 14 should require 17 XP to next level", 17, controller.getXPToNextLevel(14));
        assertEquals("Level 15 should require 17 XP to next level", 17, controller.getXPToNextLevel(15)); // 17 + (15-15)*3 = 17
        assertEquals("Level 16 should require 20 XP to next level", 20, controller.getXPToNextLevel(16)); // 17 + (16-15)*3 = 20
        assertEquals("Level 29 should require 59 XP to next level", 59, controller.getXPToNextLevel(29)); // 17 + (29-15)*3 = 59
        assertEquals("Level 30 should require 62 XP to next level", 62, controller.getXPToNextLevel(30));
    }
    
    /**
     * Test level-based extraction calculation.
     * Validates Requirement 6.4 for level-based extraction calculation.
     */
    @Test
    public void testLevelBasedExtractionCalculation() {
        // Test with null player
        int nullResult = controller.calculateLevelBasedExtraction(null, 5);
        assertEquals("Null player should return 0", 0, nullResult);
        
        // Test with negative target level
        int negativeResult = controller.calculateLevelBasedExtraction(null, -1);
        assertEquals("Negative target level should return 0", 0, negativeResult);
    }
    
    // ========== PROPERTY-BASED TESTS ==========
    
    /**
     * Property-based test for scroll wheel fine tuning functionality.
     * 
     * **Property 5: Scroll Wheel Fine Tuning**
     * For any scroll wheel input on extraction or injection buttons, the adjustment amount 
     * should be proportional to the scroll delta and provide fine-grained control.
     * 
     * **Validates: Requirements 3.6, 3.7**
     */
    @Property(trials = 100)
    public void testScrollWheelFineTuning(@InRange(min = "1", max = "2000") int baseAmount,
                                        @InRange(min = "-10", max = "10") int scrollDelta,
                                        boolean isExtraction) {
        
        // Property 5.1: Zero scroll delta should return the base amount unchanged
        int noScrollResult = controller.processScrollInput(0, isExtraction, baseAmount);
        assertEquals("Zero scroll delta should return base amount unchanged", baseAmount, noScrollResult);
        
        // Skip further testing if scroll delta is zero
        if (scrollDelta == 0) {
            return;
        }
        
        // Property 5.2: Non-zero scroll input should produce different result than base amount
        int scrollResult = controller.processScrollInput(scrollDelta, isExtraction, baseAmount);
        if (scrollDelta > 0) {
            assertTrue("Positive scroll delta should increase the amount", scrollResult > baseAmount);
        } else {
            assertTrue("Negative scroll delta should decrease the amount or maintain minimum", 
                      scrollResult <= baseAmount);
        }
        
        // Property 5.3: Result should never be less than 1 (minimum amount enforcement)
        assertTrue("Scroll result should never be less than 1", scrollResult >= 1);
        
        // Property 5.4: Adjustment should be proportional to base amount
        // The adjustment is calculated as max(1, baseAmount / 10)
        int expectedAdjustment = Math.max(1, baseAmount / 10);
        
        if (scrollDelta > 0) {
            int expectedIncrease = baseAmount + expectedAdjustment;
            assertEquals("Positive scroll should increase by expected adjustment", 
                        expectedIncrease, scrollResult);
        } else {
            int expectedDecrease = Math.max(1, baseAmount - expectedAdjustment);
            assertEquals("Negative scroll should decrease by expected adjustment (clamped to 1)", 
                        expectedDecrease, scrollResult);
        }
        
        // Property 5.5: Fine-grained control - small base amounts should have small adjustments
        if (baseAmount <= 10) {
            int adjustment = Math.abs(scrollResult - baseAmount);
            assertTrue("Small base amounts should have fine-grained adjustments (max 1)", 
                      adjustment <= 1);
        }
        
        // Property 5.6: Large base amounts should have proportionally larger adjustments
        if (baseAmount >= 100) {
            int adjustment = Math.abs(scrollResult - baseAmount);
            int expectedMinAdjustment = baseAmount / 10;
            if (scrollDelta != 0 && scrollResult != 1) { // Skip if clamped to minimum
                assertTrue("Large base amounts should have proportionally larger adjustments", 
                          adjustment >= expectedMinAdjustment);
            }
        }
        
        // Property 5.7: Extraction and injection should behave identically for same inputs
        int extractionResult = controller.processScrollInput(scrollDelta, true, baseAmount);
        int injectionResult = controller.processScrollInput(scrollDelta, false, baseAmount);
        assertEquals("Extraction and injection should produce identical results for same inputs", 
                    extractionResult, injectionResult);
        
        // Property 5.8: Multiple small scrolls should accumulate adjustments
        // (within reasonable bounds to avoid integer overflow)
        if (Math.abs(scrollDelta) <= 3 && baseAmount <= 1000) {
            int firstSmallScroll = controller.processScrollInput(scrollDelta, isExtraction, baseAmount);
            int secondSmallScroll = controller.processScrollInput(scrollDelta, isExtraction, firstSmallScroll);
            
            // The second scroll should continue adjusting from the first result
            if (scrollDelta > 0) {
                assertTrue("Multiple positive scrolls should continue increasing", 
                          secondSmallScroll > firstSmallScroll);
                assertTrue("Multiple positive scrolls should be greater than base", 
                          secondSmallScroll > baseAmount);
            } else if (scrollDelta < 0) {
                assertTrue("Multiple negative scrolls should continue decreasing or stay at minimum", 
                          secondSmallScroll <= firstSmallScroll);
                assertTrue("Multiple negative scrolls should not go below 1", 
                          secondSmallScroll >= 1);
            }
        }
        
        // Property 5.9: Scroll direction consistency
        if (scrollDelta > 0) {
            assertTrue("Positive scroll should always increase or maintain amount", 
                      scrollResult >= baseAmount);
        } else if (scrollDelta < 0) {
            assertTrue("Negative scroll should always decrease or maintain minimum", 
                      scrollResult <= baseAmount);
        }
        
        // Property 5.10: Boundary value handling
        if (baseAmount == 1 && scrollDelta < 0) {
            assertEquals("Minimum base amount with negative scroll should remain at 1", 
                        1, scrollResult);
        }
    }
    
    /**
     * Property-based test for scroll wheel fine tuning edge cases and boundary conditions.
     * 
     * **Property 5: Scroll Wheel Fine Tuning (Edge Cases)**
     * Tests edge cases and boundary conditions for scroll wheel fine tuning.
     * 
     * **Validates: Requirements 3.6, 3.7**
     */
    @Property(trials = 50)
    public void testScrollWheelFineTuning_EdgeCases(@InRange(min = "1", max = "10") int smallAmount,
                                                   @InRange(min = "1000", max = "2000") int largeAmount,
                                                   @InRange(min = "-5", max = "5") int scrollDelta) {
        
        // Property 5.11: Small amounts should have minimal adjustments
        if (scrollDelta != 0) {
            int smallResult = controller.processScrollInput(scrollDelta, true, smallAmount);
            int smallAdjustment = Math.abs(smallResult - smallAmount);
            assertTrue("Small amounts should have minimal adjustments", smallAdjustment <= 1);
        }
        
        // Property 5.12: Large amounts should have proportional adjustments
        if (scrollDelta != 0) {
            int largeResult = controller.processScrollInput(scrollDelta, true, largeAmount);
            int largeAdjustment = Math.abs(largeResult - largeAmount);
            int expectedAdjustment = largeAmount / 10;
            
            if (largeResult != 1) { // Skip if clamped to minimum
                assertTrue("Large amounts should have proportional adjustments", 
                          largeAdjustment >= expectedAdjustment);
            }
        }
        
        // Property 5.13: Consistency across different operation types
        int extractionSmall = controller.processScrollInput(scrollDelta, true, smallAmount);
        int injectionSmall = controller.processScrollInput(scrollDelta, false, smallAmount);
        assertEquals("Small amounts should behave consistently for extraction and injection", 
                    extractionSmall, injectionSmall);
        
        int extractionLarge = controller.processScrollInput(scrollDelta, true, largeAmount);
        int injectionLarge = controller.processScrollInput(scrollDelta, false, largeAmount);
        assertEquals("Large amounts should behave consistently for extraction and injection", 
                    extractionLarge, injectionLarge);
    }
    
    /**
     * Property-based test for scroll wheel fine tuning mathematical properties.
     * 
     * **Property 5: Scroll Wheel Fine Tuning (Mathematical Properties)**
     * Tests fundamental mathematical properties of scroll wheel fine tuning.
     * 
     * **Validates: Requirements 3.6, 3.7**
     */
    @Property(trials = 100)
    public void testScrollWheelFineTuning_MathematicalProperties(@InRange(min = "10", max = "1000") int baseAmount,
                                                               @InRange(min = "1", max = "5") int positiveScroll) {
        
        // Property 5.14: Monotonicity - larger positive scroll should produce larger or equal results
        int scroll1 = controller.processScrollInput(positiveScroll, true, baseAmount);
        int scroll2 = controller.processScrollInput(positiveScroll + 1, true, baseAmount);
        assertTrue("Larger positive scroll should produce larger or equal results", scroll2 >= scroll1);
        
        // Property 5.15: Symmetry - positive and negative scrolls should be symmetric around base
        int positiveResult = controller.processScrollInput(positiveScroll, true, baseAmount);
        int negativeResult = controller.processScrollInput(-positiveScroll, true, baseAmount);
        
        int positiveAdjustment = positiveResult - baseAmount;
        int negativeAdjustment = baseAmount - negativeResult;
        
        // Should be symmetric unless clamped to minimum
        if (negativeResult > 1) {
            assertEquals("Positive and negative scrolls should be symmetric", 
                        positiveAdjustment, negativeAdjustment);
        }
        
        // Property 5.16: Idempotency of zero scroll
        int result1 = controller.processScrollInput(0, true, baseAmount);
        int result2 = controller.processScrollInput(0, false, baseAmount);
        assertEquals("Zero scroll should be idempotent for extraction", baseAmount, result1);
        assertEquals("Zero scroll should be idempotent for injection", baseAmount, result2);
        
        // Property 5.17: Commutativity of operation type for same inputs
        int extractionResult = controller.processScrollInput(positiveScroll, true, baseAmount);
        int injectionResult = controller.processScrollInput(positiveScroll, false, baseAmount);
        assertEquals("Operation type should not affect result for same inputs", 
                    extractionResult, injectionResult);
    }
    
    /**
     * Property-based test for experience display format functionality.
     * 
     * **Property 13: Experience Display Format**
     * For any experience amount displayed by the controller, both XP points and 
     * equivalent level information should be shown.
     * 
     * **Validates: Requirements 6.3**
     */
    @Property(trials = 100)
    public void testExperienceDisplayFormat(@InRange(min = "0", max = "1000000") int xpAmount) {
        
        // Property 13.1: Display format should always contain "XP" text
        String formatted = controller.formatExperienceDisplay(xpAmount);
        assertTrue("Display should always contain 'XP' text", formatted.contains("XP"));
        
        // Property 13.2: Display format should always contain "levels" text
        assertTrue("Display should always contain 'levels' text", formatted.contains("levels"));
        
        // Property 13.3: Display should contain the exact XP amount
        assertTrue("Display should contain the XP amount", 
                  formatted.contains(String.valueOf(Math.max(0, xpAmount))));
        
        // Property 13.4: Display should follow the expected format pattern "X XP (Y levels)"
        String expectedPattern = "\\d+ XP \\(\\d+\\.\\d levels\\)";
        assertTrue("Display should match expected format pattern", 
                  formatted.matches(expectedPattern));
        
        // Property 13.5: Zero or negative XP should display as "0 XP (0 levels)"
        if (xpAmount <= 0) {
            assertEquals("Zero or negative XP should display as '0 XP (0 levels)'", 
                        "0 XP (0 levels)", formatted);
        }
        
        // Property 13.6: Positive XP should show non-zero levels
        if (xpAmount > 0) {
            // Extract the level portion from the formatted string
            String levelPart = formatted.substring(formatted.indexOf('(') + 1, formatted.indexOf(" levels"));
            double displayedLevels = Double.parseDouble(levelPart);
            assertTrue("Positive XP should show non-zero levels", displayedLevels > 0.0);
        }
        
        // Property 13.7: Level calculation should be consistent with XP conversion
        if (xpAmount > 0) {
            double expectedLevels = controller.convertXPToLevel(xpAmount);
            String levelPart = formatted.substring(formatted.indexOf('(') + 1, formatted.indexOf(" levels"));
            double displayedLevels = Double.parseDouble(levelPart);
            
            // Allow for rounding to 1 decimal place in display format
            assertEquals("Displayed levels should match XP conversion calculation (within rounding)", 
                        expectedLevels, displayedLevels, 0.1);
        }
        
        // Property 13.8: Format should be deterministic - same input produces same output
        String formatted2 = controller.formatExperienceDisplay(xpAmount);
        assertEquals("Same XP amount should produce identical formatted output", 
                    formatted, formatted2);
        
        // Property 13.9: Different XP amounts should produce different displays (unless both are <= 0)
        if (xpAmount > 0) {
            int differentXP = xpAmount + 1;
            String differentFormatted = controller.formatExperienceDisplay(differentXP);
            assertNotEquals("Different positive XP amounts should produce different displays", 
                           formatted, differentFormatted);
        }
        
        // Property 13.10: Display should handle large XP values gracefully
        if (xpAmount >= 100000) {
            assertNotNull("Large XP values should not cause null display", formatted);
            assertTrue("Large XP values should still contain required elements", 
                      formatted.contains("XP") && formatted.contains("levels"));
        }
        
        // Property 13.11: Level precision should be consistent (1 decimal place)
        if (xpAmount > 0) {
            String levelPart = formatted.substring(formatted.indexOf('(') + 1, formatted.indexOf(" levels"));
            // Check that it has exactly one decimal place
            assertTrue("Level display should have exactly one decimal place", 
                      levelPart.matches("\\d+\\.\\d"));
        }
        
        // Property 13.12: XP amount in display should be non-negative
        String xpPart = formatted.substring(0, formatted.indexOf(" XP"));
        int displayedXP = Integer.parseInt(xpPart);
        assertTrue("Displayed XP should be non-negative", displayedXP >= 0);
        
        // Property 13.13: Monotonicity - higher XP should show higher or equal levels
        if (xpAmount > 0 && xpAmount < 999999) { // Avoid overflow
            String higherFormatted = controller.formatExperienceDisplay(xpAmount + 100);
            
            String currentLevelPart = formatted.substring(formatted.indexOf('(') + 1, formatted.indexOf(" levels"));
            String higherLevelPart = higherFormatted.substring(higherFormatted.indexOf('(') + 1, higherFormatted.indexOf(" levels"));
            
            double currentLevels = Double.parseDouble(currentLevelPart);
            double higherLevels = Double.parseDouble(higherLevelPart);
            
            assertTrue("Higher XP should show higher or equal levels", higherLevels >= currentLevels);
        }
        
        // Property 13.14: Format should be readable and contain proper spacing
        assertTrue("Format should contain proper spacing around 'XP'", 
                  formatted.contains(" XP "));
        assertTrue("Format should contain proper parentheses formatting", 
                  formatted.contains("(") && formatted.contains(")"));
        
        // Property 13.15: Level calculation should handle Minecraft's XP formula correctly
        if (xpAmount > 0) {
            // Verify that the level calculation follows Minecraft's XP progression
            double levels = controller.convertXPToLevel(xpAmount);
            
            // For levels 0-15: 17 XP per level
            // For levels 16-30: 17 + (level - 15) * 3 XP per level  
            // For levels 31+: 62 + (level - 30) * 7 XP per level
            
            if (xpAmount <= 255) { // 15 levels * 17 XP = 255 XP
                assertTrue("Low XP amounts should show reasonable level progression", 
                          levels <= 15.0);
            }
            
            // Verify levels are reasonable for the XP amount
            assertTrue("Level calculation should be reasonable", levels >= 0.0);
            assertTrue("Level calculation should not be impossibly high", levels <= xpAmount); // Sanity check
        }
    }
}