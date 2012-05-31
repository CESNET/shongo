package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.PersistentObject;

import javax.persistence.*;
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

    /**
     * Persons that are requested to use the device to connect into compartment.
     */
    private List<PersonRequest> requestedPersons;

    /**
     * Defines who should initiate the call to this device.
     */
    private Initiation initiation;

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

    /**
     * @return {@link #requestedPersons}
     */
    @OneToMany
    public List<PersonRequest> getRequestedPersons()
    {
        return requestedPersons;
    }

    /**
     * @param requestedPersons sets the {@link #requestedPersons}
     */
    private void setRequestedPersons(List<PersonRequest> requestedPersons)
    {
        this.requestedPersons = requestedPersons;
    }

    /**
     * @param personRequest person to be added to the list of requested persons for the resource
     */
    public void addRequestedPerson(PersonRequest personRequest)
    {
        this.requestedPersons.add(personRequest);
    }

    /**
     * @return {@link #initiation}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public Initiation getInitiation()
    {
        return initiation;
    }

    /**
     * @param initiation sets the {@link #initiation}
     */
    public void setInitiation(Initiation initiation)
    {
        this.initiation = initiation;
    }
}
