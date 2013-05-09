package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.annotation.Required;

import java.util.Set;

/**
 * Represents a {@link Specification} for value(s) from {@link ValueProvider}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ValueSpecification extends Specification
{
    /**
     * {@link Resource} with {@link ValueProvider} from which the value(s) should be allocated.
     */
    public static final String RESOURCE_ID = "resourceId";

    /**
     * Restricts {@link cz.cesnet.shongo.AliasType} for allocation of {@link cz.cesnet.shongo.api.Alias}.
     */
    public static final String VALUES = "values";

    /**
     * Constructor.
     */
    public ValueSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param resourceId sets the {@link #RESOURCE_ID}
     */
    public ValueSpecification(String resourceId)
    {
        setResourceId(resourceId);
    }

    /**
     * Constructor.
     *
     * @param resourceId sets the {@link #RESOURCE_ID}
     * @param value to be added to the {@link #VALUES}
     */
    public ValueSpecification(String resourceId, String value)
    {
        setResourceId(resourceId);
        addValue(value);
    }

    /**
     * @return {@link #RESOURCE_ID}
     */
    @Required
    public String getResourceId()
    {
        return getPropertyStorage().getValue(RESOURCE_ID);
    }

    /**
     * @param resourceId sets the {@link #RESOURCE_ID}
     */
    public void setResourceId(String resourceId)
    {
        getPropertyStorage().setValue(RESOURCE_ID, resourceId);
    }

    /**
     * @return {@link #VALUES}
     */
    public Set<String> getValues()
    {
        return getPropertyStorage().getCollection(VALUES, Set.class);
    }

    /**
     * @param values sets the {@link #VALUES}
     */
    public void setValues(Set<String> values)
    {
        getPropertyStorage().setCollection(VALUES, values);
    }

    /**
     * @param value to be added to the {@link #VALUES}
     */
    public void addValue(String value)
    {
        getPropertyStorage().addCollectionItem(VALUES, value, Set.class);
    }

    /**
     * @param value to be removed from the {@link #VALUES}
     */
    public void removeValue(String value)
    {
        getPropertyStorage().removeCollectionItem(VALUES, value);
    }
}
