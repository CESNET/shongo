package cz.cesnet.shongo.util;

/**
 * Helper class for mathematical operations.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MathHelper
{
    /**
     * @param percent ratio representing % (in range 0.0 - 1.0)
     * @param maxDb
     * @return dB from given {@code percent}
     */
    public static double getDbFromPercent(double percent, double maxDb)
    {
        double percentSign = (percent < 0.0 ? -1.0 : 1.0);
        double percentAbsolute = Math.abs(percent);
        if (percentAbsolute > 1.0 || percentAbsolute < 0.0) {
            throw new IllegalArgumentException("Percents must be in range 0-1.");
        }
        if (percentAbsolute == 0.0) {
            return 0;
        }
        else if (percentAbsolute == 1.0) {
            return percentSign * maxDb;
        }
        else {
            return percentSign * Math.abs(Math.log10(1.0 - percentAbsolute) * maxDb);
        }
    }

    /**
     * @param db amount of dB
     * @param maxDb
     * @return % from given {@code db}
     */
    public static double getPercentFromDb(double db, double maxDb)
    {
        double dbSign = (db < 0.0 ? -1.0 : 1.0);
        double dbAbsolute = Math.abs(db);
        if (dbAbsolute > maxDb || dbAbsolute < 0.0) {
            throw new IllegalArgumentException("Decibels must be in range 0-1.");
        }

        if (dbAbsolute == maxDb) {
            return dbSign * 1.0;
        }
        else {
            return dbSign * (1.0 - Math.pow(10, -dbAbsolute / 20));
        }
    }
}
