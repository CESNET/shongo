package cz.cesnet.shongo.controller.api.domains.response;

import cz.cesnet.shongo.Technology;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents domain login response
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DomainResource {
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

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTechnology(Technology technology) {
        this.technology = technology;
    }

    public void setCalendarPublic(boolean calendarPublic) {
        this.calendarPublic = calendarPublic;
    }

    public void setCalendarUriKey(String calendarUriKey) {
        this.calendarUriKey = calendarUriKey;
    }

    public void setLicenseCount(Integer licenseCount) {
        this.licenseCount = licenseCount;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
}
