package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.api.DataMap;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link Specification} for multiple {@link AliasSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasSetSpecification extends Specification
{
    /**
     * {@link AliasSpecification}s for the virtual room.
     */
    private List<AliasSpecification> aliasSpecifications = new LinkedList<AliasSpecification>();

    /**
     * Constructor.
     */
    public AliasSetSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param aliasType for which should be added new {@link AliasSpecification} to the {@link #aliasSpecifications}
     */
    public AliasSetSpecification(AliasType aliasType)
    {
        addAlias(new AliasSpecification(aliasType));
    }

    /**
     * Constructor.
     *
     * @param aliasTypes for which should be added new {@link AliasSpecification}s to the {@link #aliasSpecifications}
     */
    public AliasSetSpecification(AliasType[] aliasTypes)
    {
        for (AliasType aliasType : aliasTypes) {
            addAlias(new AliasSpecification(aliasType));
        }
    }

    /**
     * @return {@link #aliasSpecifications}
     */
    public List<AliasSpecification> getAliases()
    {
        return aliasSpecifications;
    }

    /**
     * @param aliasSpecifications sets the {@link #aliasSpecifications}
     */
    public void setAliases(List<AliasSpecification> aliasSpecifications)
    {
        this.aliasSpecifications = aliasSpecifications;
    }

    /**
     * @param aliasSpecification to be added to the {@link #aliasSpecifications}
     */
    public void addAlias(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.add(aliasSpecification);
    }

    /**
     * @param aliasSpecification to be removed from the {@link #aliasSpecifications}
     */
    public void removeAlias(AliasSpecification aliasSpecification)
    {
        aliasSpecifications.remove(aliasSpecification);
    }

    public static final String ALIAS_SPECIFICATIONS = "aliasSpecifications";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ALIAS_SPECIFICATIONS, aliasSpecifications);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        aliasSpecifications = dataMap.getList(ALIAS_SPECIFICATIONS, AliasSpecification.class);
    }
}
