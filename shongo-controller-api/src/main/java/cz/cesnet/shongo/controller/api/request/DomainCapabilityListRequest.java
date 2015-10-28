package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.Interval;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * List request of domain capabilities.
 *
 * @author Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class DomainCapabilityListRequest extends AbstractRequest
{
    /**
     * {@link Type#RESOURCE} of the resources to be listed.
     */
    private Type capabilityType;

    private List<Set<Technology>> technologyVariants;

    private Interval interval;

    private ObjectPermission permission;

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

    public DomainCapabilityListRequest(Type capabilityType)
    {
        this.capabilityType = capabilityType;
    }

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

    public Type getCapabilityType()
    {
        return capabilityType;
    }

    public void setCapabilityType(Type capabilityType)
    {
        this.capabilityType = capabilityType;
    }

    public String getResourceType()
    {
        return resourceType;
    }

    public void setResourceType(String resourceType)
    {
        this.resourceType = resourceType;
    }

    public Interval getInterval() {
        return interval;
    }

    public void setInterval(Interval interval) {
        this.interval = interval;
    }

    public List<Set<Technology>> getTechnologyVariants()
    {
        return technologyVariants;
    }

    public void setTechnologyVariants(List<Set<Technology>> technologyVariants)
    {
        this.technologyVariants = technologyVariants;
    }

    public ObjectPermission getPermission()
    {
        return permission;
    }

    public void setPermission(ObjectPermission permission)
    {
        this.permission = permission;
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

    public enum Type
    {
        /**
         * Used for resources, which has {@link RoomProviderCapability}.
         */
        VIRTUAL_ROOM,

        /**
         * Used for resources, that can be allocated over Inter Domain Protocol.
         */
        RESOURCE
    }
}
