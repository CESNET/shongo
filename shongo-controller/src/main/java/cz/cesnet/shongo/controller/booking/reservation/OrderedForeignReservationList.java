package cz.cesnet.shongo.controller.booking.reservation;

import cz.cesnet.shongo.controller.api.domains.response.Reservation;

import java.util.LinkedList;

/**
 * Represents a list of {@link cz.cesnet.shongo.controller.api.domains.response.Reservation} ordered by their price.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class OrderedForeignReservationList extends LinkedList<Reservation>
{

}
