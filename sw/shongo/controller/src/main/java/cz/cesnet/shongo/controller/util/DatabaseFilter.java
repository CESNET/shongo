package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.Query;
import java.util.*;

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
     * Add user-id filter (entity referenced by {@link #alias} must contain "userId" property).
     *
     * @param userId user-id of the user
     */
    public void addUserId(String userId)
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
     * @param filter        from which the user-id should be parsed
     * @param defaultUserId value which will be returned when the user-id isn't present in the {@code filter}
     * @return user-id from given {@code filter}
     */
    public static String getUserIdFromFilter(Map<String, Object> filter, String defaultUserId)
    {
        String userId = defaultUserId;
        if (filter != null && filter.containsKey("userId")) {
            Object value = filter.get("userId");
            // All users
            if (value.equals("*")) {
                userId = null;
            }
            // One selected user
            else {
                userId = (value != null ? value.toString() : null);
            }
        }
        return userId;
    }

    public static Set<Technology> getTechnologiesFromFilter(Map<String, Object> filter) throws FaultException
    {
        if (filter != null && filter.containsKey("technology")) {
            @SuppressWarnings("unchecked")
            Set<Technology> technologies = (Set<Technology>) Converter.convert(filter.get("technology"), Set.class,
                    new Class[]{Technology.class});
            return technologies;
        }
        return null;
    }

    public static <T> Set<Class<? extends T>> getClassesFromFilter(Map<String, Object> filter, String key,
            Class<T> type) throws FaultException
    {
        if (filter != null && filter.containsKey(key)) {
            Object value = filter.get(key);
            if (value instanceof String) {
                value = new Object[]{value};
            }
            @SuppressWarnings("unchecked")
            Set<String> classNames = (Set<String>) Converter.convert(value, Set.class, new Class[]{String.class});
            if (classNames.size() > 0) {
                Set<Class<? extends T>> classes = new HashSet<Class<? extends T>>();
                for (String className : classNames) {
                    try {
                        @SuppressWarnings("unchecked")
                        Class<? extends T> specificationType = (Class<? extends T>) Class.forName(
                                String.format("%s.%s", type.getPackage().getName(), className));
                        classes.add(specificationType);
                    }
                    catch (ClassNotFoundException exception) {
                        throw new FaultException(exception, CommonFault.CLASS_NOT_DEFINED, className);
                    }
                }
                return classes;
            }
        }
        return null;
    }
}
