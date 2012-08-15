package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

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
    @ManyToOne
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
     * @return new instance of API capability
     */
    protected abstract cz.cesnet.shongo.controller.api.Capability createApi();

    /**
     * @param api API capability to be filled
     */
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        api.setId(getId().intValue());
    }

    /**
     * Synchronize capability from API
     *
     * @param api
     * @param entityManager
     * @throws FaultException
     */
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
    }

    /**
     * @param api
     * @param entityManager
     * @return new instance of {@link Capability} from API
     * @throws FaultException
     */
    public static Capability fromAPI(cz.cesnet.shongo.controller.api.Capability api,
            EntityManager entityManager) throws FaultException
    {
        Capability resourceSpecification;
        if (api instanceof cz.cesnet.shongo.controller.api.VirtualRoomsCapability) {
            resourceSpecification = new VirtualRoomsCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.StandaloneTerminalCapability) {
            resourceSpecification = new StandaloneTerminalCapability();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.TerminalCapability) {
            resourceSpecification = new TerminalCapability();
        }
        else {
            throw new TodoImplementException();
        }
        resourceSpecification.fromApi(api, entityManager);
        return resourceSpecification;
    }
}
