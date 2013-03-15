package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * Represents a capability that a resource can have.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Capability extends PersistentObject
{
    /**
     * Resource to which the capability is applied.
     */
    private Resource resource;

    /**
     * @return {@link #resource}
     */
    @ManyToOne(optional = false)
    @Access(AccessType.FIELD)
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        // Manage bidirectional association
        if (resource != this.resource) {
            if (this.resource != null) {
                Resource oldResource = this.resource;
                this.resource = null;
                oldResource.removeCapability(this);
            }
            if (resource != null) {
                this.resource = resource;
                this.resource.addCapability(this);
            }
        }
    }

    /**
     * @param dateTime          date/time which is checked for availability
     * @param referenceDateTime reference date/time used e.g., as base date/time for relative date/time
     * @return true if the {@link Capability} (or the {@link #resource}) is available at given {@code dateTime},
     *         false otherwise
     */
    public boolean isAvailableInFuture(DateTime dateTime, DateTime referenceDateTime)
    {
        return resource.isAvailableInFuture(dateTime, referenceDateTime);
    }

    /**
     * @return converted capability to API
     * @throws FaultException
     */
    public final cz.cesnet.shongo.controller.api.Capability toApi()
    {
        cz.cesnet.shongo.controller.api.Capability api = createApi();
        toApi(api);
        return api;
    }

    /**
     * @param api
     * @param entityManager
     * @return new instance of {@link Capability} from API
     * @throws FaultException
     */
    public static Capability createFromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
        Capability capability;
        if (api instanceof cz.cesnet.shongo.controller.api.RoomProviderCapability) {
            capability = new RoomProviderCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.StandaloneTerminalCapability) {
            capability = new StandaloneTerminalCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.TerminalCapability) {
            capability = new TerminalCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ValueProviderCapability) {
            capability = new ValueProviderCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.AliasProviderCapability) {
            capability = new AliasProviderCapability();
        }
        else {
            throw new TodoImplementException(api.getClass().getName());
        }
        capability.fromApi(api, entityManager);
        return capability;
    }

    /**
     * @return new instance of API capability
     */
    protected abstract cz.cesnet.shongo.controller.api.Capability createApi();

    /**
     * @param api API capability to be filled
     */
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        api.setId(getId());
    }

    /**
     * Synchronize capability from API
     *
     * @param api
     * @param entityManager
     * @throws FaultException
     */
    protected void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
    }
}
