package cz.cesnet.shongo.controller.booking.domain;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.Tag;

import javax.persistence.*;

/**
 * Represents Resource mapped to domain with its parameters.
 *
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"domain_id", "resource_id"}))
public class DomainResource extends SimplePersistentObject {
    private Domain domain;

    private Resource resource;

    private Integer licenseCount;

    private Integer price;

    private Integer priority;

    @ManyToOne(optional = false)
    @Access(AccessType.FIELD)
    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    @ManyToOne(optional = false)
    @Access(AccessType.FIELD)
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Integer getLicenseCount() {
        return licenseCount;
    }

    public void setLicenseCount(Integer licenseCount) {
        this.licenseCount = licenseCount;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
