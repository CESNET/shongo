package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.AuthorizationService;
import org.joda.time.DateTime;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link ReservationRequestModel} for modification.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestModificationModel extends ReservationRequestModel
{
    private boolean adhocRoomRetainRoomName = false;

    private ReservationRequestModel original;

    public ReservationRequestModificationModel(AbstractReservationRequest reservationRequest,
            CacheProvider cacheProvider, AuthorizationService authorizationService)
    {
        super(reservationRequest, cacheProvider);

        if (specificationType.equals(SpecificationType.ADHOC_ROOM)) {
            // Get allocated room name
            ReservationRequestSummary reservationRequestSummary =
                    cacheProvider.getAllocatedReservationRequestSummary(reservationRequest.getId());
            if (reservationRequestSummary != null) {
                String reservationId = reservationRequestSummary.getAllocatedReservationId();
                if (reservationId != null) {
                    Reservation reservation = cacheProvider.getReservation(reservationId);
                    AbstractRoomExecutable roomExecutable = (AbstractRoomExecutable) reservation.getExecutable();
                    Alias roomNameAlias = roomExecutable.getAliasByType(AliasType.ROOM_NAME);
                    if (roomNameAlias == null) {
                        throw new UnsupportedApiException("Room must have name.");
                    }
                    roomName = roomNameAlias.getValue();
                }
            }
            if (roomName != null) {
                // Room name should be retained
                adhocRoomRetainRoomName = true;
            }
        }

        // Load user roles
        loadUserRoles(cacheProvider.getSecurityToken(), authorizationService);
        for (UserRoleModel userRole : getUserRoles()) {
            userRole.setDeletable(false);
        }

        // Store old values
        this.original = new ReservationRequestModel(cacheProvider);
        this.original.id = this.id;
        this.original.parentReservationRequestId = this.parentReservationRequestId;
        this.original.type = this.type;
        this.original.description = this.description;
        this.original.dateTime = this.dateTime;
        this.original.technology = this.technology;
        this.original.timeZone = this.timeZone;
        this.original.start = this.start;
        this.original.end = this.end;
        this.original.durationCount = this.durationCount;
        this.original.durationType = this.durationType;
        this.original.slotBeforeMinutes = this.slotBeforeMinutes;
        this.original.slotAfterMinutes = this.slotAfterMinutes;
        this.original.periodicityType = this.periodicityType;
        this.original.periodicityEnd = this.periodicityEnd;
        this.original.specificationType = this.specificationType;
        this.original.roomName = this.roomName;
        this.original.permanentRoomReservationRequestId = this.permanentRoomReservationRequestId;
        this.original.permanentRoomReservationRequest = this.permanentRoomReservationRequest;
        this.original.roomParticipantCount = this.roomParticipantCount;
        this.original.roomPin = this.roomPin;
        this.original.roomRecorded = this.roomRecorded;
        this.original.roomAccessMode = this.roomAccessMode;
        this.original.roomMeetingName = this.roomMeetingName;
        this.original.roomMeetingDescription = this.roomMeetingDescription;
    }

    public Boolean getAdhocRoomRetainRoomName()
    {
        return adhocRoomRetainRoomName;
    }

    public void setAdhocRoomRetainRoomName(Boolean adhocRoomRetainRoomName)
    {
        this.adhocRoomRetainRoomName = adhocRoomRetainRoomName;
    }

    public ReservationRequestModel getOriginal()
    {
        return original;
    }

    public boolean isEndChanged()
    {
        return !this.end.equals(this.original.end);
    }

    public boolean isRoomParticipantCountChanged()
    {
        return !this.roomParticipantCount.equals(this.original.roomParticipantCount);
    }

    @Override
    public AbstractReservationRequest toApi(HttpServletRequest request)
    {
        AbstractReservationRequest abstractReservationRequest = super.toApi(request);
        Specification specification = abstractReservationRequest.getSpecification();
        if (specificationType.equals(SpecificationType.ADHOC_ROOM)) {
            RoomSpecification roomSpecification = (RoomSpecification) specification;
            if (adhocRoomRetainRoomName && !Strings.isNullOrEmpty(roomName)) {
                // Room name should be retained
                roomSpecification.getEstablishment().addAliasSpecification(
                        new AliasSpecification(AliasType.ROOM_NAME).withValue(roomName));
            }
        }
        return abstractReservationRequest;
    }
}
