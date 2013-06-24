package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.Specification;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestListRequest extends ListRequest
{
    private String reservationRequestId;

    private Set<Technology> technologies = new HashSet<Technology>();

    private Set<Class<? extends Specification>> specificationClasses = new HashSet<Class<? extends Specification>>();

    private Set<String> providedReservationIds = new HashSet<String>();

    private Sort sort;

    private Boolean sortDescending;

    public ReservationRequestListRequest()
    {
    }

    public ReservationRequestListRequest(SecurityToken securityToken)
    {
        super(securityToken);
    }

    public ReservationRequestListRequest(SecurityToken securityToken, Technology[] technologies)
    {
        super(securityToken);
        for (Technology technology : technologies) {
            this.technologies.add(technology);
        }
    }

    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    public void setReservationRequestId(String reservationRequestId)
    {
        this.reservationRequestId = reservationRequestId;
    }

    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    public Set<Class<? extends Specification>> getSpecificationClasses()
    {
        return specificationClasses;
    }

    public void setSpecificationClasses(Set<Class<? extends Specification>> specificationClasses)
    {
        this.specificationClasses = specificationClasses;
    }

    public void addSpecificationClass(Class<? extends Specification> specificationClass)
    {
        specificationClasses.add(specificationClass);
    }

    public Set<String> getProvidedReservationIds()
    {
        return providedReservationIds;
    }

    public void setProvidedReservationIds(Set<String> providedReservationIds)
    {
        this.providedReservationIds = providedReservationIds;
    }

    public void addProvidedReservationId(String providedReservationId)
    {
        providedReservationIds.add(providedReservationId);
    }

    public Sort getSort()
    {
        return sort;
    }

    public void setSort(Sort sort)
    {
        this.sort = sort;
    }

    public Boolean getSortDescending()
    {
        return sortDescending;
    }

    public void setSortDescending(Boolean sortDescending)
    {
        this.sortDescending = sortDescending;
    }

    public static enum Sort
    {
        DATETIME
    }

    private static final String RESERVATION_REQUEST_ID = "reservationRequestId";
    private static final String TECHNOLOGIES = "technologies";
    private static final String SPECIFICATION_CLASSES = "specificationClasses";
    private static final String PROVIDED_RESERVATION_IDS = "providedReservationIds";
    private static final String SORT = "sort";
    private static final String SORT_DESCENDING = "sortDescending";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESERVATION_REQUEST_ID, reservationRequestId);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(SPECIFICATION_CLASSES, specificationClasses);
        dataMap.set(PROVIDED_RESERVATION_IDS, providedReservationIds);
        dataMap.set(SORT, sort);
        dataMap.set(SORT_DESCENDING, sortDescending);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reservationRequestId = dataMap.getString(RESERVATION_REQUEST_ID);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        specificationClasses = (Set) dataMap.getSet(SPECIFICATION_CLASSES, Class.class);
        providedReservationIds = dataMap.getSet(PROVIDED_RESERVATION_IDS, String.class);
        sort = dataMap.getEnum(SORT, Sort.class);
        sortDescending = dataMap.getBool(SORT_DESCENDING);
    }
}
