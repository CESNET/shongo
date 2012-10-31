package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;

import javax.persistence.Entity;

/**
 * Represents an {@link Specification} of a target which can participate in a {@link cz.cesnet.shongo.controller.executor.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class ParticipantSpecification extends Specification
{
    /**
     * @param domain
     * @return {@link cz.cesnet.shongo.controller.request.ParticipantSpecification} converted to
     *         {@link cz.cesnet.shongo.controller.api.ParticipantSpecification}
     */
    @Override
    public cz.cesnet.shongo.controller.api.ParticipantSpecification toApi(Domain domain)
    {
        return (cz.cesnet.shongo.controller.api.ParticipantSpecification) super.toApi(domain);
    }
}
