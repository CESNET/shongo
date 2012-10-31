package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.scheduler.report.AllocatingCompartmentReport;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;

/**
 * Represents an object which can be executed by an {@link cz.cesnet.shongo.controller.Executor}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Executable extends PersistentObject
{
    /**
     * Interval start date/time.
     */
    private DateTime slotStart;

    /**
     * Interval end date/time.
     */
    private DateTime slotEnd;

    /**
     * Current state of the {@link Compartment}.
     */
    private State state;

    /**
     * @return {@link #slotStart}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getSlotStart()
    {
        return slotStart;
    }

    /**
     * @param slotStart sets the {@link #slotStart}
     */
    public void setSlotStart(DateTime slotStart)
    {
        this.slotStart = slotStart;
    }

    /**
     * @return {@link #slotEnd}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getSlotEnd()
    {
        return slotEnd;
    }

    /**
     * @param slotEnd sets the {@link #slotEnd}
     */
    public void setSlotEnd(DateTime slotEnd)
    {
        this.slotEnd = slotEnd;
    }

    /**
     * @return slot ({@link #slotStart}, {@link #slotEnd})
     */
    @Transient
    public Interval getSlot()
    {
        return new Interval(slotStart, slotEnd);
    }

    /**
     * @param slot sets the slot
     */
    public void setSlot(Interval slot)
    {
        setSlotStart(slot.getStart());
        setSlotEnd(slot.getEnd());
    }

    /**
     * Sets the slot to new interval created from given {@code start} and {@code end}.
     *
     * @param start
     * @param end
     */
    public void setSlot(DateTime start, DateTime end)
    {
        setSlotStart(start);
        setSlotEnd(end);
    }

    /**
     * @return {@link #state}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    @PrePersist
    protected void onCreate()
    {
        if (state == null) {
            state = State.NOT_STARTED;
        }
    }

    /**
     * @return printable name of this {@link Executable}
     */
    @Transient
    public String getName()
    {
        return String.format("executable '%d'", getId());
    }

    /**
     * Start given {@code executable}.
     *
     * @param executorThread thread which is executing
     * @param entityManager  which can be used for starting
     */
    public abstract void start(ExecutorThread executorThread, EntityManager entityManager);

    /**
     * Resume given {@code executable}.
     *
     * @param executorThread thread which is executing
     * @param entityManager  which can be used for resuming
     */
    public abstract void resume(ExecutorThread executorThread, EntityManager entityManager);

    /**
     * Stop given {@code executable}.
     *
     * @param executorThread thread which is executing
     * @param entityManager  which can be used for stopping
     */
    public abstract void stop(ExecutorThread executorThread, EntityManager entityManager);

    /**
     * State of the {@link Compartment}.
     */
    public static enum State
    {
        /**
         * {@link Compartment} which has not been fully allocated yet (e.g., {@link Compartment} is stored
         * for {@link AllocatingCompartmentReport}).
         */
        NOT_ALLOCATED,

        /**
         * {@link Compartment} has not been created yet.
         */
        NOT_STARTED,

        /**
         * {@link Compartment} is already created.
         */
        STARTED,

        /**
         * {@link Compartment} has been already deleted.
         */
        FINISHED;

        /**
         * @return converted to {@link cz.cesnet.shongo.controller.api.Compartment.State}
         */
        public cz.cesnet.shongo.controller.api.Compartment.State toApi()
        {
            switch (this) {
                case NOT_STARTED:
                    return cz.cesnet.shongo.controller.api.Compartment.State.NOT_STARTED;
                case STARTED:
                    return cz.cesnet.shongo.controller.api.Compartment.State.STARTED;
                case FINISHED:
                    return cz.cesnet.shongo.controller.api.Compartment.State.FINISHED;
                default:
                    throw new IllegalStateException("Cannot convert " + this.toString() + " to API.");
            }
        }
    }
}
