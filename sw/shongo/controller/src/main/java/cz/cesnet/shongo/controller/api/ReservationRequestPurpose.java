package cz.cesnet.shongo.controller.api;

/**
 * A purpose for which the created reservation from reservation request will be used.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ReservationRequestPurpose
{
    /**
     * Reservation will be used e.g., for research purposes.
     */
    SCIENCE,

    /**
     * Reservation will be used for education purposes (e.g., for a lecture).
     */
    EDUCATION
}
