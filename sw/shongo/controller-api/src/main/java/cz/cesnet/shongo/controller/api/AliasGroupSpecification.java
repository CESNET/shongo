package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.RoomSetting;
import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.api.annotation.Transient;

import java.util.List;
import java.util.Set;

/**
 * {@link Specification} for multiple {@link AliasSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasGroupSpecification extends Specification
{
    /**
     * {@link AliasSpecification}s for the virtual room.
     */
    public static final String ALIAS_SPECIFICATIONS = "aliasSpecifications";

    /**
     * Constructor.
     */
    public AliasGroupSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param aliasType for which should be added new {@link AliasSpecification} to the {@link #ALIAS_SPECIFICATIONS}
     */
    public AliasGroupSpecification(AliasType aliasType)
    {
        addAliasSpecification(new AliasSpecification(aliasType));
    }

    /**
     * @return {@link #ALIAS_SPECIFICATIONS}
     */
    public List<AliasSpecification> getAliasSpecifications()
    {
        return getPropertyStorage().getCollection(ALIAS_SPECIFICATIONS, List.class);
    }

    /**
     * @param aliasSpecifications sets the {@link #ALIAS_SPECIFICATIONS}
     */
    public void setAliasSpecifications(List<AliasSpecification> aliasSpecifications)
    {
        getPropertyStorage().setCollection(ALIAS_SPECIFICATIONS, aliasSpecifications);
    }

    /**
     * @param aliasSpecification to be added to the {@link #ALIAS_SPECIFICATIONS}
     */
    public void addAliasSpecification(AliasSpecification aliasSpecification)
    {
        getPropertyStorage().addCollectionItem(ALIAS_SPECIFICATIONS, aliasSpecification, List.class);
    }

    /**
     * @param aliasSpecification to be removed from the {@link #ALIAS_SPECIFICATIONS}
     */
    public void removeAliasSpecification(AliasSpecification aliasSpecification)
    {
        getPropertyStorage().removeCollectionItem(ALIAS_SPECIFICATIONS, aliasSpecification);
    }
}
