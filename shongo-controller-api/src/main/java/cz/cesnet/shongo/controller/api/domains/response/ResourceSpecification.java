package cz.cesnet.shongo.controller.api.domains.response;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a resource specification for foreign {@link Reservation}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class ResourceSpecification extends ForeignSpecification
{
    @JsonProperty("foreignResourceId")
    private String foreignResourceId;

    @JsonProperty("resourceName")
    private String resourceName;

    @JsonProperty("resourceDescription")
    private String resourceDescription;

    @JsonCreator
    public ResourceSpecification(@JsonProperty("foreignResourceId") String foreignResourceId,
                                 @JsonProperty("resourceName") String resourceName,
                                 @JsonProperty("resourceDescription") String resourceDescription)
    {
        this.foreignResourceId = foreignResourceId;
        this.resourceName = resourceName;
        this.resourceDescription = resourceDescription;
    }

    public ResourceSpecification(String foreignResourceId)
    {
        this.foreignResourceId = foreignResourceId;
    }

    public String getForeignResourceId()
    {
        return foreignResourceId;
    }

    public void setForeignResourceId(String foreignResourceId)
    {
        this.foreignResourceId = foreignResourceId;
    }

    public String getResourceName()
    {
        return resourceName;
    }

    public void setResourceName(String resourceName)
    {
        this.resourceName = resourceName;
    }

    public String getResourceDescription()
    {
        return resourceDescription;
    }

    public void setResourceDescription(String resourceDescription)
    {
        this.resourceDescription = resourceDescription;
    }

}
