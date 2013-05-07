package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.*;

/**
 * Represents a {@link Scheduler} task which results into {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ReservationTask
{
    /**
     * Context.
     */
    private SchedulerContext schedulerContext;

    /**
     * List of child {@link Reservation}s.
     */
    private List<Reservation> childReservations = new ArrayList<Reservation>();

    /**
     * List of reports.
     */
    private List<SchedulerReport> reports = new ArrayList<SchedulerReport>();

    /**
     * Stack of active {@link SchedulerReport}s.
     */
    private Stack<SchedulerReport> activeReports = new Stack<SchedulerReport>();

    /**
     * Constructor.
     */
    public ReservationTask(SchedulerContext schedulerContext)
    {
        this.schedulerContext = schedulerContext;
    }

    /**
     * @return {@link #schedulerContext}
     */
    public SchedulerContext getSchedulerContext()
    {
        return schedulerContext;
    }

    /**
     * @return {@link SchedulerContext#getInterval()}
     */
    public Interval getInterval()
    {
        return schedulerContext.getInterval();
    }

    /**
     * @return {@link SchedulerContext#cache}
     */
    public Cache getCache()
    {
        return schedulerContext.getCache();
    }

    /**
     * @return {@link #childReservations}
     */
    protected List<Reservation> getChildReservations()
    {
        return childReservations;
    }

    /**
     * @param reservation to be added to the {@link #childReservations}
     * @return given {@code reservation} or {@link ExistingReservation#getTargetReservation()}
     */
    public Reservation addChildReservation(Reservation reservation)
    {
        childReservations.add(reservation);
        schedulerContext.addAllocatedReservation(reservation);
        return reservation.getTargetReservation();
    }

    /**
     * @param reservation to be added to the {@link #childReservations}
     * @return given {@code reservation} or {@link ExistingReservation#getTargetReservation()}
     */
    public final <R extends Reservation> R addChildReservation(Reservation reservation, Class<R> reservationClass)
    {
        childReservations.add(reservation);
        schedulerContext.addAllocatedReservation(reservation);
        return reservationClass.cast(reservation.getTargetReservation());
    }

    /**
     * @param reservationTask to be performed
     * @return resulting {@link Reservation}
     * @throws SchedulerException
     */
    private Reservation performChildReservationTask(ReservationTask reservationTask) throws SchedulerException
    {
        try {
            Reservation reservation = reservationTask.perform();
            addReports(reservationTask);
            return reservation;
        }
        catch (SchedulerException exception) {
            addReport(exception.getTopReport());
            exception.setReport(getCurrentReport());
            throw exception;
        }
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final Reservation addChildReservation(ReservationTask reservationTask)
            throws SchedulerException
    {
        return addChildReservation(performChildReservationTask(reservationTask));
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final <R extends Reservation> R addChildReservation(ReservationTask reservationTask,
            Class<R> reservationClass) throws SchedulerException
    {
        return reservationClass.cast(addChildReservation(reservationTask));
    }

    /**
     * Add child {@link Reservation} to the task allocated by given {@code reservationTask}.
     *
     * @param reservationTask used for allocation of {@link Reservation}
     */
    public final <R extends Reservation> Collection<R> addMultiChildReservation(ReservationTask reservationTask,
            Class<R> reservationClass) throws SchedulerException
    {
        Reservation reservation = performChildReservationTask(reservationTask);
        List<R> reservations = new ArrayList<R>();
        Reservation targetReservation = reservation.getTargetReservation();
        if (reservationClass.isInstance(targetReservation)) {
            childReservations.add(reservation);
            reservations.add(reservationClass.cast(targetReservation));
        }
        else {
            for (Reservation childReservation : reservation.getChildReservations()) {
                childReservations.add(childReservation);
                reservations.add(childReservation.getTargetReservation(reservationClass));
            }
        }
        return reservations;
    }

    /**
     * Add child {@link Reservation} to the task allocated from a {@link ReservationTask} created from given
     * {@code reservationTaskProvider}.
     *
     * @param reservationTaskProvider used for creating {@link ReservationTask}
     */
    public final Reservation addChildReservation(ReservationTaskProvider reservationTaskProvider)
            throws SchedulerException
    {
        ReservationTask reservationTask = reservationTaskProvider.createReservationTask(getSchedulerContext());
        return addChildReservation(reservationTask);
    }

    /**
     * Add child {@link Reservation} to the task allocated from a {@link ReservationTask} created from given
     * {@code reservationTaskProvider}.
     *
     * @param reservationTaskProvider used for creating {@link ReservationTask}
     */
    public final <R extends Reservation> R addChildReservation(ReservationTaskProvider reservationTaskProvider,
            Class<R> reservationClass) throws SchedulerException
    {
        Reservation reservation = addChildReservation(reservationTaskProvider);
        return reservationClass.cast(reservation);
    }

    /**
     * @return {@link #reports}
     */
    public List<SchedulerReport> getReports()
    {
        return reports;
    }

    /**
     * @param report to be added to the {@link #reports}
     */
    protected <T extends SchedulerReport> T addReport(T report)
    {
        if (activeReports.empty()) {
            reports.add(report);
        }
        else {
            activeReports.peek().addChildReport(report);
        }
        return report;
    }

    /**
     * Add report from another {@code reservationTask}.
     *
     * @param reservationTask
     */
    protected void addReports(ReservationTask reservationTask)
    {
        for (SchedulerReport report : reservationTask.getReports()) {
            addReport(report);
        }
    }

    /**
     * @param report to be added and to be used as parent for next reports until {@link #endReport()} is called
     */
    protected void beginReport(SchedulerReport report)
    {
        addReport(report);
        activeReports.push(report);
    }

    /**
     * Stop using active report as parent for next reports
     */
    protected void endReport()
    {
        activeReports.pop();
    }

    /**
     * Stop using active report as parent for next reports
     *
     * @param errorReport to be added to the active report as error
     */
    protected void endReportError(SchedulerReport errorReport)
    {
        SchedulerReport report = activeReports.pop();

        report.addChildReport(errorReport);
    }

    /**
     * @return current top {@link SchedulerReport}
     */
    protected SchedulerReport getCurrentReport()
    {
        SchedulerReport report;
        if (activeReports.empty()) {
            if (reports.size() == 0) {
                throw new IllegalStateException("No report is active");
            }
            // Use the last added
            report = reports.get(reports.size() - 1);
        }
        else {
            report = activeReports.peek();
        }
        return report;
    }

    /**
     * @return {@link SchedulerException} which should be added to the {@link #reports}
     *         when the {@link Reservation} is started allocating, or null when no report should be added
     */
    protected SchedulerReport createMainReport()
    {
        return null;
    }

    /**
     * Perform the {@link ReservationTask}.
     *
     * @return created {@link Reservation}
     * @throws SchedulerException when the {@link ReservationTask} failed
     */
    public final Reservation perform() throws SchedulerException
    {
        Reservation reservation = null;
        SchedulerReport mainReport = createMainReport();
        if (mainReport != null) {
            beginReport(mainReport);
            try {
                reservation = createReservation();
                validateReservation(reservation);
            }
            catch (SchedulerException exception) {
                SchedulerReport report = exception.getTopReport();
                if (report != mainReport) {
                    // Report from exception isn't added to root report so we add it and throw the root report
                    addReport(report);
                    exception.setReport(mainReport);
                    report = mainReport;
                }
                throw new SchedulerException(report);
            }
            finally {
                endReport();
            }
        }
        else {
            reservation = createReservation();
        }

        // Add child reservations
        for (Reservation childReservation : getChildReservations()) {
            reservation.addChildReservation(childReservation);
        }

        // Add reservation to the context
        schedulerContext.addAllocatedReservation(reservation);

        return reservation;
    }

    /**
     * Perform the {@link ReservationTask}.
     *
     * @return created {@link Reservation}
     * @throws SchedulerException when the {@link ReservationTask} failed
     */
    public final <R> R perform(Class<R> reservationClass) throws SchedulerException
    {
        return reservationClass.cast(perform());
    }

    /**
     * @return created {@link Reservation}
     * @throws SchedulerException when the {@link Reservation} cannot be created
     */
    protected abstract Reservation createReservation() throws SchedulerException;

    /**
     * @param type to be validated
     * @throws SchedulerReportSet.DurationLongerThanMaximumException
     *          when the validation failed
     */
    protected void validateReservationSlot(Class<? extends Reservation> type)
            throws SchedulerReportSet.DurationLongerThanMaximumException
    {

    }

    /**
     * @param reservation to be validated
     * @throws SchedulerException when the validation failed
     */
    protected void validateReservation(Reservation reservation) throws SchedulerException
    {
    }

    /**
     * Check maximum duration.
     *
     * @param slot            to be checked
     * @param maximumDuration maximum allowed duration
     * @throws SchedulerReportSet.DurationLongerThanMaximumException
     *
     */
    protected static void checkMaximumDuration(Interval slot, Period maximumDuration)
            throws SchedulerReportSet.DurationLongerThanMaximumException
    {
        Period duration = Temporal.getIntervalDuration(slot);
        if (Temporal.isPeriodLongerThan(duration, maximumDuration)) {
            throw new SchedulerReportSet.DurationLongerThanMaximumException(duration, maximumDuration);
        }
    }


    protected <T extends Reservation> void sortAvailableReservations(
            List<AvailableReservation<T>> availableReservations)
    {
        final Interval interval = getInterval();
        Collections.sort(availableReservations, new Comparator<AvailableReservation>()
        {
            @Override
            public int compare(AvailableReservation reservation1, AvailableReservation reservation2)
            {
                // Prefer reservations for the whole interval
                boolean firstContainsInterval = reservation1.getOriginalReservation().getSlot().contains(interval);
                boolean secondContainsInterval = reservation2.getOriginalReservation().getSlot().contains(interval);
                if (secondContainsInterval && !firstContainsInterval) {
                    return 1;
                }

                // Prefer reallocatable reservations
                boolean firstReallocatable = reservation1.getType().equals(AvailableReservation.Type.REALLOCATABLE);
                boolean secondReallocatable = reservation2.getType().equals(AvailableReservation.Type.REALLOCATABLE);
                if (secondReallocatable && !firstReallocatable) {
                    return 1;
                }

                return 0;
            }
        });
    }
}
