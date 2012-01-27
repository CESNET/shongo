package cz.cesnet.shongo.measurement.fuse;

import cz.cesnet.shongo.measurement.common.Application;

public class FuseApplication extends Application {

    public static void main(String[] args) {
        Application.runApplication(args, new FuseApplication());
    }

    @Override
    public Class getAgentClass() {
        return FuseAgent.class;
    }
}
