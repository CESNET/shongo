package cz.cesnet.shongo.controller.compartment;

import javax.persistence.Entity;

/**
 * Represents an {@link Endpoint} which is able to interconnect multiple other {@link Endpoint}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class VirtualRoom extends Endpoint
{
}
