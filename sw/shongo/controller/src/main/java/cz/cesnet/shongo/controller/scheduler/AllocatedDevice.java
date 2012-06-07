package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.common.Person;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.DeviceResource;

import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a special type of {@link AllocatedResource} an allocated {@link DeviceResource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedDevice extends AllocatedResource
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
    public List<Person> getPersons()
    {
        return persons;
    }

    /**
     * @param persons sets the {@link #persons}
     */
    private void setPersons(List<Person> persons)
    {
        this.persons = persons;
    }

    /**
     * @param person person to be added to the {@link #persons}
     */
    public void addPeson(Person person)
    {
        this.persons.add(person);
    }

    /**
     * @return {@link #aliases}
     */
    @OneToMany
    public List<Alias> getAliases()
    {
        return aliases;
    }

    /**
     * @param aliases sets the {@link #aliases}
     */
    private void setAliases(List<Alias> aliases)
    {
        this.aliases = aliases;
    }

    /**
     * @param alias alias to be added to the {@link #aliases}
     */
    public void addAlias(Alias alias)
    {
        this.aliases.add(alias);
    }
}
