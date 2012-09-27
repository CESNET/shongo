package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

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


    // CONNECTOR FUNCTIONALITY

    /**
     * Send a command to the device.
     * In case of an error, throws a CommandException with a detailed message.
     *
     * @param command command to be issued
     * @return the result of the command
     */
    protected Document issueCommand(Command command) throws CommandException
    {
        logger.info(String.format("%s issuing command %s on %s", CodecC90Connector.class, command,
                info.getDeviceAddress()));

        try {
            Document result = exec(command);
            if (isError(result)) {
                logger.info(String.format("Command %s failed on %s: %s", command, info.getDeviceAddress(),
                        getErrorMessage(result)));
                throw new CommandException(getErrorMessage(result));
            }
            else {
                logger.info(String.format("Command %s succeeded on %s", command, info.getDeviceAddress()));
                return result;
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Command issuing error", e);
        }
        catch (SAXException e) {
            throw new RuntimeException("Command result parsing error", e);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException("Command result handling error", e);
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException("Error initializing result parser", e);
        }
    }

    /**
     * Sends a command to the device. Blocks until response to the command is complete.
     *
     * @param command a command to the device
     * @return output of the command
     * @throws IOException
     */
    abstract protected Document exec(Command command) throws IOException, SAXException, ParserConfigurationException;

    /**
     * Finds out whether a given result XML denotes an error.
     *
     * @param result an XML document - result of a command
     * @return true if the result marks an error, false if the result is an ordinary result record
     */
    abstract protected boolean isError(Document result) throws XPathExpressionException;

    /**
     * Given an XML result of an erroneous command, returns the error message.
     *
     * @param result an XML document - result of a command
     * @return error message contained in the result document, or null if the document does not denote an error
     */
    abstract protected String getErrorMessage(Document result) throws XPathExpressionException;



    // HELPER METHODS

    private static XPathFactory xPathFactory = XPathFactory.newInstance();
    private static Map<String, XPathExpression> xPathExpressionCache = new HashMap<String, XPathExpression>();
    /**
     * Returns the result of an XPath expression on a given document. Caches the expressions for further usage.
     *
     * @param result      an XML document
     * @param xPathString an XPath expression
     * @return result of the XPath expression
     */
    protected static String getResultString(Document result, String xPathString) throws XPathExpressionException
    {
        XPathExpression expr = xPathExpressionCache.get(xPathString);
        if (expr == null) {
            expr = xPathFactory.newXPath().compile(xPathString);
            xPathExpressionCache.put(xPathString, expr);
        }
        return expr.evaluate(result);
    }





    /**
     * Just for debugging purposes, for printing results of commands.
     * <p/>
     * Taken from:
     * http://stackoverflow.com/questions/2325388/java-shortest-way-to-pretty-print-to-stdout-a-org-w3c-dom-document
     *
     * @param doc
     * @param out
     * @throws IOException
     * @throws javax.xml.transform.TransformerException
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
