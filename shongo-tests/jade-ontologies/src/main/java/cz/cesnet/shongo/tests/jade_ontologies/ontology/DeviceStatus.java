package cz.cesnet.shongo.tests.jade_ontologies.ontology;

import jade.content.Concept;

/**
 * A POJO holding device status info - CPU and memory usage.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DeviceStatus implements Concept
{
    private double cpuLoad;
    private long memoryUsage;

    public double getCpuLoad()
    {
        return cpuLoad;
    }

    public void setCpuLoad(double cpuLoad)
    {
        this.cpuLoad = cpuLoad;
    }

    public long getMemoryUsage()
    {
        return memoryUsage;
    }

    public void setMemoryUsage(long memoryUsage)
    {
        this.memoryUsage = memoryUsage;
    }
}
