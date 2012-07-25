package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.Fault;
import cz.cesnet.shongo.api.FaultException;

import javax.persistence.*;

/**
 * Represents a capability that a resource can have.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Capability extends PersistentObject
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
    public cz.cesnet.shongo.controller.api.Capability toApi() throws FaultException
    {
        if (this instanceof VirtualRoomsCapability) {
            VirtualRoomsCapability virtualRoomsCapability = (VirtualRoomsCapability) this;
            cz.cesnet.shongo.controller.api.VirtualRoomsCapability virtualRoomsCapabilityApi =
                    new cz.cesnet.shongo.controller.api.VirtualRoomsCapability();
            virtualRoomsCapabilityApi.setId(virtualRoomsCapability.getId().intValue());
            virtualRoomsCapabilityApi.setPortCount(virtualRoomsCapability.getPortCount());
            return virtualRoomsCapabilityApi;
        }
        throw new FaultException(Fault.Common.TODO_IMPLEMENT);
    }
}
