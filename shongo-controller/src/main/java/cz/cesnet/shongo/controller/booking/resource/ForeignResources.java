package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.booking.domain.Domain;

import javax.persistence.*;

/**
 * Represents one or group of foreign resources by its type.
 *
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"domain_id", "foreign_resource_id"}), @UniqueConstraint(columnNames = {"domain_id", "type"})})
public class ForeignResources extends SimplePersistentObject
{
    private Domain domain;

    private Long foreignResourceId;

    private String type;

    @ManyToOne(optional = false)
    @Access(AccessType.FIELD)
    @JoinColumn(name = "domain_id")
    public Domain getDomain()
    {
        return domain;
    }

    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    @Column(name = "foreign_resource_id", nullable = true)
    public Long getForeignResourceId()
    {
        return foreignResourceId;
    }

    public void setForeignResourceId(Long foreignResourceId)
    {
        this.foreignResourceId = foreignResourceId;
    }

    @Column(name = "type", nullable = true, length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
