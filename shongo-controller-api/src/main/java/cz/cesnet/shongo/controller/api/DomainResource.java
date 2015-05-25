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

    private ResourceSummary resourceSummary;

    private Integer licenseCount;

    private Integer price;

    private Integer priority;

    private String type;

    public Domain getDomain() {
        return domain;
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public ResourceSummary getResourceSummary() {
        return resourceSummary;
    }

    public void setResourceSummary(ResourceSummary resourceSummary) {
        this.resourceSummary = resourceSummary;
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

    private static final String DOMAIN = "domain";
    private static final String RESOURCE_SUMMARY = "resourceSummary";
    private static final String LICENSE_COUNT = "licenseCount";
    private static final String PRICE = "price";
    private static final String PRIORITY = "priority";
    private static final String TYPE = "type";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(DOMAIN, domain);
        dataMap.set(RESOURCE_SUMMARY, resourceSummary);
        dataMap.set(LICENSE_COUNT, licenseCount);
        dataMap.set(PRICE, price);
        dataMap.set(PRIORITY, priority);
        dataMap.set(TYPE, type);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        domain = dataMap.getComplexType(DOMAIN, Domain.class);
        resourceSummary = dataMap.getComplexType(RESOURCE_SUMMARY, ResourceSummary.class);
        licenseCount = dataMap.getInteger(LICENSE_COUNT);
        price = dataMap.getInteger(PRICE);
        priority = dataMap.getInteger(PRIORITY);
        type = dataMap.getString(type);
    }
}
