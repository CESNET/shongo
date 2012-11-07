package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.util.IdentifiedObject;
import org.joda.time.Interval;

/**
 * Represents an allocated object which can be executed.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Executable extends IdentifiedObject
{
    /**
     * Slot of the {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    private Interval slot;

    /**
     * Current state of the {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    private State state;

    /**
     * @return {@link #slot}
     */
    public Interval getSlot()
    {
        return slot;
    }

    /**
     * @param slot sets the {@link #slot}
     */
    public void setSlot(Interval slot)
    {
        this.slot = slot;
    }

    /**
     * @return {@link #state}
     */
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    /**
     * State of the {@link cz.cesnet.shongo.controller.api.Executable}.
     */
    public static enum State
    {
        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} has not been started yet.
         */
        NOT_STARTED,

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} is already started.
         */
        STARTED,

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} failed to start.
         */
        STARTING_FAILED,

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} has been already stopped.
         */
        STOPPED
    }
}
