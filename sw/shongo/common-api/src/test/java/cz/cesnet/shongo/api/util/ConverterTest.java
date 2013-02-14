package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.xmlrpc.AtomicType;
import org.joda.time.*;
import org.joda.time.chrono.ISOChronology;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 * Tests for {@link Converter}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ConverterTest
{
    private static final Options OPTIONS = Options.SERVER;

    /**
     * Register {@link Entity} and {@link SubEntity} for {@link Converter}.
     */
    @Before
    public void onBefore()
    {
        try {
            ClassHelper.registerClassShortName(Entity.class);
            ClassHelper.registerClassShortName(SubEntity.class);
        }
        catch (IllegalStateException exception) {
        }
    }

    /**
     * Convert types to string.
     *
     * @throws Exception
     */
    @Test
    public void testToString() throws Exception
    {
        Assert.assertEquals("1", Converter.convert((int) 1, String.class));
        Assert.assertEquals("1", Converter.convert((long) 1, String.class));
        Assert.assertEquals("TYPE1", Converter.convert(Entity.Type.TYPE1, String.class));
        Assert.assertEquals("2012-01-01T00:00:00.000+01:00",
                Converter.convert(DateTime.parse("2012-01-01T00:00+01:00"), String.class));
        Assert.assertEquals("P1Y2M3DT4H5M6S",
                Converter.convert(Period.parse("P1Y2M3DT4H5M6S"), String.class));
        Assert.assertEquals("2012-01-01T00:00:00.000+01:00",
                Converter.convert(DateTime.parse("2012-01-01T00:00+01:00"), String.class));
    }

    /**
     * Test converting entity to {@link Map}.
     *
     * @throws Exception
     */
    @Test
    public void testConvertToBasic() throws Exception
    {
        Object entityBasic = createEntityBasic();
        Object convertedEntityBasic = Converter.convertToBasic(createEntity(), OPTIONS);
        assertBasicEquals(entityBasic, convertedEntityBasic);
    }

    /**
     * Test converting {@link Map} to entity.
     *
     * @throws Exception
     */
    @Test
    public void testConvertFromBasic() throws Exception
    {
        Object entity = createEntity();
        Object convertedEntity = Converter.convertFromBasic(createEntityBasic(), OPTIONS);
        assertEntityEquals(entity, convertedEntity);
    }

    /**
     * Test converting {@link String} to {@link ReadablePartial}.
     *
     * @throws Exception
     */
    @Test
    public void testAtomicReadablePartialFromString() throws Exception
    {
        String[] values = new String[]{"2012", "2012-12", "2012-12-01", "2012-12-01T12", "2012-12-01T12:34"};
        for (String value : values) {
            ReadablePartial readablePartial = Converter.Atomic.convertStringToReadablePartial(value);
            Assert.assertEquals(value, readablePartial.toString());
        }

        ReadablePartial readablePartial;
        readablePartial = Converter.Atomic.convertStringToReadablePartial("2012");
        Assert.assertEquals(2012, readablePartial.get(DateTimeFieldType.year()));
        try {
            readablePartial.get(DateTimeFieldType.monthOfYear());
            Assert.fail("Exception should be thrown");
        }
        catch (IllegalArgumentException exception) {
        }
        readablePartial = Converter.Atomic.convertStringToReadablePartial("2012-01-01T12");
        Assert.assertEquals(2012, readablePartial.get(DateTimeFieldType.year()));
        Assert.assertEquals(1, readablePartial.get(DateTimeFieldType.monthOfYear()));
        Assert.assertEquals(1, readablePartial.get(DateTimeFieldType.dayOfMonth()));
        Assert.assertEquals(12, readablePartial.get(DateTimeFieldType.hourOfDay()));
        try {
            readablePartial.get(DateTimeFieldType.minuteOfHour());
            Assert.fail("Exception should be thrown");
        }
        catch (IllegalArgumentException exception) {
        }
    }

    /**
     * @return entity for testing
     */
    private Entity createEntity()
    {
        Entity entity = new Entity();
        entity.setString("string");
        entity.setIntPrimitive(1);
        entity.setIntObject(2);
        entity.setLongPrimitive(3);
        entity.setLongObject((long) 4);
        entity.setCustomType(new CustomType("customType"));
        entity.setDateTime(DateTime.parse("2012-01-01T12:00+03:00"));
        entity.setPeriod(Period.parse("P1Y2M3DT4H5M6S"));
        entity.setInterval(new Interval("2012-01-01T00:00:00/2012-01-01T23:59:59",
                ISOChronology.getInstance(DateTimeZone.forID("+03:00"))));
        entity.setType(Entity.Type.TYPE1);
        entity.addType(Entity.Type.TYPE1);
        entity.addType(Entity.Type.TYPE2);
        entity.addSubEntity(new SubEntity("subEntity1"));
        entity.addSubEntity(new SubEntity("subEntity2"));
        entity.addDescriptionByType(Entity.Type.TYPE1, "type1");
        entity.addDescriptionByType(Entity.Type.TYPE2, "type2");
        entity.addSubEntityByType(Entity.Type.TYPE1, new SubEntity("subEntity3"));
        entity.addSubEntityByType(Entity.Type.TYPE2, new SubEntity("subEntity4"));
        return entity;
    }

    /**
     * @param string sets the {@link SubEntity#string}
     * @return new sub entity as {@link Map}
     */
    private Map createSubEntityBasic(String string)
    {
        Map<String, Object> subEntityMap = new HashMap<String, Object>();
        subEntityMap.put("class", ClassHelper.getClassShortName(SubEntity.class));
        subEntityMap.put("string", string);
        return subEntityMap;
    }

    /**
     * @return {@link Map} which corresponds to the result of {@link #createEntity()}
     */
    private Map createEntityBasic()
    {
        Map<String, Object> entityMap = new HashMap<String, Object>();
        entityMap.put("class", ClassHelper.getClassShortName(Entity.class));
        entityMap.put("string", "string");
        entityMap.put("intPrimitive", 1);
        entityMap.put("intObject", 2);
        entityMap.put("longPrimitive", (long) 3);
        entityMap.put("longObject", (long) 4);
        entityMap.put("dateTime", "2012-01-01T12:00:00.000+03:00");
        entityMap.put("period", "P1Y2M3DT4H5M6S");
        entityMap.put("interval", "2012-01-01T00:00:00.000+03:00/2012-01-01T23:59:59.000+03:00");
        entityMap.put("customType", "customType");
        entityMap.put("type", "TYPE1");
        entityMap.put("types", new HashSet<String>()
        {{
                add("TYPE1");
                add("TYPE2");
            }});
        entityMap.put("subEntities", new Object[]{
                createSubEntityBasic("subEntity1"),
                createSubEntityBasic("subEntity2")
        });
        entityMap.put("descriptionByType", new HashMap<String, String>()
        {{
                put("TYPE1", "type1");
                put("TYPE2", "type2");
            }});
        entityMap.put("subEntityByType", new HashMap<String, Map>()
        {{
                put("TYPE1", createSubEntityBasic("subEntity3"));
                put("TYPE2", createSubEntityBasic("subEntity4"));
            }});
        return entityMap;
    }

    /**
     * Test equality of two basic objects.
     *
     * @param expected
     * @param object
     * @throws Exception
     */
    private void assertBasicEquals(Object expected, Object object) throws Exception
    {
        if (expected instanceof Map && object instanceof Map) {
            Map expectedMap = (Map) expected;
            Map objectMap = (Map) object;
            Assert.assertEquals(expectedMap.keySet(), objectMap.keySet());
            for (Object key : expectedMap.keySet()) {
                Object expectedValue = expectedMap.get(key);
                Object objectValue = objectMap.get(key);
                if (expectedValue instanceof Object[] && objectValue instanceof Object[]) {
                    Assert.assertArrayEquals((Object[]) expectedValue, (Object[]) objectValue);
                }
                else if (expectedValue instanceof Set && objectValue instanceof Object[]) {
                    Set<Object> objectSet = new HashSet<Object>();
                    Collections.addAll(objectSet, (Object[]) objectValue);
                    Assert.assertEquals(expectedValue, objectSet);
                }
                else {
                    Assert.assertEquals(expectedValue, objectValue);
                }
            }
        }
    }

    /**
     * Test equality of two entities.
     *
     * @param expected
     * @param object
     * @throws Exception
     */
    private void assertEntityEquals(Object expected, Object object) throws Exception
    {
        Assert.assertEquals(expected.getClass(), object.getClass());
        Collection<String> propertyNames = Property.getClassHierarchyPropertyNames(expected.getClass());
        for (String propertyName : propertyNames) {
            Property property = Property.getProperty(expected.getClass(), propertyName);
            Object expectedValue = property.getValue(expected);
            Object objectValue = property.getValue(object);
            Assert.assertEquals(expectedValue, objectValue);
        }
    }

    /**
     * Testing entity.
     */
    public static class Entity
    {
        private String string;

        private int intPrimitive;

        private Integer intObject;

        private long longPrimitive;

        private Long longObject;

        private DateTime dateTime;

        private Period period;

        private Interval interval;

        private ReadablePartial readablePartial;

        private CustomType customType;

        private Type type;

        private List<SubEntity> subEntities = new ArrayList<SubEntity>();

        private Set<Type> types = new HashSet<Type>();

        private Map<Type, String> descriptionByType = new HashMap<Type, String>();

        private Map<Type, SubEntity> subEntityByType = new HashMap<Type, SubEntity>();

        public String getString()
        {
            return string;
        }

        public void setString(String string)
        {
            this.string = string;
        }

        public int getIntPrimitive()
        {
            return intPrimitive;
        }

        public void setIntPrimitive(int intPrimitive)
        {
            this.intPrimitive = intPrimitive;
        }

        public Integer getIntObject()
        {
            return intObject;
        }

        public void setIntObject(Integer intObject)
        {
            this.intObject = intObject;
        }

        public long getLongPrimitive()
        {
            return longPrimitive;
        }

        public void setLongPrimitive(long longPrimitive)
        {
            this.longPrimitive = longPrimitive;
        }

        public Long getLongObject()
        {
            return longObject;
        }

        public void setLongObject(Long longObject)
        {
            this.longObject = longObject;
        }

        public DateTime getDateTime()
        {
            return dateTime;
        }

        public void setDateTime(DateTime dateTime)
        {
            this.dateTime = dateTime;
        }

        public Period getPeriod()
        {
            return period;
        }

        public void setPeriod(Period period)
        {
            this.period = period;
        }

        public Interval getInterval()
        {
            return interval;
        }

        public void setInterval(Interval interval)
        {
            this.interval = interval;
        }

        public ReadablePartial getReadablePartial()
        {
            return readablePartial;
        }

        public void setReadablePartial(ReadablePartial readablePartial)
        {
            this.readablePartial = readablePartial;
        }

        public CustomType getCustomType()
        {
            return customType;
        }

        public void setCustomType(CustomType customType)
        {
            this.customType = customType;
        }

        public Type getType()
        {
            return type;
        }

        public void setType(Type type)
        {
            this.type = type;
        }

        public List<SubEntity> getSubEntities()
        {
            return subEntities;
        }

        public void setSubEntities(List<SubEntity> subEntities)
        {
            this.subEntities = subEntities;
        }

        public void addSubEntity(SubEntity subEntity)
        {
            subEntities.add(subEntity);
        }

        public Set<Type> getTypes()
        {
            return types;
        }

        public void setTypes(Set<Type> types)
        {
            this.types = types;
        }

        public void addType(Type type)
        {
            types.add(type);
        }

        public Map<Type, String> getDescriptionByType()
        {
            return descriptionByType;
        }

        public void setDescriptionByType(Map<Type, String> descriptionByType)
        {
            this.descriptionByType = descriptionByType;
        }

        public void addDescriptionByType(Type type, String description)
        {
            descriptionByType.put(type, description);
        }

        public Map<Type, SubEntity> getSubEntityByType()
        {
            return subEntityByType;
        }

        public void setSubEntityByType(Map<Type, SubEntity> subEntityByType)
        {
            this.subEntityByType = subEntityByType;
        }

        public void addSubEntityByType(Type type, SubEntity subEntity)
        {
            subEntityByType.put(type, subEntity);
        }

        public static enum Type
        {
            TYPE1,
            TYPE2
        }
    }

    /**
     * Testing entity used as child.
     */
    public static class SubEntity
    {
        private String string;

        public SubEntity()
        {
        }

        public SubEntity(String string)
        {
            this.string = string;
        }

        public String getString()
        {
            return string;
        }

        public void setString(String string)
        {
            this.string = string;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null || !(obj instanceof SubEntity)) {
                return false;
            }
            SubEntity subEntity = (SubEntity) obj;
            return string.equals(subEntity.string);
        }
    }

    /**
     * Testing atomic type.
     */
    public static class CustomType implements AtomicType
    {
        private String value;

        public CustomType()
        {
        }

        public CustomType(String value)
        {
            this.value = value;
        }

        @Override
        public void fromString(String value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return value;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null || !(obj instanceof CustomType)) {
                return false;
            }
            CustomType customType = (CustomType) obj;
            return value.equals(customType.value);
        }
    }
}
