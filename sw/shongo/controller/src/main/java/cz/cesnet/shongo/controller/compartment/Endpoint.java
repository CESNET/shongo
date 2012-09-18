package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.request.CallInitiation;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents an entity (or multiple entities) which can participate in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Endpoint extends PersistentObject
{
    /**
     * List of {@link Person}s which use the {@link Endpoint} in the {@link Compartment}.
     */
    private List<Person> persons = new ArrayList<Person>();

    /**
     * {@link Alias}es that are additionally assigned to the {@link Endpoint}.
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
     * @param person to be added to the {@link #persons}
     */
    public void addPerson(Person person)
    {
        persons.add(person);
    }

    /**
     * @param person to be removed from the {@link #persons}
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
     * @return number of the endpoints which the {@link Endpoint} represents.
     */
    @Transient
    public int getCount()
    {
        return 1;
    }

    /**
     * @return set of technologies which are supported by the {@link Endpoint}
     */

    @Transient
    public abstract Set<Technology> getTechnologies();

    /**
     * @return true if device can participate in 2-point video conference without virtual room,
     *         false otherwise
     */
    @Transient
    public boolean isStandalone()
    {
        return false;
    }

    /**
     * @return IP address or URL of the {@link Endpoint}
     */
    @Transient
    public Address getAddress()
    {
        return null;
    }

    /**
     * @return description of the {@link Endpoint} for a {@link Report}
     */
    @Transient
    public String getReportDescription()
    {
        return toString();
    }

    /**
     * Defines who should initiate the call to this endpoint ({@code null} means that the {@link Scheduler}
     * can decide it).
     */
    @Transient
    public CallInitiation getCallInitiation()
    {
        return null;
    }

    /**
     * Assign {@link #aliases} to the {@link Endpoint}.
     *
     * @param compartmentExecutor
     */
    public void assignAliases(CompartmentExecutor compartmentExecutor)
    {
        List<Alias> aliases = getAliases();
        for (Alias alias : aliases) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("Assigning alias '%s' to %s .", alias.getValue(), getReportDescription()));
            compartmentExecutor.getLogger().debug(message.toString());
        }
    }
}
