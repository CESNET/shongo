package cz.cesnet.shongo.jade.command;

import cz.cesnet.shongo.jade.Agent;

/**
 * A command for starting managing a device by a connector agent.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ManageCommand implements Command
{
    /**
     * Connector class to be used to manage the device.
     */
    private String connectorClass;

    /**
     * Address of the device to manage.
     */
    private String deviceAddress;

    /**
     * Username for managing the device.
     */
    private String authUsername;

    /**
     * Password for managing the device.
     */
    private String authPassword;


    public ManageCommand(String connectorClass, String deviceAddress, String authUsername, String authPassword)
    {
        this.connectorClass = connectorClass;
        this.deviceAddress = deviceAddress;
        this.authUsername = authUsername;
        this.authPassword = authPassword;
    }

    public String getAuthPassword()
    {
        return authPassword;
    }

    public void setAuthPassword(String authPassword)
    {
        this.authPassword = authPassword;
    }

    public String getAuthUsername()
    {
        return authUsername;
    }

    public void setAuthUsername(String authUsername)
    {
        this.authUsername = authUsername;
    }

    public String getConnectorClass()
    {
        return connectorClass;
    }

    public void setConnectorClass(String connectorClass)
    {
        this.connectorClass = connectorClass;
    }

    public String getDeviceAddress()
    {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress)
    {
        this.deviceAddress = deviceAddress;
    }

    @Override
    public boolean process(Agent agent)
    {
        // FIXME: implement
        System.out.printf("Processing the 'manage' command with args: %s, %s, %s, %s\n",
                getConnectorClass(), getDeviceAddress(), getAuthPassword(), getAuthPassword()
        );

        return true;
    }
}
