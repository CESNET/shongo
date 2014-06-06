package cz.cesnet.shongo.controller.booking.streaming;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.api.Specification;
import cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification;

import javax.persistence.Entity;

/**
 * {@link cz.cesnet.shongo.controller.booking.specification.ExecutableServiceSpecification} for streaming.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class StreamingServiceSpecification extends ExecutableServiceSpecification
{
    @Override
    protected Specification createApi()
    {
        throw new TodoImplementException();
    }
}
