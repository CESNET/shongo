package cz.cesnet.shongo.controller.api;

/**
* TODO:
*
* @author Martin Srom <martin.srom@cesnet.cz>
*/
public class AllocatedVirtualRoom extends AllocatedResource
{
    private Integer portCount;

    public Integer getPortCount()
    {
        return portCount;
    }

    public void setPortCount(Integer portCount)
    {
        this.portCount = portCount;
    }
}
