package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;

import java.util.List;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class NormalReservationRequest extends AbstractReservationRequest
{
    /**
     * @see cz.cesnet.shongo.controller.ReservationRequestPurpose
     */
    public static final String PURPOSE = "purpose";

    /**
     * {@link Specification} which is requested for the reservation.
     */
    public static final String SPECIFICATION = "specification";

    /**
     * Specifies whether the scheduler should try allocate resources from other domains.
     */
    public static final String INTER_DOMAIN = "interDomain";

    /**
     * Collection of shongo-ids for {@link cz.cesnet.shongo.controller.api.Reservation}s which are provided
     * to the {@link cz.cesnet.shongo.controller.api.NormalReservationRequest}.
     */
    public static final String PROVIDED_RESERVATION_IDS = "providedReservationIds";

    /**
     * Constructor.
     */
    public NormalReservationRequest()
    {
    }

    /**
     * @return {@link #PURPOSE}
     */
    @Required
    public ReservationRequestPurpose getPurpose()
    {
        return getPropertyStorage().getValue(PURPOSE);
    }

    /**
     * @param purpose sets the {@link #PURPOSE}
     */
    public void setPurpose(ReservationRequestPurpose purpose)
    {
        getPropertyStorage().setValue(PURPOSE, purpose);
    }

    /**
     * @return {@link #SPECIFICATION}
     */
    @Required
    public Specification getSpecification()
    {
        return getPropertyStorage().getValue(SPECIFICATION);
    }

    /**
     * @param specification sets the {@link #SPECIFICATION}
     */
    public <T extends Specification> T setSpecification(T specification)
    {
        getPropertyStorage().setValue(SPECIFICATION, specification);
        return specification;
    }

    /**
     * @return {@link #INTER_DOMAIN}
     */
    public Boolean getInterDomain()
    {
        return getPropertyStorage().getValue(INTER_DOMAIN);
    }

    /**
     * @param interDomain sets the {@link #INTER_DOMAIN}
     */
    public void setInterDomain(Boolean interDomain)
    {
        getPropertyStorage().setValue(INTER_DOMAIN, interDomain);
    }

    /**
     * @return {@link #PROVIDED_RESERVATION_IDS}
     */
    public List<String> getProvidedReservationIds()
    {
        return getPropertyStorage().getCollection(PROVIDED_RESERVATION_IDS, List.class);
    }

    /**
     * @param providedReservationIds sets the {@link #PROVIDED_RESERVATION_IDS}
     */
    public void setProvidedReservationIds(List<String> providedReservationIds)
    {
        getPropertyStorage().setCollection(PROVIDED_RESERVATION_IDS, providedReservationIds);
    }

    /**
     * @param providedReservationId to be added to the {@link #PROVIDED_RESERVATION_IDS}
     */
    public void addProvidedReservationId(String providedReservationId)
    {
        getPropertyStorage().addCollectionItem(PROVIDED_RESERVATION_IDS, providedReservationId,
                List.class);
    }

    /**
     * @param providedReservationId to be removed from the {@link #PROVIDED_RESERVATION_IDS}
     */
    public void removeProvidedReservationId(String providedReservationId)
    {
        getPropertyStorage().removeCollectionItem(PROVIDED_RESERVATION_IDS, providedReservationId);
    }
}
