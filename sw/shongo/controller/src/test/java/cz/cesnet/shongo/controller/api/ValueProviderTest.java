package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.FilterType;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * Tests for {@link cz.cesnet.shongo.controller.booking.value.provider.ValueProvider}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ValueProviderTest extends AbstractControllerTest
{
    @Test
    public void testDeletion() throws Exception
    {
        Resource valueProvider = new Resource();
        valueProvider.setName("valueProvider");
        valueProvider.setAllocatable(true);
        valueProvider.addCapability(new ValueProviderCapability("{hash}").withAllowedAnyRequestedValue());
        String valueProviderId = createResource(SECURITY_TOKEN, valueProvider);
        Assert.assertEquals(1, getAliasProviderCount());

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability();
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, valueProviderId));
        aliasProviderCapability.addAlias(new Alias(AliasType.ROOM_NAME, "{value}"));
        aliasProvider.addCapability(aliasProviderCapability);
        String aliasProviderId = createResource(SECURITY_TOKEN, aliasProvider);
        Assert.assertEquals(2, getAliasProviderCount());

        // Test remove value provider from alias provider by setting resourceId
        aliasProvider = getResourceService().getResource(SECURITY_TOKEN, aliasProviderId);
        aliasProviderCapability = (AliasProviderCapability) aliasProvider.getCapabilities().get(0);
        aliasProviderCapability.setValueProvider(valueProviderId);
        getResourceService().modifyResource(SECURITY_TOKEN, aliasProvider);
        Assert.assertEquals(1, getAliasProviderCount());

        // Create new filtered value provider with pattern value provider in alias provider
        aliasProvider = getResourceService().getResource(SECURITY_TOKEN, aliasProviderId);
        aliasProviderCapability = (AliasProviderCapability) aliasProvider.getCapabilities().get(0);
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, new ValueProvider.Pattern("{hash}")));
        getResourceService().modifyResource(SECURITY_TOKEN, aliasProvider);
        Assert.assertEquals(3, getAliasProviderCount());

        // Test remove value provider from filtered value provider by setting resourceId
        aliasProvider = getResourceService().getResource(SECURITY_TOKEN, aliasProviderId);
        aliasProviderCapability = (AliasProviderCapability) aliasProvider.getCapabilities().get(0);
        ((ValueProvider.Filtered) aliasProviderCapability.getValueProvider()).setValueProvider(valueProviderId);
        getResourceService().modifyResource(SECURITY_TOKEN, aliasProvider);
        Assert.assertEquals(2, getAliasProviderCount());

        // Test remove value provider from alias provider by setting new value provider
        aliasProvider = getResourceService().getResource(SECURITY_TOKEN, aliasProviderId);
        aliasProviderCapability = (AliasProviderCapability) aliasProvider.getCapabilities().get(0);
        aliasProviderCapability.setValueProvider(new ValueProvider.Pattern("{hash}"));
        getResourceService().modifyResource(SECURITY_TOKEN, aliasProvider);
        Assert.assertEquals(2, getAliasProviderCount());

        // Create new filtered value provider with pattern value provider in alias provider
        aliasProvider = getResourceService().getResource(SECURITY_TOKEN, aliasProviderId);
        aliasProviderCapability = (AliasProviderCapability) aliasProvider.getCapabilities().get(0);
        aliasProviderCapability.setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, new ValueProvider.Pattern("{hash}")));
        getResourceService().modifyResource(SECURITY_TOKEN, aliasProvider);
        Assert.assertEquals(3, getAliasProviderCount());

        // Test remove value provider from filtered value provider by setting new value provider
        aliasProvider = getResourceService().getResource(SECURITY_TOKEN, aliasProviderId);
        aliasProviderCapability = (AliasProviderCapability) aliasProvider.getCapabilities().get(0);
        ((ValueProvider.Filtered) aliasProviderCapability.getValueProvider()).setValueProvider(
                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL,
                        new ValueProvider.Filtered(FilterType.CONVERT_TO_URL,
                                new ValueProvider.Filtered(FilterType.CONVERT_TO_URL, valueProviderId))));
        getResourceService().modifyResource(SECURITY_TOKEN, aliasProvider);
        Assert.assertEquals(5, getAliasProviderCount());

        // Test remove multiple value providers from alias provider by setting resourceId
        aliasProvider = getResourceService().getResource(SECURITY_TOKEN, aliasProviderId);
        aliasProviderCapability = (AliasProviderCapability) aliasProvider.getCapabilities().get(0);
        aliasProviderCapability.setValueProvider(valueProviderId);
        getResourceService().modifyResource(SECURITY_TOKEN, aliasProvider);
        Assert.assertEquals(1, getAliasProviderCount());
    }

    /**
     * @return number of all persisted {@link ValueProvider}s
     */
    private int getAliasProviderCount()
    {
        EntityManager entityManager = createEntityManager();
        long result = (Long) entityManager.createQuery("SELECT count(valueProvider) FROM ValueProvider valueProvider")
                .getSingleResult();
        entityManager.close();
        return (int) result;
    }
}
