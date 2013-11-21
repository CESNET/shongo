package cz.cesnet.shongo.client.web;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.api.*;

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
    private Cache cache;

    /**
     * {@link SecurityToken} to be used for retrieving {@link UserInformation} by the {@link #cache}.
     */
    private SecurityToken securityToken;

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
     * @param reservationRequestId
     * @return {@link ReservationRequestSummary} for given {@code reservationRequestId}
     */
    public ReservationRequestSummary getReservationRequestSummary(String reservationRequestId)
    {
        return cache.getReservationRequestSummary(securityToken, reservationRequestId);
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
