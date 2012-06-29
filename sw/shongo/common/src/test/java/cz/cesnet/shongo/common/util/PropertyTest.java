package cz.cesnet.shongo.common.util;

import cz.cesnet.shongo.common.api.API;
import cz.cesnet.shongo.common.api.ComplexType;
import cz.cesnet.shongo.common.api.Fault;
import cz.cesnet.shongo.common.api.FaultException;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertArrayEquals;

/**
 * Test fro {@link Property}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PropertyTest
{
    public static class Foo
    {
        public String field1;

        public Integer getField2()
        {
            return 2;
        }

        public void setField3(Long field3)
        {
        }

        private String fieldNot;

        public List<API.SecurityToken> field4;

        @ComplexType.AllowedTypes({String.class, API.AbsoluteDateTime.class})
        public List<Object> field5;

        @ComplexType.AllowedTypes({String.class, API.AbsoluteDateTime.class})
        public Object field6;

        public API.SecurityToken[] field7;
    }

    @Test
    public void test() throws Exception
    {
        Foo foo = new Foo();

        // hasProperty
        assertTrue(Property.hasProperty(Foo.class, "field1"));
        assertTrue(Property.hasProperty(Foo.class, "field2"));
        assertTrue(Property.hasProperty(Foo.class, "field3"));
        assertTrue(Property.hasProperty(Foo.class, "field4"));
        assertTrue(Property.hasProperty(Foo.class, "field5"));
        assertTrue(Property.hasProperty(Foo.class, "field6"));
        assertFalse(Property.hasProperty(Foo.class, "fieldNot"));

        // getPropertyNames
        String[] propertyNames = Property.getPropertyNames(Foo.class);
        Arrays.sort(propertyNames);
        assertArrayEquals(new String[]{"field1", "field2", "field3", "field4", "field5", "field6", "field7"},
                propertyNames);

        // setPropertyValue
        Property.setPropertyValue(foo, "field1", "test");
        Property.setPropertyValue(foo, "field3", Long.valueOf(3));
        try {
            Property.setPropertyValue(foo, "field2", "test");
            fail("Exception that field is read-only should be thrown.");
        }
        catch (FaultException exception) {
            assertEquals(Fault.Common.CLASS_ATTRIBUTE_READ_ONLY.getCode(), exception.getCode());
        }
        try {
            Property.setPropertyValue(foo, "fieldNot", "test");
            fail("Exception that field is not defined should be thrown.");
        }
        catch (FaultException exception) {
            assertEquals(Fault.Common.CLASS_ATTRIBUTE_NOT_DEFINED.getCode(), exception.getCode());
        }

        // getPropertyValue
        assertEquals("test", Property.getPropertyValue(foo, "field1"));
        assertEquals(2, Property.getPropertyValue(foo, "field2"));
        try {
            assertEquals(Long.valueOf(1), Property.getPropertyValue(foo, "field3"));
            fail("Exception that field is write-only should be thrown.");
        }
        catch (FaultException exception) {
            assertEquals(Fault.Common.CLASS_ATTRIBUTE_WRITE_ONLY.getCode(), exception.getCode());
        }
        try {
            Property.getPropertyValue(foo, "fieldNot");
            fail("Exception that field is not defined should be thrown.");
        }
        catch (FaultException exception) {
            assertEquals(Fault.Common.CLASS_ATTRIBUTE_NOT_DEFINED.getCode(), exception.getCode());
        }

        // Get getPropertyType and getPropertyAllowedTypes
        assertEquals(List.class, Property.getPropertyType(Foo.class, "field4"));
        assertArrayEquals(new Class[]{API.SecurityToken.class}, Property.getPropertyAllowedTypes(Foo.class, "field4"));
        assertEquals(List.class, Property.getPropertyType(Foo.class, "field5"));
        assertArrayEquals(new Class[]{String.class, API.AbsoluteDateTime.class},
                Property.getPropertyAllowedTypes(Foo.class, "field5"));
        assertEquals(Object.class, Property.getPropertyType(Foo.class, "field6"));
        assertArrayEquals(new Class[]{String.class, API.AbsoluteDateTime.class},
                Property.getPropertyAllowedTypes(Foo.class, "field6"));
        assertEquals(API.SecurityToken[].class, Property.getPropertyType(Foo.class, "field7"));
        assertArrayEquals(new Class[]{API.SecurityToken.class},
                Property.getPropertyAllowedTypes(Foo.class, "field7"));
    }
}
