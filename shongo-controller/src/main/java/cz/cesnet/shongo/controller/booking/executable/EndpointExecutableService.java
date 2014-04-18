package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;

/**
 * Interface to be implemented by {@link ExecutableService}s which can represents an additional endpoint.
 * For instance {@link RoomEndpoint} should get allocated additional license for this kind of service.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface EndpointExecutableService
{
    /**
     * @return true whether this {@link ExecutableService} represents an additional {@link Endpoint},
     *         false otherwise
     */
    boolean isEndpoint();
}
