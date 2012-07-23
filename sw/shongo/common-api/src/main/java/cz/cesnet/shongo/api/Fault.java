package cz.cesnet.shongo.api;

/**
 * Fault interface. Every fault must implement fault
 * interface. Contains common faults.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Fault
{
    /**
     * Get fault code
     *
     * @return fault code
     */
    public int getCode();

    /**
     * Get fault string
     *
     * @return fault string
     */
    public String getString();

    /**
     * Fault enumeration for common faults
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    public static enum Common implements Fault
    {
        UNKNOWN_FAULT(
                0, "Unknown fault: %s"),

        CLASS_NOT_DEFINED(
                10, "Class '%s' is not defined."),
        CLASS_CANNOT_BE_INSTANCED(
                11, "Class '%s' cannot be instanced."),
        CLASS_ATTRIBUTE_NOT_DEFINED(
                12, "Attribute '%s' in class '%s' is not defined."),
        CLASS_ATTRIBUTE_TYPE_MISMATCH(
                13, "Attribute '%s' in class '%s' has type '%s' but '%s' was presented."),
        CLASS_ATTRIBUTE_IS_REQUIRED(
                14, "Attribute '%s' in class '%s' wasn't present and is required."),
        CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED(
                15, "Collection '%s' in class '%s' is empty and is required."),
        CLASS_ATTRIBUTE_READ_ONLY(
                16, "Cannot set attribute '%s' for object of class '%s' because it is read-only."),
        CLASS_ATTRIBUTE_WRITE_ONLY(
                17, "Cannot get attribute '%s' from object of class '%s' because it is write-only."),

        ENUM_VALUE_NOT_DEFINED(
                20, "Enum value '%s' is not defined in enum '%s'."),
        DATETIME_PARSING_FAILED(
                21, "Failed to parse date/time '%s'."),
        PERIOD_PARSING_FAILED(
                22, "Failed to parse period '%s'."),
        INTERVAL_PARSING_FAILED(
                23, "Failed to parse interval '%s'."),

        COLLECTION_ITEM_TYPE_MISMATCH(
                30, "Collection '%s' can contain items of type '%s' but '%s' was presented."),
        COLLECTION_ITEM_NULL(
                31, "Null value cannot be added to collection '%s'."),

        RECORD_NOT_EXIST(
                41, "Record '%s' with id '%d' doesn't exist."),

        TODO_IMPLEMENT(
                99, "TODO: Implement");

        private int code;
        private String string;

        private Common(int code, String string)
        {
            this.code = code;
            this.string = string;
        }

        public int getCode()
        {
            return code;
        }

        public String getString()
        {
            return string;
        }
    }
}
