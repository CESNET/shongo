package cz.cesnet.shongo.measurement.mule;

import cz.cesnet.shongo.measurement.common.EsbApplication;

/**
 * FUSE application
 */
public class MuleApplication extends EsbApplication
{
    /**
     * Create FUSE application
     */
    public MuleApplication()
    {
        super("mule");
    }

    /**
     * Main FUSE application method
     *
     * @param args
     */
    public static void main(String[] args)
    {
        EsbApplication.runApplication(args, new MuleApplication());
    }

    /**
     * Get agent class for FUSE application
     *
     * @return class
     */
    @Override
    public Class getAgentClass()
    {
        return MuleAgent.class;
    }
}
