package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;

/**
 * {@link ReservationRequestModel} for detail page.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestDetailModel extends ReservationRequestModel
{
    private ReservationRequestState state;

    private AllocationState allocationState;

    private String allocationStateReport;

    private RoomModel room;

    public ReservationRequestDetailModel(AbstractReservationRequest abstractReservationRequest, Reservation reservation,
            CacheProvider cacheProvider, MessageProvider messageProvider, ExecutableService executableService,
            UserSession userSession)
    {
        super(abstractReservationRequest);

        if (specificationType.equals(SpecificationType.PERMANENT_ROOM_CAPACITY) && cacheProvider != null) {
            loadPermanentRoom(cacheProvider);
        }

        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;

            // Allocation state
            allocationState = reservationRequest.getAllocationState();

            if (AllocationState.ALLOCATION_FAILED.equals(allocationState)) {
                AllocationStateReport allocationStateReport = reservationRequest.getAllocationStateReport();
                if (userSession.isAdmin()) {
                    this.allocationStateReport = allocationStateReport.toString(
                            messageProvider.getLocale(), messageProvider.getTimeZone());
                }
                else {
                    this.allocationStateReport = allocationStateReport.toUserError().getMessage(
                            messageProvider.getLocale(), messageProvider.getTimeZone());
                }
            }

            // Executable state, reservation and room
            ExecutableState executableState = null;
            if (reservation != null) {
                // Reservation should contain allocated room
                AbstractRoomExecutable roomExecutable = (AbstractRoomExecutable) reservation.getExecutable();
                if (roomExecutable != null) {
                    executableState = roomExecutable.getState();
                    room = new RoomModel(roomExecutable, getId(), cacheProvider,
                            messageProvider, executableService, userSession);
                }
            }

            // Reservation request state
            state = ReservationRequestState.fromApi(allocationState, executableState,
                    (room != null ? room.getUsageState() : null),
                    abstractReservationRequest.getType(), getSpecificationType(),
                    (reservation != null ? reservation.getId() : null));
        }
    }

    public ReservationRequestState getState()
    {
        return state;
    }

    public AllocationState getAllocationState()
    {
        return allocationState;
    }

    public String getAllocationStateReport()
    {
        return allocationStateReport;
    }

    public RoomModel getRoom()
    {
        return room;
    }
}
