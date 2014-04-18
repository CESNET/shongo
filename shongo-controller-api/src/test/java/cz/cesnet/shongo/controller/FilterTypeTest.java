package cz.cesnet.shongo.controller;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link FilterType}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class FilterTypeTest
{
    @Test
    public void testApplyFilter() throws Exception
    {
        // Test diacritics, lower case and numbers
        Assert.assertEquals("escrzyaieuuescrzyaieuu0123456789",
                FilterType.applyFilter("ěščřžýáíéÚŮĚŠČŘŽÝÁÍÉÚŮ0123456789", FilterType.CONVERT_TO_URL));

        // Test special symbols
        Assert.assertEquals("_--", FilterType.applyFilter("+*_- ", FilterType.CONVERT_TO_URL));

        // Test values
        Assert.assertEquals("test-test-01-02", FilterType.applyFilter("Test Test 01 02", FilterType.CONVERT_TO_URL));
        Assert.assertEquals("test-test_01_02", FilterType.applyFilter("Test Test_01_02", FilterType.CONVERT_TO_URL));
    }
}
