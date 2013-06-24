package cz.cesnet.shongo.api;

import jade.content.Concept;

/**
 * Current device load information.
 * <p/>
 * A null value in any attribute means the value could not be determined.
 * <p/>
 * Note that some values are represented as strings. This is because the objects need to be transferred through XML-RPC,
 * which does not support long integer, thus, long integers are represented by strings containing decimal representation
 * of the long values.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DeviceLoadInfo implements Concept
{
    private Double cpuLoad = null;
    private String memoryOccupied = null;
    private String memoryAvailable = null;
    private String diskSpaceOccupied = null;
    private String diskSpaceAvailable = null;
    private Integer uptime = null;

    /**
     * @return CPU load as a percentage of maximum
     */
    public Double getCpuLoad()
    {
        return cpuLoad;
    }

    /**
     * @param cpuLoad CPU load as a percentage of maximum
     */
    public void setCpuLoad(Double cpuLoad)
    {
        this.cpuLoad = cpuLoad;
    }

    /**
     * @return total amount of available disk space in bytes;
     *         as a string containing decimal representation of long integer
     */
    public String getDiskSpaceAvailable()
    {
        return diskSpaceAvailable;
    }

    public void setDiskSpaceAvailable(long diskSpaceAvailable)
    {
        this.diskSpaceAvailable = String.valueOf(diskSpaceAvailable);
    }

    public void setDiskSpaceAvailable(String diskSpaceAvailable)
    {
        this.diskSpaceAvailable = diskSpaceAvailable;
    }

    /**
     * @return total amount of occupied disk space in bytes;
     *         as a string containing decimal representation of long integer
     */
    public String getDiskSpaceOccupied()
    {
        return diskSpaceOccupied;
    }

    public void setDiskSpaceOccupied(long diskSpaceOccupied)
    {
        this.diskSpaceOccupied = String.valueOf(diskSpaceOccupied);
    }

    public void setDiskSpaceOccupied(String diskSpaceOccupied)
    {
        this.diskSpaceOccupied = diskSpaceOccupied;
    }

    /**
     * @return total amount of available memory in bytes;
     *         as a string containing decimal representation of long integer
     */
    public String getMemoryAvailable()
    {
        return memoryAvailable;
    }

    public void setMemoryAvailable(long memoryAvailable)
    {
        this.memoryAvailable = String.valueOf(memoryAvailable);
    }

    public void setMemoryAvailable(String memoryAvailable)
    {
        this.memoryAvailable = memoryAvailable;
    }

    /**
     * @return total amount of memory currently occupied in bytes;
     *         as a string containing decimal representation of long integer
     */
    public String getMemoryOccupied()
    {
        return memoryOccupied;
    }

    public void setMemoryOccupied(long memoryOccupied)
    {
        this.memoryOccupied = String.valueOf(memoryOccupied);
    }

    public void setMemoryOccupied(String memoryOccupied)
    {
        this.memoryOccupied = memoryOccupied;
    }

    /**
     * @return device uptime in seconds
     */
    public Integer getUptime()
    {
        return uptime;
    }

    /**
     * @param uptime device uptime in seconds
     */
    public void setUptime(Integer uptime)
    {
        this.uptime = uptime;
    }
}
