package cz.cesnet.shongo.controller.api.rpc;

import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

/**
 * Logger for XML-RPC request and response XMLs.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RpcServerRequestLogger
{
    private static Logger logger = LoggerFactory.getLogger(RpcServerRequestLogger.class);

    /**
     * Specifies whether logging is enabled.
     */
    private static boolean enabled = false;

    /**
     * @param content string which should be logged
     */
    public static void log(String content)
    {
        logger.debug(content);
    }

    /**
     * @param request string which should be logged as request
     */
    public static void logRequest(String request)
    {
        log("REQUEST:\n" + toPrettyXml(request));
    }

    /**
     * @param response string which should be logged as response
     */
    public static void logResponse(String response)
    {
        log("RESPONSE:\n" + toPrettyXml(response));
    }

    /**
     * Log everything from the input stream as request.
     *
     * @param inputStream
     * @return new input stream
     * @throws XmlRpcException
     */
    public static InputStream logRequest(InputStream inputStream) throws XmlRpcException
    {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            StringBuilder requestBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                requestBuilder.append(line);
            }
            String request = requestBuilder.toString();
            logRequest(requestBuilder.toString());
            return new ByteArrayInputStream(request.getBytes());
        }
        catch (IOException e) {
            throw new XmlRpcException(e.getMessage(), e);
        }
        finally {
            try {
                reader.close();
            }
            catch (IOException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }
        }
    }

    /**
     * Log everything what will be written to the output stream as response.
     *
     * @param outputStream
     * @return new output stream
     */
    public static OutputStream logResponse(final OutputStream outputStream)
    {
        if (outputStream instanceof ByteArrayOutputStream) {
            return new ByteArrayOutputStream()
            {
                @Override
                public void writeTo(OutputStream outputStream) throws IOException
                {
                    logResponse(toString());
                    super.writeTo(outputStream);
                }
            };
        }
        else {
            return new OutputStream()
            {
                private StringBuilder stringBuilder = new StringBuilder();

                @Override
                public void write(int b)
                {
                    stringBuilder.append((char) b);
                    try {
                        outputStream.write(b);
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void close() throws IOException
                {
                    if (stringBuilder.length() > 0) {
                        logResponse(stringBuilder.toString());
                    }
                    outputStream.close();
                }
            };
        }
    }

    /**
     * @param xml
     * @return formatted xml
     */
    private static String toPrettyXml(String xml)
    {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult result = new StreamResult(new StringWriter());
            StreamSource source = new StreamSource(new StringReader(xml));
            transformer.transform(source, result);
            return result.getWriter().toString();
        }
        catch (Exception e) {
            System.err.println(xml);
            throw new RuntimeException(e);
        }
    }

    /**
     * @return true if logger is enabled, false otherwise
     */
    public static boolean isEnabled()
    {
        return enabled;
    }

    /**
     * @param enabled sets the {@link #enabled}
     */
    public static void setEnabled(boolean enabled)
    {
        RpcServerRequestLogger.enabled = enabled;
    }
}
