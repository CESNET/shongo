package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.booking.domain.Domain;

import javax.persistence.*;

/**
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "tag_id", "foreign_resources_id"}))
public class ResourceTag extends SimplePersistentObject {
    private Resource resource;

    private Tag tag;

    private ForeignResources foreignResources;

    @ManyToOne
    @Access(AccessType.FIELD)
    @JoinColumn(name = "resource_id")
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @ManyToOne(optional = false)
    @Access(AccessType.FIELD)
    @JoinColumn(name = "tag_id")
    public Tag getTag() {
        return tag;
    }

    /**
     * @param tag sets the {@link #tag}
     */
    public void setTag(Tag tag)
    {
        this.tag = tag;
    }

    @ManyToOne(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    @JoinColumn(name = "foreign_resources_id")
    public ForeignResources getForeignResources()
    {
        return foreignResources;
    }

    public void setForeignResources(ForeignResources foreignResources)
    {
        this.foreignResources = foreignResources;
    }
}
