package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;

/**
 * Represents an available {@link Alias} in a {@link Resource} with {@link AliasProviderCapability}.
 */
public class AvailableAlias
{
    /**
     * {@link AliasProviderCapability} in which the {@link #aliasValue} is available.
     */
    private AliasProviderCapability aliasProviderCapability;

    /**
     * Available {@link Alias} value.
     */
    private String aliasValue;

    /**
     *  Provided {@link AliasReservation} by which the {@link AvailableAlias} is already allocated.
     */
    private AliasReservation aliasReservation;

    /**
     * @return {@link #aliasProviderCapability}
     */
    public AliasProviderCapability getAliasProviderCapability()
    {
        return aliasProviderCapability;
    }

    /**
     * @param aliasProviderCapability sets the {@link #aliasProviderCapability}
     */
    public void setAliasProviderCapability(AliasProviderCapability aliasProviderCapability)
    {
        this.aliasProviderCapability = aliasProviderCapability;
    }

    /**
     * @return {@link #aliasValue}
     */
    public String getAliasValue()
    {
        return aliasValue;
    }

    /**
     * @param aliasValue sets the {@link #aliasValue}
     */
    public void setAliasValue(String aliasValue)
    {
        this.aliasValue = aliasValue;
    }

    /**
     * @return {@link #aliasReservation}
     */
    public AliasReservation getAliasReservation()
    {
        return aliasReservation;
    }

    /**
     * @param aliasReservation {@link #aliasReservation}
     */
    public void setAliasReservation(AliasReservation aliasReservation)
    {
        this.aliasReservation = aliasReservation;
    }
}
