package cz.cesnet.shongo.controller.rest.models;

/**
 * Common validation methods.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommonModel
{
    /**
     * Prefix for new unique identifiers.
     */
    private static final String NEW_ID_PREFIX = "new-";

    /**
     * Last auto-generated identifier index.
     */
    private static int lastGeneratedId = 0;

    /**
     * @param id to be checked
     * @return true whether given {@code id} is auto-generated, false otherwise
     */
    public synchronized static boolean isNewId(String id)
    {
        return id.startsWith(NEW_ID_PREFIX);
    }

    /**
     * @return new auto-generated identifier
     */
    public synchronized static String getNewId()
    {
        return NEW_ID_PREFIX + ++lastGeneratedId;
    }

    /**
     * @param string
     * @return given {@code string} which can be used in double quoted string (e.g., "<string>")
     */
    public static String escapeDoubleQuotedString(String string)
    {
        if (string == null) {
            return null;
        }
        string = string.replaceAll("\n", "\\\\n");
        string = string.replaceAll("\"", "\\\\\"");
        return string;
    }
}
