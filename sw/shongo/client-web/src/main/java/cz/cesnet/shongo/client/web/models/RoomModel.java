package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.MessageProvider;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.ExecutableSummary;
import cz.cesnet.shongo.controller.api.RoomExecutable;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.request.ExecutableListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.springframework.context.MessageSource;

import java.util.List;
import java.util.Locale;

/**
 * TODO:
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

    private State state;

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
            this.state = State.NOT_AVAILABLE;
            switch (roomExecutable.getState()) {
                case STARTED:
                case STOPPING_FAILED:
                    if (this.licenseCount > 0) {
                        this.state = State.AVAILABLE;
                    }
                    break;
                case STARTING_FAILED:
                    this.state = State.FAILED;
                    break;
                case STOPPED:
                    this.state = State.STOPPED;
                    break;
            }
        }
        else {
            this.state = State.valueOf(roomExecutable.getState().toString());
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

    public State getState()
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

    public enum State
    {
        NOT_STARTED(Executable.State.NOT_STARTED.isAvailable()),

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} is already started.
         */
        STARTED(Executable.State.STARTED.isAvailable()),

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} failed to start.
         */
        STARTING_FAILED(Executable.State.STARTING_FAILED.isAvailable()),

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} has been already stopped.
         */
        STOPPED(Executable.State.STOPPED.isAvailable()),

        /**
         * {@link cz.cesnet.shongo.controller.api.Executable} failed to stop.
         */
        STOPPING_FAILED(Executable.State.STOPPING_FAILED.isAvailable()),

        /**
         * Permanent room is not available for participants to join.
         */
        NOT_AVAILABLE(false),

        /**
         * Permanent room is available for participants to join.
         */
        AVAILABLE(true),

        /**
         * Permanent room is not available for participants to join due to error.
         */
        FAILED(false);

        /**
         * Specifies whether this state represents an available room.
         */
        private final boolean isAvailable;

        /**
         * Constructor.
         *
         * @param isAvailable sets the {@link #isAvailable}
         */
        private State(boolean isAvailable)
        {
            this.isAvailable = isAvailable;
        }

        /**
         * @return {@link #isAvailable}
         */
        public boolean isAvailable()
        {
            return isAvailable;
        }


        public static State fromRoomState(Executable.State roomState, boolean permanentRoom, int licenseCount)
        {
            if (permanentRoom) {
                switch (roomState) {
                    case STARTED:
                    case STOPPING_FAILED:
                        if (licenseCount > 0) {
                            return State.AVAILABLE;
                        }
                        break;
                    case STARTING_FAILED:
                        return State.FAILED;
                    case STOPPED:
                        return State.STOPPED;
                }
                return NOT_AVAILABLE;
            }
            else {
                return State.valueOf(roomState.toString());
            }
        }
    }
}
