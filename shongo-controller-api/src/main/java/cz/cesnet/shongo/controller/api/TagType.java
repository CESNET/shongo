package cz.cesnet.shongo.controller.api;

/**
 * Type of {@link cz.cesnet.shongo.controller.booking.resource.Tag}.
 */
public enum TagType
{
    /**
     * Simple tag. Does not do anything special.
     */
    DEFAULT,

    /**
     * Sends notifications to the email addresses specified in this {@link cz.cesnet.shongo.controller.booking.resource.Tag}.
     */
    NOTIFY_EMAIL,

    /**
     * Adds additional information specified in {@link cz.cesnet.shongo.controller.booking.resource.Tag}
     * to {@link cz.cesnet.shongo.controller.booking.reservation.Reservation}.
     */
    RESERVATION_DATA,
}
