package cz.cesnet.shongo.common.xmlrpc;

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
        UNKNOWN_FAULT(0, "Unknown fault: %s"),
        CLASS_NOT_DEFINED(1, "Class '%s' is not defined."),
        CLASS_ATTRIBUTE_NOT_DEFINED(2, "Attribute '%s' in class '%s' is not defined."),
        CLASS_ATTRIBUTE_TYPE_MISMATCH(3, "Attribute '%s' in class '%s' has type '%s' but '%s' was presented."),
        ENUM_VALUE_NOT_DEFINED(4, "Enum value '%s' is not defined in enum '%s'."),
        CLASS_CANNOT_BE_INSTANCED(5, "Class '%s' cannot be instanced without arguments."),
        CLASS_ATTRIBUTE_IS_REQUIRED(6, "Attribute '%s' in class '%s' wasn't present and is required."),
        COLLECTION_ITEM_TYPE_MISMATCH(3, "Collection '%s' can contain items of type '%s' but '%s' was presented.");

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
