package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.api.request.ListRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.util.DatabaseFilter;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Abstract implementation of service.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractServiceImpl extends Component
{
    protected  <T> List<T> performListRequest(String queryAlias, String querySelect, Class<T> querySelectClass,
            String queryFrom, DatabaseFilter filter, ListRequest listRequest, ListResponse listResponse,
            EntityManager entityManager)
    {
        String queryWhere = filter.toQueryWhere();

        // Create query for listing
        StringBuilder listQueryBuilder = new StringBuilder();
        listQueryBuilder.append(querySelect);
        listQueryBuilder.append(" ");
        listQueryBuilder.append(queryFrom);
        listQueryBuilder.append(" WHERE ");
        listQueryBuilder.append(queryWhere);
        TypedQuery<T> listQuery = entityManager.createQuery(listQueryBuilder.toString(), querySelectClass);

        // Create query for counting records
        StringBuilder countQueryBuilder = new StringBuilder();
        countQueryBuilder.append("SELECT COUNT(");
        countQueryBuilder.append(queryAlias);
        countQueryBuilder.append(".id) ");
        countQueryBuilder.append(queryFrom);
        countQueryBuilder.append(" WHERE ");
        countQueryBuilder.append(queryWhere);
        TypedQuery<Long> countQuery = entityManager.createQuery(countQueryBuilder.toString(), Long.class);

        // Fill filter parameters to queries
        filter.fillQueryParameters(listQuery);
        filter.fillQueryParameters(countQuery);

        // Get total record count
        Integer totalResultCount = countQuery.getSingleResult().intValue();

        // Restrict first result
        Integer firstResult = listRequest.getStart(0);
        if (firstResult < 0) {
            firstResult = 0;
        }
        listQuery.setFirstResult(firstResult);

        // Restrict result count
        Integer maxResultCount = listRequest.getCount(-1);
        if (maxResultCount != null && maxResultCount != -1) {
            if ((firstResult + maxResultCount) > totalResultCount) {
                maxResultCount = totalResultCount - firstResult;
            }
            listQuery.setMaxResults(maxResultCount);
        }

        // Setup response
        listResponse.setCount(totalResultCount);
        listResponse.setStart(firstResult);

        // List requested results
        return listQuery.getResultList();
    }
}
