package cz.cesnet.shongo.measurement.fuse;

import cz.cesnet.shongo.measurement.common.EsbApplication;

/**
 * FUSE application
 */
public class FuseApplication extends EsbApplication
{
    /**
     * Create FUSE application
     */
    public FuseApplication()
    {
        super("fuse");
    }

    /**
     * Main FUSE application method
     *
     * @param args
     */
    public static void main(String[] args)
    {
        EsbApplication.runApplication(args, new FuseApplication());
    }

    /**
     * Get agent class for FUSE application
     *
     * @return class
     */
    @Override
    public Class getAgentClass()
    {
        return FuseAgent.class;
    }
}
