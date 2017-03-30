package cz.cesnet.shongo.client.web.models;



import java.util.LinkedList;
import java.util.List;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class ValueProviderCapabilityModel {

    String valueProviderType;

    private List<String> patterns = new LinkedList<String>();

    private Boolean allowAnyRequestedValue;

    private String ownerResourceId;

    private String filteredResourceId;

    public ValueProviderCapabilityModel() {
    }

    public String getValueProviderType()
    {
        return valueProviderType;
    }

    public void setValueProviderType(String valueProviderType)
    {
        this.valueProviderType = valueProviderType;
    }

    public List<String> getPatterns()
    {
        return patterns;
    }

    public void setPatterns(List<String> patterns)
    {
        this.patterns = patterns;
    }

    public Boolean getAllowAnyRequestedValue()
    {
        return allowAnyRequestedValue;
    }

    public void setAllowAnyRequestedValue(Boolean allowAnyRequestedValue)
    {
        this.allowAnyRequestedValue = allowAnyRequestedValue;
    }

    public String getOwnerResourceId()
    {
        return ownerResourceId;
    }

    public void setOwnerResourceId(String resourceId)
    {
        this.ownerResourceId = resourceId;
    }

    public String getFilteredResourceId()
    {
        return filteredResourceId;
    }

    public void setFilteredResourceId(String filteredResourceId)
    {
        this.filteredResourceId = filteredResourceId;
    }
}
