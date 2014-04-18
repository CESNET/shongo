package cz.cesnet.shongo.client.web.support.editors;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link java.beans.PropertyEditorSupport} for {@link org.joda.time.LocalDate}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class LocalDateTimeEditor extends PropertyEditorSupport
{
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");

    @Override
    public String getAsText()
    {
        if (getValue() == null) {
            return "";
        }
        LocalDateTime value = (LocalDateTime) getValue();
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
            setValue(dateTimeFormatter.parseLocalDateTime(text));
        }
    }
}