package cz.cesnet.shongo.controller.booking.resource;

import com.google.common.base.Strings;
import cz.cesnet.shongo.CommonReportSet;
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

    /**
     * Validate resource.
     *
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectInvalidException
     *
     */
    public void validate() throws CommonReportSet.ObjectInvalidException
    {
        if (domain == null) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Domain cannot be null.");
        }
        if (type == null && foreignResourceId == null) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Type or resource ID has to be set.");
        }
        if (type != null && foreignResourceId != null) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "Type and resource ID cannot be set at the same time.");
        }
    }

    public void validateSingleResource() throws CommonReportSet.ObjectInvalidException
    {
        if (domain == null || foreignResourceId == null || type != null) {
            throw new CommonReportSet.ObjectInvalidException(getClass().getSimpleName(),
                    "This ForeignResources is not for single resource or missing required field.");
        }
    }
}
