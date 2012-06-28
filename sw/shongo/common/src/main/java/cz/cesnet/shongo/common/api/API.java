package cz.cesnet.shongo.common.api;

/**
 * Common API data types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class API
{
    /**
     * Represents a security token.
     */
    public static class SecurityToken extends ComplexType
    {
        private String test;

        public void setTest(String test)
        {
            this.test = test;
        }

        public String getTest()
        {
            return test;
        }
    }

    /**
     * Represents an absolute date/time.
     */
    public static class AbsoluteDateTime implements AtomicType
    {
        @Override
        public void fromString(String string)
        {
            throw new RuntimeException("TODO: Implement AbsoluteDateTime.fromString");
        }
    }
}
