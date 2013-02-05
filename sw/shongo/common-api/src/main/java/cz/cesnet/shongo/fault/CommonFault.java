package cz.cesnet.shongo.fault;

import java.util.HashMap;
import java.util.Map;

import static cz.cesnet.shongo.api.util.ClassHelper.getClassShortName;

/**
 * Common faults.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommonFault
{
    public static final int UNKNOWN = 0;

    public static final Fault CLASS_NOT_DEFINED = new SimpleFault(
            10, "Class '%s' is not defined.");
    public static final Fault CLASS_CANNOT_BE_INSTANCED = new SimpleFault(
            11, "Class '%s' cannot be instanced.");
    public static final Fault CLASS_ATTRIBUTE_NOT_DEFINED = new SimpleFault(
            12, "Attribute '%s' in class '%s' is not defined.");
    public static final Fault CLASS_ATTRIBUTE_TYPE_MISMATCH = new SimpleFault(
            13, "Attribute '%s' in class '%s' has type '%s' but '%s' was presented.");
    public static final Fault CLASS_ATTRIBUTE_IS_REQUIRED = new SimpleFault(
            14, "Attribute '%s' in class '%s' wasn't present and is required.");
    public static final Fault CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED = new SimpleFault(
            15, "Collection '%s' in class '%s' is empty and is required.");
    public static final Fault CLASS_ATTRIBUTE_READ_ONLY = new SimpleFault(
            16, "Cannot set attribute '%s' for object of class '%s' because it is read-only.");
    public static final Fault CLASS_ATTRIBUTE_WRONG_VALUE = new SimpleFault(
            17, "Cannot set attribute '%s' for object of class '%s' because wrong value '%s' was present.");

    public static final Fault ENUM_VALUE_NOT_DEFINED = new SimpleFault(
            20, "Enum value '%s' is not defined in enum '%s'.");
    public static final Fault DATETIME_PARSING_FAILED = new SimpleFault(
            21, "Failed to parse date/time '%s'.");
    public static final Fault PERIOD_PARSING_FAILED = new SimpleFault(
            22, "Failed to parse period '%s'.");
    public static final Fault INTERVAL_PARSING_FAILED = new SimpleFault(
            23, "Failed to parse interval '%s'.");
    public static final Fault PARTIAL_DATETIME_PARSING_FAILED = new SimpleFault(
            24, "Failed to parse partial date/time '%s'.");

    public static final Fault COLLECTION_ITEM_TYPE_MISMATCH = new SimpleFault(
            30, "Collection '%s' can contain items of type '%s' but '%s' was presented.");
    public static final Fault COLLECTION_ITEM_NULL = new SimpleFault(
            31, "Null value cannot be added to collection '%s'.");

    /**
     * @see EntityNotFoundException
     */
    public static final int ENTITY_NOT_FOUND = 40;
    /**
     * @see EntityValidationException
     */
    public static final int ENTITY_VALIDATION = 41;
    /**
     * @see EntityToDeleteIsReferencedException
     */
    public static final int ENTITY_TO_DELETE_IS_REFERENCED = 42;

    /**
     * @see SecurityException
     */
    public static final int SECURITY_UNKNOWN = 50;

    /**
     * @see cz.cesnet.shongo.fault.jade.CommandFailure
     */
    public static final int JADE_COMMAND_UNKNOWN = 60;
    /**
     * @see cz.cesnet.shongo.fault.jade.CommandTimeout
     */
    public static final int JADE_COMMAND_TIMEOUT = 61;
    /**
     * @see cz.cesnet.shongo.fault.jade.CommandResultDecoding
     */
    public static final int JADE_COMMAND_RESULT_DECODING = 62;
    /**
     * @see cz.cesnet.shongo.fault.jade.CommandNotUnderstood
     */
    public static final int JADE_COMMAND_NOT_UNDERSTOOD = 63;
    /**
     * @see cz.cesnet.shongo.fault.jade.CommandRefused
     */
    public static final int JADE_COMMAND_REFUSED = 64;
    /**
     * @see cz.cesnet.shongo.fault.jade.CommandAgentNotFound
     */
    public static final int JADE_COMMAND_CONNECTOR_NOT_FOUND = 65;
    /**
     * @see cz.cesnet.shongo.fault.jade.CommandAgentNotStarted
     */
    public static final int JADE_COMMAND_AGENT_NOT_STARTED = 66;

    /**
     * @see TodoImplementException
     */
    public static final int TODO_IMPLEMENT = 99;

    /**
     * List of exception classes bound to fault code.
     */
    private Map<Integer, Class<? extends Exception>> classes;

    /**
     * Add new exception class to be bound to a fault code.
     *
     * @param code
     * @param type
     */
    public void add(int code, Class<? extends Exception> type)
    {
        classes.put(code, type);
    }

    /**
     * Fill exception classes.
     */
    protected void fill()
    {
        add(ENTITY_NOT_FOUND, EntityNotFoundException.class);
        add(ENTITY_TO_DELETE_IS_REFERENCED, EntityToDeleteIsReferencedException.class);
        add(TODO_IMPLEMENT, TodoImplementException.class);
    }

    /**
     * @return map of exception class by fault code
     */
    public final Map<Integer, Class<? extends Exception>> getClasses()
    {
        if (classes == null) {
            classes = new HashMap<Integer, Class<? extends Exception>>();
            fill();
        }
        return classes;
    }

    /**
     * Format fault message.
     *
     * @param message
     * @param objects
     */
    public static String formatMessage(String message, Object... objects)
    {
        return String.format(message, evaluateParameters(objects));
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

    /**
     * Represents a fault which doesn't have own exception class but the message should be stored in one place.
     */
    public static class SimpleFault implements Fault
    {
        /**
         * Fault code.
         */
        private int code;

        /**
         * Fault string (can contain placeholders, e.g., "%s").
         */
        private String message;

        /**
         * Constructor.
         *
         * @param code    sets the {@link #code}
         * @param message sets the {@link #message}
         */
        public SimpleFault(int code, String message)
        {
            this.code = code;
            this.message = message;
        }

        @Override
        public int getCode()
        {
            return code;
        }

        @Override
        public String getMessage()
        {
            return message;
        }
    }
}
