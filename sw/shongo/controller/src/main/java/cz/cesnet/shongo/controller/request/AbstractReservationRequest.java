package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.report.ReportablePersistentObject;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.reservation.ResourceReservation;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a common attributes for all types of reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractReservationRequest extends ReportablePersistentObject
{
    /**
     * Date/time when the reservation request was created.
     */
    private DateTime created;

    /**
     * Type of the reservation. Permanent reservation are created by resource owners to
     * allocate the resource for theirs activity.
     */
    private ReservationRequestType type;

    /**
     * Purpose for the reservation (science/education).
     */
    private ReservationRequestPurpose purpose;

    /**
     * Name of the reservation that is shown to users.
     */
    private String name;

    /**
     * Description of the reservation that is shown to users.
     */
    private String description;

    /**
     * Option that specifies whether inter-domain resource lookup can be performed.
     */
    private boolean interDomain;

    /**
     * List of {@link Reservation}s of allocated resources which can be used by {@link Scheduler} for allocation of
     * this {@link AbstractReservationRequest}.
     */
    private List<Reservation> providedReservations = new ArrayList<Reservation>();

    /**
     * @return {@link #created}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.FIELD)
    public DateTime getCreated()
    {
        return created;
    }

    /**
     * @return {@link #type}
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public ReservationRequestType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(ReservationRequestType type)
    {
        this.type = type;
    }

    /**
     * @return {@link #purpose}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public ReservationRequestPurpose getPurpose()
    {
        return purpose;
    }

    /**
     * @param purpose sets the {@link #purpose}
     */
    public void setPurpose(ReservationRequestPurpose purpose)
    {
        this.purpose = purpose;
    }

    /**
     * @return {@link #name}
     */
    @Column
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #name}
     */
    @Column
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description sets the {@link #description}
     */
    public void setDescription(String description)
    {
        this.description = description;
    }


    /**
     * @return {@link #interDomain}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isInterDomain()
    {
        return interDomain;
    }

    /**
     * @param interDomain sets the {@link #interDomain}
     */
    public void setInterDomain(boolean interDomain)
    {
        this.interDomain = interDomain;
    }

    /**
     * @return {@link #providedReservations}
     */
    @ManyToMany
    @Access(AccessType.FIELD)
    public List<Reservation> getProvidedReservations()
    {
        return providedReservations;
    }

    /**
     * @param providedReservations sets the {@link #providedReservations}
     */
    public void setProvidedReservations(List<Reservation> providedReservations)
    {
        this.providedReservations.clear();
        for (Reservation providedReservation : providedReservations) {
            this.providedReservations.add(providedReservation);
        }
    }

    /**
     * @param providedReservation to be added to the {@link #providedReservations}
     */
    public void addProvidedReservation(Reservation providedReservation)
    {
        providedReservations.add(providedReservation);
    }

    /**
     * @param providedReservationIdentifier for {@link Reservation} to be removed from {@link #providedReservations}
     */
    public void removeProvidedReservation(Long providedReservationIdentifier)
    {
        for (int index = 0; index < providedReservations.size(); index++) {
            if (providedReservations.get(index).getId().equals(providedReservationIdentifier)) {
                providedReservations.remove(index);
                break;
            }
        }
    }

    /**
     * Synchronize properties from given {@code abstractReservationRequest}.
     *
     * @param abstractReservationRequest from which will be copied all properties values to
     *                                   this {@link AbstractReservationRequest}
     * @return true if some modification was made
     */
    public boolean synchronizeFrom(AbstractReservationRequest abstractReservationRequest)
    {
        boolean modified = !ObjectUtils.equals(getType(), abstractReservationRequest.getType())
                || !ObjectUtils.equals(getPurpose(), abstractReservationRequest.getPurpose())
                || !ObjectUtils.equals(getName(), abstractReservationRequest.getName())
                || !ObjectUtils.equals(getDescription(), abstractReservationRequest.getDescription())
                || !ObjectUtils.equals(isInterDomain(), abstractReservationRequest.isInterDomain());
        setType(abstractReservationRequest.getType());
        setPurpose(abstractReservationRequest.getPurpose());
        setName(abstractReservationRequest.getName());
        setDescription(abstractReservationRequest.getDescription());
        setInterDomain(abstractReservationRequest.isInterDomain());
        if (!ObjectUtils.equals(getProvidedReservations(), abstractReservationRequest.getProvidedReservations())) {
            setProvidedReservations(abstractReservationRequest.getProvidedReservations());
            modified = true;
        }
        return modified;
    }

    @PrePersist
    protected void onCreate()
    {
        if (created == null) {
            created = DateTime.now();
        }
    }

    /**
     * @return converted {@link AbstractReservationRequest}
     *         to {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     * @throws FaultException
     */
    public cz.cesnet.shongo.controller.api.AbstractReservationRequest toApi(Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.AbstractReservationRequest api = createApi();
        toApi(api, domain);
        return api;
    }

    /**
     * @param api
     * @param entityManager
     * @return new instance of {@link AbstractReservationRequest} from
     *         {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     * @throws FaultException
     */
    public static AbstractReservationRequest createFromApi(
            cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        AbstractReservationRequest reservationRequest;
        if (api instanceof cz.cesnet.shongo.controller.api.ReservationRequest) {
            reservationRequest = new ReservationRequest();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ReservationRequestSet) {
            reservationRequest = new ReservationRequestSet();
        }
        else {
            throw new TodoImplementException();
        }
        reservationRequest.fromApi(api, entityManager, domain);
        return reservationRequest;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}
     */
    protected abstract cz.cesnet.shongo.controller.api.AbstractReservationRequest createApi();

    /**
     * @param api    {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest} to be filled
     * @param domain
     */
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, Domain domain)
            throws FaultException
    {
        api.setId(getId().intValue());
        api.setIdentifier(domain.formatIdentifier(getId()));
        api.setCreated(getCreated());
        api.setType(getType());
        api.setName(getName());
        api.setDescription(getDescription());
        api.setPurpose(getPurpose());
        api.setInterDomain(isInterDomain());
        for (Reservation providedReservation : getProvidedReservations()) {
            api.addProvidedReservationIdentifier(domain.formatIdentifier(providedReservation.getId()));
        }
    }

    /**
     * Synchronize {@link AbstractReservationRequest} from
     * {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}.
     *
     * @param api
     * @param entityManager
     * @throws FaultException
     */
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.AbstractReservationRequest.TYPE)) {
            setType(api.getType());
        }
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.AbstractReservationRequest.NAME)) {
            setName(api.getName());
        }
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.AbstractReservationRequest.DESCRIPTION)) {
            setDescription(api.getDescription());
        }
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.AbstractReservationRequest.PURPOSE)) {
            setPurpose(api.getPurpose());
        }
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.AbstractReservationRequest.INTER_DOMAIN)) {
            setInterDomain(api.getInterDomain());
        }

        // Create/modify provided reservations
        ReservationManager reservationManager = new ReservationManager(entityManager);
        for (String providedReservationIdentifier : api.getProvidedReservationIdentifiers()) {
            if (api.isCollectionItemMarkedAsNew(api.PROVIDED_RESERVATION_IDENTIFIERS, providedReservationIdentifier)) {
                Long providedReservationId = domain.parseIdentifier(providedReservationIdentifier);
                Reservation providedReservation = reservationManager.get(providedReservationId);
                addProvidedReservation(providedReservation);
            }
        }
        // Delete provided reservations
        Set<String> apiDeletedProvidedReservationIdentifiers =
                api.getCollectionItemsMarkedAsDeleted(api.PROVIDED_RESERVATION_IDENTIFIERS);
        for (String providedReservationIdentifier : apiDeletedProvidedReservationIdentifiers) {
            Long providedReservationId = domain.parseIdentifier(providedReservationIdentifier);
            removeProvidedReservation(providedReservationId);
        }
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("type", getType());
        map.put("purpose", getPurpose());
    }
}
