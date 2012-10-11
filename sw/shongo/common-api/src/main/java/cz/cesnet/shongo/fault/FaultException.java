package cz.cesnet.shongo.fault;

import cz.cesnet.shongo.api.util.ClassHelper;
import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.api.util.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.cesnet.shongo.api.util.ClassHelper.getClassShortName;

/**
 * Exception that represents and implements {@link Fault}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class FaultException extends Exception implements Fault
{
    /**
     * Fault code.
     */
    int code;

    public FaultException()
    {
        this.code = CommonFault.UNKNOWN;
    }

    /**
     * Constructor.
     *
     * @param code
     */
    public FaultException(int code)
    {
        this.code = code;
    }

    /**
     * Constructor.
     *
     * @param code
     * @param message
     */
    public FaultException(int code, String message)
    {
        super(message);
        this.code = code;
    }

    /**
     * Constructor.
     *
     * @param code
     * @param message
     */
    public FaultException(int code, String message, Throwable throwable)
    {
        super(message, throwable);
        this.code = code;
    }

    /**
     * Constructor.
     *
     * @param code
     * @param throwable
     */
    public FaultException(int code, Throwable throwable)
    {
        super(throwable);
        this.code = code;
    }

    /**
     * Constructor.
     *
     * @param code
     * @param message
     * @param objects
     */
    public FaultException(int code, String message, Object... objects)
    {
        this(code, String.format(message, evaluateParameters(objects)));
    }

    /**
     * Constructor.
     *
     * @param fault
     * @param objects
     */
    public FaultException(Fault fault, Object... objects)
    {
        this(fault.getCode(), String.format(fault.getMessage(), evaluateParameters(objects)));
    }

    /**
     * Constructor.
     *
     * @param throwable
     * @param fault
     * @param objects
     */
    public FaultException(Throwable throwable, Fault fault, Object... objects)
    {
        this(fault.getCode(), String.format(fault.getMessage(), evaluateParameters(objects)), throwable);
    }

    /**
     * Constructor.
     *
     * @param throwable
     * @param message
     * @param objects
     */
    public FaultException(Throwable throwable, String message, Object... objects)
    {
        this(CommonFault.UNKNOWN, String.format(message, evaluateParameters(objects)), throwable);
    }

    /**
     * Constructor.
     *
     * @param faultString
     * @param objects
     */
    public FaultException(String faultString, Object... objects)
    {
        this(CommonFault.UNKNOWN, faultString, objects);
    }

    /**
     * Construct unknown fault by description string.
     *
     * @param faultString
     */
    public FaultException(String faultString)
    {
        this(CommonFault.UNKNOWN, faultString);
    }

    /**
     * Evaluate all given parameters (e.g., classes to class names).
     *
     * @param objects
     * @return array of evaluated parameters
     */
    private static Object[] evaluateParameters(Object[] objects)
    {
        for (int index = 0; index < objects.length; index++) {
            if (objects[index] instanceof Class) {
                objects[index] = getClassShortName((Class) objects[index]);
            }
        }
        return objects;
    }

    @Override
    public int getCode()
    {
        return code;
    }

    /**
     * @param code sets the {@link #code}
     */
    public void setCode(int code)
    {
        this.code = code;
    }

    /**
     * Fill parameters to {@code message} object which will be passed to client.
     *
     * @param message
     */
    public void fillMessageParameters(Message message)
    {
        try {
            String[] propertyNames = Property.getPropertyNames(getClass(), FaultException.class);
            for (String propertyName : propertyNames) {
                message.setParameter(propertyName, Property.getPropertyValue(this, propertyName));
            }
        }
        catch (FaultException exception) {
            throw new IllegalStateException("Cannot get property value from exception '"
                    + getClass().getCanonicalName() + "'!", exception);
        }
    }

    /**
     * Represents a message and it's parameters which can be converted to/from string.
     */
    public static class Message
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
         * @param name
         * @return value of parameter with given {@code name} as {@link Class}
         */
        public Class getParameterAsClass(String name)
        {
            String value = getParameter(name);
            if (value == null) {
                return null;
            }
            try {
                return ClassHelper.getClassFromShortName(value);
            }
            catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Cannot get class from '" + value + "'.", exception);
            }
        }

        /**
         * @param name
         * @return value of parameter with given {@code name} as {@link Long}
         */
        public Long getParameterAsLong(String name)
        {
            String value = getParameter(name);
            if (value == null) {
                return null;
            }
            return Long.parseLong(value);
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
            }

            Matcher parameterMatcher = PATTERN_PARAMETER.matcher(message);
            while (parameterMatcher.find()) {
                setParameter(parameterMatcher.group(1), parameterMatcher.group(2));
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
    }
}
