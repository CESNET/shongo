package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import org.joda.time.Interval;

/**
 * Represents summary of an allocated {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableSummary extends IdentifiedComplexType
{
    /**
     * Slot of the {@link ExecutableSummary}.
     */
    private Interval slot;

    /**
     * Current state of the {@link ExecutableSummary}.
     */
    private Executable.State state;

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

    private static final String SLOT = "slot";
    private static final String STATE = "state";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(SLOT, slot);
        dataMap.set(STATE, state);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        slot = dataMap.getInterval(SLOT);
        state = dataMap.getEnum(STATE, Executable.State.class);
    }
}
