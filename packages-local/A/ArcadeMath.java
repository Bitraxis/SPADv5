public class ArcadeMath {
    public static int roll(int sides, int seed) {
        int safeSides = Math.max(2, sides);
        int n = Math.abs(seed * 1103515245 + 12345);
        return (n % safeSides) + 1;
    }

    public static int comboScore(int hits, int streak) {
        int safeHits = Math.max(0, hits);
        int safeStreak = Math.max(1, streak);
        return safeHits * safeStreak * 10;
    }
}
