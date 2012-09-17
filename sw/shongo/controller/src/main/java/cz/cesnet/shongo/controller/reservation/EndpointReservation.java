package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.compartment.Endpoint;
import cz.cesnet.shongo.controller.compartment.EndpointProvider;
import cz.cesnet.shongo.controller.compartment.ResourceEndpoint;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a {@link Reservation} for a {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class EndpointReservation extends ResourceReservation implements EndpointProvider
{
    /**
     * List of persons which use the device in specified date/time slot.
     */
    private List<Person> persons = new ArrayList<Person>();

    /**
     * Aliases that are additionally assigned to the device.
     */
    private List<Alias> aliases = new ArrayList<Alias>();

    /**
     * @return {@link #persons}
     */
    @OneToMany
    @Access(AccessType.FIELD)
    public List<Person> getPersons()
    {
        return persons;
    }

    /**
     * @param person person to be added to the {@link #persons}
     */
    public void addPerson(Person person)
    {
        persons.add(person);
    }

    /**
     * @param person person to be removed from the {@link #persons}
     */
    public void removePerson(Person person)
    {
        persons.remove(person);
    }

    /**
     * @return {@link #aliases}
     */
    @OneToMany
    @Access(AccessType.FIELD)
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param alias alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        aliases.add(alias);
    }

    /**
     * @param alias alias to be removed from the {@link #aliases}
     */
    public void removeAlias(Alias alias)
    {
        aliases.remove(alias);
    }

    /**
     * @return {@link #resource} as {@link DeviceResource}
     */
    @Transient
    public DeviceResource getDeviceResource()
    {
        return (DeviceResource) getResource();
    }

    @Override
    public void setResource(Resource resource)
    {
        if (!(resource instanceof DeviceResource)) {
            throw new IllegalArgumentException("Resource must be device to be endpoint.");
        }
        super.setResource(resource);
    }

    /**
     * @return allocated {@link Endpoint} by the {@link EndpointReservation}
     */
    @Transient
    public Endpoint createEndpoint()
    {
        return new ResourceEndpoint(this);
    }
}
