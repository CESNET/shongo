package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.AbstractReservationRequest;
import cz.cesnet.shongo.controller.api.AclRecord;
import cz.cesnet.shongo.controller.api.Reservation;

/**
 * Specifies how a {@link AbstractReservationRequest} can be reused in other {@link AbstractReservationRequest}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ReservationRequestReusement
{
    /**
     * {@link AbstractReservationRequest} cannot be reused in other {@link AbstractReservationRequest}s.
     */
    NONE,

    /**
     * {@link AbstractReservationRequest} can be reused in other {@link AbstractReservationRequest}s
     * (i.e., {@link Reservation}s allocated for the original {@link AbstractReservationRequest} can be reused
     * in {@link Reservation}s allocated for {@link AbstractReservationRequest}s which reuse
     * the original {@link AbstractReservationRequest}).
     */
    ARBITRARY,

    /**
     * {@link AbstractReservationRequest} can be reused in other {@link AbstractReservationRequest}s
     * (same as {@link #ARBITRARY}), but additionally the {@link AclRecord}s of the original
     * {@link AbstractReservationRequest} are propagated to {@link AbstractReservationRequest} which reuses
     * the original {@link AbstractReservationRequest} (i.e., the owner of the original
     * {@link AbstractReservationRequest} will be also the owner of the {@link AbstractReservationRequest}s
     * which reuse the original one and so on).
     */
    OWNED
}
