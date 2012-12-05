package cz.cesnet.shongo.controller.util;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for filtering database records.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DatabaseFilter
{
    /**
     * Query alias of the main entity.
     */
    String alias;

    /**
     * List of filters.
     */
    List<String> filters = new ArrayList<String>();

    /**
     * Map of parameters for filters.
     */
    Map<String, Object> parameters = new HashMap<String, Object>();

    /**
     * Constructor.
     *
     * @param alias sets the {@link #alias}
     */
    public DatabaseFilter(String alias)
    {
        this.alias = alias;
    }

    /**
     * @param filter to be added to the {@link #filters}
     */
    public void addFilter(String filter)
    {
        filters.add(filter);
    }

    /**
     * Add parameter for filters.
     *
     * @param name  of the parameter
     * @param value of the parameter
     */
    public void addFilterParameter(String name, Object value)
    {
        parameters.put(name, value);
    }

    /**
     * Add user identifier filter (entity referenced by {@link #alias} must contain "userId" property).
     *
     * @param userId identifier of the user
     */
    public void addUserId(Long userId)
    {
        if (userId != null) {
            addFilter(alias + ".userId = :userId");
            addFilterParameter("userId", userId);
        }
    }

    /**
     * @param query to which all {@link #parameters} shoud be added
     */
    public void fillQueryParameters(Query query)
    {
        for (String name : parameters.keySet()) {
            query.setParameter(name, parameters.get(name));
        }
    }

    /**
     * @return filter formatted to query where string
     */
    public String toQueryWhere()
    {
        StringBuilder queryWhere = new StringBuilder("1=1");
        for (String filter : filters) {
            queryWhere.append(" AND (");
            queryWhere.append(filter);
            queryWhere.append(")");
        }
        return queryWhere.toString();
    }

    /**
     * @param filter        from which the user identifier should be parsed
     * @param defaultUserId value which will be returned when the user identifier isn't present in the {@code filter}
     * @return user identifier from given {@code filter}
     */
    public static Long getUserIdFromFilter(Map<String, Object> filter, Long defaultUserId)
    {
        Long userId = defaultUserId;
        if (filter != null) {
            if (filter.containsKey("userId")) {
                Object value = filter.get("userId");
                // All users
                if (value.equals("*")) {
                    userId = null;
                }
                // One selected user
                else {
                    userId = (value != null ? Long.valueOf(value.toString()) : null);
                }
            }
        }
        return userId;
    }
}
