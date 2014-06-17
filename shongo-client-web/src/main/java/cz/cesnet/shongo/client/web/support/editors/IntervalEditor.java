package cz.cesnet.shongo.client.web.support.editors;

import cz.cesnet.shongo.api.Converter;
import org.joda.time.Interval;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link java.beans.PropertyEditorSupport} for {@link org.joda.time.Interval}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class IntervalEditor extends PropertyEditorSupport
{
    public IntervalEditor()
    {
    }

    @Override
    public String getAsText()
    {
        if (getValue() == null) {
            return "";
        }
        Interval value = (Interval) getValue();
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        if (!StringUtils.hasText(text)) {
            setValue(null);
        }
        else {
            setValue(Converter.convertStringToInterval(text));
        }
    }
}