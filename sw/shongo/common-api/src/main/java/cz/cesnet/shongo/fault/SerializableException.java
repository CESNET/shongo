package cz.cesnet.shongo.fault;

import cz.cesnet.shongo.api.util.ClassHelper;
import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.api.util.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface specifying that {@link Fault} {@link Exception} can be serialized to {@link SerializableException.Content}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface SerializableException
{
    /**
     * Represents a message and it's parameters which can be converted to/from string.
     */
    public static class Content
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
            if (value instanceof String) {
                string = (String) value;
            }
            else if (Converter.isPrimitive(value)) {
                string = value.toString();
            }
            else if (value instanceof Class) {
                string = ClassHelper.getClassShortName((Class) value);
            }
            else {
                throw new IllegalArgumentException();
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
            } else {
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
         * Load {@link Content} from given {@code fromException}.
         *
         * @param fromException to load from
         */
        public static Content fromException(Exception fromException)
        {
            Content content = new Content();
            content.setMessage(fromException.getMessage());
            try {
                String[] propertyNames = Property.getPropertyNames(fromException.getClass(), Exception.class);
                for (String propertyName : propertyNames) {
                    if (propertyName.equals("message") || propertyName.equals("code")) {
                        continue;
                    }
                    content.setParameter(propertyName, Property.getPropertyValue(fromException, propertyName));
                }
            }
            catch (FaultException exception) {
                throw new IllegalStateException("Cannot get property value from exception '"
                        + fromException.getClass().getCanonicalName() + "'!", exception);
            }
            return content;
        }

        /**
         * Store {@link Content} to given {@code toException}.
         *
         * @param toException to store to
         */
        public void toException(Exception toException)
        {
            String[] propertyNames = Property.getPropertyNames(toException.getClass(), Exception.class);
            try {
                for (String propertyName : propertyNames) {
                    String propertyValue = getParameter(propertyName);
                    if ( propertyValue != null) {
                        Property property = Property.getProperty(toException.getClass(), propertyName);
                        Class propertyType = property.getType();
                        Object propertyConvertedValue;
                        if (propertyType.equals(Class.class)) {
                            propertyConvertedValue = ClassHelper.getClassFromShortName(propertyValue);
                        } else {
                            propertyConvertedValue = Converter.convert(propertyValue, propertyType);
                        }
                        Property.setPropertyValue(toException, propertyName, propertyConvertedValue, true);
                    }
                }
            }
            catch (Exception exception) {
                throw new IllegalStateException("Cannot set property value from exception '"
                        + toException.getClass().getCanonicalName() + "'!", exception);
            }
        }
    }
}
