package cz.cesnet.shongo.controller.api.domains.response;


import cz.cesnet.shongo.Technology;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a room specification for foreign {@link Reservation}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class RoomSpecification extends ForeignSpecification
{
    @JsonProperty("licenseCount")
    private Integer licenseCount;

    @JsonProperty("meetingName")
    private String meetingName;

    @JsonProperty("technologies")
    private Set<Technology> technologies;

    @JsonCreator
    public RoomSpecification(@JsonProperty("licenseCount") Integer licenseCount,
                             @JsonProperty("meetingName") String meetingName,
                             @JsonProperty("technologies") Set<Technology> technologies)
    {
        this.licenseCount = licenseCount;
        this.meetingName = meetingName;
        this.technologies = technologies;
    }

    public RoomSpecification()
    {
    }

    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    public void addTechnology(Technology technology)
    {
        if (technologies == null) {
            technologies = new HashSet<>();
        }
        this.technologies.add(technology);
    }

    public String getMeetingName()
    {
        return meetingName;
    }

    public void setMeetingName(String meetingName)
    {
        this.meetingName = meetingName;
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
