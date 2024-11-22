package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.booking.executable.ExecutionTarget;
import cz.cesnet.shongo.hibernate.PersistentDateTime;
import cz.cesnet.shongo.report.AbstractReport;
import org.hibernate.annotations.Parameter;
import org.hibernate.usertype.UserTypeLegacyBridge;
import org.joda.time.DateTime;

import jakarta.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(length = 50)
public abstract class ExecutionReport extends AbstractReport
{
    /**
     * Persistent object must have an unique id.
     */
    private Long id;

    /**
     * {@link ExecutionTarget} to which the {@link ExecutionReport} belongs.
     */
    private ExecutionTarget executionTarget;

    /**
     * Date/time when the report was created.
     */
    private DateTime dateTime;

    /**
     * Constructor.
     */
    public ExecutionReport()
    {
    }

    /**
     * @return {@link #id}
     */
    @Id
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * @return {@link #executionTarget}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public ExecutionTarget getExecutionTarget()
    {
        return executionTarget;
    }

    /**
     * @param ExecutionTarget sets the {@link #executionTarget}
     */
    public void setExecutionTarget(ExecutionTarget ExecutionTarget)
    {
        // Manage bidirectional association
        if (ExecutionTarget != this.executionTarget) {
            if (this.executionTarget != null) {
                ExecutionTarget oldExecutionTarget = this.executionTarget;
                this.executionTarget = null;
                oldExecutionTarget.removeReport(this);
            }
            if (ExecutionTarget != null) {
                this.executionTarget = ExecutionTarget;
                this.executionTarget.addReport(this);
            }
        }
    }

    /**
     * @return {@link #dateTime}
     */
    @Column
    @org.hibernate.annotations.Type(PersistentDateTime.class)
    public DateTime getDateTime()
    {
        return dateTime;
    }

    /**
     * @param dateTime sets the {@link #dateTime}
     */
    public void setDateTime(DateTime dateTime)
    {
        this.dateTime = dateTime;
    }
}
