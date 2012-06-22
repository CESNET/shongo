package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.Person;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.resource.Technology;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Test;

import javax.persistence.EntityManager;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Tests for creating/updating {@link CompartmentRequest}(s) based on {@link Compartment}
 * from {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentRequestTest extends AbstractDatabaseTest
{
    EntityManager entityManager;

    CompartmentRequestManager compartmentRequestManager;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        compartmentRequestManager = new CompartmentRequestManager(entityManager);
    }

    @Override
    public void tearDown()
    {
        entityManager.getTransaction().commit();

        super.tearDown();
    }

    @Test
    public void testCreateCompartmentRequest() throws Exception
    {
        Person person1 = new Person("Martin Srom", "srom@cesnet.cz");
        Person person2 = new Person("Ondrej Bouda", "bouda@cesnet.cz");

        // Create compartment
        Compartment compartment = new Compartment();
        compartment.addRequestedResource(new ExternalEndpointSpecification(Technology.H323), person1);
        compartment.addRequestedPerson(person2);
        entityManager.persist(compartment);

        // Create compartment request from the compartment
        CompartmentRequest compartmentRequest = compartmentRequestManager.create(compartment,
                new Interval(DateTime.parse("2012-06-01T15:00"), Period.parse("PT2H")));
        assertEquals(2, compartmentRequest.getRequestedPersons().size());
        assertEquals(person1, compartmentRequest.getRequestedPersons().get(0).getPerson());
        assertEquals(person2, compartmentRequest.getRequestedPersons().get(1).getPerson());
    }

    @Test
    public void testCreateCompartmentRequestNotAllowingSamePersons()
    {
        Compartment compartment = new Compartment();
        compartment.addRequestedPerson(new Person("Martin Srom", "srom@cesnet.cz"));
        compartment.addRequestedResource(new ExternalEndpointSpecification(Technology.H323),
                new Person("Martin", "srom@cesnet.cz"));
        entityManager.persist(compartment);

        try {
            compartmentRequestManager.create(compartment,
                new Interval(DateTime.parse("2012-06-01T15:00"), Period.parse("PT2H")));
            fail("Creating compartment request should throw an exception that same persons are present!");
        } catch (IllegalStateException exception) {
        }
    }

    @Test
    public void testUpdateCompartmentRequest()
    {
        Person person1 = new Person("Martin Srom", "srom@cesnet.cz");
        Person person2 = new Person("Ondrej Bouda", "bouda@cesnet.cz");
        Person person3 = new Person("Petr Holub", "hopet@cesnet.cz");
        Person person4 = new Person("Jan Ruzicka", "janru@cesnet.cz");
        ResourceSpecification resource1 = new ExternalEndpointSpecification(Technology.H323);
        ResourceSpecification resource2 = new ExternalEndpointSpecification(Technology.H323);

        // Create compartment
        Compartment compartment = new Compartment();
        compartment.addRequestedResource(resource1, person1);
        compartment.addRequestedResource(resource2, person2);
        compartment.addRequestedPerson(person3);
        compartment.addRequestedPerson(person4);

        // Create compartment request from the compartment
        entityManager.persist(compartment);
        CompartmentRequest compartmentRequest = compartmentRequestManager.create(compartment,
                new Interval(DateTime.parse("2012-06-01T15:00"), Period.parse("PT2H")));
        assertEquals(4, compartmentRequest.getRequestedPersons().size());

        // Remove resource and person from request
        compartment.removeRequestedResource(resource1);
        compartment.removeRequestedPerson(person3);
        compartmentRequestManager.update(compartmentRequest, compartment);
        assertEquals(2, compartmentRequest.getRequestedPersons().size());
        assertEquals(person2, compartmentRequest.getRequestedPersons().get(0).getPerson());
        assertEquals(person4, compartmentRequest.getRequestedPersons().get(1).getPerson());

        // Add resource and person to request
        compartment.addRequestedResource(resource1);
        compartment.addRequestedPerson(person3);
        compartmentRequestManager.update(compartmentRequest, compartment);
        assertEquals(4, compartmentRequest.getRequestedPersons().size());
        assertEquals(person1, compartmentRequest.getRequestedPersons().get(2).getPerson());
        assertEquals(person3, compartmentRequest.getRequestedPersons().get(3).getPerson());

        // Remove person request from resource specification
        resource1.removeRequestedPerson(person1);
        compartmentRequestManager.update(compartmentRequest, compartment);
        assertEquals(3, compartmentRequest.getRequestedPersons().size());
        assertEquals(person3, compartmentRequest.getRequestedPersons().get(2).getPerson());
    }
}
