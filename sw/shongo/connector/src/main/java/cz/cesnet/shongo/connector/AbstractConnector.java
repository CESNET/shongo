package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A common functionality for connectors.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
abstract public class AbstractConnector implements CommonService
{
    private static Logger logger = LoggerFactory.getLogger(AbstractConnector.class);

    /**
     * Info about the connector and the device.
     */
    protected ConnectorInfo info = new ConnectorInfo(getClass().getSimpleName());

    @Override
    public ConnectorInfo getConnectorInfo()
    {
        return info;
    }

    /**
     * Lists all implemented methods supported by the implementing connector.
     * <p/>
     * Uses reflection.
     * <p/>
     * Any method that declares throwing CommandUnsupportedException is considered not implemented on the connector.
     * Thus, relies just on the fact that the method is not declaring throwing CommandUnsupportedException.
     * Note that even if a method is actually implemented and works, it is not listed by getSupportedMethods() if it
     * still declares throwing CommandUnsupportedException (which is needless, though).
     *
     * @return collection of public methods implemented from an interface, not throwing CommandUnsupportedException
     */
    @Override
    public Collection<Method> getSupportedMethods()
    {
        List<Method> result = new ArrayList<Method>();

        // get public methods not raising CommandUnsupportedException
        Map<String, Class[]> methods = new HashMap<String, Class[]>();
MethodsLoop:
        for (Method m : getClass().getMethods()) {
            final Class[] exceptionTypes = m.getExceptionTypes();
            for (Class ex : exceptionTypes) {
                if (ex.equals(CommandUnsupportedException.class)) {
                    continue MethodsLoop;
                }
            }
            // CommandUnsupportedException not found - the method seems good
            methods.put(m.getName(), m.getParameterTypes());
        }
        // promote those not implementing an interface
        Class[] intfcs = getClass().getInterfaces();
        for (Class intfc : intfcs) {
            for (Method m : intfc.getMethods()) {
                final String mName = m.getName();
                if (methods.containsKey(mName) && Arrays.equals(m.getParameterTypes(), methods.get(mName))) {
                    result.add(m);
                }
            }
        }

        return result;
    }


    /**
     * Just for debugging purposes, for printing results of commands.
     * <p/>
     * Taken from:
     * http://stackoverflow.com/questions/2325388/java-shortest-way-to-pretty-print-to-stdout-a-org-w3c-dom-document
     *
     * @param doc XML document to be printed
     * @param out stream to print the document to
     * @throws IOException
     * @throws javax.xml.transform.TransformerException
     *
     */
    protected static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException
    {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }
}
