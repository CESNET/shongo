package cz.cesnet.shongo.controller.resource;

import javax.persistence.*;

/**
 * Represents a capability that a resource can have.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Capability
{
    /**
     * Unique identifier in a domain controller database.
     */
    private Long id;

    /**
     * Resource to which the capability is applied.
     */
    private Resource resource;

    /**
     * @return {@link #id}
     */
    @Id
    @GeneratedValue
    @Access(AccessType.FIELD)
    public Long getId()
    {
        return id;
    }

    /**
     * @return {@link #resource}
     */
    @ManyToOne
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }
}
