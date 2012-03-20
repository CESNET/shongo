package cz.cesnet.shongo.common;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.MapParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.MapSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.Iterator;
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
        if ( MapSerializer.STRUCT_TAG.equals(pLocalName) ) {
            return new MapParser(pConfig, pContext, this){
                @Override
                public void endElement(String pURI, String pLocalName, String pQName) throws SAXException {
                    super.endElement(pURI, pLocalName, pQName);
                    try {
                        Map map = (Map)getResult();
                        if ( map != null ) {
                            String className = (String)map.get("class");
                            Class objectClass = Class.forName("cz.cesnet.shongo." + className);
                            Object object = objectClass.newInstance();
                            BeanUtils.getInstance().populate(object, map);
                            setResult(object);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        } else {
            return super.getParser(pConfig, pContext, pURI, pLocalName);
        }
    }

    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
        TypeSerializer serializer = super.getSerializer(pConfig, pObject);
        if ( serializer == null ) {
            serializer = new MapSerializer(this, pConfig) {
                @Override
                public void write(ContentHandler pHandler, Object pObject) throws SAXException {
                    Map<String, Object> map = null;
                    try {
                        map = BeanUtils.getInstance().describeRecursive(pObject);
                        // Remove null values
                        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
                        while ( iterator.hasNext() ) {
                            Map.Entry<String, Object> entry = iterator.next();
                            if ( entry.getValue() == null )
                                iterator.remove();
                        }
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
