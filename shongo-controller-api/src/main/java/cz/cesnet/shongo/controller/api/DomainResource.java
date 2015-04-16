package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;

/**
 * Information about controlled resources for foreign domain.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainResource extends IdentifiedComplexType
{
    private Domain domain;

    private Resource resource;

    private Integer licenseCount;

    private Integer price;

    private Integer priority;

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

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

    private static final String DOMAIN = "domain";
    private static final String RESOURCE = "resource";
    private static final String LICENSE_COUNT = "licenseCount";
    private static final String PRICE = "price";
    private static final String PRIORITY = "priority";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(DOMAIN, domain);
        dataMap.set(RESOURCE, resource);
        dataMap.set(LICENSE_COUNT, licenseCount);
        dataMap.set(PRICE, price);
        dataMap.set(PRIORITY, priority);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        domain = dataMap.getComplexType(DOMAIN, Domain.class);
        resource = dataMap.getComplexType(RESOURCE, Resource.class);
        licenseCount = dataMap.getInteger(LICENSE_COUNT);
        price = dataMap.getInteger(PRICE);
        priority = dataMap.getInteger(PRIORITY);
    }
}
