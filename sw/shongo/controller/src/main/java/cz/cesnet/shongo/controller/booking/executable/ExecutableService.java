package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;

/**
 * Represents a service for an {@link Executable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class ExecutableService extends SimplePersistentObject
{
    /**
     * {@link Executable} for which the {@link ExecutableService} is allocated.
     */
    private Executable executable;

    /**
     * @see State
     */
    private State state;

    /**
     * Interval start date/time.
     */
    private DateTime slotStart;

    /**
     * Interval end date/time.
     */
    private DateTime slotEnd;

    /**
     * @return {@link #executable}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public Executable getExecutable()
    {
        return executable;
    }

    /**
     * @param executable sets the {@link #executable}
     */
    public void setExecutable(Executable executable)
    {
        // Manage bidirectional association
        if (executable != this.executable) {
            if (this.executable != null) {
                Executable oldExecutable = this.executable;
                this.executable = null;
                oldExecutable.removeService(this);
            }
            if (executable != null) {
                this.executable = executable;
                this.executable.addService(this);
            }
        }
    }

    /**
     * @return {@link #state}
     */
    @Column(nullable = false)
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

    /**
     * @return {@link #slotStart}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.FIELD)
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
    @Access(AccessType.FIELD)
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
    @Transient
    public void setSlot(Interval slot)
    {
        setSlot(slot.getStart(), slot.getEnd());
    }

    /**
     * Sets the slot to new interval created from given {@code start} and {@code end}.
     *
     * @param slotStart
     * @param slotEnd
     */
    @Transient
    public void setSlot(DateTime slotStart, DateTime slotEnd)
    {
        this.slotStart = slotStart;
        this.slotEnd = slotEnd;
    }

    /**
     * @return {@link Executable} converted to {@link cz.cesnet.shongo.controller.api.ExecutableService}
     */
    public final cz.cesnet.shongo.controller.api.ExecutableService toApi()
    {
        cz.cesnet.shongo.controller.api.ExecutableService executableServiceApi = createApi();
        toApi(executableServiceApi);
        return executableServiceApi;
    }

    /**
     * @return new instance of {@link cz.cesnet.shongo.controller.api.ExecutableService}
     */
    protected abstract cz.cesnet.shongo.controller.api.ExecutableService createApi();

    /**
     * Synchronize to {@link cz.cesnet.shongo.controller.api.ExecutableService}.
     *
     * @param executableServiceApi which should be filled from this {@link ExecutableService}
     */
    public void toApi(cz.cesnet.shongo.controller.api.ExecutableService executableServiceApi)
    {
        executableServiceApi.setId(getId());
        executableServiceApi.setActive(State.ACTIVE.equals(state));
        executableServiceApi.setSlot(getSlot());
    }

    /**
     * Enumeration of possible states for {@link ExecutableService}.
     */
    public static enum State
    {
        /**
         * {@link ExecutableService} isn't active (isn't started)
         */
        NOT_ACTIVE,

        /**
         * {@link ExecutableService} should be started.
         */
        PREPARED,

        /**
         * {@link ExecutableService} is active (is started).
         */
        ACTIVE
    }
}
