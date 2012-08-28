package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;

/**
 * Represents a special type of {@link AliasProviderCapability} which
 * can allocate aliases from an alias range.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RangeAliasProviderCapability extends AliasProviderCapability
{
    /**
     * Range starting value.
     */
    private String startValue;

    /**
     * Range end value.
     */
    private String endValue;

    /**
     * @return {@link #startValue}
     */
    @Column
    public String getStartValue()
    {
        return startValue;
    }

    /**
     * @param startValue sets the {@link #startValue}
     */
    public void setStartValue(String startValue)
    {
        this.startValue = startValue;
    }

    /**
     * @return {@link #endValue}
     */
    @Column
    public String getEndValue()
    {
        return endValue;
    }

    /**
     * @param endValue sets the {@link #endValue}
     */
    public void setEndValue(String endValue)
    {
        this.endValue = endValue;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.RangeAliasProviderCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.RangeAliasProviderCapability apiRangeAliasProvider =
                (cz.cesnet.shongo.controller.api.RangeAliasProviderCapability) api;
        apiRangeAliasProvider.setTechnology(getTechnology());
        apiRangeAliasProvider.setType(getType());
        apiRangeAliasProvider.setStartValue(getStartValue());
        apiRangeAliasProvider.setEndValue(getEndValue());
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.RangeAliasProviderCapability apiRangeAliasProvider =
                (cz.cesnet.shongo.controller.api.RangeAliasProviderCapability) api;
        if (apiRangeAliasProvider.isPropertyFilled(apiRangeAliasProvider.TECHNOLOGY)) {
            setTechnology(apiRangeAliasProvider.getTechnology());
        }
        if (apiRangeAliasProvider.isPropertyFilled(apiRangeAliasProvider.TYPE)) {
            setType(apiRangeAliasProvider.getType());
        }
        if (apiRangeAliasProvider.isPropertyFilled(apiRangeAliasProvider.START_VALUE)) {
            setStartValue(apiRangeAliasProvider.getStartValue());
        }
        if (apiRangeAliasProvider.isPropertyFilled(apiRangeAliasProvider.END_VALUE)) {
            setEndValue(apiRangeAliasProvider.getEndValue());
        }
        super.fromApi(api, entityManager);
    }
}
