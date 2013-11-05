package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.api.Specification;

import javax.persistence.Entity;

/**
 * {@link EndpointServiceSpecification} for streaming.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class StreamingServiceSpecification extends EndpointServiceSpecification
{
    @Override
    protected Specification createApi()
    {
        return cz.cesnet.shongo.controller.api.EndpointServiceSpecification.createStreaming();
    }
}
