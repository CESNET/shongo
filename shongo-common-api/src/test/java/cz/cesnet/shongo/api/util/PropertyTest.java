package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.CommonReportSet;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

        public List<Object> field5;

        public Object field6;

        public SecurityToken[] field7;
    }

    @Test
    public void test() throws Exception
    {
        Foo foo = new Foo();

        // hasProperty
        Assert.assertTrue(Property.hasProperty(Foo.class, "field1"));
        Assert.assertTrue(Property.hasProperty(Foo.class, "field2"));
        Assert.assertFalse(Property.hasProperty(Foo.class, "field3"));
        Assert.assertFalse(Property.hasProperty(Foo.class, "field3")); // From Property cache, also should be tested
        Assert.assertTrue(Property.hasProperty(Foo.class, "field4"));
        Assert.assertTrue(Property.hasProperty(Foo.class, "field5"));
        Assert.assertTrue(Property.hasProperty(Foo.class, "field6"));
        Assert.assertFalse(Property.hasProperty(Foo.class, "fieldNot"));

        // getClassHierarchyPropertyNames
        Collection<String> propertyNames = Property.getClassHierarchyPropertyNames(Foo.class);
        String[] propertyNameArray = propertyNames.toArray(new String[propertyNames.size()]);
        Arrays.sort(propertyNameArray);
        Assert.assertArrayEquals(new String[]{"field1", "field2", "field4", "field5", "field6", "field7"},
                propertyNameArray);

        // setPropertyValue
        Property.setPropertyValue(foo, "field1", "test", true);
        try {
            Property.setPropertyValue(foo, "field2", 111);
            Assert.fail("Exception that field is read-only should be thrown.");
        }
        catch (CommonReportSet.ClassAttributeReadonlyException exception) {
        }
        try {
            Property.setPropertyValue(foo, "fieldNot", "test");
            Assert.fail("Exception that field is not defined should be thrown.");
        }
        catch (CommonReportSet.ClassAttributeUndefinedException exception) {
        }

        // getPropertyValue
        Assert.assertEquals("by setter test", Property.getPropertyValue(foo, "field1"));
        Assert.assertEquals(2, Property.getPropertyValue(foo, "field2"));
        try {
            Assert.assertEquals(Long.valueOf(1), Property.getPropertyValue(foo, "field3"));
            Assert.fail("Exception that field is not defined should be thrown.");
        }
        catch (CommonReportSet.ClassAttributeUndefinedException exception) {
        }
        try {
            Property.getPropertyValue(foo, "fieldNot");
            Assert.fail("Exception that field is not defined should be thrown.");
        }
        catch (CommonReportSet.ClassAttributeUndefinedException exception) {
        }

        // getPropertyType and getPropertyValueAllowedTypes
        Assert.assertEquals(List.class, Property.getPropertyType(Foo.class, "field4"));
        Assert.assertArrayEquals(new Class[]{SecurityToken.class}, Property.getPropertyValueAllowedTypes(Foo.class, "field4"));
        Assert.assertEquals(List.class, Property.getPropertyType(Foo.class, "field5"));
        Assert.assertEquals(Object.class, Property.getPropertyType(Foo.class, "field6"));
        Assert.assertEquals(SecurityToken[].class, Property.getPropertyType(Foo.class, "field7"));
        Assert.assertArrayEquals(new Class[]{SecurityToken.class},
                Property.getPropertyValueAllowedTypes(Foo.class, "field7"));
    }
}
