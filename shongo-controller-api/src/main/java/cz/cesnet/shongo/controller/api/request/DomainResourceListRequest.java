package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.controller.api.RoomProviderCapability;

/**
 * Request
 * @author Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class DomainResourceListRequest extends AbstractRequest
{
    String domainId;

    /**
     * {@link ResourceType#RESOURCE} of the resources to be listed.
     */
    ResourceType resourceType;

    public DomainResourceListRequest(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String resourceId) {
        this.domainId = resourceId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public enum ResourceType
    {
        /**
         * Used for resources, which has {@link RoomProviderCapability}.
         */
        ROOM_PROVIDER,

        /**
         * Used for resources, that can be allocated over Inter Domain Protocol.
         */
        RESOURCE
    }
}
