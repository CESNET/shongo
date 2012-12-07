package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.executor.RoomEndpoint;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingRoomReport extends Report
{
    /**
     * @see cz.cesnet.shongo.controller.executor.RoomEndpoint
     */
    private RoomEndpoint roomEndpoint;

    /**
     * Constructor.
     */
    public AllocatingRoomReport()
    {
    }

    /**
     * Constructor.
     *
     * @param roomEndpoint
     */
    public AllocatingRoomReport(RoomEndpoint roomEndpoint)
    {
        this.roomEndpoint = roomEndpoint;
    }

    /**
     * @return {@link #roomEndpoint}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public RoomEndpoint getRoomEndpoint()
    {
        return roomEndpoint;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Allocating virtual room in %s for %d licenses.",
                roomEndpoint.getReportDescription(), roomEndpoint.getRoomConfiguration().getLicenseCount());
    }
}
