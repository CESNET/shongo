package cz.cesnet.shongo.controller.api.domains;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AdobeConnectPermissions;
import cz.cesnet.shongo.controller.api.domains.request.AbstractDomainRoomAction;
import cz.cesnet.shongo.controller.api.domains.request.CapabilitySpecificationRequest;
import cz.cesnet.shongo.controller.api.domains.response.RoomParticipant;
import cz.cesnet.shongo.controller.api.domains.request.ForeignRoomParticipantRole;
import cz.cesnet.shongo.controller.api.domains.response.*;
import org.joda.time.Interval;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Interface to Inter Domain protocol for Shongo domains
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public interface InterDomainProtocol {

    DomainLogin handleLogin(HttpServletRequest request);

    DomainStatus handleDomainStatus(HttpServletRequest request);

    List<DomainCapability> handleListCapabilities(HttpServletRequest request, Interval slot, List<CapabilitySpecificationRequest> capabilitySpecificationRequests)
            throws NotAuthorizedException;

    Reservation handleAllocateResource(HttpServletRequest request, Interval slot, String resourceId,
                                              String userId, String description, String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException;

    Reservation handleAllocateRoom(HttpServletRequest request, Interval slot, int participantCount, List<Technology> technologies,
                                          String userId, String description, String roomPin, AdobeConnectPermissions roomAccessMode,
                                          Boolean roomRecorded, String reservationRequestId, List<ForeignRoomParticipantRole> participants)
            throws NotAuthorizedException, ForbiddenException;

    Reservation handleGetReservation(HttpServletRequest request, String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException;

    AbstractResponse handleDeleteReservationRequest(HttpServletRequest request, String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException;

    List<Reservation> handleListReservations(HttpServletRequest request, String resourceId, Interval slot)
            throws NotAuthorizedException, ForbiddenException;

    AbstractResponse handleSetParticipants(HttpServletRequest request, String reservationRequestId, List<ForeignRoomParticipantRole> participants)
            throws NotAuthorizedException, ForbiddenException;

    List<RoomParticipant> handleGetParticipants(HttpServletRequest request, String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException;

    AbstractResponse handleRoomAction(HttpServletRequest request, String reservationRequestId, AbstractDomainRoomAction action)
            throws NotAuthorizedException, ForbiddenException;

    AbstractResponse handleDeletedReservation(HttpServletRequest request, String foreignReservationRequestId, String reason)
            throws NotAuthorizedException, ForbiddenException;

    class NotAuthorizedException extends Exception {
        public NotAuthorizedException(String message) {
            super(message);
        }
    }

    class ForbiddenException extends Exception {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}
