package cz.cesnet.shongo.controller.booking.domain;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.resource.Resource;

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

    private String type;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return domainResource converted capability to API
     */
    public final cz.cesnet.shongo.controller.api.DomainResource toApi()
    {
        cz.cesnet.shongo.controller.api.DomainResource domainResourceApi = new cz.cesnet.shongo.controller.api.DomainResource();
        toApi(domainResourceApi);
        return domainResourceApi;
    }

    public void toApi(cz.cesnet.shongo.controller.api.DomainResource domainResourceApi)
    {
        throw new TodoImplementException();
//        domainResourceApi.setId(ObjectIdentifier.formatId(this));
    }

    /**
     * @param domainResourceApi
     * @return domainResource converted from API
     */
    public static DomainResource createFromApi(cz.cesnet.shongo.controller.api.DomainResource domainResourceApi)
    {
        DomainResource domainResource = new DomainResource();
        domainResource.fromApi(domainResourceApi);
        return domainResource;
    }

    public void fromApi(cz.cesnet.shongo.controller.api.DomainResource domainResourceApi)
    {
        throw new TodoImplementException();
//        this.setDomain(Domain.createFromApi(domainResourceApi.getDomainName()));
    }
}
