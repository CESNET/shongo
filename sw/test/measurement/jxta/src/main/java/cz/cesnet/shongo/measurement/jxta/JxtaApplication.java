package cz.cesnet.shongo.measurement.jxta;

import cz.cesnet.shongo.measurement.common.Application;

/**
 * JXTA application
 *
 * @author Martin Srom
 */
public class JxtaApplication extends Application {

    /**
     * Create JXTA application
     */
    public JxtaApplication()
    {
        super("jxta");
    }

    /**
     * Main JXTA application method
     *
     * @param args
     */
    public static void main(String[] args) {
        Application.runApplication(args, new JxtaApplication());
    }

    /**
     * Get agent class for JXTA application
     *
     * @return class
     */
    @Override
    public Class getAgentClass() {
        return JxtaAgent.class;
    }
}
