package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Tests for serializing exceptions though XML-RPC.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExceptionSerializingTest extends AbstractControllerTest
{
    /**
     * @see ReservationService
     */
    private ResourceService resourceService;

    @Override
    protected void onInitController(cz.cesnet.shongo.controller.Controller controller)
    {
        ResourceServiceImpl resourceService = new ResourceServiceImpl();
        resourceService.setCache(new Cache());
        controller.addService(resourceService);
    }

    @Override
    protected void onControllerClientReady(ControllerClient controllerClient)
    {
        resourceService = controllerClient.getService(ResourceService.class);
    }

    @Test
    public void testCreateReservationRequest() throws Exception
    {
        try {
            resourceService.getResource(TESTING_SECURITY_TOKEN, "1");
            fail(EntityNotFoundException.class.getSimpleName() + " should be thrown.");
        } catch (EntityNotFoundException exception) {
            assertEquals(Long.valueOf(1), exception.getEntityIdentifier());
            assertEquals(Resource.class, exception.getEntityType());
        }
    }
}