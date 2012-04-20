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
        UnknownFault(0, "Unknown fault: %s"),
        ClassNotDefined(1, "Class '%s' is not defined."),
        AttributeNotDefined(2, "Attribute '%s' in class '%s' is not defined."),
        AttributeTypeMismatch(3, "Attribute '%s' in class '%s' has type '%s' but '%s' was presented."),
        EnumNotDefined(4, "Enum value '%s' is not defined in enum '%s'."),
        ClassCannotBeInstanced(5, "Class '%s' cannot be instancied without arguments.");

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
