package cz.cesnet.shongo.connector.api;

/**
 * Current device load information.
 * <p/>
 * A negative value in any attribute means the value could not be determined.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DeviceLoadInfo
{
    private float cpuLoad = -1;
    private long memoryOccupied = -1L;
    private long memoryAvailable = -1L;
    private long diskSpaceOccupied = -1L;
    private long diskSpaceAvailable = -1L;
    private long upTime = -1L;

    public float getCpuLoad()
    {
        return cpuLoad;
    }

    public void setCpuLoad(float cpuLoad)
    {
        this.cpuLoad = cpuLoad;
    }

    public long getDiskSpaceAvailable()
    {
        return diskSpaceAvailable;
    }

    public void setDiskSpaceAvailable(long diskSpaceAvailable)
    {
        this.diskSpaceAvailable = diskSpaceAvailable;
    }

    public long getDiskSpaceOccupied()
    {
        return diskSpaceOccupied;
    }

    public void setDiskSpaceOccupied(long diskSpaceOccupied)
    {
        this.diskSpaceOccupied = diskSpaceOccupied;
    }

    public long getMemoryAvailable()
    {
        return memoryAvailable;
    }

    public void setMemoryAvailable(long memoryAvailable)
    {
        this.memoryAvailable = memoryAvailable;
    }

    public long getMemoryOccupied()
    {
        return memoryOccupied;
    }

    public void setMemoryOccupied(long memoryOccupied)
    {
        this.memoryOccupied = memoryOccupied;
    }

    public long getUpTime()
    {
        return upTime;
    }

    public void setUpTime(long upTime)
    {
        this.upTime = upTime;
    }
}
