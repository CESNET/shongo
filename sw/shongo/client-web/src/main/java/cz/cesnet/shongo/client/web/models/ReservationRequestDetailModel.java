package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.H323RoomSetting;
import cz.cesnet.shongo.api.RoomSetting;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.client.web.Cache;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.MessageProvider;
import cz.cesnet.shongo.controller.Permission;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.Role;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;

/**
 * TODO:
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
            CacheProvider cacheProvider, MessageProvider messageProvider, ExecutableService executableService)
    {
        super(abstractReservationRequest, cacheProvider);

        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;

            // Allocation state
            allocationState = reservationRequest.getAllocationState();
            allocationStateReport = reservationRequest.getAllocationStateReport();

            // Executable state, reservation and room
            ExecutableState executableState = null;
            if (reservation != null) {
                // Reservation should contain allocated room
                AbstractRoomExecutable roomExecutable = (AbstractRoomExecutable) reservation.getExecutable();
                if (roomExecutable != null) {
                    executableState = roomExecutable.getState();
                    room = new RoomModel(roomExecutable, cacheProvider, messageProvider, executableService);
                }
            }

            // Reservation request state
            state = ReservationRequestState.fromApi(allocationState, executableState,
                    abstractReservationRequest.getType(), (reservation != null ? reservation.getId() : null));
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
