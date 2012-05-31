package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.PersistentObject;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import java.util.List;

/**
 * Represents a requested resource to a compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceSpecification extends PersistentObject
{
    /**
     * Compartment in which the person request is located.
     */
    private Compartment compartment;

    private List<PersonRequest> personRequestList;

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
