package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.TemporalHelper;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class NoAvailableVirtualRoomReport extends Report
{
    /**
     * List of {@link TechnologySet}s.
     */
    private List<TechnologySet> technologySets = new ArrayList<TechnologySet>();

    /**
     * Number of required ports.
     */
    private Integer portCount;

    /**
     * Constructor.
     */
    public NoAvailableVirtualRoomReport()
    {
    }

    /**
     * Constructor.
     *
     * @param technologySets
     * @param portCount
     */
    public NoAvailableVirtualRoomReport(Collection<Set<Technology>> technologySets, Integer portCount)
    {
        for (Set<Technology> technologies : technologySets) {
            this.technologySets.add(new TechnologySet(technologies));
        }
        this.portCount = portCount;
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
     * @param technologySets sets the {@link #technologySets}
     */
    public void setTechnologySets(List<TechnologySet> technologySets)
    {
        this.technologySets = technologySets;
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
        return String.format("No single virtual room was found for following specification:\n"
                + "      Technology: %s\n"
                + " Number of ports: %d",
                technologySetsToString(),
                portCount);
    }
}
