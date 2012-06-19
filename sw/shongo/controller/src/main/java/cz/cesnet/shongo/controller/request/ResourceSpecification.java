package cz.cesnet.shongo.controller.request;

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
    @Access(AccessType.FIELD)
    public Compartment getCompartment()
    {
        return compartment;
    }

    /**
     * @param compartment sets the {@link #compartment}
     */
    public void setCompartment(Compartment compartment)
    {
        // Manage bidirectional association
        if (compartment != this.compartment) {
            if (this.compartment != null) {
                Compartment oldCompartment = this.compartment;
                this.compartment = null;
                oldCompartment.removeRequestedResource(this);
            }
            if (compartment != null) {
                this.compartment = compartment;
                this.compartment.addRequestedResource(this);
            }
        }
    }

    /**
     * @return {@link #requestedPersons}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Person> getRequestedPersons()
    {
        return requestedPersons;
    }

    /**
     * @param person person to be added to the {@link #requestedPersons}
     */
    public void addRequestedPerson(Person person)
    {
        requestedPersons.add(person);
    }

    /**
     * @param person person to be removed from the {@link #requestedPersons}
     */
    public void removeRequestedPerson(Person person)
    {
        requestedPersons.remove(person);
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
        if (callInitiation != null && callInitiation != CallInitiation.DEFAULT) {
            map.put("callInitiation", callInitiation.toString());
        }
    }
}
