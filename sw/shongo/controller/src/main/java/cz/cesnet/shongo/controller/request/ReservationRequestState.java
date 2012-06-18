package cz.cesnet.shongo.controller.request;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a state of a reservation request which can vary in time.
 * This class holds all states for all time intervals for a single reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Embeddable
public class ReservationRequestState
{
    /**
     * State tells that reservation request hasn't corresponding compartment requests created
     * or the reservation request has changed they are out-of-sync.
     */
    public static final State NOT_PREPROCESSED = State.NOT_PREPROCESSED;

    /**
     * State tells that reservation request has corresponding compartment requests synced.
     */
    public static final State PREPROCESSED = State.PREPROCESSED;

    /**
     * Type of reservation request.
     */
    public static enum State
    {
        /**
         * @see ReservationRequestState#NOT_PREPROCESSED
         */
        NOT_PREPROCESSED,

        /**
         * @see ReservationRequestState#PREPROCESSED
         */
        PREPROCESSED
    }

    /**
     * Record which defines {@link State#PREPROCESSED} state for a single interval.
     */
    @Embeddable
    private static class Record
    {
        /**
         * Date/time interval from.
         */
        private DateTime from;

        /**
         * Date/time interval to.
         */
        private DateTime to;

        /**
         * Constructor.
         *
         * @param from
         * @param to
         */
        public Record(DateTime from, DateTime to)
        {
            this.from = from;
            this.to = to;
        }

        /**
         * @return {@link #from}
         */
        @Column
        @Type(type = "cz.cesnet.shongo.common.joda.PersistentDateTime")
        public DateTime getFrom()
        {
            return from;
        }

        /**
         * @param from sets the {@link #from}
         */
        public void setFrom(DateTime from)
        {
            this.from = from;
        }

        /**
         * @return {@link #to}
         */
        @Column
        @Type(type = "cz.cesnet.shongo.common.joda.PersistentDateTime")
        public DateTime getTo()
        {
            return to;
        }

        /**
         * @param to sets the {@link #to}
         */
        public void setTo(DateTime to)
        {
            this.to = to;
        }
    }

    /**
     * List of state records.
     */
    @ElementCollection
    @OrderColumn(name = "position")
    @Access(AccessType.FIELD)
    private List<Record> records = new ArrayList<Record>();

    /**
     * @return number of records which implements the reservation state
     */
    public int getRecordCount()
    {
        return records.size();
    }

    /**
     * @param state
     */
    public void setState(State state)
    {
        if (state == State.NOT_PREPROCESSED) {
            records.clear();
        }
        else {
            throw new IllegalArgumentException("Cannot set " + State.PREPROCESSED.toString()
                    + "for the whole time interval!");
        }
    }

    /**
     * Set reservation request state in the given interval.
     *
     * @param state
     * @param from
     * @param to
     */
    public void setState(State state, DateTime from, DateTime to)
    {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Interval 'from' and 'to' should not be empty!");
        }
        if (from.equals(to)) {
            return;
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Interval 'from' should not be after 'to' (from="
                    + from.toString() + ", to=" + to.toString() + ")!");
        }

        // Add the record before first
        if (records.size() == 0 || to.isBefore(records.get(0).getFrom())) {
            // Skip the not-preprocessed
            if (state == State.PREPROCESSED) {
                records.add(0, new Record(from, to));
            }
        }
        // Add the record after last
        else if (from.isAfter(records.get(records.size() - 1).getTo())) {
            // Skip the not-preprocessed
            if (state == State.PREPROCESSED) {
                records.add(new Record(from, to));
            }
        }
        else {
            // For preprocessed state, a record can be created or some records can be merged
            if (state == State.PREPROCESSED) {
                Record newRecord = null;
                for (int index = 0; index < records.size(); index++) {
                    Record record = records.get(index);

                    // Compute statements
                    boolean fromIsBeforeOrEqual = !from.isAfter(record.getFrom());
                    boolean fromIsInside = !from.isBefore(record.getFrom()) && !from.isAfter(record.getTo());
                    boolean toIsAfterOrEqual = !to.isBefore(record.getTo());
                    boolean toIsInside = !to.isBefore(record.getFrom()) && !to.isAfter(record.getTo());

                    // If the interval overlaps the entire record, merge or delete the record
                    if ( fromIsBeforeOrEqual && toIsAfterOrEqual ) {
                        if ( newRecord == null ) {
                            newRecord = record;
                            newRecord.setFrom(from);
                            newRecord.setTo(to);
                        }
                        else {
                            records.remove(index);
                            index--;
                        }
                    }
                    // If the interval overlaps the beginning of the record, merge or delete the record
                    else if (fromIsBeforeOrEqual && toIsInside ) {
                        if ( newRecord == null ) {
                            newRecord = record;
                            newRecord.setFrom(from);
                        } else {
                            newRecord.setTo(record.getTo());
                            records.remove(index);
                            index--;
                        }
                        break;
                    }
                    // If the interval overlaps the end of the record, update the record
                    else if (fromIsInside && toIsAfterOrEqual ) {
                        if ( newRecord != null ) {
                            throw new IllegalStateException("Should never happen!");
                        }
                        newRecord = record;
                        newRecord.setTo(to);
                    }
                    // If the interval is inside the record, do nothing
                    else if ( fromIsInside && toIsInside ) {
                        break;
                    }
                }
            }
            // For non-preprocessed state some records may be erased
            else {
                for (int index = 0; index < records.size(); index++) {
                    Record record = records.get(index);

                    // Compute statements
                    boolean fromIsBeforeOrEqual = !from.isAfter(record.getFrom());
                    boolean fromIsInside = !from.isBefore(record.getFrom()) && !from.isAfter(record.getTo());
                    boolean toIsAfterOrEqual = !to.isBefore(record.getTo());
                    boolean toIsInside = !to.isBefore(record.getFrom()) && !to.isAfter(record.getTo());

                    // If the interval overlaps the entire record, delete the record
                    if ( fromIsBeforeOrEqual && toIsAfterOrEqual ) {
                        records.remove(index);
                        index--;
                    }
                    // If the interval overlaps the beginning of the record, update the record
                    else if (fromIsBeforeOrEqual && toIsInside ) {
                        record.setFrom(to);
                        break;
                    }
                    // If the interval overlaps the end of the record, update the record
                    else if (fromIsInside && toIsAfterOrEqual ) {
                        record.setTo(from);
                    }
                    // If the interval is inside the record, split the record
                    else if ( fromIsInside && toIsInside ) {
                        index++;
                        records.add(index, new Record(to, record.getTo()));
                        record.setTo(from);
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
    public State getState(DateTime dateTime)
    {
        for (Record record : records) {
            if (!dateTime.isBefore(record.getFrom()) && !dateTime.isAfter(record.getTo())) {
                return State.PREPROCESSED;
            }
        }
        return State.NOT_PREPROCESSED;
    }

    /**
     * If both states are found the {@link #NOT_PREPROCESSED} is returned.
     *
     * @param from
     * @param to
     * @return state of reservation request in given interval
     */
    public State getState(DateTime from, DateTime to)
    {
        for (Record record : records) {
            if (!from.isBefore(record.getFrom()) && !to.isAfter(record.getTo())) {
                return State.PREPROCESSED;
            }
        }
        return State.NOT_PREPROCESSED;
    }

    /**
     * @param state
     * @param from
     * @param to
     * @return sub-interval from given interval where the reservation request has given state
     */
    public Interval getInterval(State state, DateTime from, DateTime to)
    {
        DateTime intervalFrom = null;
        DateTime intervalTo = null;
        
        if ( state == State.NOT_PREPROCESSED ) {
            intervalFrom = from;
            intervalTo = to;
            for ( Record record : records ) {
                boolean fromIsInside = !intervalFrom.isBefore(record.getFrom()) && intervalFrom.isBefore(record.getTo());
                boolean toIsInside = intervalTo.isAfter(record.getFrom()) && !intervalTo.isAfter(record.getTo());
                if ( fromIsInside && toIsInside ) {
                    return null;
                } else if ( fromIsInside ) {
                    intervalFrom = record.getTo().plusSeconds(1);
                } else if ( toIsInside ) {
                    intervalTo = record.getFrom().minusSeconds(1);
                }
            }
        }
        else {
            throw new RuntimeException("TODO: Implement");
        }
        
        if ( intervalFrom != null && intervalTo != null ) {
            return new Interval(intervalFrom, intervalTo);
        } else {
            return null;
        }
    }

    @PostLoad
    private void postLoad()
    {
        for (int index = 0; index < records.size(); index++) {
            Record record = records.get(index);
            DateTime recordFrom = record.getFrom();
            DateTime recordTo = record.getTo();
            if (recordFrom.isAfter(recordTo)) {
                throw new IllegalArgumentException("Interval 'from' should not be after 'to' (from="
                        + recordFrom.toString() + ", to=" + recordTo.toString() + ")!");
            }
            if (index >= 1) {
                Record previousRecord = records.get(index - 1);
                if (!recordFrom.isAfter(previousRecord.getTo())) {
                    throw new IllegalArgumentException("Interval 'from' should be after previous interval 'to' (from="
                            + recordFrom.toString() + ", previous-to=" + previousRecord.getTo().toString() + ")!");
                }
            }
        }
    }
}
