package org.joda.time.format;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodCzechFormatterBuilder extends PeriodFormatterBuilder
{
    private final Method appendSuffixMethod;

    public PeriodCzechFormatterBuilder()
    {
        try {
            appendSuffixMethod  = PeriodFormatterBuilder.class.getDeclaredMethod("appendSuffix",
                    PeriodFieldAffix.class);
            appendSuffixMethod.setAccessible(true);
        }
        catch (NoSuchMethodException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public PeriodCzechFormatterBuilder appendSeparator(String text, String finalText, String[] variants)
    {
        return (PeriodCzechFormatterBuilder) super.appendSeparator(text, finalText, variants);
    }

    @Override
    public PeriodCzechFormatterBuilder appendYears()
    {
        return (PeriodCzechFormatterBuilder) super.appendYears();
    }

    @Override
    public PeriodCzechFormatterBuilder appendMonths()
    {
        return (PeriodCzechFormatterBuilder) super.appendMonths();
    }

    @Override
    public PeriodCzechFormatterBuilder appendWeeks()
    {
        return (PeriodCzechFormatterBuilder) super.appendWeeks();
    }

    @Override
    public PeriodCzechFormatterBuilder appendDays()
    {
        return (PeriodCzechFormatterBuilder) super.appendDays();
    }

    @Override
    public PeriodCzechFormatterBuilder appendHours()
    {
        return (PeriodCzechFormatterBuilder) super.appendHours();
    }

    @Override
    public PeriodCzechFormatterBuilder appendMinutes()
    {
        return (PeriodCzechFormatterBuilder) super.appendMinutes();
    }

    @Override
    public PeriodCzechFormatterBuilder appendSeconds()
    {
        return (PeriodCzechFormatterBuilder) super.appendSeconds();
    }

    @Override
    public PeriodCzechFormatterBuilder appendMillis()
    {
        return (PeriodCzechFormatterBuilder) super.appendMillis();
    }

    public PeriodCzechFormatterBuilder appendSuffix(String singularText, String fewText, String pluralText)
    {
        try {
            appendSuffixMethod.invoke(this, new PeriodCzechAffix(singularText, fewText, pluralText));
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
        return this;
    }
}
