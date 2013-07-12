package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.controller.api.SecurityToken;
import cz.cesnet.shongo.controller.api.Specification;

import java.util.HashSet;
import java.util.Set;

/**
 * {@link ListRequest} for reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestListRequest extends SortableListRequest<ReservationRequestListRequest.Sort>
{
    private Set<String> reservationRequestIds = new HashSet<String>();

    private Set<Technology> technologies = new HashSet<Technology>();

    private Set<Class<? extends Specification>> specificationClasses = new HashSet<Class<? extends Specification>>();

    private String providedReservationRequestId;

    private String historyReservationRequestId;

    public ReservationRequestListRequest()
    {
        super(Sort.class);
    }

    public ReservationRequestListRequest(SecurityToken securityToken)
    {
        super(Sort.class, securityToken);
    }

    public ReservationRequestListRequest(SecurityToken securityToken, Technology[] technologies)
    {
        super(Sort.class, securityToken);
        for (Technology technology : technologies) {
            this.technologies.add(technology);
        }
    }

    public Set<String> getReservationRequestIds()
    {
        return reservationRequestIds;
    }

    public void setReservationRequestIds(Set<String> reservationRequestIds)
    {
        this.reservationRequestIds = reservationRequestIds;
    }

    public void addReservationRequestId(String reservationRequestId)
    {
        reservationRequestIds.add(reservationRequestId);
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

    public String getProvidedReservationRequestId()
    {
        return providedReservationRequestId;
    }

    public void setProvidedReservationRequestId(String providedReservationRequestId)
    {
        this.providedReservationRequestId = providedReservationRequestId;
    }

    public String getHistoryReservationRequestId()
    {
        return historyReservationRequestId;
    }

    public void setHistoryReservationRequestId(String historyReservationRequestId)
    {
        this.historyReservationRequestId = historyReservationRequestId;
    }

    public static enum Sort
    {
        DATETIME
    }

    private static final String RESERVATION_REQUEST_IDS = "reservationRequestIds";
    private static final String TECHNOLOGIES = "technologies";
    private static final String SPECIFICATION_CLASSES = "specificationClasses";
    private static final String PROVIDED_RESERVATION_REQUEST_ID = "providedReservationRequestId";
    private static final String HISTORY_RESERVATION_REQUEST_ID = "historyReservationRequestId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESERVATION_REQUEST_IDS, reservationRequestIds);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(SPECIFICATION_CLASSES, specificationClasses);
        dataMap.set(PROVIDED_RESERVATION_REQUEST_ID, providedReservationRequestId);
        dataMap.set(HISTORY_RESERVATION_REQUEST_ID, historyReservationRequestId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reservationRequestIds = dataMap.getSet(RESERVATION_REQUEST_IDS, String.class);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        specificationClasses = (Set) dataMap.getSet(SPECIFICATION_CLASSES, Class.class);
        providedReservationRequestId = dataMap.getString(PROVIDED_RESERVATION_REQUEST_ID);
        historyReservationRequestId = dataMap.getString(HISTORY_RESERVATION_REQUEST_ID);
    }
}
