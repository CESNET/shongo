package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.executor.VirtualRoom;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingVirtualRoomReport extends Report
{
    /**
     * @see VirtualRoom
     */
    private VirtualRoom virtualRoom;

    /**
     * Constructor.
     */
    public AllocatingVirtualRoomReport()
    {
    }

    /**
     * Constructor.
     *
     * @param virtualRoom
     */
    public AllocatingVirtualRoomReport(VirtualRoom virtualRoom)
    {
        this.virtualRoom = virtualRoom;
    }

    /**
     * @return {@link #virtualRoom}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public VirtualRoom getVirtualRoom()
    {
        return virtualRoom;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Allocating virtual room in %s for %d ports.",
                virtualRoom.getReportDescription(), virtualRoom.getPortCount());
    }
}
