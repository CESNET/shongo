package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.AbsoluteDateTime;

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
    private final AbsoluteDateTime from;

    /**
     * Scheduler time span end
     */
    private final AbsoluteDateTime to;

    /**
     * Constructor.
     *
     * @param from sets the {@link #from}
     * @param to   sets the {@link #to}
     */
    public Epoch(AbsoluteDateTime from, AbsoluteDateTime to)
    {
        this.from = from;
        this.to = to;
    }

    /**
     * @return {@link #from}
     */
    public AbsoluteDateTime getFrom()
    {
        return from;
    }

    /**
     * @return {@link #to}
     */
    public AbsoluteDateTime getTo()
    {
        return to;
    }
}
