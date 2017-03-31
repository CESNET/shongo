package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.api.AliasProviderCapability;
import cz.cesnet.shongo.controller.api.ValueProvider;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class AliasProviderCapabilityModel {

    private String valueProviderType;

    private List<String> patterns = new LinkedList<String>();

    private Boolean allowAnyRequestedValue;

    private String ownerResourceId;

    private String filteredResourceId;

    private Boolean restrictedToResource;

    private String remoteResourceString;

    private List<Alias> aliases = new LinkedList<Alias>();

    public String getRemoteResourceString() {
        return remoteResourceString;
    }

    public void setRemoteResourceString(String remoteResourceString) {
        this.remoteResourceString = remoteResourceString;
    }

    public List<Alias> getAliases() {
        return aliases;
    }

    public void setAliases(List<Alias> aliases) {
        this.aliases = aliases;
    }


    public Boolean getRestrictedToResource() {
        return restrictedToResource;
    }

    public void setRestrictedToResource(Boolean restrictedToResource) {
        this.restrictedToResource = restrictedToResource;
    }

    public AliasProviderCapabilityModel() {
    }

    public String getValueProviderType() {
        return valueProviderType;
    }

    public void setValueProviderType(String valueProviderType) {
        this.valueProviderType = valueProviderType;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public Boolean getAllowAnyRequestedValue() {
        return allowAnyRequestedValue;
    }

    public void setAllowAnyRequestedValue(Boolean allowAnyRequestedValue) {
        this.allowAnyRequestedValue = allowAnyRequestedValue;
    }

    public String getOwnerResourceId() {
        return ownerResourceId;
    }

    public void setOwnerResourceId(String ownerResourceId) {
        this.ownerResourceId = ownerResourceId;
    }

    public String getFilteredResourceId() {
        return filteredResourceId;
    }

    public void setFilteredResourceId(String filteredResourceId) {
        this.filteredResourceId = filteredResourceId;
    }

    public AliasProviderCapability toApi () {
        AliasProviderCapability newAliasProvider = new AliasProviderCapability();

        //Set value provider based on type
        switch (getValueProviderType()) {
            case "resource":
                newAliasProvider.setValueProvider(getRemoteResourceString());
                break;

            case "pattern":
                ValueProvider.Pattern valueProvider= new ValueProvider.Pattern();
                for (String pattern: getPatterns()) {
                    valueProvider.addPattern(pattern);
                }
                newAliasProvider.setValueProvider(valueProvider);
                break;

            case "filtered":
                ValueProvider.Filtered filteredValueProvider = new ValueProvider.Filtered();
                filteredValueProvider.setValueProvider(getFilteredResourceId());
                newAliasProvider.setValueProvider(filteredValueProvider);
                break;

            default:
                throw new IllegalArgumentException("Value provider type not implemented.");
        }

        //Set aliases
        for (Alias alias: getAliases()) {
            newAliasProvider.addAlias(alias);
        }

        //Set restricted to resource attribute
        newAliasProvider.setRestrictedToResource(getRestrictedToResource());

        return newAliasProvider;
    }

}
