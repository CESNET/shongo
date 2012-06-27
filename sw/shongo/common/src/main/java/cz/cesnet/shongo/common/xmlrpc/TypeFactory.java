package cz.cesnet.shongo.common.xmlrpc;

import cz.cesnet.shongo.common.util.Converter;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.MapParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.MapSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.Map;

/**
 * TypeFactory that converts between objects and maps
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TypeFactory extends TypeFactoryImpl
{
    private static Logger logger = LoggerFactory.getLogger(TypeFactory.class);

    public TypeFactory(XmlRpcController pController)
    {
        super(pController);
    }

    public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI,
            String pLocalName)
    {
        // Allow for converting struct with class attribute to object
        if (MapSerializer.STRUCT_TAG.equals(pLocalName)) {
            // Create custom map parser that checks class attribute
            return new MapParser(pConfig, pContext, this)
            {
                @Override
                public void endElement(String pURI, String pLocalName, String pQName) throws SAXException
                {
                    super.endElement(pURI, pLocalName, pQName);
                    Map map = null;
                    try {
                        map = (Map) getResult();
                    }
                    catch (XmlRpcException exception) {
                        throw new SAXException(exception);
                    }
                    // If the class key is present convert the map to object
                    if (map != null && map.containsKey("class")) {
                        String className = (String) map.get("class");
                        Class objectClass = null;

                        // Get object class
                        try {
                            objectClass = Class.forName(Converter.getFullClassName(className));
                        }
                        catch (ClassNotFoundException exception) {
                            throw new SAXException(new FaultException(Fault.Common.CLASS_NOT_DEFINED, className));
                        }

                        // Convert map to object of the class
                        try {
                            setResult(Converter.convertMapToObject(map, objectClass));
                        }
                        catch (FaultException exception) {
                            throw new SAXException(exception);
                        }
                    }
                }
            };
        }
        else {
            return super.getParser(pConfig, pContext, pURI, pLocalName);
        }
    }

    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException
    {
        if (pObject != null && pObject.getClass().isEnum()) {
            pObject = pObject.toString();
        }
        TypeSerializer serializer = super.getSerializer(pConfig, pObject);
        // If none serializer was found, serialize by object attributes
        if (serializer == null) {
            // Create custom map serializer to serialize object to map
            serializer = new MapSerializer(this, pConfig)
            {
                @Override
                public void write(ContentHandler pHandler, Object pObject) throws SAXException
                {
                    try {
                        Map<String, Object> map = Converter.convertObjectToMap(pObject);
                        super.write(pHandler, map);
                    }
                    catch (FaultException exception) {
                        throw new SAXException(exception);
                    }
                }
            };
        }
        return serializer;
    }
}
