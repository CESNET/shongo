package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.report.AbstractReport;
import cz.cesnet.shongo.report.SerializableReport;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(length = 50)
public abstract class ExecutableReport extends AbstractReport
{
    /**
     * Persistent object must have an unique id.
     */
    private Long id;

    /**
     * {@link Executable} to which the {@link cz.cesnet.shongo.report.AbstractReport} belongs.
     */
    private Executable executable;

    /**
     * Date/time when the report was created.
     */
    private DateTime dateTime;

    /**
     * Constructor.
     */
    public ExecutableReport()
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
     * @return {@link #executable}
     */
    @OneToOne
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
                oldExecutable.removeReport(this);
            }
            if (executable != null) {
                this.executable = executable;
                this.executable.addReport(this);
            }
        }
    }

    /**
     * @return {@link #dateTime}
     */
    @Column
    @org.hibernate.annotations.Type(type = "DateTime")
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
