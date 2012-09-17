package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.request.CallInitiation;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;
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
     * @param alias to be assign to the {@link Endpoint}
     */
    @Transient
    public abstract void addAlias(Alias alias);

    /**
     * @return list of aliases for the {@link Endpoint}
     */
    @Transient
    public abstract List<Alias> getAliases();

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
}
