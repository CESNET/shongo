package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.domains.request.CapabilitySpecificationRequest;
import cz.cesnet.shongo.controller.api.domains.response.DomainCapability;
import org.joda.time.Interval;

import java.util.*;

/**
 * List request for domain capabilities.
 *
 * @author Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class DomainCapabilityListRequest extends AbstractRequest
{
    /**
     * Slot for the request.
     */
    private Interval slot;

    /**
     * For filtering resources by ids.
     */
    private Set<String> resourceIds = new HashSet<>();

    /**
     * For filtering capabilities by {@code resourceType}, not usable with {@code resourceType}
     */
    private Domain domain;

    /**
     * For filtering capabilities by {@code resourceType}, {@code domain} must be set. Not usable with {@code resourceType}
     */
    private String resourceType;

    private Boolean onlyAllocatable;

    private List<CapabilitySpecificationRequest> capabilitySpecificationRequests = new ArrayList<>();

    /**
     * Constructor
     */
    public DomainCapabilityListRequest()
    {
    }

    /**
     * Constructor.
     *
     * Create instance with one {@link CapabilitySpecificationRequest} with {@code capabilityType} set.
     *
     * @param capabilityType to be set
     */
    public DomainCapabilityListRequest(DomainCapability.Type capabilityType)
    {
        addCapabilityListRequest(new CapabilitySpecificationRequest(capabilityType));
    }

    /**
     * Constructor.
     *
     * @param domain specified for domain capability request
     */
    public DomainCapabilityListRequest(Domain domain) {
        this.domain = domain;
    }

    public Domain getDomain()
    {
        return domain;
    }

    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    public String getDomainName()
    {
        return (domain == null ? null : domain.getName());
    }

    public String getDomainId()
    {
        return (domain == null ? null : domain.getId());
    }

    public String getResourceType()
    {
        return resourceType;
    }

    public void setResourceType(String resourceType)
    {
        this.resourceType = resourceType;
    }

    public Interval getSlot() {
        return slot;
    }

    public void setSlot(Interval slot) {
        this.slot = slot;
    }

    public Set<String> getResourceIds()
    {
        return resourceIds;
    }

    public void setResourceIds(Set<String> resourceIds)
    {
        this.resourceIds = resourceIds;
    }

    public void addResourceId(String resourceId)
    {
        this.resourceIds.add(resourceId);
    }

    public void addResourcesIds(Collection<Long> resourcesIds)
    {
        this.resourceIds.addAll(resourceIds);
    }

    public Boolean getOnlyAllocatable()
    {
        return onlyAllocatable;
    }

    public void setOnlyAllocatable(Boolean onlyAllocatable)
    {
        this.onlyAllocatable = onlyAllocatable;
    }


    public List<CapabilitySpecificationRequest> getCapabilitySpecificationRequests()
    {
        return capabilitySpecificationRequests;
    }

    public void setCapabilitySpecificationRequests(List<CapabilitySpecificationRequest> capabilitySpecificationRequests)
    {
        this.capabilitySpecificationRequests = capabilitySpecificationRequests;
    }

    public void addCapabilityListRequest(CapabilitySpecificationRequest capabilitySpecificationRequest)
    {
        this.capabilitySpecificationRequests.add(capabilitySpecificationRequest);
    }

    public List<DomainCapability.Type> getRequestedCapabilitiesTypes() {
        List<DomainCapability.Type> types = new ArrayList<>();
        for (CapabilitySpecificationRequest capabilitySpecificationRequest : capabilitySpecificationRequests) {
            types.add(capabilitySpecificationRequest.getCapabilityType());
        }
        return types;
    }

    public void validateForResource()
    {
        if (this.capabilitySpecificationRequests.size() != 1) {
            throw new IllegalArgumentException("CapabilityListRequest must be set");
        }
        if (!DomainCapability.Type.RESOURCE.equals(this.capabilitySpecificationRequests.get(0).getCapabilityType())) {
            throw new IllegalArgumentException("Request's type must be RESOURCE.");
        }
    }
}