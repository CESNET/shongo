package cz.cesnet.shongo.connector.util;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.connector.common.RequestAttributeList;
import java.io.UnsupportedEncodingException;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public final class HttpReqUtils {

    public static String getCallUrl(String callPath, RequestAttributeList attributes) throws CommandException
    {
        if (callPath == null) {
            throw new CommandException("Call path cannot be null.");
        }

        String queryString = "";
        if (attributes != null) {
            try {
                queryString = "?" + attributes.getAttributesQuery();

            } catch (UnsupportedEncodingException e) {
                throw new CommandException("Failed to process command " + callPath + ": ", e);
            }
        }
        return callPath + queryString;
    }

}
