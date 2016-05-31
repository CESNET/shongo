package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.client.web.support.MessageProvider;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import cz.cesnet.shongo.controller.api.rpc.ExecutableService;

import java.util.List;

/**
 * {@link ReservationRequestModel} for detail page.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestDetailModel extends ReservationRequestModel
{
    private ReservationRequestState state;

    private String stateHelp;

    private AllocationState allocationState;

    private String allocationStateHelp;

    private String reservationId;

    private RoomModel room;

    public ReservationRequestDetailModel(AbstractReservationRequest abstractReservationRequest, Reservation reservation,
            CacheProvider cacheProvider, MessageProvider messageProvider, ExecutableService executableService,
            UserSession userSession)
    {
        super(abstractReservationRequest, cacheProvider);

        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;

            // Allocation state
            allocationState = reservationRequest.getAllocationState();
            reservationId = reservationRequest.getLastReservationId();
            if (allocationState != null) {
                allocationStateHelp = messageProvider.getMessage("views.reservationRequest.allocationStateHelp." + allocationState);

                if (AllocationState.ALLOCATION_FAILED.equals(allocationState)) {
                    AllocationStateReport allocationStateReport = reservationRequest.getAllocationStateReport();
                    StringBuilder allocationStateHelpBuilder = new StringBuilder();
                    if (userSession.isAdministrationMode()) {
                        allocationStateHelpBuilder.append(allocationStateHelp);
                        allocationStateHelpBuilder.append("<pre>");
                        allocationStateHelpBuilder.append(allocationStateReport.toString(
                                messageProvider.getLocale(), messageProvider.getTimeZone()));
                        allocationStateHelpBuilder.append("</pre>");
                    }
                    else {
                        allocationStateHelpBuilder.append("<strong>");
                        allocationStateHelpBuilder.append(allocationStateReport.toUserError().getMessage(
                                messageProvider.getLocale(), messageProvider.getTimeZone()));
                        allocationStateHelpBuilder.append("</strong><br/>");
                        allocationStateHelpBuilder.append(allocationStateHelp);
                    }
                    allocationStateHelp = allocationStateHelpBuilder.toString();
                }
            }

            // Executable state, reservation and room
            ExecutableState executableState = null;
            if (reservation != null) {
                // Reservation should contain allocated room
                AbstractRoomExecutable roomExecutable = (AbstractRoomExecutable) reservation.getExecutable();
                if (roomExecutable != null) {
                    executableState = roomExecutable.getState();
                    room = new RoomModel(roomExecutable, cacheProvider, messageProvider,
                            executableService, userSession, false);
                }
            }

            // Reservation request state
            state = ReservationRequestState.fromApi(allocationState, executableState,
                    (room != null ? room.getUsageState() : null),
                    abstractReservationRequest.getType(), getSpecificationType(),
                    (reservation != null ? reservation.getId() : null));

            // Set help for reservation request state
            if (AllocationState.ALLOCATION_FAILED.equals(allocationState)) {
                // Use allocation failed help
                stateHelp = allocationStateHelp;
            }
            else if (AllocationState.DENIED.equals(allocationState)) {
                StringBuilder reservationRequestDeniedHelp = new StringBuilder();
                AllocationStateReport.UserError userError = reservationRequest.getAllocationStateReport().toUserError();
                // Find user who denied the reservation request
                if (userError instanceof AllocationStateReport.UserIdentityRequired) {
                    AllocationStateReport.ReservationRequestDenied userErrorWithUserId;
                    userErrorWithUserId = (AllocationStateReport.ReservationRequestDenied) userError;
                    String userName = cacheProvider.getUserInformation(userErrorWithUserId.getUserId()).getFullName();
                    userErrorWithUserId.setUserName(userName);
                }
                reservationRequestDeniedHelp.append("<strong>");
                reservationRequestDeniedHelp.append(userError.getMessage(
                        messageProvider.getLocale(), messageProvider.getTimeZone()));
                reservationRequestDeniedHelp.append("</strong><br/>");
                reservationRequestDeniedHelp.append(allocationStateHelp);
                stateHelp = reservationRequestDeniedHelp.toString();
            }
            else if (room != null && RoomState.FAILED.equals(room.getState())) {
                // Use room failed help
                stateHelp = RoomState.FAILED.getHelp(
                        messageProvider.getMessageSource(), messageProvider.getLocale(), room.getType());
            }
            else if (allocationState != null) {
                // Use original reservation request state help
                stateHelp = state.getHelp(
                        messageProvider.getMessageSource(), messageProvider.getLocale(), specificationType);
            }
        }
    }

    public ReservationRequestState getState()
    {
        return state;
    }

    public String getStateHelp()
    {
        return stateHelp;
    }

    public AllocationState getAllocationState()
    {
        return allocationState;
    }

    public String getAllocationStateHelp()
    {
        return allocationStateHelp;
    }

    public String getReservationId()
    {
        return reservationId;
    }

    public RoomModel getRoom()
    {
        return room;
    }

    @Override
    public List<? extends ParticipantModel> getRoomParticipants()
    {
        if (room != null) {
            return room.getParticipants();
        }
        return super.getRoomParticipants();
    }
}
