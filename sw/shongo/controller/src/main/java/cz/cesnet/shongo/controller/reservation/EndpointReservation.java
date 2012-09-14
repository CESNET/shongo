package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.compartment.Endpoint;

import javax.persistence.Entity;

/**
 * Represents a {@link Reservation} for a {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class EndpointReservation extends Reservation
{
}
