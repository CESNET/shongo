package cz.cesnet.shongo.controller.api.domains;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.domains.response.*;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;
import org.joda.time.Interval;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * Interface to Inter Domain protocol for Shongo domains
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public interface InterDomainProtocol {

    public DomainLogin handleLogin(HttpServletRequest request);
    /**
     *
     * @param request
     * @return status of local domain
     */
    public DomainStatus handleDomainStatus(HttpServletRequest request);

    public List<DomainCapability> handleListCapabilities(HttpServletRequest request, DomainCapabilityListRequest.Type type,
                                                         Interval interval, List<Set<Technology>> technologyVariants)
            throws NotAuthorizedException;

    public Reservation handleAllocateResource(HttpServletRequest request, Interval slot, String resourceId,
                                              String userId, String description, String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException;

    public Reservation handleAllocateRoom(HttpServletRequest request, Interval slot, Technology technology,
                                          String userId, String description, String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException;

    public Reservation handleGetReservation(HttpServletRequest request, String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException;

    public AbstractResponse handleDeleteReservationRequest(HttpServletRequest request, String reservationRequestId)
            throws NotAuthorizedException, ForbiddenException;

    public List<Reservation> handleListReservations(HttpServletRequest request, String resourceId, Interval slot)
            throws NotAuthorizedException, ForbiddenException;

    public class NotAuthorizedException extends Exception {
        public NotAuthorizedException(String message) {
            super(message);
        }
    }

    public class ForbiddenException extends Exception {
        public ForbiddenException(String message) {
            super(message);
        }
    }
}
