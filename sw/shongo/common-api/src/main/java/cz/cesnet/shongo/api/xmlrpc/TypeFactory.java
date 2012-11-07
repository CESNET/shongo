package cz.cesnet.shongo.api.xmlrpc;

import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.api.util.Options;
import cz.cesnet.shongo.api.util.TypeFlags;
import cz.cesnet.shongo.fault.FaultException;
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

import java.util.HashMap;
import java.util.Map;

/**
 * TypeFactory that converts between objects and maps
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TypeFactory extends TypeFactoryImpl
{
    /**
     * @see Options
     */
    private Options options;

    /**
     * Constructor.
     *
     * @param pController
     * @param options     sets the {@link #options}
     */
    public TypeFactory(XmlRpcController pController, Options options)
    {
        super(pController);

        this.options = options;
    }

    @Override
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
                    // Empty map means null
                    if (map == null || map.size() == 0) {
                        setResult(null);
                    }
                    // If the class key is present convert the map to object
                    else {
                        // Convert map to object of the class
                        try {
                            setResult(Converter.convertFromBasic(map, options));
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

    /**
     * @param pConfig
     * @return {@link TypeSerializer} for {@code null} value
     */
    private TypeSerializer getNullSerializer(XmlRpcStreamConfig pConfig)
    {
        return new MapSerializer(this, pConfig)
        {
            @Override
            public void write(ContentHandler pHandler, Object pObject) throws SAXException
            {
                super.write(pHandler, new HashMap());
            }
        };
    }

    /**
     * {@link TypeSerializer} for values to be converted by {@link Converter} to {@link TypeFlags#BASIC} types.
     */
    public class ConverterTypeSerializer implements TypeSerializer
    {
        /**
         * @see XmlRpcStreamConfig
         */
        XmlRpcStreamConfig config;

        /**
         * Constructor.
         *
         * @param config sets the {@link #config}
         */
        public ConverterTypeSerializer(XmlRpcStreamConfig config)
        {
            this.config = config;
        }

        @Override
        public void write(ContentHandler handler, Object object) throws SAXException
        {
            try {
                object = Converter.convertToBasic(object, options);
            }
            catch (FaultException exception) {
                throw new SAXException(exception);
            }
            TypeSerializer serializer = null;
            if (object == null) {
                serializer = getNullSerializer(config);
            }
            else {
                serializer = getSerializer(config, object);
            }
            serializer.write(handler, object);
        }
    }

    @Override
    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException
    {
        int typeFlags = TypeFlags.get(pObject);
        // Null values are serialized as empty maps
        if (pObject == null) {
            return getNullSerializer(pConfig);
        }
        // Basic values are serialized by default
        else if ( TypeFlags.isBasic(typeFlags)) {
            return super.getSerializer(pConfig, pObject);
        }
        // Other values must be converted to basic and serialized by default
        return new ConverterTypeSerializer(pConfig);
    }
}
