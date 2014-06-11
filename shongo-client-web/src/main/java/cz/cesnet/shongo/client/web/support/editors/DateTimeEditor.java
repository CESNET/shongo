package cz.cesnet.shongo.client.web.support.editors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link PropertyEditorSupport} for {@link DateTime}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeEditor extends PropertyEditorSupport
{
    private static final DateTimeFormatter dateTimeParser = new DateTimeFormatterBuilder()
            .append(DateTimeFormat.forPattern("yyyy-MM-dd"))
            .appendOptional(DateTimeFormat.forPattern(" HH:mm").getParser())
            .toFormatter();

    private static final DateTimeFormatter dateTimePrinter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");

    private DateTimeZone timeZone;

    public DateTimeEditor(DateTimeZone timeZone)
    {
        this.timeZone = timeZone;
    }

    @Override
    public String getAsText()
    {
        if (getValue() == null) {
            return "";
        }
        DateTime value = (DateTime) getValue();
        if (value == null) {
            return "";
        }
        return dateTimePrinter.withZone(timeZone).print(value);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        if (!StringUtils.hasText(text)) {
            setValue(null);
        }
        else {
            try {
                setValue(dateTimeParser.withZone(timeZone).parseDateTime(text));
            }
            catch (IllegalArgumentException exception) {
                setValue(DateTime.parse(text));
            }
        }
    }
}