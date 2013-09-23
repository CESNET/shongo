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

    private String parentReservationRequestId;

    private Set<Technology> technologies = new HashSet<Technology>();

    private Set<Class<? extends Specification>> specificationClasses = new HashSet<Class<? extends Specification>>();

    private String reusedReservationRequestId;

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

    public String getParentReservationRequestId()
    {
        return parentReservationRequestId;
    }

    public void setParentReservationRequestId(String parentReservationRequestId)
    {
        this.parentReservationRequestId = parentReservationRequestId;
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

    public String getReusedReservationRequestId()
    {
        return reusedReservationRequestId;
    }

    public void setReusedReservationRequestId(String reusedReservationRequestId)
    {
        this.reusedReservationRequestId = reusedReservationRequestId;
    }

    public static enum Sort
    {
        ALIAS_ROOM_NAME,
        DATETIME,
        REUSED_RESERVATION_REQUEST,
        ROOM_PARTICIPANT_COUNT,
        SLOT,
        SLOT_NEAREST,
        STATE,
        TECHNOLOGY,
        TYPE,
        USER
    }

    private static final String RESERVATION_REQUEST_IDS = "reservationRequestIds";
    private static final String PARENT_RESERVATION_REQUEST_ID = "parentReservationRequestId";
    private static final String TECHNOLOGIES = "technologies";
    private static final String SPECIFICATION_CLASSES = "specificationClasses";
    private static final String REUSED_RESERVATION_REQUEST_ID = "reusedReservationRequestId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESERVATION_REQUEST_IDS, reservationRequestIds);
        dataMap.set(PARENT_RESERVATION_REQUEST_ID, parentReservationRequestId);
        dataMap.set(TECHNOLOGIES, technologies);
        dataMap.set(SPECIFICATION_CLASSES, specificationClasses);
        dataMap.set(REUSED_RESERVATION_REQUEST_ID, reusedReservationRequestId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reservationRequestIds = dataMap.getSet(RESERVATION_REQUEST_IDS, String.class);
        parentReservationRequestId = dataMap.getString(PARENT_RESERVATION_REQUEST_ID);
        technologies = dataMap.getSet(TECHNOLOGIES, Technology.class);
        specificationClasses = (Set) dataMap.getSet(SPECIFICATION_CLASSES, Class.class);
        reusedReservationRequestId = dataMap.getString(REUSED_RESERVATION_REQUEST_ID);
    }
}
