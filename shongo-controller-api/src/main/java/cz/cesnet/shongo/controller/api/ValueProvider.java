package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import cz.cesnet.shongo.controller.FilterType;

import java.util.LinkedList;
import java.util.List;

/**
 * Objects which can allocate unique values.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ValueProvider extends IdentifiedComplexType
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
        private List<String> patterns = new LinkedList<String>();

        /**
         * Option specifying whether any requested values are allowed event those which doesn't
         * match the {@link #patterns}.
         */
        private Boolean allowAnyRequestedValue;

        /**
         * Constructor.
         */
        public Pattern()
        {
        }

        /**
         * Constructor.
         *
         * @param pattern to be added to the {@link #patterns}
         */
        public Pattern(String pattern)
        {
            addPattern(pattern);
        }

        /**
         * @return {@link #patterns}
         */
        public List<String> getPatterns()
        {
            return patterns;
        }

        /**
         * @param pattern to be added to the {@link #patterns}
         */
        public void addPattern(String pattern)
        {
            patterns.add(pattern);
        }

        /**
         * @param pattern to be removed from the {@link #patterns}
         */
        public void removePattern(String pattern)
        {
            patterns.remove(pattern);
        }

        /**
         * @return {@link #allowAnyRequestedValue}
         */
        public Boolean getAllowAnyRequestedValue()
        {
            return allowAnyRequestedValue;
        }

        /**
         * @param allowAnyRequestedValue sets the {@link #allowAnyRequestedValue}
         */
        public void setAllowAnyRequestedValue(Boolean allowAnyRequestedValue)
        {
            this.allowAnyRequestedValue = allowAnyRequestedValue;
        }

        public static final String PATTERNS = "patterns";
        public static final String ALLOW_ANY_REQUESTED_VALUE = "allowAnyRequestedValue";

        @Override
        public DataMap toData()
        {
            DataMap dataMap = super.toData();
            dataMap.set(PATTERNS, patterns);
            dataMap.set(ALLOW_ANY_REQUESTED_VALUE, allowAnyRequestedValue);
            return dataMap;
        }

        @Override
        public void fromData(DataMap dataMap)
        {
            super.fromData(dataMap);
            patterns = dataMap.getStringListRequired(PATTERNS, DEFAULT_COLUMN_LENGTH);
            allowAnyRequestedValue = dataMap.getBool(ALLOW_ANY_REQUESTED_VALUE);
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
        private Object valueProvider;

        /**
         * Filtration type.
         */
        private FilterType filterType;

        /**
         * Constructor.
         */
        public Filtered()
        {
        }

        /**
         * Constructor.
         *
         * @param filterType       sets the {@link #filterType}
         * @param resourceId sets the {@link #valueProvider}
         */
        public Filtered(FilterType filterType, String resourceId)
        {
            setFilterType(filterType);
            setValueProvider(resourceId);
        }

        /**
         * Constructor.
         *
         * @param filterType          sets the {@link #filterType}
         * @param valueProvider sets the {@link #valueProvider}
         */
        public Filtered(FilterType filterType, ValueProvider valueProvider)
        {
            setFilterType(filterType);
            setValueProvider(valueProvider);
        }

        /**
         * @return {@link #valueProvider}
         */
        public Object getValueProvider()
        {
            return valueProvider;
        }

        /**
         * @param valueProvider sets the {@link #valueProvider}
         */
        public void setValueProvider(Object valueProvider)
        {
            if (valueProvider instanceof ValueProvider || valueProvider instanceof String) {
                this.valueProvider = valueProvider;
            }
            else {
                throw new TodoImplementException(valueProvider.getClass());
            }
        }

        /**
         * @return {@link #filterType}
         */
        public FilterType getFilterType()
        {
            return filterType;
        }

        /**
         * @param filterType sets the {@link #filterType}
         */
        public void setFilterType(FilterType filterType)
        {
            this.filterType = filterType;
        }

        public static final String VALUE_PROVIDER = "valueProvider";
        public static final String FILTER_TYPE = "filterType";

        @Override
        public DataMap toData()
        {
            DataMap dataMap = super.toData();
            dataMap.set(FILTER_TYPE, filterType);

            if (valueProvider instanceof String) {
                dataMap.set(VALUE_PROVIDER, (String) valueProvider);
            }
            else if (valueProvider instanceof ValueProvider) {
                dataMap.set(VALUE_PROVIDER, (ValueProvider) valueProvider);
            }

            return dataMap;
        }

        @Override
        public void fromData(DataMap dataMap)
        {
            super.fromData(dataMap);
            valueProvider = dataMap.getVariantRequired(VALUE_PROVIDER, ValueProvider.class, String.class);
            filterType = dataMap.getEnumRequired(FILTER_TYPE, FilterType.class);
        }
    }
}
