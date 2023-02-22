package cz.cesnet.shongo.util;


import org.junit.Assert;
import org.junit.Test;
import java.math.BigDecimal;

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
        Assert.assertEquals(MAX, MathHelper.getDbFromPercent(1.0, MAX), 0.01);
        Assert.assertEquals(0.0, MathHelper.getDbFromPercent(0.0, MAX), 0);
        Assert.assertEquals(-MAX, MathHelper.getDbFromPercent(-1.0, MAX), 0.01);
        for (int level = -5; level <= 5; level++) {
            double db = MathHelper.getDbFromPercent((double) level / 5.0, MAX);
            double computedLevel = MathHelper.getPercentFromDb(db, MAX);
            System.out.println(level + " -> " + db);
            Assert.assertEquals(level, Math.round(computedLevel * 5.0));
        }
    }

    @Test
    public void testToBytes()
    {
        org.junit.Assert.assertEquals(1024*1024,MathHelper.toBytes("1MB"));
        org.junit.Assert.assertEquals(1024 * 1024, MathHelper.toBytes("1 MB"));
        org.junit.Assert.assertEquals(BigDecimal.valueOf(1.2 * 1024 * 1024).longValue(),MathHelper.toBytes("1.2 MB"));
        org.junit.Assert.assertEquals(BigDecimal.valueOf(1.2 * 1024 * 1024).longValue(),MathHelper.toBytes("1,2 MB"));
        org.junit.Assert.assertEquals(BigDecimal.valueOf(1.2 * 1024 * 1024 * 1024).longValue(),MathHelper.toBytes("1.2 GB"));
        org.junit.Assert.assertEquals(BigDecimal.valueOf(1.2 *  1024).longValue(),MathHelper.toBytes("1.2 KB"));
    }
}
