package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.util.IdentifiedObject;
import org.joda.time.Interval;

/**
 * Represents summary of an allocated {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableSummary extends IdentifiedObject
{
    /**
     * Identifier of the owner user.
     */
    private Integer userId;

    /**
     * Type of {@link Executable}.
     */
    private Type type;

    /**
     * Slot of the {@link ExecutableSummary}.
     */
    private Interval slot;

    /**
     * Current state of the {@link ExecutableSummary}.
     */
    private Executable.State state;

    /**
     * @return {@link #userId}
     */
    public Integer getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(Integer userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #type}
     */
    public Type getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(Type type)
    {
        this.type = type;
    }

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
    public Executable.State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(Executable.State state)
    {
        this.state = state;
    }

    /**
     * Type of {@link Executable}.
     */
    public static enum Type
    {
        /**
         * Represents {@link cz.cesnet.shongo.controller.api.Executable.Compartment}
         */
        COMPARTMENT,

        /**
         * Represents {@link cz.cesnet.shongo.controller.api.Executable.Room}
         */
        VIRTUAL_ROOM
    }
}
