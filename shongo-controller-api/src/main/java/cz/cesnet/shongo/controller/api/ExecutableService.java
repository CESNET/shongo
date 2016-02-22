package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import org.joda.time.Interval;

/**
 * Represents a service for an {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExecutableService extends IdentifiedComplexType
{
    /**
     * Identifier of {@link Executable} for which the service has been allocated.
     *
     * For foreign rooms/reservations it contains foreign reservation request id.
     */
    private String executableId;

    /**
     * Specifies whether service is currently active.
     */
    private boolean active;

    /**
     * Booked time slot for the service.
     */
    private Interval slot;

    /**
     * @return {@link #executableId}
     */
    public String getExecutableId()
    {
        return executableId;
    }

    /**
     * @param executableId sets the {@link #executableId}
     */
    public void setExecutableId(String executableId)
    {
        this.executableId = executableId;
    }

    /**
     * @return {@link #active}
     */
    public boolean isActive()
    {
        return active;
    }

    /**
     * @param active sets the {@link #active}
     */
    public void setActive(boolean active)
    {
        this.active = active;
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

    private static final String EXECUTABLE_ID = "executableId";
    private static final String ACTIVE = "active";
    private static final String SLOT = "slot";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(EXECUTABLE_ID, executableId);
        dataMap.set(ACTIVE, active);
        dataMap.set(SLOT, slot);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        executableId = dataMap.getStringRequired(EXECUTABLE_ID);
        active = dataMap.getBool(ACTIVE);
        slot = dataMap.getInterval(SLOT);
    }
}
