package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.TransactionHelper;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.report.SpecificationNotAllocatableReport;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a component of a domain controller that is responsible for allocating {@link ReservationRequest}
 * to the {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Scheduler extends Component implements Component.DomainAware, Component.NotificationManagerAware
{
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    /**
     * @see Cache
     */
    private Cache cache;

    /**
     * @see Domain
     */
    private Domain domain;

    /**
     * @see NotificationManager
     */
    private NotificationManager notificationManager;

    /**
     * @param cache sets the {@link #cache}
     */
    public void setCache(Cache cache)
    {
        this.cache = cache;
    }

    @Override
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    @Override
    public void setNotificationManager(NotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(cache, Cache.class);
        super.init(configuration);
    }

    /**
     * Run scheduler for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval, EntityManager entityManager)
    {
        logger.info("Running scheduler for interval '{}'...", TemporalHelper.formatInterval(interval));

        // Set current interval as working to the cache (it will reload allocations only when
        // the interval changes)
        cache.setWorkingInterval(interval, entityManager);

        ReservationManager reservationManager = new ReservationManager(entityManager);
        ExecutableManager executableManager = new ExecutableManager(entityManager);

        TransactionHelper.Transaction transaction = TransactionHelper.beginTransaction(entityManager);

        try {
            // Delete all reservations which was marked for deletion
            reservationManager.deleteAllNotReferenced(cache);
            // Delete all compartments which should be deleted
            executableManager.deleteAllNotReferenced();

            ReservationRequestManager compartmentRequestManager = new ReservationRequestManager(entityManager);
            List<ReservationRequest> reservationRequests =
                    compartmentRequestManager.listCompletedReservationRequests(interval);

            // TODO: Apply some other priority to reservation requests

            List<Reservation> newReservations = new ArrayList<Reservation>();

            for (ReservationRequest reservationRequest : reservationRequests) {
                Reservation reservation = allocateReservationRequest(reservationRequest, entityManager);
                if (reservation != null) {
                    newReservations.add(reservation);
                }
            }

            // Delete all compartments which should be deleted
            executableManager.deleteAllNotReferenced();

            transaction.commit();

            // Notify about new reservations
            if (notificationManager != null) {
                notificationManager.notifyNewReservations(newReservations);
            }
        }
        catch (Exception exception) {
            transaction.rollback();
            cache.reset(entityManager);
            throw new IllegalStateException("Scheduler failed", exception);
        }
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param reservationRequest to be allocated
     */
    private Reservation allocateReservationRequest(ReservationRequest reservationRequest, EntityManager entityManager)
    {
        logger.info("Allocating reservation request '{}'...", reservationRequest.getId());

        reservationRequest.clearReports();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);

        // Get existing reservation
        Reservation reservation = reservationManager.getByReservationRequest(reservationRequest);

        // TODO: Try to intelligently reallocate and not delete old reservation
        // Delete old reservation
        if (reservation != null) {
            reservationManager.delete(reservation, cache);
        }

        // Get requested slot and check it's maximum duration
        Interval slot = reservationRequest.getSlot();

        // Create new scheduler task
        String userId = reservationRequest.getUserId();
        ReservationTask.Context context = new ReservationTask.Context(userId, cache, slot);
        ReservationTask reservationTask = null;

        try {
            // Fill provided reservations to transaction
            for (Reservation providedReservation : reservationRequest.getProvidedReservations()) {
                context.getCacheTransaction().addProvidedReservation(providedReservation);
            }

            // Get reservation task
            Specification specification = reservationRequest.getSpecification();
            if (specification instanceof ReservationTaskProvider) {
                ReservationTaskProvider reservationTaskProvider = (ReservationTaskProvider) specification;
                reservationTask = reservationTaskProvider.createReservationTask(context);
            }
            else {
                throw new SpecificationNotAllocatableReport(specification).exception();
            }

            reservation = reservationTask.perform();
            reservationManager.create(reservation);

            // Update cache
            cache.addReservation(reservation);
            for (Reservation childReservation : reservation.getChildReservations()) {
                cache.addReservation(childReservation);
            }

            // Update reservation request
            reservationRequest.setReservation(reservation);
            reservationRequest.setState(ReservationRequest.State.ALLOCATED);
            reservationRequest.setReports(reservationTask.getReports());
            reservationRequestManager.update(reservationRequest);
        }
        catch (ReportException exception) {
            reservationRequest.setState(ReservationRequest.State.ALLOCATION_FAILED);
            Report report = exception.getReport();
            if (reservationTask != null) {
                Report currentReport = reservationTask.getCurrentReport();
                if (currentReport != null && currentReport.getParentReport() != null) {
                    Report parentReport = currentReport.getParentReport();
                    parentReport.replaceChildReport(currentReport, report);
                    report.addChildReport(currentReport);
                    reservationRequest.setReports(reservationTask.getReports());
                    report = null;
                }
                else {
                    report.addChildReports(reservationTask.getReports());
                }
            }
            if (report != null) {
                reservationRequest.addReport(report);
            }
        }

        return reservation;
    }

    /**
     * Run scheduler on given entityManagerFactory and interval.
     *
     * @param entityManager
     * @param interval
     */
    public static void createAndRun(Interval interval, EntityManager entityManager, Cache cache,
            NotificationManager notificationManager, Domain domain) throws FaultException
    {
        Scheduler scheduler = new Scheduler();
        scheduler.setCache(cache);
        scheduler.setDomain(domain);
        scheduler.setNotificationManager(notificationManager);
        scheduler.init();
        scheduler.run(interval, entityManager);
        scheduler.destroy();
    }
}
