package cz.cesnet.shongo.measurement.jxta;

import cz.cesnet.shongo.measurement.common.Application;

/**
 * JXTA application
 *
 * @author Martin Srom
 */
public class JxtaApplication extends Application {

    public static void main(String[] args) {
        Application.runApplication(args, new JxtaApplication());
    }

    @Override
    public Class getAgentClass() {
        return JxtaAgent.class;
    }
}
