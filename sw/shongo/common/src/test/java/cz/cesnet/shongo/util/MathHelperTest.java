package cz.cesnet.shongo.util;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Tests for {@link MathHelper}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MathHelperTest
{
    @Test
    public void test() throws Exception
    {
        final double MAX = 20.0;
        Assert.assertEquals(MAX, MathHelper.getDbFromPercent(1.0, MAX));
        Assert.assertEquals(0.0, MathHelper.getDbFromPercent(0.0, MAX));
        Assert.assertEquals(-MAX, MathHelper.getDbFromPercent(-1.0, MAX));
        for (int level = -5; level <= 5; level++) {
            double db = MathHelper.getDbFromPercent((double) level / 5.0, MAX);
            double computedLevel = MathHelper.getPercentFromDb(db, MAX);
            System.out.println(level + " -> " + db);
            Assert.assertEquals(level, Math.round(computedLevel * 5.0));
        }
    }
}
