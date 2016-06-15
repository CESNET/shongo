package cz.cesnet.shongo.api.rpc;

import cz.cesnet.shongo.api.AtomicType;
import cz.cesnet.shongo.api.ComplexType;
import cz.cesnet.shongo.api.Converter;
import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.MapParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.*;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.Collection;
import java.util.Map;

/**
 * TypeFactory that converts between objects and maps
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TypeFactory extends TypeFactoryImpl
{
    /**
     * Constructor.
     *
     * @param pController
     */
    public TypeFactory(XmlRpcController pController)
    {
        super(pController);
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
                    // If the class key is present convert the map to complex type
                    else if (map.containsKey(ComplexType.CLASS_PROPERTY)) {
                        setResult(Converter.convertMapToComplexType(map, ComplexType.class));
                    }
                }
            };
        }
        else {
            return super.getParser(pConfig, pContext, pURI, pLocalName);
        }
    }

    /**
     * {@link TypeSerializer} for {@link AtomicType}.
     */
    public static class NullSerializer extends TypeSerializerImpl
    {
        @Override
        public void write(ContentHandler handler, Object object) throws SAXException
        {
            write(handler);
        }

        /**
         * Write null to given {@code handler}.
         *
         * @param handler
         * @throws SAXException
         */
        public static void write(ContentHandler handler) throws SAXException
        {
            handler.startElement("", VALUE_TAG, VALUE_TAG, ZERO_ATTRIBUTES);
            handler.startElement("", "struct", "struct", ZERO_ATTRIBUTES);
            handler.endElement("", "struct", "struct");
            handler.endElement("", VALUE_TAG, VALUE_TAG);
        }
    }

    /**
     * {@link TypeSerializer} for {@link Enum}.
     */
    public static class ClassSerializer extends StringSerializer
    {
        @Override
        public void write(ContentHandler handler, Object object) throws SAXException
        {
            super.write(handler, Converter.convertClassToString((Class) object));
        }
    }

    /**
     * {@link TypeSerializer} for {@link Enum}.
     */
    public static class EnumSerializer extends StringSerializer
    {
        @Override
        public void write(ContentHandler handler, Object object) throws SAXException
        {
            super.write(handler, Converter.convertEnumToString((Enum) object));
        }
    }

    /**
     * {@link TypeSerializer} for {@link DateTime}.
     */
    public static class DateTimeSerializer extends StringSerializer
    {
        @Override
        public void write(ContentHandler handler, Object object) throws SAXException
        {
            super.write(handler, Converter.convertDateTimeToString((DateTime) object));
        }
    }

    /**
     * {@link TypeSerializer} for {@link LocalDate}.
     */
    public static class LocalDateSerializer extends StringSerializer
    {
        @Override
        public void write(ContentHandler handler, Object object) throws SAXException
        {
            super.write(handler, Converter.convertLocalDateToString((LocalDate) object));
        }
    }

    /**
     * {@link TypeSerializer} for {@link Period}.
     */
    public static class PeriodSerializer extends StringSerializer
    {
        @Override
        public void write(ContentHandler handler, Object object) throws SAXException
        {
            super.write(handler, Converter.convertPeriodToString((Period) object));
        }
    }

    /**
     * {@link TypeSerializer} for {@link Interval}.
     */
    public static class IntervalSerializer extends StringSerializer
    {
        @Override
        public void write(ContentHandler handler, Object object) throws SAXException
        {
            super.write(handler, Converter.convertIntervalToString((Interval) object));
        }
    }

    /**
     * {@link TypeSerializer} for {@link java.util.Collection}.
     */
    public class CollectionSerializer extends ObjectArraySerializer
    {
        /**
         * Constructor.
         *
         * @param pTypeFactory
         * @param pConfig
         */
        public CollectionSerializer(org.apache.xmlrpc.common.TypeFactory pTypeFactory, XmlRpcStreamConfig pConfig)
        {
            super(pTypeFactory, pConfig);
        }

        @Override
        protected void writeData(ContentHandler pHandler, Object pObject) throws SAXException
        {
            Collection collection = (Collection) pObject;
            for (Object item : collection) {
                writeObject(pHandler, item);
            }
        }
    }

    /**
     * {@link TypeSerializer} for {@link ComplexType}.
     */
    public static class ComplexTypeSerializer extends MapSerializer
    {
        /**
         * Constructor.
         *
         * @param pTypeFactory
         * @param pConfig
         */
        public ComplexTypeSerializer(org.apache.xmlrpc.common.TypeFactory pTypeFactory, XmlRpcStreamConfig pConfig)
        {
            super(pTypeFactory, pConfig);
        }

        @Override
        public void write(ContentHandler handler, Object object) throws SAXException
        {
            super.write(handler, Converter.convertComplexTypeToMap((ComplexType) object));
        }
    }

    /**
     * {@link TypeSerializer} for {@link AtomicType}.
     */
    public static class AtomicTypeSerializer extends StringSerializer
    {
        @Override
        public void write(ContentHandler handler, Object object) throws SAXException
        {
            String value = Converter.convertAtomicTypeToString((AtomicType) object);
            if (value != null) {
                super.write(handler, value);
            }
            else {
                NullSerializer.write(handler);
            }
        }
    }

    private static final TypeSerializer NULL_SERIALIZER = new NullSerializer();
    private static final TypeSerializer CLASS_SERIALIZER = new ClassSerializer();
    private static final TypeSerializer ENUM_SERIALIZER = new EnumSerializer();
    private static final TypeSerializer DATETIME_SERIALIZER = new DateTimeSerializer();
    private static final TypeSerializer LOCAL_DATE_SERIALIZER = new LocalDateSerializer();
    private static final TypeSerializer PERIOD_SERIALIZER = new PeriodSerializer();
    private static final TypeSerializer INTERVAL_SERIALIZER = new IntervalSerializer();
    private static final TypeSerializer ATOMIC_TYPE_SERIALIZER = new AtomicTypeSerializer();

    @Override
    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig, Object pObject) throws SAXException
    {
        if (pObject == null) {
            return NULL_SERIALIZER;
        }
        else if (pObject instanceof Class) {
            return CLASS_SERIALIZER;
        }
        else if (pObject instanceof Enum) {
            return ENUM_SERIALIZER;
        }
        else if (pObject instanceof DateTime) {
            return DATETIME_SERIALIZER;
        }
        else if (pObject instanceof LocalDate) {
            return LOCAL_DATE_SERIALIZER;
        }
        else if (pObject instanceof Period) {
            return PERIOD_SERIALIZER;
        }
        else if (pObject instanceof Interval) {
            return INTERVAL_SERIALIZER;
        }
        else if (pObject instanceof Collection) {
            return new CollectionSerializer(this, pConfig);
        }
        else if (pObject instanceof AtomicType) {
            return ATOMIC_TYPE_SERIALIZER;
        }
        else if (pObject instanceof ComplexType) {
            return new ComplexTypeSerializer(this, pConfig);
        }
        return super.getSerializer(pConfig, pObject);
    }
}
