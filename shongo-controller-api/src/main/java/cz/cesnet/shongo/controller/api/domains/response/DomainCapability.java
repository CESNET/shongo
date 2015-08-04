package cz.cesnet.shongo.controller.api.domains.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceSummary;

/**
 * Represents domain login response
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@JsonIgnoreProperties({"className"})
public class DomainCapability extends IdentifiedComplexType
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

    @JsonProperty("type")
    private String type;

    @JsonProperty("available")
    private Boolean available;

    @Override
    public void setId(String id)
    {
        this.id = id;
    }

    @Override
    public String getId()
    {
        return id;
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

    public void setType(String type)
    {
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Technology getTechnology()
    {
        return technology;
    }

    public boolean isCalendarPublic()
    {
        return calendarPublic;
    }

    public String getCalendarUriKey()
    {
        return calendarUriKey;
    }

    public Integer getLicenseCount()
    {
        return licenseCount;
    }

    public Integer getPrice()
    {
        return price;
    }

    public String getType()
    {
        return type;
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

    public Resource toResource()
    {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setName(name);
        resource.setDescription(description);
        resource.setAllocatable(available);
        resource.setCalendarPublic(calendarPublic);
        resource.setCalendarUriKey(calendarUriKey);

        return resource;
    }

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TYPE = "type";
    public static final String AVAILABLE = "available";
    public static final String CALENDAR_PUBLIC = "calendarPublic";
    public static final String TECHNOLOGY = "technology";
    public static final String LICENSE_COUNT = "licenseCount";
    public static final String PRICE = "price";
    public static final String CALENDAR_URI_KEY = "calendarUriKey";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ID, id);
        dataMap.set(NAME, name);
        dataMap.set(DESCRIPTION, description);
        dataMap.set(TYPE, type);
        dataMap.set(AVAILABLE, available);
        dataMap.set(TECHNOLOGY, technology == null ? null : technology.toString());
        dataMap.set(LICENSE_COUNT, licenseCount);
        dataMap.set(PRICE, price);
        dataMap.set(CALENDAR_PUBLIC, calendarPublic);
        dataMap.set(CALENDAR_URI_KEY, calendarUriKey);

        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        id = dataMap.getStringRequired(ID);
        name = dataMap.getStringRequired(NAME);
        description = dataMap.getString(DESCRIPTION);
        type = dataMap.getString(TYPE);
        available = dataMap.getBoolean(AVAILABLE);
        technology = Technology.valueOf(dataMap.getString(TECHNOLOGY));
        licenseCount = dataMap.getInteger(LICENSE_COUNT);
        price = dataMap.getInteger(PRICE);
        calendarPublic = dataMap.getBoolean(CALENDAR_PUBLIC);
        if (calendarPublic) {
            calendarUriKey = dataMap.getStringRequired(CALENDAR_URI_KEY);
        }
    }
}
