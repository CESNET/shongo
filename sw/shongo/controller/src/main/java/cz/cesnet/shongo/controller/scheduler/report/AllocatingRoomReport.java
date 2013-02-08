package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingRoomReport extends Report
{
    /**
     * List of {@link TechnologySet}s.
     */
    private List<TechnologySet> technologySets = new ArrayList<TechnologySet>();

    /**
     * Number of required ports.
     */
    private Integer participantCount;

    /**
     * Constructor.
     */
    private AllocatingRoomReport()
    {
    }

    /**
     * Constructor.
     *
     * @param technologyVariants sets the {@link #technologySets}
     * @param participantCount   sets the {@link #participantCount}
     */
    public AllocatingRoomReport(Collection<Set<Technology>> technologyVariants, Integer participantCount)
    {
        for (Set<Technology> technologyVariant : technologyVariants) {
            addTechnologies(technologyVariant);
        }
        setParticipantCount(participantCount);
    }

    /**
     * @return {@link #technologySets}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<TechnologySet> getTechnologySets()
    {
        return technologySets;
    }

    /**
     * @param technologies to be added to the {@link #technologySets}
     */
    public void addTechnologies(Set<Technology> technologies)
    {
        this.technologySets.add(new TechnologySet(technologies));
    }

    /**
     * @return {@link #participantCount}
     */
    @Column
    public Integer getParticipantCount()
    {
        return participantCount;
    }

    /**
     * @param participantCount sets the {@link #participantCount}
     */
    public void setParticipantCount(Integer participantCount)
    {
        this.participantCount = participantCount;
    }

    /**
     * @return formatted {@code #technologySets} as string
     */
    private String technologySetsToString()
    {
        StringBuilder builder = new StringBuilder();
        for (TechnologySet technologySet : technologySets) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("[");
            builder.append(Technology.formatTechnologies(technologySet.getTechnologies()));
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Allocating room for the following specification:\n"
                + "             Technology: %s\n"
                + " Number of participants: %d",
                technologySetsToString(),
                participantCount);
    }
}
