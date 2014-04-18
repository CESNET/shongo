package cz.cesnet.shongo.controller.booking.datetime;

import cz.cesnet.shongo.controller.booking.datetime.DateTimeSpecification;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link cz.cesnet.shongo.controller.booking.datetime.DateTimeSpecification}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeSpecificationTest
{
    @Test
    public void testAbsolute() throws Exception
    {
        DateTimeSpecification dateTimeSpecification = DateTimeSpecification.fromString("2012-01-02T13:04:05");
        Assert.assertEquals(DateTimeSpecification.Type.ABSOLUTE, dateTimeSpecification.getType());
        Assert.assertNotNull(dateTimeSpecification.getAbsoluteDateTime());
        Assert.assertNull(dateTimeSpecification.getRelativeDateTime());

        Assert.assertEquals(DateTime.parse("1234-04-05T14:30"),
                DateTimeSpecification.fromString("1234-04-05T14:30").getEarliest(DateTime.parse("1234-04-05T14:29")));

        Assert.assertNull("A datetime which will not occur since given datetime",
                DateTimeSpecification.fromString("9876-04-05T14:30").getEarliest(DateTime.parse("9876-04-05T14:31")));

        Assert.assertNull("The comparison should work with strict inequality",
                DateTimeSpecification.fromString("1234-04-05T14:30").getEarliest(DateTime.parse("1234-04-05T14:30")));
    }

    @Test
    public void testRelative() throws Exception
    {
        DateTimeSpecification dateTimeSpecification = DateTimeSpecification.fromString("P1Y2M3DT4H5M6S");
        Assert.assertEquals(DateTimeSpecification.Type.RELATIVE, dateTimeSpecification.getType());
        Assert.assertNull(dateTimeSpecification.getAbsoluteDateTime());
        Assert.assertNotNull(dateTimeSpecification.getRelativeDateTime());

        DateTime referenceDateTime = DateTime.parse("2012-01-01T12:00:00");
        Assert.assertEquals(DateTime.parse("2013-03-04T16:05:06"),
                dateTimeSpecification.getEarliest(referenceDateTime));
    }
}
