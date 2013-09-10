package cz.cesnet.shongo.client.web.support.editors;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link PropertyEditorSupport} for {@link DateTime}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodEditor extends PropertyEditorSupport
{
    @Override
    public String getAsText()
    {
        if (getValue() == null) {
            return "";
        }
        Period value = (Period) getValue();
        return (value != null ? value.toString() : "");
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        if (!StringUtils.hasText(text)) {
            setValue(null);
        }
        else {
            setValue(Period.parse(text));
        }
    }
}