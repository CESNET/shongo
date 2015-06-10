package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;

import javax.persistence.*;

/**
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "tag_id", "full_resource_id"}))
public class ResourceTag extends SimplePersistentObject {
    private Resource resource;

    private Tag tag;

    private String foreignResourceId;

    @ManyToOne(optional = true)
    @Access(AccessType.FIELD)
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @ManyToOne(optional = false)
    @Access(AccessType.FIELD)
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

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getForeignResourceId()
    {
        return foreignResourceId;
    }

    public void setForeignResourceId(String fullResourceId)
    {
        this.foreignResourceId = fullResourceId;
    }
}
