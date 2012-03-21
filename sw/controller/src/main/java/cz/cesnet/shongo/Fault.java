package cz.cesnet.shongo;

/**
 * Fault interface
 *
 * @author Martin Srom
 */
public interface Fault
{
    public int getCode();

    public String getString();

    /**
     * Fault enumeration for common faults
     *
     * @author Martin Srom
     */
    public static enum Common implements Fault
    {
        UnknownFault(0, "Unknown fault: %s"),
        ClassNotDefined(1, "Class '%s' is not defined."),
        EnumNotDefined(2, "Enum value '%s' is not defined in enum '%s'.");

        private int code;
        private String string;

        private Common(int code, String string) {
            this.code = code;
            this.string = string;
        }

        public int getCode() {
            return code;
        }

        public String getString() {
            return string;
        }
    }
}
