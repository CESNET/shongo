package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.Technology;
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
    private Set<Technology> technologies = new HashSet<Technology>();

    private Set<Class<? extends Specification>> specificationClasses = new HashSet<Class<? extends Specification>>();

    private Set<String> providedReservationIds = new HashSet<String>();

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
}
