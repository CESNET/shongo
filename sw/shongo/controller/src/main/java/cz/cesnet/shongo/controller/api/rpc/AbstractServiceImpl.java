package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.api.request.ListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.util.QueryFilter;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Abstract implementation of service.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractServiceImpl extends Component
{
    /**
     * @param query            query
     * @param queryFilter      {@link QueryFilter} for filtering select and count statement
     * @param listRequest      {@link ListRequest}  object
     * @param listResponse     {@link ListResponse} object
     * @param entityManager    to be used for performing queries
     * @return list of objects
     */
    protected List<Object[]> performNativeListRequest(String query, QueryFilter queryFilter,
            ListRequest listRequest, ListResponse listResponse, EntityManager entityManager)
    {
        StringBuilder countQueryBuilder = new StringBuilder();
        countQueryBuilder.append("SELECT COUNT(*) FROM (");
        countQueryBuilder.append(query);
        countQueryBuilder.append(") AS count");

        Query queryList = entityManager.createNativeQuery(query);
        Query queryCount = entityManager.createNativeQuery(countQueryBuilder.toString());
        return getResponse(queryList, queryCount, queryFilter, listRequest, listResponse);
    }

    /**
     * @param query            query
     * @param queryFilter      {@link QueryFilter} for filtering select and count statement
     * @param queryResultClass query result type
     * @param listRequest      {@link ListRequest}  object
     * @param listResponse     {@link ListResponse} object
     * @param entityManager    to be used for performing queries
     * @return list of objects of given {@code querySelectClass}
     */
    protected <T> List<T> performListRequest(String query, QueryFilter queryFilter, Class<T> queryResultClass,
            ListRequest listRequest, ListResponse listResponse, EntityManager entityManager)
    {
        int fromPosition = query.indexOf("FROM");
        String countQuery = "SELECT COUNT(*) " + query.substring(fromPosition);

        TypedQuery<T> queryList = entityManager.createQuery(query, queryResultClass);
        TypedQuery<Long> queryCount = entityManager.createQuery(countQuery, Long.class);
        return getResponse(queryList, queryCount, queryFilter, listRequest, listResponse);
    }

    private <T> List<T> getResponse(Query queryList, Query queryCount, QueryFilter queryFilter,
            ListRequest listRequest, ListResponse listResponse)
    {
        // Fill filter parameters to queries
        queryFilter.fillQueryParameters(queryList);
        queryFilter.fillQueryParameters(queryCount);

        // Restrict first result
        Integer firstResult = listRequest.getStart(0);
        if (firstResult < 0) {
            firstResult = 0;
        }
        queryList.setFirstResult(firstResult);

        // Restrict result count
        Integer totalResultCount = null;
        Integer maxResultCount = listRequest.getCount(-1);
        if (maxResultCount != null && maxResultCount != -1) {
            totalResultCount = ((Number) queryCount.getSingleResult()).intValue();
            if ((firstResult + maxResultCount) > totalResultCount) {
                maxResultCount = totalResultCount - firstResult;
            }
            queryList.setMaxResults(maxResultCount);
        }

        // List requested results
        @SuppressWarnings("unchecked")
        List<T> resultList = (List<T>) queryList.getResultList();
        if (totalResultCount == null) {
            totalResultCount = resultList.size();
        }

        // Setup response
        listResponse.setCount(totalResultCount);
        listResponse.setStart(firstResult);

        return resultList;
    }
}
