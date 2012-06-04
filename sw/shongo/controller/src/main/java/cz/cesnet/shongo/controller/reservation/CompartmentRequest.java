package cz.cesnet.shongo.controller.reservation;

import javax.persistence.Entity;

/**
 * Represents a compartment that is requested for a specific date/time slot.
 * The compartment should be copied to compartment request(s), because each
 * request can be filled by different additional information.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class CompartmentRequest extends Compartment
{
    // TODO:
}
