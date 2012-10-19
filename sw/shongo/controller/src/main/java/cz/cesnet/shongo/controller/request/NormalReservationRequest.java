package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.fault.FaultException;
import org.apache.commons.lang.ObjectUtils;

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
public abstract class NormalReservationRequest extends AbstractReservationRequest
{
    /**
     * Purpose for the reservation (science/education).
     */
    private ReservationRequestPurpose purpose;

    /**
     * Option that specifies whether inter-domain resource lookup can be performed.
     */
    private boolean interDomain;

    /**
     * List of {@link cz.cesnet.shongo.controller.reservation.Reservation}s of allocated resources which can be used by {@link cz.cesnet.shongo.controller.Scheduler} for allocation of
     * this {@link cz.cesnet.shongo.controller.request.NormalReservationRequest}.
     */
    private List<Reservation> providedReservations = new ArrayList<Reservation>();

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
     * @param providedReservationIdentifier for {@link cz.cesnet.shongo.controller.reservation.Reservation} to be removed from {@link #providedReservations}
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
     *                                   this {@link cz.cesnet.shongo.controller.request.NormalReservationRequest}
     * @return true if some modification was made
     */
    public boolean synchronizeFrom(NormalReservationRequest abstractReservationRequest)
    {
        boolean modified = super.synchronizeFrom(abstractReservationRequest)
                || !ObjectUtils.equals(getPurpose(), abstractReservationRequest.getPurpose())
                || !ObjectUtils.equals(isInterDomain(), abstractReservationRequest.isInterDomain());
        setPurpose(abstractReservationRequest.getPurpose());
        setInterDomain(abstractReservationRequest.isInterDomain());
        if (!ObjectUtils.equals(getProvidedReservations(), abstractReservationRequest.getProvidedReservations())) {
            setProvidedReservations(abstractReservationRequest.getProvidedReservations());
            modified = true;
        }
        return modified;
    }

    /**
     * @param api    {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest} to be filled
     * @param domain
     */
    protected void toApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, Domain domain)
            throws FaultException
    {
        super.toApi(api, domain);

        cz.cesnet.shongo.controller.api.NormalReservationRequest normalReservationRequestApi =
                (cz.cesnet.shongo.controller.api.NormalReservationRequest) api;
        normalReservationRequestApi.setPurpose(getPurpose());
        normalReservationRequestApi.setInterDomain(isInterDomain());
        for (Reservation providedReservation : getProvidedReservations()) {
            normalReservationRequestApi
                    .addProvidedReservationIdentifier(domain.formatIdentifier(providedReservation.getId()));
        }
    }

    /**
     * Synchronize {@link cz.cesnet.shongo.controller.request.NormalReservationRequest} from
     * {@link cz.cesnet.shongo.controller.api.AbstractReservationRequest}.
     *
     * @param api
     * @param entityManager
     * @throws cz.cesnet.shongo.fault.FaultException
     *
     */
    public void fromApi(cz.cesnet.shongo.controller.api.AbstractReservationRequest api, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        super.fromApi(api, entityManager, domain);

        cz.cesnet.shongo.controller.api.NormalReservationRequest normalReservationRequestApi =
                (cz.cesnet.shongo.controller.api.NormalReservationRequest) api;
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.NormalReservationRequest.PURPOSE)) {
            setPurpose(normalReservationRequestApi.getPurpose());
        }
        if (api.isPropertyFilled(cz.cesnet.shongo.controller.api.NormalReservationRequest.INTER_DOMAIN)) {
            setInterDomain(normalReservationRequestApi.getInterDomain());
        }

        // Create/modify provided reservations
        ReservationManager reservationManager = new ReservationManager(entityManager);
        for (String providedReservationIdentifier : normalReservationRequestApi.getProvidedReservationIdentifiers()) {
            if (api.isCollectionItemMarkedAsNew(normalReservationRequestApi.PROVIDED_RESERVATION_IDENTIFIERS,
                    providedReservationIdentifier)) {
                Long providedReservationId = domain.parseIdentifier(providedReservationIdentifier);
                Reservation providedReservation = reservationManager.get(providedReservationId);
                addProvidedReservation(providedReservation);
            }
        }
        // Delete provided reservations
        Set<String> apiDeletedProvidedReservationIdentifiers =
                api.getCollectionItemsMarkedAsDeleted(normalReservationRequestApi.PROVIDED_RESERVATION_IDENTIFIERS);
        for (String providedReservationIdentifier : apiDeletedProvidedReservationIdentifiers) {
            Long providedReservationId = domain.parseIdentifier(providedReservationIdentifier);
            removeProvidedReservation(providedReservationId);
        }
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("purpose", getPurpose());
        map.put("interDomain", isInterDomain());
    }
}
