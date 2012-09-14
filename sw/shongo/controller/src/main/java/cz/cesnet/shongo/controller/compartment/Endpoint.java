package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.List;
import java.util.Set;

/**
 * Represents an entity (or multiple entities) which can participate in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class Endpoint extends PersistentObject
{
    /**
     * @return number of the endpoints which the {@link Endpoint} represents.
     */
    @Transient
    public int getCount()
    {
        throw new TodoImplementException();
    }

    /**
     * @return set of technologies which are supported by the {@link Endpoint}
     */

    @Transient
    public Set<Technology> getSupportedTechnologies()
    {
        throw new TodoImplementException();
    }

    /**
     * @return true if device can participate in 2-point video conference without virtual room,
     *         false otherwise
     */
    @Transient
    public boolean isStandalone()
    {
        throw new TodoImplementException();
    }

    /**
     * @param alias to be assign to the {@link Endpoint}
     */
    public void assignAlias(Alias alias)
    {
        throw new TodoImplementException();
    }

    /**
     * @return list of aliases for the {@link Endpoint}
     */
    @Transient
    public List<Alias> getAssignedAliases()
    {
        throw new TodoImplementException();
    }

    /**
     * @return IP address or URL of the {@link Endpoint}
     */
    @Transient
    public Address getAddress()
    {
        throw new TodoImplementException();
    }

    /**
     * @return description of the {@link Endpoint} for a {@link Report}
     */
    @Transient
    public String getReportDescription()
    {
        /*if (endpoint instanceof VirtualRoom) {
            VirtualRoom virtualRoom = (VirtualRoom) endpoint;
            return String.format("virtual room in %s",
                    AbstractResourceReport.formatResource(virtualRoom.getResource()));
        }
        if (endpoint instanceof AllocatedResource) {
            AllocatedResource allocatedResource = (AllocatedResource) endpoint;
            return AbstractResourceReport.formatResource(allocatedResource.getResource());
        }
        else if (endpoint instanceof AllocatedExternalEndpoint) {
            AllocatedExternalEndpoint allocatedExternalEndpoint = (AllocatedExternalEndpoint) endpoint;
            return String.format("external endpoint(count: %d)",
                    allocatedExternalEndpoint.getExternalEndpointSpecification().getCount());
        }
        else {
            return endpoint.toString();
        }*/
        throw new TodoImplementException();
    }
}
