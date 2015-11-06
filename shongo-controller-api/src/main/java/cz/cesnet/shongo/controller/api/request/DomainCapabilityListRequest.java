package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import cz.cesnet.shongo.controller.api.RecordingCapability;
import cz.cesnet.shongo.controller.api.domains.request.CapabilityListRequest;
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
     * {@link cz.cesnet.shongo.controller.api.domains.response.DomainCapability.Type#RESOURCE} of the resources to be listed.
     */
//    private Type capabilityType;

//    private List<Set<Technology>> technologyVariants;

    private Interval slot;

//    private Integer licenseCount;

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

    private List<CapabilityListRequest> capabilityListRequests = new ArrayList<>();

    /**
     * Constructor
     */
    public DomainCapabilityListRequest()
    {
    }

    /**
     * Constructor.
     *
     * Create instance with one {@link CapabilityListRequest} with {@code capabilityType} set.
     *
     * @param capabilityType to be set
     */
    public DomainCapabilityListRequest(DomainCapability.Type capabilityType)
    {
        addCapabilityListRequest(new CapabilityListRequest(capabilityType));
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

//    public Type getCapabilityType()
//    {
//        return capabilityType;
//    }
//
//    public void setCapabilityType(Type capabilityType)
//    {
//        this.capabilityType = capabilityType;
//    }

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

//    public List<Set<Technology>> getTechnologyVariants()
//    {
//        return technologyVariants;
//    }
//
//    public void setTechnologyVariants(List<Set<Technology>> technologyVariants)
//    {
//        this.technologyVariants = technologyVariants;
//    }

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

//    public Integer getLicenseCount()
//    {
//        return licenseCount;
//    }
//
//    public void setLicenseCount(Integer licenseCount)
//    {
//        this.licenseCount = licenseCount;
//    }

    public List<CapabilityListRequest> getCapabilityListRequests()
    {
        return capabilityListRequests;
    }

    public void setCapabilityListRequests(List<CapabilityListRequest> capabilityListRequests)
    {
        this.capabilityListRequests = capabilityListRequests;
    }

    public void addCapabilityListRequest(CapabilityListRequest capabilityListRequest)
    {
        this.capabilityListRequests.add(capabilityListRequest);
    }

    public List<DomainCapability.Type> getRequestedCapabilitiesTypes() {
        List<DomainCapability.Type> types = new ArrayList<>();
        for (CapabilityListRequest capabilityListRequest : capabilityListRequests) {
            types.add(capabilityListRequest.getCapabilityType());
        }
        return types;
    }

    public void validateForResource()
    {
        if (this.capabilityListRequests.size() != 1) {
            throw new IllegalArgumentException("CapabilityListRequest must be set");
        }
        if (!DomainCapability.Type.RESOURCE.equals(this.capabilityListRequests.get(0).getCapabilityType())) {
            throw new IllegalArgumentException("Request's type must be RESOURCE.");
        }
    }
}