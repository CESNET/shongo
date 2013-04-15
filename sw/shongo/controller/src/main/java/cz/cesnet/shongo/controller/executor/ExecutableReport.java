package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.report.Report;
import jade.content.Concept;
import org.joda.time.DateTime;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(length = 50)
public abstract class ExecutableReport extends Report
{
    /**
     * Persistent object must have an unique id.
     */
    private Long id;

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
     * @return {@link #dateTime}
     */
    @Column
    @org.hibernate.annotations.Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
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
