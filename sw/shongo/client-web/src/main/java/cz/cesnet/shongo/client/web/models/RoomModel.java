package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.MessageProvider;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.List;

/**
 * Represents a room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomModel
{
    private final MessageProvider messageProvider;

    private String id;

    private String reservationRequestId;

    private Interval slot;

    private TechnologyModel technology;

    private int licenseCount;

    private boolean isPermanent = false;

    private List<Alias> aliases;

    private RoomState state;

    private String stateReport;

    public RoomModel(RoomExecutable roomExecutable, CacheProvider cacheProvider, MessageProvider messageProvider,
            ExecutableService executableService)
    {
        this.messageProvider = messageProvider;

        this.id = roomExecutable.getId();
        this.reservationRequestId = cacheProvider.getReservationRequestIdByReservation(roomExecutable);
        this.slot = roomExecutable.getSlot();
        this.technology = TechnologyModel.find(roomExecutable.getTechnologies());
        this.aliases = roomExecutable.getAliases();
        this.licenseCount = roomExecutable.getLicenseCount();

        if (this.licenseCount == 0) {
            // Permanent room
            this.isPermanent = true;

            // Get license count from active usage
            ExecutableListRequest request = new ExecutableListRequest();
            request.setSecurityToken(cacheProvider.getSecurityToken());
            request.setRoomId(roomExecutable.getId());
            ListResponse<ExecutableSummary> executableSummaries = executableService.listExecutables(request);
            DateTime dateTimeNow = DateTime.now();
            for (ExecutableSummary executableSummary : executableSummaries) {
                if (executableSummary.getSlot().contains(dateTimeNow) && executableSummary.getState().isAvailable()) {
                    licenseCount = executableSummary.getRoomLicenseCount();
                    break;
                }
            }
        }

        // State
        if (this.isPermanent) {
            this.state = RoomState.NOT_AVAILABLE;
            switch (roomExecutable.getState()) {
                case STARTED:
                case STOPPING_FAILED:
                    if (this.licenseCount > 0) {
                        this.state = RoomState.AVAILABLE;
                    }
                    break;
                case STARTING_FAILED:
                    this.state = RoomState.FAILED;
                    break;
                case STOPPED:
                    this.state = RoomState.STOPPED;
                    break;
            }
        }
        else {
            this.state = RoomState.valueOf(roomExecutable.getState().toString());
        }
        this.stateReport = roomExecutable.getStateReport();
    }

    public String getId()
    {
        return id;
    }

    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    public Interval getSlot()
    {
        return slot;
    }

    public TechnologyModel getTechnology()
    {
        return technology;
    }

    public int getLicenseCount()
    {
        return licenseCount;
    }

    public RoomState getState()
    {
        return state;
    }

    public String getStateReport()
    {
        return stateReport;
    }

    public boolean isAvailable()
    {
        return state.isAvailable();
    }

    public String getAliases()
    {
        return ReservationRequestModel.formatAliases(aliases, isAvailable());
    }

    public String getAliasesDescription()
    {
        return ReservationRequestModel.formatAliasesDescription(aliases, isAvailable(), messageProvider);
    }

}
