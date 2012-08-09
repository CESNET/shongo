package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.FaultException;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a capability that
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class DeviceCapability extends Capability
{
    /**
     * Set of technologies for which the device capability is applied.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Access(AccessType.FIELD)
    @Enumerated(EnumType.STRING)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technologies sets the {@link #technologies}
     */
    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    /**
     * @param technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    @Override
    public void setResource(Resource resource)
    {
        if (resource != null && (resource instanceof DeviceResource) == false) {
            throw new IllegalArgumentException("Device capability can be inserted only to device resource!");
        }
        super.setResource(resource);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
        // Create/modify technologies
        for (Technology technology : api.getTechnologies()) {
            if (api.isCollectionItemMarkedAsNew(api.TECHNOLOGIES, technology)) {
                addTechnology(technology);
            }
        }
        // Delete technologies
        Set<Technology> apiDeletedTechnologies = api.getCollectionItemsMarkedAsDeleted(api.TECHNOLOGIES);
        for (Technology technology : apiDeletedTechnologies) {
            removeTechnology(technology);
        }
        super.fromApi(api, entityManager);
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        for (Technology technology : technologies) {
            api.addTechnology(technology);
        }
        super.toApi(api);
    }
}
