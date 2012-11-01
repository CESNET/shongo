package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.CallInitiation;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.report.Report;
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
public abstract class Endpoint extends Executable
{
    /**
     * List of {@link Person}s which use the {@link Endpoint} in the {@link Compartment}.
     */
    private List<Person> persons = new ArrayList<Person>();

    /**
     * {@link Alias}es that are additionally assigned to the {@link Endpoint}.
     */
    private List<Alias> assignedAliases = new ArrayList<Alias>();

    /**
     * @return {@link #persons}
     */
    @OneToMany(cascade = CascadeType.ALL)
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
     * @return {@link #assignedAliases}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Alias> getAssignedAliases()
    {
        return assignedAliases;
    }

    /**
     * @param assignedAlias alias to be added to the {@link #assignedAliases}
     */
    public void addAssignedAlias(Alias assignedAlias)
    {
        assignedAliases.add(assignedAlias);
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
     * @return {@link #assignedAliases}
     */
    @Transient
    public List<Alias> getAliases()
    {
        return assignedAliases;
    }

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
     * Assign {@link #assignedAliases} to the {@link Endpoint}.
     *
     * @param executorThread
     */
    public void assignAliases(ExecutorThread executorThread)
    {
        List<Alias> aliases = getAssignedAliases();
        for (Alias alias : aliases) {
            StringBuilder message = new StringBuilder();
            message.append(String.format("Assigning alias '%s' to %s .", alias.getValue(), getReportDescription()));
            executorThread.getLogger().debug(message.toString());
        }
    }
}
