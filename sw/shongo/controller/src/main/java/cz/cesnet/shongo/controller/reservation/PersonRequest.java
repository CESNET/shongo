package cz.cesnet.shongo.controller.reservation;


import cz.cesnet.shongo.controller.common.PersistentObject;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * Represents a requested person to a compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PersonRequest extends PersistentObject
{
    /**
     * Compartment in which the person request is located.
     */
    private Compartment compartment;

    /**
     * @return {@link #compartment}
     */
    @ManyToOne
    public Compartment getCompartment()
    {
        return compartment;
    }

    /**
     * @param compartment sets the {@link #compartment}
     */
    public void setCompartment(Compartment compartment)
    {
        this.compartment = compartment;
    }
}
