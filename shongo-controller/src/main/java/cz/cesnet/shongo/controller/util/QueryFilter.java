package cz.cesnet.shongo.controller.util;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Converter;
import cz.cesnet.shongo.controller.ObjectPermission;
import cz.cesnet.shongo.controller.acl.AclObjectClass;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.authorization.Authorization;

import javax.persistence.Query;
import java.util.*;

/**
 * Utility class which represents a SQL where clause for filtering database records.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class QueryFilter
{
    /**
     * Specifies whether filter should be for native query.
     */
    private boolean nativeQuery = false;

    /**
     * Query alias of the main entity.
     */
    private String alias;

    /**
     * List of filters.
     */
    private List<String> filters = new ArrayList<String>();

    /**
     * Map of parameters for filters.
     */
    private Map<String, Object> parameters = new HashMap<String, Object>();

    /**
     * Constructor.
     *
     * @param alias sets the {@link #alias}
     */
    public QueryFilter(String alias)
    {
        this.alias = alias;
    }

    /**
     * Constructor.
     *
     * @param alias sets the {@link #alias}
     */
    public QueryFilter(String alias, boolean nativeQuery)
    {
        this.alias = alias;
        this.nativeQuery = nativeQuery;
    }

    /**
     * @param filter to be added to the {@link #filters}
     */
    public void addFilter(String filter)
    {
        filters.add(filter);
    }

    /**
     * @param filter         to be added to the {@link #filters}
     * @param parameterName  name of parameter to be added
     * @param parameterValue value of parameter to be added
     */
    public void addFilter(String filter, String parameterName, Object parameterValue)
    {
        filters.add(filter);
        parameters.put(parameterName, parameterValue);
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
     * Add parameter for filters.
     *
     * @param name  of the parameter
     * @param enumValues of the parameter
     */
    public <T extends Enum> void addFilterParameter(String name, Collection<T> enumValues)
    {
        if (nativeQuery) {
            Set<String> values = new HashSet<String>();
            for (T enumValue : enumValues) {
                values.add(enumValue.toString());
            }
            parameters.put(name, values);
        }
        else {
            parameters.put(name, enumValues);
        }
    }

    /**
     * Add identifier filter.
     *
     * @param values allowed identifiers of the entity.
     */
    public void addFilterIn(String column, Set<?> values)
    {
        if (values != null) {
            if (values.isEmpty()) {
                addFilter(alias + "." + column + " IN (0)");
            }
            else {
                addFilter(alias + "." + column + " IN (:ids)");
                addFilterParameter("ids", values);
            }
        }
    }

    /**
     * Add identifier filter.
     *
     * @param authorization
     * @param securityToken
     * @param objectClass
     * @param objectPermission
     */
    public void addFilterId(Authorization authorization, SecurityToken securityToken,
            Class<? extends PersistentObject> objectClass, ObjectPermission objectPermission)
    {
        addFilterId("id", authorization, securityToken, objectClass, objectPermission);
    }

    /**
     * Add identifier filter.
     *
     * @param column
     * @param authorization
     * @param securityToken
     * @param objectClass
     * @param objectPermission
     */
    public void addFilterId(String column, Authorization authorization, SecurityToken securityToken,
            Class<? extends PersistentObject> objectClass, ObjectPermission objectPermission)
    {
        addFilterIn(column, authorization.getEntitiesWithPermission(securityToken, objectClass, objectPermission));
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

    public static Set<Technology> getTechnologiesFromFilter(Map<String, Object> filter)
    {
        if (filter != null && filter.containsKey("technology")) {
            Object value = filter.get("technology");
            if (value instanceof String) {
                value = new Object[]{value};
            }
            @SuppressWarnings("unchecked")
            Set<Technology> technologies = (Set<Technology>) Converter.convertToSet(value, Technology.class);
            return technologies;
        }
        return null;
    }

    public static <T> Set<Class<? extends T>> getClassesFromFilter(Map<String, Object> filter, String key,
            Class<T> type)
    {
        if (filter != null && filter.containsKey(key)) {
            Object value = filter.get(key);
            if (value instanceof String) {
                value = new Object[]{value};
            }
            @SuppressWarnings("unchecked")
            Set<String> classNames = (Set<String>) Converter.convertToSet(value, String.class);
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
                        throw new CommonReportSet.ClassUndefinedException(className);
                    }
                }
                return classes;
            }
        }
        return null;
    }
}
