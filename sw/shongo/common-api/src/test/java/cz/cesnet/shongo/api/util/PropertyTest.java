package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.CommonFaultSet;
import cz.cesnet.shongo.api.annotation.AllowedTypes;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.old.CommonFault;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertArrayEquals;

/**
 * Test for {@link Property}
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PropertyTest
{
    public static class SecurityToken
    {
    }

    public static class DateTime
    {
    }

    public static class Bar
    {
        public String field1;

        private void setField1(String field1)
        {
            this.field1 = "by setter " + field1;
        }
    }

    public static class Foo extends Bar
    {
        public Integer getField2()
        {
            return 2;
        }

        public void setField3(Long field3)
        {
        }

        private String fieldNot;

        public List<SecurityToken> field4;

        @AllowedTypes({String.class, DateTime.class})
        public List<Object> field5;

        @AllowedTypes({String.class, DateTime.class})
        public Object field6;

        public SecurityToken[] field7;
    }

    @Test
    public void test() throws Exception
    {
        Foo foo = new Foo();

        // hasProperty
        assertTrue(Property.hasProperty(Foo.class, "field1"));
        assertTrue(Property.hasProperty(Foo.class, "field2"));
        assertFalse(Property.hasProperty(Foo.class, "field3"));
        assertFalse(Property.hasProperty(Foo.class, "field3")); // From Property cache, also should be tested
        assertTrue(Property.hasProperty(Foo.class, "field4"));
        assertTrue(Property.hasProperty(Foo.class, "field5"));
        assertTrue(Property.hasProperty(Foo.class, "field6"));
        assertFalse(Property.hasProperty(Foo.class, "fieldNot"));

        // getClassHierarchyPropertyNames
        Collection<String> propertyNames = Property.getClassHierarchyPropertyNames(Foo.class);
        String[] propertyNameArray = propertyNames.toArray(new String[propertyNames.size()]);
        Arrays.sort(propertyNameArray);
        assertArrayEquals(new String[]{"field1", "field2", "field4", "field5", "field6", "field7"},
                propertyNameArray);

        // setPropertyValue
        Property.setPropertyValue(foo, "field1", "test", true);
        try {
            Property.setPropertyValue(foo, "field2", 111);
            fail("Exception that field is read-only should be thrown.");
        }
        catch (FaultException exception) {
            assertEquals(CommonFaultSet.ClassAttributeReadonlyFault.class, exception.getFaultClass());
        }
        try {
            Property.setPropertyValue(foo, "fieldNot", "test");
            fail("Exception that field is not defined should be thrown.");
        }
        catch (FaultException exception) {
            assertEquals(CommonFaultSet.ClassAttributeUndefinedFault.class, exception.getFaultClass());
        }

        // getPropertyValue
        assertEquals("by setter test", Property.getPropertyValue(foo, "field1"));
        assertEquals(2, Property.getPropertyValue(foo, "field2"));
        try {
            assertEquals(Long.valueOf(1), Property.getPropertyValue(foo, "field3"));
            fail("Exception that field is not defined should be thrown.");
        }
        catch (FaultException exception) {
            assertEquals(CommonFaultSet.ClassAttributeUndefinedFault.class, exception.getFaultClass());
        }
        try {
            Property.getPropertyValue(foo, "fieldNot");
            fail("Exception that field is not defined should be thrown.");
        }
        catch (FaultException exception) {
            assertEquals(CommonFaultSet.ClassAttributeUndefinedFault.class, exception.getFaultClass());
        }

        // getPropertyType and getPropertyValueAllowedTypes
        assertEquals(List.class, Property.getPropertyType(Foo.class, "field4"));
        assertArrayEquals(new Class[]{SecurityToken.class}, Property.getPropertyValueAllowedTypes(Foo.class, "field4"));
        assertEquals(List.class, Property.getPropertyType(Foo.class, "field5"));
        assertArrayEquals(new Class[]{String.class, DateTime.class},
                Property.getPropertyValueAllowedTypes(Foo.class, "field5"));
        assertEquals(Object.class, Property.getPropertyType(Foo.class, "field6"));
        assertArrayEquals(new Class[]{String.class, DateTime.class},
                Property.getPropertyValueAllowedTypes(Foo.class, "field6"));
        assertEquals(SecurityToken[].class, Property.getPropertyType(Foo.class, "field7"));
        assertArrayEquals(new Class[]{SecurityToken.class},
                Property.getPropertyValueAllowedTypes(Foo.class, "field7"));
    }
}
