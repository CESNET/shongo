package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.allocation.AllocatedVirtualRoom;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingVirtualRoomReport extends AbstractResourceReport
{
    /**
     * Virtual room port count.
     */
    private Integer portCount;

    /**
     * Constructor.
     */
    public AllocatingVirtualRoomReport()
    {
    }

    /**
     * Constructor.
     *
     * @param allocatedVirtualRoom
     */
    public AllocatingVirtualRoomReport(AllocatedVirtualRoom allocatedVirtualRoom)
    {
        super(allocatedVirtualRoom.getDeviceResource());
        this.portCount = allocatedVirtualRoom.getPortCount();
    }

    /**
     * @return {@link #portCount}
     */
    @Column
    @Access(AccessType.FIELD)
    public Integer getPortCount()
    {
        return portCount;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Allocating virtual room in %s for %d ports.",
                getResourceAsString(), getPortCount());
    }
}
