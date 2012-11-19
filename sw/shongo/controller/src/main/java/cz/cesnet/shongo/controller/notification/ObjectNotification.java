package cz.cesnet.shongo.controller.notification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents {@link Notification} of an object with properties.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ObjectNotification extends Notification
{
    /**
     * List of property names in the right order.
     */
    private List<String> propertyNames = new ArrayList<String>();

    /**
     * Map of property values by property names to which they belongs.
     */
    private Map<String, Object> propertyValueByName = new HashMap<String, Object>();

    /**
     * Add new property to the {@link ObjectNotification}.
     *
     * @param propertyName  specifies the name of the property
     * @param propertyValue specifies the value of the property
     */
    public void addProperty(String propertyName, Object propertyValue)
    {
        propertyNames.add(propertyName);
        propertyValueByName.put(propertyName, propertyValue);
    }

    /**
     * @return {@link #propertyNames}
     */
    public List<String> getPropertyNames()
    {
        return propertyNames;
    }

    /**
     * @param propertyName for which the value should be returned
     * @return value of given {@code propertyName}
     */
    public Object getPropertyValue(String propertyName)
    {
        return propertyValueByName.get(propertyName);
    }
}
