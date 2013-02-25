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
    public static final String ALIASES = "aliases";

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
     * @param aliasType for which should be added new {@link AliasSpecification} to the {@link #ALIASES}
     */
    public AliasSetSpecification(AliasType aliasType)
    {
        addAlias(new AliasSpecification(aliasType));
    }

    /**
     * Constructor.
     *
     * @param aliasTypes for which should be added new {@link AliasSpecification}s to the {@link #ALIASES}
     */
    public AliasSetSpecification(AliasType[] aliasTypes)
    {
        for (AliasType aliasType : aliasTypes) {
            addAlias(new AliasSpecification(aliasType));
        }
    }

    /**
     * @return {@link #ALIASES}
     */
    public List<AliasSpecification> getAliases()
    {
        return getPropertyStorage().getCollection(ALIASES, List.class);
    }

    /**
     * @param aliasSpecifications sets the {@link #ALIASES}
     */
    public void setAliases(List<AliasSpecification> aliasSpecifications)
    {
        getPropertyStorage().setCollection(ALIASES, aliasSpecifications);
    }

    /**
     * @param aliasSpecification to be added to the {@link #ALIASES}
     */
    public void addAlias(AliasSpecification aliasSpecification)
    {
        getPropertyStorage().addCollectionItem(ALIASES, aliasSpecification, List.class);
    }

    /**
     * @param aliasSpecification to be removed from the {@link #ALIASES}
     */
    public void removeAlias(AliasSpecification aliasSpecification)
    {
        getPropertyStorage().removeCollectionItem(ALIASES, aliasSpecification);
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
