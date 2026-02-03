public class ExperienceCalculator {
    public static void main(String[] args) {
        // Calculate required tank levels for multiple experience levels
        int[] experienceLevels = {100, 500, 1000, 2000};
        
        for (int level : experienceLevels) {
            long totalXP = calculateTotalXP(level);
            int tankLevel = calculateRequiredTankLevel(totalXP);
            long tankCapacity = calculateTankCapacity(tankLevel);
            
            System.out.println("Experience level " + level + ":");
            System.out.println("  Total XP: " + totalXP);
            System.out.println("  Required tank level: " + tankLevel);
            System.out.println("  Tank capacity: " + tankCapacity);
            System.out.println("  Remaining capacity: " + (tankCapacity - totalXP));
            System.out.println();
        }
    }
    
    /**
     * Calculate total XP required to reach the given level
     */
    public static long calculateTotalXP(int level) {
        long totalXP = 0;
        for (int i = 0; i < level; i++) {
            if (i <= 16) {
                totalXP += i * 17;
            } else if (i <= 31) {
                totalXP += Math.round(2.5 * i * i - 40.5 * i + 360);
            } else {
                totalXP += Math.round(4.5 * i * i - 162.5 * i + 2220);
            }
        }
        return totalXP;
    }
    
    /**
     * Calculate tank capacity for the given tank level
     */
    public static long calculateTankCapacity(int tankLevel) {
        long capacity = 1000; // Level 1 capacity
        for (int i = 1; i < tankLevel; i++) {
            capacity *= 2;
        }
        return capacity;
    }
    
    /**
     * Calculate required tank level for the given XP amount
     */
    public static int calculateRequiredTankLevel(long requiredXP) {
        long capacity = 1000; // Level 1 capacity
        int tankLevel = 1;
        
        while (capacity < requiredXP) {
            capacity *= 2;
            tankLevel++;
        }
        
        return tankLevel;
    }
}