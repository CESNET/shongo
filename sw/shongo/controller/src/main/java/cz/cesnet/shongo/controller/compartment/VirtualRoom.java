package cz.cesnet.shongo.controller.compartment;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents an {@link Endpoint} which is able to interconnect multiple other {@link Endpoint}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class VirtualRoom extends Endpoint
{
    /**
     * @return port count of the {@link VirtualRoom}
     */
    @Transient
    public abstract Integer getPortCount();
}
