package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.util.StringHelper;

import java.text.Normalizer;

/**
 * Represents filtration types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum FilterType
{
    /**
     * Filter for converting value to url.
     */
    CONVERT_TO_URL;

    /**
     * @param value      to be filtered
     * @param filterType type of filter
     * @return given {@code value} filtered by given {@code filterType}
     */
    public static String applyFilter(String value, FilterType filterType)
    {
        switch (filterType) {
            case CONVERT_TO_URL:
                String filteredValue = StringHelper.removeAccents(value);
                filteredValue = filteredValue.toLowerCase();
                filteredValue = filteredValue.replaceAll(" ", "-");
                filteredValue = filteredValue.replaceAll("[^a-z0-9_-]", "");
                return filteredValue;
            default:
                throw new TodoImplementException(filterType);
        }
    }
}
