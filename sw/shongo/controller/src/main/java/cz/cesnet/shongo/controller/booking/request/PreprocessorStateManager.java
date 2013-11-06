package cz.cesnet.shongo.controller.booking.request;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.Temporal;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.Embeddable;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a state of a {@link AbstractReservationRequest} which can vary in time.
 * This class holds all states for all time intervals for a single reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Embeddable
public class PreprocessorStateManager extends AbstractManager
{
    /**
     * Date/time value that represents "infinite". Is used for preprocessed state's interval end
     * when a reservation request hasn't got any requested slot in future.
     */
    public static final DateTime MAXIMUM_INTERVAL_END = Temporal.DATETIME_INFINITY_END;

    /**
     * {@link ReservationRequestSet} for which the states are managed.
     */
    private AbstractReservationRequest reservationRequest;

    /**
     * List of reservation request preprocessed states.
     */
    List<PreprocessedState> preprocessedStates = new ArrayList<PreprocessedState>();

    /**
     * Construct manager for reservation request state.
     *
     * @param entityManager
     */
    public PreprocessorStateManager(EntityManager entityManager, AbstractReservationRequest reservationRequest)
    {
        super(entityManager);

        // Keep reference to reservation request
        this.reservationRequest = reservationRequest;

        loadStates();
    }

    /**
     * Load all reservation request states from database.
     */
    private void loadStates()
    {
        // Load all existing preprocessed states
        preprocessedStates = entityManager.createQuery("SELECT state FROM PreprocessedState state "
                + "WHERE state.reservationRequest = :reservationRequest ORDER BY state.start",
                PreprocessedState.class)
                .setParameter("reservationRequest", reservationRequest).getResultList();

        // Check whether they don't overlap
        for (int index = 0; index < preprocessedStates.size(); index++) {
            PreprocessedState preprocessedState = preprocessedStates.get(index);
            DateTime recordStart = preprocessedState.getStart();
            DateTime recordEnd = preprocessedState.getEnd();
            if (recordStart.isAfter(recordEnd)) {
                throw new IllegalArgumentException("Interval 'start' should not be after 'end' (start="
                        + recordStart.toString() + ", end=" + recordEnd.toString() + ")!");
            }
            if (index >= 1) {
                PreprocessedState previousPreprocessedState = preprocessedStates.get(index - 1);
                if (!recordStart.isAfter(previousPreprocessedState.getEnd())) {
                    throw new IllegalArgumentException(
                            "Interval 'start' should be after previous interval 'end' (start="
                                    + recordStart.toString() + ", end=" + previousPreprocessedState.getEnd()
                                    .toString() + ")!");
                }
            }
        }
    }

    /**
     * Refresh states from database (and flush all changes before it)
     */
    public void refresh()
    {
        if (entityManager.getTransaction().isActive()) {
            entityManager.flush();
        }
        loadStates();
    }

    /**
     * @return number of records which implements the reservation state
     */
    public int getRecordCount()
    {
        return preprocessedStates.size();
    }

    /**
     * @param state
     */
    public void setState(PreprocessorState state)
    {
        if (state == PreprocessorState.NOT_PREPROCESSED) {
            for (PreprocessedState preprocessedState : preprocessedStates) {
                delete(preprocessedState);
            }
            preprocessedStates.clear();
        }
        else {
            throw new IllegalArgumentException("Cannot set " + PreprocessorState.PREPROCESSED.toString()
                    + "for the whole time interval!");
        }
    }

    /**
     * Set reservation request state in the given interval.
     *
     * @param state
     * @param interval
     */
    public void setState(PreprocessorState state, Interval interval)
    {
        setState(state, interval.getStart(), interval.getEnd());
    }

    /**
     * Set reservation request state in the given interval.
     *
     * @param state
     * @param start
     * @param end
     */
    public void setState(PreprocessorState state, DateTime start, DateTime end)
    {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Interval 'start' and 'end' should not be empty!");
        }
        if (start.equals(end)) {
            return;
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Interval 'start' should not be after 'end' (start="
                    + start.toString() + ", end=" + end.toString() + ")!");
        }

        // Add the record before first
        if (preprocessedStates.size() == 0 || end.isBefore(preprocessedStates.get(0).getStart())) {
            // Skip the not-preprocessed
            if (state == PreprocessorState.PREPROCESSED) {
                PreprocessedState preprocessedState = new PreprocessedState();
                preprocessedState.setReservationRequest(reservationRequest);
                preprocessedState.setInterval(new Interval(start, end));
                create(preprocessedState);
                preprocessedStates.add(0, preprocessedState);
            }
        }
        // Add the record after last
        else if (start.isAfter(preprocessedStates.get(preprocessedStates.size() - 1).getEnd())) {
            // Skip the not-preprocessed
            if (state == PreprocessorState.PREPROCESSED) {
                PreprocessedState preprocessedState = new PreprocessedState();
                preprocessedState.setReservationRequest(reservationRequest);
                preprocessedState.setInterval(new Interval(start, end));
                create(preprocessedState);
                preprocessedStates.add(preprocessedState);
            }
        }
        else {
            // For preprocessed state, a record can be created or some records can be merged
            if (state == PreprocessorState.PREPROCESSED) {
                PreprocessedState newPreprocessedState = null;
                for (int index = 0; index < preprocessedStates.size(); index++) {
                    PreprocessedState preprocessedState = preprocessedStates.get(index);

                    // Compute statements
                    boolean startIsBeforeOrEqual = !start.isAfter(preprocessedState.getStart());
                    boolean startIsInside = !start.isBefore(preprocessedState.getStart())
                            && !start.isAfter(preprocessedState.getEnd());
                    boolean endIsAfterOrEqual = !end.isBefore(preprocessedState.getEnd());
                    boolean endIsInside = !end.isBefore(preprocessedState.getStart())
                            && !end.isAfter(preprocessedState.getEnd());

                    // If the interval overlaps the entire record, merge or delete the record
                    if (startIsBeforeOrEqual && endIsAfterOrEqual) {
                        if (newPreprocessedState == null) {
                            newPreprocessedState = preprocessedState;
                            newPreprocessedState.setStart(start);
                            newPreprocessedState.setEnd(end);
                        }
                        else {
                            delete(preprocessedStates.get(index));
                            preprocessedStates.remove(index);
                            index--;
                        }
                    }
                    // If the interval overlaps the beginning of the record, merge or delete the record
                    else if (startIsBeforeOrEqual && endIsInside) {
                        if (newPreprocessedState == null) {
                            newPreprocessedState = preprocessedState;
                            newPreprocessedState.setStart(start);
                        }
                        else {
                            newPreprocessedState.setEnd(preprocessedState.getEnd());
                            delete(preprocessedStates.get(index));
                            preprocessedStates.remove(index);
                            index--;
                        }
                        break;
                    }
                    // If the interval overlaps the end of the record, update the record
                    else if (startIsInside && endIsAfterOrEqual) {
                        if (newPreprocessedState != null) {
                            throw new RuntimeException("Should never happen!");
                        }
                        newPreprocessedState = preprocessedState;
                        newPreprocessedState.setEnd(end);
                    }
                    // If the interval is inside the record, do nothing
                    else if (startIsInside && endIsInside) {
                        break;
                    }
                }
            }
            // For non-preprocessed state some records may be erased
            else {
                for (int index = 0; index < preprocessedStates.size(); index++) {
                    PreprocessedState preprocessedState = preprocessedStates.get(index);

                    // Compute statements
                    boolean startIsBeforeOrEqual = !start.isAfter(preprocessedState.getStart());
                    boolean startIsInside = !start.isBefore(preprocessedState.getStart())
                            && !start.isAfter(preprocessedState.getEnd());
                    boolean endIsAfterOrEqual = !end.isBefore(preprocessedState.getEnd());
                    boolean endIsInside = !end.isBefore(preprocessedState.getStart())
                            && !end.isAfter(preprocessedState.getEnd());

                    // If the interval overlaps the entire record, delete the record
                    if (startIsBeforeOrEqual && endIsAfterOrEqual) {
                        delete(preprocessedStates.get(index));
                        preprocessedStates.remove(index);
                        index--;
                    }
                    // If the interval overlaps the beginning of the record, update the record
                    else if (startIsBeforeOrEqual && endIsInside) {
                        preprocessedState.setStart(end);
                        break;
                    }
                    // If the interval overlaps the end of the record, update the record
                    else if (startIsInside && endIsAfterOrEqual) {
                        preprocessedState.setEnd(start);
                    }
                    // If the interval is inside the record, split the record
                    else if (startIsInside && endIsInside) {
                        index++;
                        PreprocessedState newPreprocessedState = new PreprocessedState();
                        newPreprocessedState.setReservationRequest(reservationRequest);
                        newPreprocessedState.setInterval(new Interval(end, preprocessedState.getEnd()));
                        create(newPreprocessedState);
                        preprocessedStates.add(index, newPreprocessedState);
                        preprocessedState.setEnd(start);
                        break;
                    }
                }
            }
        }
    }

    /**
     * @param dateTime
     * @return state of reservation request at given date/time
     */
    public PreprocessorState getState(DateTime dateTime)
    {
        for (PreprocessedState preprocessedState : preprocessedStates) {
            if (!dateTime.isBefore(preprocessedState.getStart()) && !dateTime.isAfter(preprocessedState.getEnd())) {
                return PreprocessorState.PREPROCESSED;
            }
        }
        return PreprocessorState.NOT_PREPROCESSED;
    }

    /**
     * If both states are found the {@link PreprocessorState#NOT_PREPROCESSED} is returned.
     *
     * @param start
     * @param end
     * @return state of reservation request in given interval
     */
    public PreprocessorState getState(DateTime start, DateTime end)
    {
        return getState(new Interval(start, end));
    }

    /**
     * If both states are present the {@link PreprocessorState#NOT_PREPROCESSED} is returned.
     *
     * @param interval
     * @return state of reservation request in given interval
     */
    public PreprocessorState getState(Interval interval)
    {
        for (PreprocessedState preprocessedState : preprocessedStates) {
            if (preprocessedState.getInterval().contains(interval)) {
                return PreprocessorState.PREPROCESSED;
            }
        }
        return PreprocessorState.NOT_PREPROCESSED;
    }

    /**
     * @param state
     * @param start
     * @param end
     * @return sub-interval from given interval where the reservation request has given state
     */
    public Interval getInterval(PreprocessorState state, DateTime start, DateTime end)
    {
        return getInterval(state, new Interval(start, end));
    }

    /**
     * @param state
     * @param interval
     * @return sub-interval from given interval where the reservation request has given state
     */
    public Interval getInterval(PreprocessorState state, Interval interval)
    {
        DateTime intervalStart = null;
        DateTime intervalEnd = null;

        if (state == PreprocessorState.NOT_PREPROCESSED) {
            intervalStart = interval.getStart();
            intervalEnd = interval.getEnd();
            for (PreprocessedState preprocessedState : preprocessedStates) {
                boolean startIsInside = !intervalStart.isBefore(preprocessedState.getStart())
                        && intervalStart.isBefore(preprocessedState.getEnd());
                boolean endIsInside = intervalEnd.isAfter(preprocessedState.getStart())
                        && !intervalEnd.isAfter(preprocessedState.getEnd());
                if (startIsInside && endIsInside) {
                    return null;
                }
                else if (startIsInside) {
                    intervalStart = preprocessedState.getEnd().plusSeconds(1);
                }
                else if (endIsInside) {
                    intervalEnd = preprocessedState.getStart().minusSeconds(1);
                }
            }
        }
        else {
            throw new RuntimeException("TODO: Implement");
        }

        if (intervalStart != null && intervalEnd != null) {
            return new Interval(intervalStart, intervalEnd);
        }
        else {
            return null;
        }
    }

    /**
     * Set state to reservation request for the whole interval.
     *
     * @param entityManager
     * @param reservationRequest
     * @param state
     */
    public static void setState(EntityManager entityManager, AbstractReservationRequest reservationRequest,
            PreprocessorState state)
    {
        PreprocessorStateManager stateManager =
                new PreprocessorStateManager(entityManager, reservationRequest);
        stateManager.setState(state);
    }

    /**
     * Set state to reservation request for the given interval.
     *
     * @param entityManager
     * @param reservationRequest
     * @param state
     * @param interval
     */
    public static void setState(EntityManager entityManager, AbstractReservationRequest reservationRequest,
            PreprocessorState state, Interval interval)
    {
        PreprocessorStateManager stateManager =
                new PreprocessorStateManager(entityManager, reservationRequest);
        stateManager.setState(state, interval);
    }

    /**
     * Clear all stored information about reservation request state.
     *
     * @param entityManager
     * @param reservationRequest
     */
    public static void clear(EntityManager entityManager, AbstractReservationRequest reservationRequest)
    {
        PreprocessorStateManager stateManager =
                new PreprocessorStateManager(entityManager, reservationRequest);
        stateManager.setState(PreprocessorState.NOT_PREPROCESSED);
    }
}
