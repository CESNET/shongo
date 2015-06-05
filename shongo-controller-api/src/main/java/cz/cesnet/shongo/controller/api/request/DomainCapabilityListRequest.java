package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import org.joda.time.Interval;

/**
 * Request
 * @author Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class DomainCapabilityListRequest extends AbstractRequest
{
    private String domainId;

    /**
     * {@link Type#RESOURCE} of the resources to be listed.
     */
    private Type type;

    private Technology technology;

    private Interval interval;

    public DomainCapabilityListRequest(Type type)
    {
        this.type = type;
    }

    public DomainCapabilityListRequest(String domainId) {
        this.domainId = domainId;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String resourceId) {
        this.domainId = resourceId;
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
