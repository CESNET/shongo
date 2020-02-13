package cz.cesnet.shongo.controller.api.domains.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.api.Domain;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import cz.cesnet.shongo.controller.api.domains.response.DomainCapability;
import cz.cesnet.shongo.controller.api.request.AbstractRequest;
import cz.cesnet.shongo.controller.api.request.DomainCapabilityListRequest;


import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Request for domain capabilities.
 *
 * @author Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class CapabilitySpecificationRequest
{
    /**
     * {@link DomainCapability.Type#RESOURCE} of the resources to be listed.
     */
    @JsonProperty("capabilityType")
    private DomainCapability.Type capabilityType;

    @JsonProperty("technologyVariants")
    private List<Set<Technology>> technologyVariants;

    @JsonProperty("licenseCount")
    private Integer licenseCount;

    public CapabilitySpecificationRequest(DomainCapability.Type capabilityType)
    {
        this.capabilityType = capabilityType;
    }

    @JsonCreator
    public CapabilitySpecificationRequest(@JsonProperty("capabilityType") DomainCapability.Type capabilityType,
                                          @JsonProperty("technologyVariants") List<Set<Technology>> technologyVariants,
                                          @JsonProperty("licenseCount") Integer licenseCount)
    {
        this.capabilityType = capabilityType;
        this.technologyVariants = technologyVariants;
        this.licenseCount = licenseCount;
    }

    public DomainCapability.Type getCapabilityType()
    {
        return capabilityType;
    }

    public void setCapabilityType(DomainCapability.Type capabilityType)
    {
        this.capabilityType = capabilityType;
    }

    public List<Set<Technology>> getTechnologyVariants()
    {
        return technologyVariants;
    }

    public void setTechnologyVariants(List<Set<Technology>> technologyVariants)
    {
        this.technologyVariants = technologyVariants;
    }

    public Integer getLicenseCount()
    {
        return licenseCount;
    }

    public void setLicenseCount(Integer licenseCount)
    {
        this.licenseCount = licenseCount;
    }
}
