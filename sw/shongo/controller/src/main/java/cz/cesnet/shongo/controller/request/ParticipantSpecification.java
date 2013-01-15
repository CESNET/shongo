package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Domain;

import javax.persistence.Entity;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an {@link Specification} of a target which can participate in a {@link cz.cesnet.shongo.controller.executor.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class ParticipantSpecification extends Specification
{
    /**
     * @return set of {@link Technology}s by which the {@link ParticipantSpecification} can participate
     * in the {@link cz.cesnet.shongo.controller.executor.Compartment}.
     */
    @Transient
    public Set<Technology> getTechnologies()
    {
        return new HashSet<Technology>();
    }

    /**
     * @return {@link cz.cesnet.shongo.controller.request.ParticipantSpecification} converted to
     *         {@link cz.cesnet.shongo.controller.api.ParticipantSpecification}
     */
    @Override
    public cz.cesnet.shongo.controller.api.ParticipantSpecification toApi()
    {
        return (cz.cesnet.shongo.controller.api.ParticipantSpecification) super.toApi();
    }
}
