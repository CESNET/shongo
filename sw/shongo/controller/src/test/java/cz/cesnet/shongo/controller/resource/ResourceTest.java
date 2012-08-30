package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.controller.common.RelativeDateTimeSpecification;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceTest
{
    @Test
    public void testIsAvailableAt() throws Exception
    {
        Resource resource = new Resource();
        resource.setMaximumFuture(new RelativeDateTimeSpecification("P4M"));

        DateTime referenceDateTime = DateTime.now();
        assertTrue(resource.isAvailableInFuture(DateTime.parse("0"), referenceDateTime));
        assertTrue(resource.isAvailableInFuture(referenceDateTime.plus(Period.parse("P2M")), referenceDateTime));
        assertTrue(resource.isAvailableInFuture(referenceDateTime.plus(Period.parse("P4M")), referenceDateTime));
        assertFalse(resource.isAvailableInFuture(referenceDateTime.plus(Period.parse("P5M")), referenceDateTime));
    }
}
