package cz.cesnet.shongo.api.util;

import org.joda.time.DateTimeFieldType;
import org.joda.time.ReadablePartial;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConverterTest
{
    @Test
    public void testReadablePartial() throws Exception
    {
        String[] values = new String[]{"2012", "2012-12", "2012-12-01", "2012-12-01T12", "2012-12-01T12:34"};
        for (int index = 0; index < values.length; index++) {
            ReadablePartial readablePartial = Converter.convertStringToReadablePartial(values[index]);
            assertEquals(values[index], readablePartial.toString());
        }

        ReadablePartial readablePartial;
        readablePartial = Converter.convertStringToReadablePartial("2012");
        assertEquals(2012, readablePartial.get(DateTimeFieldType.year()));
        try {
            readablePartial.get(DateTimeFieldType.monthOfYear());
            fail("Exception should be thrown");
        }
        catch (IllegalArgumentException exception) {
        }
        readablePartial = Converter.convertStringToReadablePartial("2012-01-01T12");
        assertEquals(2012, readablePartial.get(DateTimeFieldType.year()));
        assertEquals(01, readablePartial.get(DateTimeFieldType.monthOfYear()));
        assertEquals(01, readablePartial.get(DateTimeFieldType.dayOfMonth()));
        assertEquals(12, readablePartial.get(DateTimeFieldType.hourOfDay()));
        try {
            readablePartial.get(DateTimeFieldType.minuteOfHour());
            fail("Exception should be thrown");
        }
        catch (IllegalArgumentException exception) {
        }
    }
}
