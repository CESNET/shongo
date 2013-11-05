package cz.cesnet.shongo.controller.request;

import javax.persistence.Entity;

/**
 * {@link EndpointServiceSpecification} for recording.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RecordingServiceSpecification extends EndpointServiceSpecification
{
    @Override
    protected cz.cesnet.shongo.controller.api.Specification createApi()
    {
        return cz.cesnet.shongo.controller.api.EndpointServiceSpecification.createRecording();
    }
}
