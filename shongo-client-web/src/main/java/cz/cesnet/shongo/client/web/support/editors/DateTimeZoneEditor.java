package cz.cesnet.shongo.client.web.support.editors;

import org.joda.time.DateTimeZone;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link java.beans.PropertyEditorSupport} for {@link org.joda.time.DateTimeZone}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeZoneEditor extends PropertyEditorSupport
{
    @Override
    public String getAsText()
    {
        if (getValue() == null) {
            return "";
        }
        DateTimeZone value = (DateTimeZone) getValue();
        if (value == null) {
            return "";
        }
        return value.getID();
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        if (!StringUtils.hasText(text)) {
            setValue(null);
        }
        else {
            setValue(DateTimeZone.forID(text));
        }
    }
}