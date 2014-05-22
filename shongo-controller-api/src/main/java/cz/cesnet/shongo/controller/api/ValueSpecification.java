package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

import java.util.HashSet;
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
    private String resourceId;


    /**
     * Restricts {@link cz.cesnet.shongo.AliasType} for allocation of {@link cz.cesnet.shongo.api.Alias}.
     */
    private Set<String> values = new HashSet<String>();

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
     * @param value      to be added to the {@link #VALUES}
     */
    public ValueSpecification(String resourceId, String value)
    {
        setResourceId(resourceId);
        addValue(value);
    }

    /**
     * @return {@link #resourceId}
     */
    public String getResourceId()
    {
        return resourceId;
    }

    /**
     * @param resourceId sets the {@link #resourceId}
     */
    public void setResourceId(String resourceId)
    {
        this.resourceId = resourceId;
    }

    /**
     * @return {@link #values}
     */
    public Set<String> getValues()
    {
        return values;
    }

    /**
     * @param values sets the {@link #values}
     */
    public void setValues(Set<String> values)
    {
        this.values = values;
    }

    /**
     * @param value to be added to the {@link #values}
     */
    public void addValue(String value)
    {
        values.add(value);
    }

    /**
     * @param value to be removed from the {@link #values}
     */
    public void removeValue(String value)
    {
        values.remove(value);
    }


    public static final String RESOURCE_ID = "resourceId";
    public static final String VALUES = "values";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(VALUES, values);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getStringRequired(RESOURCE_ID);
        values = dataMap.getStringSet(VALUES, DEFAULT_COLUMN_LENGTH);
    }
}
