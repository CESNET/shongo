package cz.cesnet.shongo.client.web.models;



import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.FilterType;
import cz.cesnet.shongo.controller.api.ValueProvider;
import cz.cesnet.shongo.controller.api.ValueProviderCapability;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class ValueProviderCapabilityModel extends DeviceCapability{

    private String valueProviderType;

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

    public ValueProviderCapability toApi() {
        ValueProviderCapability valueProviderCapability = new ValueProviderCapability();
        switch (valueProviderType) {
            case "pattern":
                ValueProvider.Pattern patternValueProvider = new ValueProvider.Pattern();
                patternValueProvider.setAllowAnyRequestedValue(getAllowAnyRequestedValue());
                //add patterns
                for (String pattern : getPatterns()) {
                    patternValueProvider.addPattern(pattern);
                }
                valueProviderCapability.setValueProvider(patternValueProvider);
                break;

            case "filtered":
                ValueProvider.Filtered filteredValueProvider = new ValueProvider.Filtered();
                filteredValueProvider.setValueProvider(getFilteredResourceId());
                filteredValueProvider.setFilterType(FilterType.CONVERT_TO_URL);
                valueProviderCapability.setValueProvider(filteredValueProvider);
                //TODO finish the filter type
                break;

            default:
                throw new TodoImplementException();

        }

        return valueProviderCapability;
    }
}
