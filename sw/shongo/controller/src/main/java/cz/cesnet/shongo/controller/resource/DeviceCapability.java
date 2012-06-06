package cz.cesnet.shongo.controller.resource;

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
public class DeviceCapability extends Capability
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

    @Override
    public void setResource(Resource resource)
    {
        if (resource != null && (resource instanceof DeviceResource) == false) {
            throw new IllegalArgumentException("Device capability can be inserted only to device resource!");
        }
        super.setResource(resource);
    }
}
