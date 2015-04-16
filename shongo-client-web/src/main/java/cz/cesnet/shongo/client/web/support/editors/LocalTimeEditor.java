
package cz.cesnet.shongo.client.web.support.editors;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.*;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link java.beans.PropertyEditorSupport} for {@link org.joda.time.LocalTime}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class LocalTimeEditor extends PropertyEditorSupport
{
    private static DateTimeFormatter dateTimeFormatterCs = DateTimeFormat.forPattern("HH:mm");
    private static DateTimeFormatter dateTimeFormatterEn = DateTimeFormat.forPattern("hh:mm aa");
    private static DateTimeParser[] parsers = {
        dateTimeFormatterCs.getParser(),
        dateTimeFormatterEn.getParser()
    };

    private static DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder().append(dateTimeFormatterEn.getPrinter(), parsers).toFormatter();

    @Override
    public String getAsText()
    {
        if (getValue() == null) {
            return "";
        }
        LocalTime value = (LocalTime) getValue();
        if (value == null) {
            return "";
        }
        return dateTimeFormatter.print(value);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        if (!StringUtils.hasText(text)) {
            setValue(null);
        }
        else {
            setValue(dateTimeFormatter.parseLocalTime(text));
        }
    }
}