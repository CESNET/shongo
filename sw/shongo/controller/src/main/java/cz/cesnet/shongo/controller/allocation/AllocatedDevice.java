package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.resource.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a special type of {@link AllocatedResource} an allocated {@link DeviceResource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AllocatedDevice extends AllocatedResource implements AllocatedEndpoint
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

    @Override
    public void setResource(Resource resource)
    {
        if (!(resource instanceof DeviceResource)) {
            throw new IllegalArgumentException("Resource which is allocated must be device.");
        }
        super.setResource(resource);
    }

    /**
     * @return {@link DeviceResource} which is allocated
     */
    @Transient
    public DeviceResource getDeviceResource()
    {
        return (DeviceResource) getResource();
    }

    @Override
    @Transient
    public int getCount()
    {
        return 1;
    }

    @Override
    @Transient
    public Set<Technology> getSupportedTechnologies()
    {
        return getDeviceResource().getTechnologies();
    }

    @Override
    @Transient
    public boolean isStandalone()
    {
        return getDeviceResource().isStandaloneTerminal();
    }

    @Override
    @Transient
    public void assignAlias(Alias alias)
    {
        addAlias(alias);
    }

    @Override
    @Transient
    public List<Alias> getAssignedAliases()
    {
        List<Alias> aliases = new ArrayList<Alias>();
        TerminalCapability terminalCapability = getDeviceResource().getCapability(TerminalCapability.class);
        if (terminalCapability != null) {
            aliases.addAll(terminalCapability.getAliases());
        }
        aliases.addAll(this.aliases);
        return aliases;
    }

    @Override
    @Transient
    public Address getAddress()
    {
        return getDeviceResource().getAddress();
    }
}
