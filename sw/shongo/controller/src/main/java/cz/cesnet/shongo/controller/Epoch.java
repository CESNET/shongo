package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.AbsoluteDateTimeSpecification;

/**
 * Represents a single epoch.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Epoch
{
    /**
     * Scheduler time span start.
     */
    private final AbsoluteDateTimeSpecification from;

    /**
     * Scheduler time span end
     */
    private final AbsoluteDateTimeSpecification to;

    /**
     * Constructor.
     *
     * @param from sets the {@link #from}
     * @param to   sets the {@link #to}
     */
    public Epoch(AbsoluteDateTimeSpecification from, AbsoluteDateTimeSpecification to)
    {
        this.from = from;
        this.to = to;
    }

    /**
     * @return {@link #from}
     */
    public AbsoluteDateTimeSpecification getFrom()
    {
        return from;
    }

    /**
     * @return {@link #to}
     */
    public AbsoluteDateTimeSpecification getTo()
    {
        return to;
    }
}
