package cz.cesnet.shongo.api;

import cz.cesnet.shongo.api.ConnectorStatus;
import cz.cesnet.shongo.api.util.DeviceAddress;

/**
 * {@link cz.cesnet.shongo.api.ConnectorStatus} for devices.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DeviceConnectorStatus extends ConnectorStatus
{
    private DeviceAddress deviceAddress;
    private String deviceName;
    private String deviceDescription;
    private String deviceSerialNumber;
    private String deviceSoftwareVersion;

    public DeviceConnectorStatus()
    {
    }

    public DeviceConnectorStatus(ConnectorStatus connectorStatus)
    {
        setState(connectorStatus.getState());
    }

    public DeviceAddress getDeviceAddress()
    {
        return deviceAddress;
    }

    public void setDeviceAddress(DeviceAddress deviceAddress)
    {
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }

    public String getDeviceDescription()
    {
        return deviceDescription;
    }

    public void setDeviceDescription(String deviceDescription)
    {
        this.deviceDescription = deviceDescription;
    }

    public String getDeviceSerialNumber()
    {
        return deviceSerialNumber;
    }

    public void setDeviceSerialNumber(String deviceSerialNumber)
    {
        this.deviceSerialNumber = deviceSerialNumber;
    }

    public String getDeviceSoftwareVersion()
    {
        return deviceSoftwareVersion;
    }

    public void setDeviceSoftwareVersion(String deviceSoftwareVersion)
    {
        this.deviceSoftwareVersion = deviceSoftwareVersion;
    }

    private static final String DEVICE_ADDRESS = "deviceAddress";
    private static final String DEVICE_NAME = "deviceName";
    private static final String DEVICE_DESCRIPTION = "deviceDescription";
    private static final String DEVICE_SERIAL_NUMBER = "deviceSerialNumber";
    private static final String DEVICE_SOFTWARE_VERSION = "deviceSoftwareVersion";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(DEVICE_ADDRESS, deviceAddress.toString());
        dataMap.set(DEVICE_NAME, deviceName);
        dataMap.set(DEVICE_DESCRIPTION, deviceDescription);
        dataMap.set(DEVICE_SERIAL_NUMBER, deviceSerialNumber);
        dataMap.set(DEVICE_SOFTWARE_VERSION, deviceSoftwareVersion);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        deviceAddress = DeviceAddress.parseAddress(dataMap.getString(DEVICE_ADDRESS));
        deviceName = dataMap.getString(DEVICE_NAME);
        deviceDescription = dataMap.getString(DEVICE_DESCRIPTION);
        deviceSerialNumber = dataMap.getString(DEVICE_SERIAL_NUMBER);
        deviceSoftwareVersion = dataMap.getString(DEVICE_SOFTWARE_VERSION);
    }

    @Override
    public String toString()
    {
        return String.format("DeviceConnectorStatus (state: %s, address: %s)", getState(), getDeviceAddress());
    }
}
