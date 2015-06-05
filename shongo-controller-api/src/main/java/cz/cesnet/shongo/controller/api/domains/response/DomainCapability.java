package cz.cesnet.shongo.controller.api.domains.response;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.ResourceSummary;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents domain login response
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainCapability
{
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("technology")
    private Technology technology;

    @JsonProperty("calendarPublic")
    private boolean calendarPublic;

    @JsonProperty("calendarUriKey")
    private String calendarUriKey;

    @JsonProperty("licenseCount")
    private Integer licenseCount;

    @JsonProperty("price")
    private Integer price;

    @JsonProperty("available")
    private Boolean available;

    public void setId(String id)
    {
        this.id = id;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setTechnology(Technology technology)
    {
        this.technology = technology;
    }

    public void setCalendarPublic(boolean calendarPublic)
    {
        this.calendarPublic = calendarPublic;
    }

    public void setCalendarUriKey(String calendarUriKey)
    {
        this.calendarUriKey = calendarUriKey;
    }

    public void setLicenseCount(Integer licenseCount)
    {
        this.licenseCount = licenseCount;
    }

    public void setPrice(Integer price)
    {
        this.price = price;
    }

    public void setAvailable(Boolean available)
    {
        this.available = available;
    }

    public Boolean getAvailable()
    {
        return (available == null ? true : available);
    }

    public ResourceSummary toResourceSummary()
    {
        ResourceSummary resourceSummary = new ResourceSummary();
        resourceSummary.setId(id);
        resourceSummary.setName(name);
        resourceSummary.setDescription(description);
        resourceSummary.setAllocatable(available);
        resourceSummary.setCalendarPublic(calendarPublic);
        resourceSummary.setCalendarUriKey(calendarUriKey);

        return resourceSummary;
    }
}