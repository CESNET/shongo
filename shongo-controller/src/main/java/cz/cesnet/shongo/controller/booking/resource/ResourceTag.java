package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;

import javax.persistence.*;
import java.util.*;

/**
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "tag_id"}))
public class ResourceTag extends SimplePersistentObject {
    private Resource resource;

    private Tag tag;

    private List<Tag> tags = new ArrayList<Tag>();

    @ManyToOne(optional = false)
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
}
