package cz.cesnet.shongo.report;

import cz.cesnet.shongo.api.util.ClassHelper;
import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.api.util.Property;
import cz.cesnet.shongo.api.util.TypeFlags;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a XML-RPC fault string with it's parameters which can be converted to/from string.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ApiFaultString
{
    /**
     * String message.
     */
    private String message;

    /**
     * Parameters for the message.
     */
    private Map<String, String> parameters = new HashMap<String, String>();

    /**
     * Constructor.
     */
    public ApiFaultString()
    {
    }

    /**
     * Constructor.
     *
     * @param message to be constructed from
     */
    public ApiFaultString(String message)
    {
        fromString(message);
    }

    /**
     * @return {@link #message}
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * @param message sets the {@link #message}
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * @param name
     * @return value of parameter with given {@code name}
     */
    public String getParameter(String name)
    {
        return parameters.get(name);
    }

    /**
     * Set {@code value} of parameter with given {@code name}.
     *
     * @param name
     * @param value
     */
    public void setParameter(String name, Object value)
    {
        String string;
        if (value == null) {
            string = null;
        }
        else if (value instanceof String) {
            string = (String) value;
        }
        else if (TypeFlags.isAtomic(TypeFlags.get(value))) {
            string = value.toString();
        }
        else if (value instanceof Class) {
            string = ClassHelper.getClassShortName((Class) value);
        }
        else if (value instanceof Report) {
            // TODO: Implement command failure serialization
            return;
        }
        else {
            throw new IllegalArgumentException(value.getClass().getCanonicalName());
        }
        parameters.put(name, string);
    }

    /**
     * Pattern for parsing string message.
     */
    private static final Pattern PATTERN_MESSAGE = Pattern.compile("<message>(.*?)</message>");

    /**
     * Pattern for parsing single parameter from message.
     */
    private static final Pattern PATTERN_PARAMETER = Pattern.compile("<param name='([^']*)'>(.*?)</param>");

    /**
     * Parse message and parameters from given {@code message} string.
     *
     * @param message
     */
    public void fromString(String message)
    {
        Matcher messageMatcher = PATTERN_MESSAGE.matcher(message);
        if (messageMatcher.find()) {
            setMessage(messageMatcher.group(1));

            Matcher parameterMatcher = PATTERN_PARAMETER.matcher(message);
            while (parameterMatcher.find()) {
                setParameter(parameterMatcher.group(1), parameterMatcher.group(2));
            }
        }
        else {
            setMessage(message);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<message>");
        stringBuilder.append(message);
        stringBuilder.append("</message>");
        for (String name : parameters.keySet()) {
            String value = parameters.get(name);
            stringBuilder.append("<param name='");
            stringBuilder.append(name);
            stringBuilder.append("'>");
            stringBuilder.append(value);
            stringBuilder.append("</param>");
        }
        return stringBuilder.toString();
    }

    /**
     * Load {@link ApiFaultString} from given {@code fault}.
     *
     * @param fault to load from
     */
    public static ApiFaultString fromFault(ApiFault fault)
    {
        ApiFaultString content = new ApiFaultString();
        content.setMessage(fault.getFaultString());
        Collection<String> propertyNames = Property.getClassHierarchyPropertyNames(fault.getClass(), Exception.class);
        for (String propertyName : propertyNames) {
            if (propertyName.equals("message") || propertyName.equals("exception")
                    || propertyName.equals("faultCode") || propertyName.equals("visibleToDomainAdminViaEmail")
                    || propertyName.equals("faultString")) {
                continue;
            }
            content.setParameter(propertyName, Property.getPropertyValue(fault, propertyName));
        }
        return content;
    }

    /**
     * Store {@link ApiFaultString} to given {@code fault}.
     *
     * @param fault to store to
     */
    public void toFault(ApiFault fault)
    {
        Collection<String> propertyNames = Property.getClassHierarchyPropertyNames(fault.getClass());
        try {
            for (String propertyName : propertyNames) {
                String propertyValue = getParameter(propertyName);
                if (propertyValue != null) {
                    Property property = Property.getProperty(fault.getClass(), propertyName);
                    Object propertyConvertedValue = Converter.convert(propertyValue, property);
                    Property.setPropertyValue(fault, propertyName, propertyConvertedValue, true);
                }
            }
        }
        catch (Exception exception) {
            throw new RuntimeException(
                    "Cannot set property value to fault '" + fault.getClass().getCanonicalName() + "'!", exception);
        }
    }
}
