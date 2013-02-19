package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;

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
    public static final String ALIAS_SPECIFICATIONS = "aliasSpecifications";

    /**
     * Specifies whether alias reservations should share executable.
     */
    public static final String SHARED_EXECUTABLE = "sharedExecutable";

    /**
     * Constructor.
     */
    public AliasSetSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param aliasType for which should be added new {@link AliasSpecification} to the {@link #ALIAS_SPECIFICATIONS}
     */
    public AliasSetSpecification(AliasType aliasType)
    {
        addAliasSpecification(new AliasSpecification(aliasType));
    }

    /**
     * Constructor.
     *
     * @param aliasTypes for which should be added new {@link AliasSpecification}s to the {@link #ALIAS_SPECIFICATIONS}
     */
    public AliasSetSpecification(AliasType[] aliasTypes)
    {
        for (AliasType aliasType : aliasTypes) {
            addAliasSpecification(new AliasSpecification(aliasType));
        }
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

    /**
     * @return {@link #SHARED_EXECUTABLE}
     */
    public Boolean getSharedExecutable()
    {
        Boolean sharedExecutable = getPropertyStorage().getValue(SHARED_EXECUTABLE);
        return (sharedExecutable != null ? sharedExecutable : Boolean.FALSE);
    }

    /**
     * @param sharedExecutable sets the {@link #SHARED_EXECUTABLE}
     */
    public void setSharedExecutable(Boolean sharedExecutable)
    {
        getPropertyStorage().setValue(SHARED_EXECUTABLE, sharedExecutable);
    }
}
