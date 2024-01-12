package cz.cesnet.shongo.controller.rest;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.Executable;
import cz.cesnet.shongo.controller.api.Group;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.api.ReservationRequestSummary;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.api.SecurityToken;

/**
 * {@link Cache} provided for specified {@link #securityToken}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CacheProvider
{
    /**
     * {@link Cache} to be used for retrieving {@link UserInformation}.
     */
    private final Cache cache;

    /**
     * {@link SecurityToken} to be used for retrieving {@link UserInformation} by the {@link #cache}.
     */
    private final SecurityToken securityToken;

    /**
     * Constructor.
     *
     * @param cache         sets the {@link #cache}
     * @param securityToken sets the {@link #securityToken}
     */
    public CacheProvider(Cache cache, SecurityToken securityToken)
    {
        this.cache = cache;
        this.securityToken = securityToken;
    }

    /**
     * @return {@link #securityToken}
     */
    public SecurityToken getSecurityToken()
    {
        return securityToken;
    }

    /**
     * @param userId
     * @return {@link UserInformation} for given {@code userId}
     */
    public UserInformation getUserInformation(String userId)
    {
        return cache.getUserInformation(securityToken, userId);
    }

    /**
     * @param groupId
     * @return {@link Group} for given {@code groupId}
     */
    public Group getGroup(String groupId)
    {
        return cache.getGroup(securityToken, groupId);
    }

    /**
     * @param resourceId
     * @return {@link ResourceSummary} for given {@code resourceId}
     */
    public ResourceSummary getResourceSummary(String resourceId)
    {
        return cache.getResourceSummary(securityToken, resourceId);
    }

    /**
     * @param reservationRequestId
     * @return {@link ReservationRequestSummary} for given {@code reservationRequestId}
     */
    public ReservationRequestSummary getAllocatedReservationRequestSummary(String reservationRequestId)
    {
        return cache.getAllocatedReservationRequestSummary(securityToken, reservationRequestId);
    }

    /**
     * @param reservationId
     * @return {@link Reservation} for given {@code reservationId}
     */
    public Reservation getReservation(String reservationId)
    {
        return cache.getReservation(securityToken, reservationId);
    }

    /**
     * @param executable
     * @return identifier of reservation request for given {@code executable}
     */
    public String getReservationRequestIdByExecutable(Executable executable)
    {
        return cache.getReservationRequestIdByExecutable(securityToken, executable);
    }

    /**
     * @param executableId
     * @return {@link Executable} for given {@code executableId}
     */
    public Executable getExecutable(String executableId)
    {
        return cache.getExecutable(securityToken, executableId);
    }
}
