package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ValueInvalidReport extends Report
{
    /**
     * Value.
     */
    private String value;

    /**
     * Constructor.
     */
    public ValueInvalidReport()
    {
    }

    /**
     * Constructor.
     *
     * @param value ses the {@link #value}
     */
    public ValueInvalidReport(String value)
    {
        this.value = value;
    }

    /**
     * @return {@link #value}
     */
    @Column
    public String getValue()
    {
        return value;
    }

    /**
     * @param value sets the {@link #value}
     */
    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    @Transient
    public State getState()
    {
        return State.ERROR;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Value '%s' is invalid", value);
    }
}
