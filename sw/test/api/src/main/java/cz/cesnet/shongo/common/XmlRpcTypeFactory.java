package cz.cesnet.shongo.common;

import cz.cesnet.shongo.Fault;
import cz.cesnet.shongo.FaultException;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.MapParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.MapSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.Map;

/**
 * XmlRpcTypeFactory that converts between objects and maps
 *
 * @author Martin Srom
 */
public class XmlRpcTypeFactory extends TypeFactoryImpl
{
    public XmlRpcTypeFactory(XmlRpcController pController) {
        super(pController);
    }

    public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String pLocalName) {
        // Convert struct with class attribute to object
        if ( MapSerializer.STRUCT_TAG.equals(pLocalName) ) {
            // Create custom map parser that checks class attribute
            return new MapParser(pConfig, pContext, this){
                @Override
                public void endElement(String pURI, String pLocalName, String pQName) throws SAXException {
                    super.endElement(pURI, pLocalName, pQName);
                    Map map = null;
                    try {
                        map = (Map)getResult();
                    } catch (XmlRpcException exception) {
                        throw new SAXException(exception);
                    }
                    // Empty struct means "null" value
                    if ( map != null && map.size() == 0 ) {
                        setResult(null);
                    }
                    // Check class key
                    else if ( map != null && map.containsKey("class") ) {
                        String className = (String)map.get("class");
                        try {
                            Class objectClass = Class.forName("cz.cesnet.shongo." + className);
                            Object object = objectClass.newInstance();
                            BeanUtils.getInstance().populateRecursive(object, map);
                            setResult(object);
                        } catch (BeanUtils.Exception exception) {
                            throw new SAXException(exception.getException());
                        } catch (ClassNotFoundException exception) {
                            throw new SAXException(new FaultException(Fault.Common.ClassNotDefined, className));
                        } catch (Exception exception) {
                            throw new SAXException(exception);
                        }
                    }
                }
            };
        } else {
            return super.getParser(pConfig, pContext, pURI, pLocalName);
        }
    }

    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
        TypeSerializer serializer = super.getSerializer(pConfig, pObject);
        // If none serializer was found, serialize by object attributes
        if ( serializer == null ) {
            // Create custom map serializer to serialize object to map
            serializer = new MapSerializer(this, pConfig) {
                @Override
                public void write(ContentHandler pHandler, Object pObject) throws SAXException {
                    Map<String, Object> map = null;
                    try {
                        // Get map from object instance
                        map = BeanUtils.getInstance().describeRecursive(pObject);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    super.write(pHandler, map);
                }
            };
        }
        return serializer;
    }
}
