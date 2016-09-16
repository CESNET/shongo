package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import org.joda.time.Interval;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Request
 * @author Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class DomainCapabilityListRequest extends AbstractRequest
{
    private Domain domain;

    /**
     * {@link Type#RESOURCE} of the resources to be listed.
     */
    private Type type;

    private Technology technology;

    private Interval interval;

    private ObjectPermission permission;

    private Set<String> resourceIds = new HashSet<>();

    public DomainCapabilityListRequest(Type type)
    {
        this.type = type;
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Interval getInterval() {
        return interval;
    }

    public void setInterval(Interval interval) {
        this.interval = interval;
    }

    public Technology getTechnology() {
        return technology;
    }

    public void setTechnology(Technology technology) {
        this.technology = technology;
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
