package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.api.DataMap;

/**
 * Represents a base information about controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Controller extends AbstractComplexType
{
    /**
     * Maximum database field length of enum columns.
     */
    public static final int USER_ID_COLUMN_LENGTH = 255;

    /**
     * Controller domain.
     */
    private Domain domain;

    /**
     * @return {@link #domain}
     */
    public Domain getDomain()
    {
        return domain;
    }

    /**
     * @param domain sets the {@link #domain}
     */
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    private static final String DOMAIN = "domain";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(DOMAIN, domain);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        domain = dataMap.getComplexType(DOMAIN, Domain.class);
    }
}
