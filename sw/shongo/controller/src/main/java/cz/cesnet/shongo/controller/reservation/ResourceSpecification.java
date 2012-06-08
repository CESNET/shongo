package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.common.Person;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a requested resource to a compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceSpecification extends PersistentObject implements Cloneable
{
    /**
     * Compartment in which the person request is located.
     */
    private Compartment compartment;

    /**
     * Persons that are requested to use the device to connect into compartment.
     */
    private List<Person> requestedPersons = new ArrayList<Person>();

    /**
     * Defines who should initiate the call to this device.
     */
    private CallInitiation callInitiation;

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
    public List<Person> getRequestedPersons()
    {
        return requestedPersons;
    }

    /**
     * @param requestedPersons sets the {@link #requestedPersons}
     */
    private void setRequestedPersons(List<Person> requestedPersons)
    {
        this.requestedPersons = requestedPersons;
    }

    /**
     * @param personRequest person to be added to the list of requested persons for the resource
     */
    public void addRequestedPerson(Person personRequest)
    {
        this.requestedPersons.add(personRequest);
    }

    /**
     * @return {@link #callInitiation}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public CallInitiation getCallInitiation()
    {
        return callInitiation;
    }

    /**
     * @param callInitiation sets the {@link #callInitiation}
     */
    public void setCallInitiation(CallInitiation callInitiation)
    {
        this.callInitiation = callInitiation;
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        addCollectionToMap(map, "requestedPersons", requestedPersons);
        if ( callInitiation != null && callInitiation != CallInitiation.DEFAULT ) {
            map.put("callInitiation", callInitiation.toString());
        }
    }
}
