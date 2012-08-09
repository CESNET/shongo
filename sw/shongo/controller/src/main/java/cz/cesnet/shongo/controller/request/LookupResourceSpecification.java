package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Fault;
import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.controller.Domain;

import javax.persistence.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents parameters for device which will be lookup and used in the compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class LookupResourceSpecification extends ResourceSpecification
{
    /**
     * Set of technologies which the resource must support.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Constructor.
     */
    public LookupResourceSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param technology
     */
    public LookupResourceSpecification(Technology technology)
    {
        addTechnology(technology);
    }

    /**
     * @return {@link #technologies}
     */
    @ElementCollection
    @Enumerated(EnumType.STRING)
    @Access(AccessType.FIELD)
    public Set<Technology> getTechnologies()
    {
        return Collections.unmodifiableSet(technologies);
    }

    /**
     * @param technology technology to be added to the {@link #technologies}
     */
    public void addTechnology(Technology technology)
    {
        technologies.add(technology);
    }

    /**
     * @param technology technology to be removed from the {@link #technologies}
     */
    public void removeTechnology(Technology technology)
    {
        technologies.remove(technology);
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        addCollectionToMap(map, "technologies", technologies);
    }

    @Override
    public cz.cesnet.shongo.controller.api.ResourceSpecification toApi(Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.LookupResourceSpecification api =
                new cz.cesnet.shongo.controller.api.LookupResourceSpecification();

        if (technologies.size() == 1) {
            api.setTechnology(technologies.iterator().next());
        }
        else {
            throw new FaultException(Fault.Common.TODO_IMPLEMENT);
        }

        super.toApi(api);

        return api;
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.ResourceSpecification api, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.LookupResourceSpecification apiExternalEndpoint =
                (cz.cesnet.shongo.controller.api.LookupResourceSpecification) api;
        if (apiExternalEndpoint.isPropertyFilled(apiExternalEndpoint.TECHNOLOGY)) {
            technologies.clear();
            addTechnology(apiExternalEndpoint.getTechnology());
        }
        super.fromApi(api, entityManager, domain);
    }
}
