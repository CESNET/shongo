package cz.cesnet.shongo.client.web.support.editors;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import org.joda.time.DateTimeZone;

/**
 * Deserializer for {@link DateTimeZone}.
 *
 * @@author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeZoneDeserializer extends JsonDeserializer<DateTimeZone>
{
    @Override
    public DateTimeZone deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException
    {
        final String text = jp.getText();
        return text != null ? DateTimeZone.forID(text) : null;
    }

}
