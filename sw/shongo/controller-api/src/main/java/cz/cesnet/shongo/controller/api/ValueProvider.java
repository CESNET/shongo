package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.oldapi.annotation.AllowedTypes;
import cz.cesnet.shongo.oldapi.annotation.Required;
import cz.cesnet.shongo.oldapi.util.IdentifiedChangeableObject;
import cz.cesnet.shongo.controller.FilterType;

import java.util.List;

/**
 * Objects which can allocate unique values.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ValueProvider extends IdentifiedChangeableObject
{
    /**
     * Object which can allocate unique values from given patterns.
     */
    public static class Pattern extends ValueProvider
    {
        /**
         * Pattern for aliases.
         * <p/>
         * Examples:
         * 1) "95{digit:3}"           will generate 95001, 95002, 95003, ...
         * 2) "95{digit:2}2{digit:2}" will generate 9500201, 9500202, ..., 9501200, 9501201, ...
         */
        public static final String PATTERNS = "patterns";

        /**
         * Option specifying whether any requested values are allowed event those which doesn't
         * match the {@link #PATTERNS}.
         */
        public static final String ALLOW_ANY_REQUESTED_VALUE = "allowAnyRequestedValue";

        /**
         * Constructor.
         */
        public Pattern()
        {
        }

        /**
         * Constructor.
         *
         * @param pattern to be added to the {@link #PATTERNS}
         */
        public Pattern(String pattern)
        {
            addPattern(pattern);
        }

        /**
         * @return {@link #PATTERNS}
         */
        @Required
        public List<String> getPatterns()
        {
            return getPropertyStorage().getCollection(PATTERNS, List.class);
        }

        /**
         * @param patterns sets the {@link #PATTERNS}
         */
        public void setPatterns(List<String> patterns)
        {
            getPropertyStorage().setValue(PATTERNS, patterns);
        }

        /**
         * @param pattern to be added to the {@link #PATTERNS}
         */
        public void addPattern(String pattern)
        {
            getPropertyStorage().addCollectionItem(PATTERNS, pattern, List.class);
        }

        /**
         * @param pattern to be removed from the {@link #PATTERNS}
         */
        public void removePattern(String pattern)
        {
            getPropertyStorage().removeCollectionItem(PATTERNS, pattern);
        }

        /**
         * @return {@link #ALLOW_ANY_REQUESTED_VALUE}
         */
        public Boolean getAllowAnyRequestedValue()
        {
            return getPropertyStorage().getValueAsBoolean(ALLOW_ANY_REQUESTED_VALUE, false);
        }

        /**
         * @param allowAnyRequestedValue sets the {@link #ALLOW_ANY_REQUESTED_VALUE}
         */
        public void setAllowAnyRequestedValue(Boolean allowAnyRequestedValue)
        {
            getPropertyStorage().setValue(ALLOW_ANY_REQUESTED_VALUE, allowAnyRequestedValue);
        }
    }

    /**
     * {@link ValueProvider} which allocates filtered values from different {@link ValueProvider}.
     */
    public static class Filtered extends ValueProvider
    {
        /**
         * Identifier of resource with {@link ValueProviderCapability} or instance of the {@link ValueProvider}.
         * {@link ValueProvider} from which the values are allocated.
         */
        public static final String VALUE_PROVIDER = "valueProvider";

        /**
         * Filtration type.
         */
        public static final String TYPE = "type";

        /**
         * Constructor.
         */
        public Filtered()
        {
        }

        /**
         * Constructor.
         *
         * @param type       sets the {@link #TYPE}
         * @param resourceId sets the {@link #VALUE_PROVIDER}
         */
        public Filtered(FilterType type, String resourceId)
        {
            setType(type);
            setValueProvider(resourceId);
        }

        /**
         * Constructor.
         *
         * @param type          sets the {@link #TYPE}
         * @param valueProvider sets the {@link #VALUE_PROVIDER}
         */
        public Filtered(FilterType type, ValueProvider valueProvider)
        {
            setType(type);
            setValueProvider(valueProvider);
        }

        /**
         * @return {@link #VALUE_PROVIDER}
         */
        @Required
        @AllowedTypes({String.class, ValueProvider.class})
        public Object getValueProvider()
        {
            return getPropertyStorage().getValue(VALUE_PROVIDER);
        }

        /**
         * @param valueProvider sets the {@link #VALUE_PROVIDER}
         */
        public void setValueProvider(Object valueProvider)
        {
            getPropertyStorage().setValue(VALUE_PROVIDER, valueProvider);
        }

        /**
         * @return {@link #TYPE}
         */
        @Required
        public FilterType getType()
        {
            return getPropertyStorage().getValue(TYPE);
        }

        /**
         * @param type sets the {@link #TYPE}
         */
        public void setType(FilterType type)
        {
            getPropertyStorage().setValue(TYPE, type);
        }
    }
}
