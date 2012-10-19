package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.api.DateTimeSlot;
import cz.cesnet.shongo.controller.api.PermanentReservationRequest;
import cz.cesnet.shongo.controller.api.Resource;
import org.junit.Test;

/**
 * Tests for {@link PermanentReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PermanentReservationRequestTest extends AbstractControllerTest
{
    @Test
    public void test() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceIdentifier = getResourceService().createResource(SECURITY_TOKEN, resource);

        PermanentReservationRequest reservationRequest = new PermanentReservationRequest();
        reservationRequest.addSlot(new DateTimeSlot("2012-06-22T14:00", "PT2H"));
        reservationRequest.setResourceIdentifier(resourceIdentifier);

        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runPreprocessor();
        checkAllocated(identifier);
    }
}
