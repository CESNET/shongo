package cz.cesnet.shongo.controller.api.domains;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.AdobeConnectPermissions;
import cz.cesnet.shongo.controller.api.domains.request.CapabilitySpecificationRequest;
import cz.cesnet.shongo.controller.api.domains.request.RoomParticipant;
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
                                          Boolean roomRecorded, String reservationRequestId, List<RoomParticipant> participants)
            throws NotAuthorizedException, ForbiddenException;

//    Reservation handleAddParticipants(HttpServletRequest request, String reservationRequestId, )
//            throws NotAuthorizedException, ForbiddenException;

    Reservation handleGetReservation(HttpServletRequest request, String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException;

    AbstractResponse handleDeleteReservationRequest(HttpServletRequest request, String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException;

    List<Reservation> handleListReservations(HttpServletRequest request, String resourceId, Interval slot)
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
