package cz.cesnet.shongo.client.web.models;

import com.google.common.base.Strings;
import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.client.web.CacheProvider;
import cz.cesnet.shongo.controller.api.*;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link ReservationRequestModel} for modification.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestModificationModel extends ReservationRequestModel
{
    private boolean adhocRoomRetainRoomName = false;

    public ReservationRequestModificationModel(AbstractReservationRequest reservationRequest,
            CacheProvider cacheProvider)
    {
        super(reservationRequest, cacheProvider);

        if (specificationType.equals(SpecificationType.ADHOC_ROOM)) {
            // Get allocated room name
            ReservationRequestSummary reservationRequestSummary =
                    cacheProvider.getReservationRequestSummary(reservationRequest.getId());
            if (reservationRequestSummary != null) {
                String reservationId = reservationRequestSummary.getLastReservationId();
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
    }

    public Boolean getAdhocRoomRetainRoomName()
    {
        return adhocRoomRetainRoomName;
    }

    public void setAdhocRoomRetainRoomName(Boolean adhocRoomRetainRoomName)
    {
        this.adhocRoomRetainRoomName = adhocRoomRetainRoomName;
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
                roomSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME).withValue(roomName));
            }
        }
        return abstractReservationRequest;
    }
}
