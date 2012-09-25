package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.compartment.Compartment;
import cz.cesnet.shongo.controller.compartment.CompartmentExecutor;
import cz.cesnet.shongo.controller.reservation.CompartmentReservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Component of a domain controller which executes actions according to allocation plan which was created
 * by the {@link Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Executor extends Component
        implements Component.WithThread, Component.EntityManagerFactoryAware, Component.ControllerAgentAware, Runnable
{
    private static Logger logger = LoggerFactory.getLogger(Executor.class);

    /**
     * {@link EntityManagerFactory} used for loading {@link cz.cesnet.shongo.controller.api.Reservation}s for execution.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * Period in which the executor works.
     */
    private Duration period = Duration.parse("PT15S");

    /**
     * Set of {@link cz.cesnet.shongo.controller.api.Reservation} identifiers which have been already executed.
     */
    private Map<Long, CompartmentExecutor> executorsById = new HashMap<Long, CompartmentExecutor>();

    @Override
    public Thread getThread()
    {
        Thread thread = new Thread(this);
        thread.setName("executor");
        return thread;
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    @Override
    public void run()
    {
        logger.info("Executor started!");

        try {
            Thread.sleep(period.getMillis());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (!Thread.interrupted()) {
            execute();
            try {
                Thread.sleep(period.getMillis());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
        }

        for (CompartmentExecutor executor : executorsById.values()) {
            if (executor.isAlive()) {
                logger.debug("Killing '{}'...", executor.getName());
                executor.stop();
            }
        }

        logger.info("Executor stopped!");
    }

    /**
     * Execute {@link cz.cesnet.shongo.controller.api.Reservation}s which should be executed.
     */
    private void execute()
    {
        logger.info("Checking compartment reservations for execution...");
        Interval interval = new Interval(
                DateTime.now().minus(Period.minutes(1)), DateTime.now().plus(Period.minutes(2)));
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<CompartmentReservation> reservations = reservationManager.listByInterval(interval,
                CompartmentReservation.class);
        for (CompartmentReservation compartmentReservation : reservations) {
            Compartment compartment = compartmentReservation.getCompartment();
            Long compartmentId = compartment.getId();
            if (executorsById.containsKey(compartmentId)) {
                continue;
            }
            CompartmentExecutor executor = new CompartmentExecutor(controllerAgent, compartment, compartmentReservation.getSlot());
            executor.start();
            executorsById.put(compartmentId, executor);
        }
    }
}
