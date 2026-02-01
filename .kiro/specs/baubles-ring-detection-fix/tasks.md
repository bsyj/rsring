# Implementation Plan: Baubles Ring Detection Fix

## Overview

This implementation plan addresses the critical bug where ring toggle functionality fails when rings are equipped in Baubles slots. The approach involves creating a centralized `RingDetectionService` class to replace the duplicated and inconsistent ring detection logic currently spread across `PacketToggleRsRing.Handler` and `CommonEventHandler`.

## Tasks

- [ ] 1. Create centralized ring detection service
  - [x] 1.1 Create RingDetectionService class with unified ring detection logic
    - Implement main entry points: `findAnyRing()` and `findRing(player, ringClass)`
    - Implement search priority: main hand → off hand → Baubles → inventory
    - Add comprehensive logging for debugging
    - _Requirements: 1.1, 1.2, 1.3, 4.1_

  - [x] 1.2 Write property test for ring detection priority order
    - **Property 1: Ring Detection Priority Order**
    - **Validates: Requirements 1.1, 1.3**

  - [x] 1.3 Write property test for ring detection success across all locations
    - **Property 2: Ring Detection Success Across All Locations**
    - **Validates: Requirements 1.2, 2.3**

- [ ] 2. Implement robust Baubles integration
  - [x] 2.1 Create BaublesIntegration helper class with enhanced error handling
    - Implement reflection-based Baubles API access with specific exception handling
    - Add proper logging for ClassNotFoundException, NoSuchMethodException, etc.
    - Ensure graceful fallback when Baubles mod is not present
    - _Requirements: 2.1, 2.2, 2.4_

  - [x] 2.2 Write property test for Baubles integration robustness
    - **Property 3: Baubles Integration Robustness**
    - **Validates: Requirements 2.1, 2.2, 2.4**

  - [x] 2.3 Write unit tests for Baubles error scenarios
    - Test behavior when Baubles mod is missing
    - Test behavior when Baubles API calls fail
    - Test proper exception logging
    - _Requirements: 2.2, 3.1_

- [ ] 3. Checkpoint - Ensure core service tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Refactor PacketToggleRsRing.Handler to use centralized service
  - [ ] 4.1 Replace local findRing methods with RingDetectionService calls
    - Remove duplicate `findRing()` and `findAnyRing()` methods
    - Update toggle logic to use `RingDetectionService.findAnyRing()`
    - Maintain existing toggle functionality and user messages
    - _Requirements: 1.2, 4.2, 4.3_

  - [ ] 4.2 Write property test for backward compatibility
    - **Property 6: Backward Compatibility Preservation**
    - **Validates: Requirements 4.4**

- [ ] 5. Refactor CommonEventHandler to use centralized service
  - [ ] 5.1 Replace local findRing methods with RingDetectionService calls
    - Remove duplicate `findRing()` methods from CommonEventHandler
    - Update `onPlayerTick()` to use RingDetectionService
    - Update `onPlayerInteract()` to use RingDetectionService for held ring detection
    - _Requirements: 4.2, 4.3_

  - [ ] 5.2 Write integration tests for event handler functionality
    - Test ring detection during player tick events
    - Test ring detection during player interaction events
    - Verify functionality works with rings in all locations
    - _Requirements: 1.2, 4.4_

- [ ] 6. Implement enhanced logging and error handling
  - [ ] 6.1 Add comprehensive logging throughout ring detection process
    - Add debug logging for successful ring detection with location details
    - Add error logging for failed searches with specific failure reasons
    - Implement appropriate log levels (ERROR for failures, DEBUG for success)
    - _Requirements: 3.1, 3.2, 3.3, 3.4_

  - [ ] 6.2 Write property test for comprehensive error logging
    - **Property 4: Comprehensive Error Logging**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4**

- [ ] 7. Implement ring state synchronization improvements
  - [ ] 7.1 Enhance Baubles inventory synchronization
    - Improve `markBaublesDirtyIfNeeded()` method with better error handling
    - Ensure ring capability changes are properly synced to ItemStack
    - Add error logging for synchronization failures without interrupting operation
    - _Requirements: 5.1, 5.2, 5.3_

  - [ ] 7.2 Write property test for ring state synchronization
    - **Property 5: Ring State Synchronization**
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4**

  - [ ] 7.3 Write unit tests for synchronization edge cases
    - Test synchronization when Baubles API fails
    - Test capability sync across inventory movements
    - Test error recovery during sync failures
    - _Requirements: 5.3, 5.4_

- [ ] 8. Integration testing and validation
  - [ ] 8.1 Create comprehensive integration tests
    - Test the complete K-key toggle flow with rings in different locations
    - Test ring detection during player tick events
    - Test ring binding functionality with centralized detection
    - _Requirements: 1.1, 1.2, 1.3, 4.4_

  - [ ] 8.2 Write end-to-end property tests
    - Test complete ring toggle workflow across all inventory locations
    - Verify error handling and recovery in realistic scenarios
    - Test performance with multiple rings and complex inventory states
    - _Requirements: 1.1, 1.2, 1.3, 2.3_

- [ ] 9. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties with minimum 100 iterations
- Unit tests validate specific examples and edge cases
- The implementation maintains backward compatibility while fixing the core bug