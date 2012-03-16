package cz.cesnet.shongo.rpctest.xmlrpc;

import cz.cesnet.shongo.rpctest.common.API;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.AtomicParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.MapSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.Map;

public class TypeFactory extends TypeFactoryImpl {
    public TypeFactory(XmlRpcController pController) {
        super(pController);
    }

    public TypeParser getParser(XmlRpcStreamConfig pConfig, NamespaceContextImpl pContext, String pURI, String pLocalName) {
        if ( DateSerializer.DATE_TAG.equals(pLocalName) ) {
            return new DateParser();
        }
        if ( PeriodicDateSerializer.PERIODIC_DATE_TAG.equals(pLocalName) ) {
            return new PeriodicDateParser();
        }
        return super.getParser(pConfig, pContext, pURI, pLocalName);
    }

    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException {
        if ( pObject instanceof API.PeriodicDate ) {
            return new PeriodicDateSerializer();
        } else if ( pObject instanceof API.Date ) {
            return new DateSerializer();
        }

        TypeSerializer serializer = super.getSerializer(pConfig, pObject);
        if ( serializer == null ) {
            serializer = new MapSerializer(this, pConfig) {
                @Override
                public void write(ContentHandler pHandler, Object pObject) throws SAXException {
                    Map map = null;
                    try {
                        map = BeanUtils.describe(pObject);
                        map.remove("class");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    super.write(pHandler, map);
                }
            };
        }

        return serializer;
    }

    private static class DateSerializer extends TypeSerializerImpl {
        public static final String DATE_TAG = "dateTime";

        public void write(ContentHandler pHandler, Object pObject) throws SAXException {
            write(pHandler, DATE_TAG, DATE_TAG, ((API.Date)pObject).date);
        }
    }

    private static class DateParser extends AtomicParser {
        protected void setResult(String pResult) throws SAXException {
            super.setResult(new API.Date(pResult));
        }
    }

    private static class PeriodicDateSerializer extends TypeSerializerImpl {
        public static final String PERIODIC_DATE_TAG = "periodicDateTime";

        public void write(ContentHandler pHandler, Object pObject) throws SAXException {
            API.PeriodicDate periodicDate = (API.PeriodicDate)pObject;
            write(pHandler, PERIODIC_DATE_TAG, PERIODIC_DATE_TAG, periodicDate.date + "_" + periodicDate.period);
        }
    }

    private static class PeriodicDateParser extends AtomicParser {
        protected void setResult(String pResult) throws SAXException {
            String[] parts = pResult.split("_");
            assert(parts.length == 2);
            super.setResult(new API.PeriodicDate(parts[0], parts[1]));
        }
    }
}
