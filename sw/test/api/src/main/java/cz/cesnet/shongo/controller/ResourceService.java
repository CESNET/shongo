package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.*;

import java.util.ArrayList;

/**
 * Resource service
 *
 * @author Martin Srom
 */
public class ResourceService
{
    /**
     * List managed resources
     *
     * @return
     */
    public Resource[] listResources() {
        ArrayList<Resource> resources = new ArrayList<Resource>();
        resources.add(new Resource("urn:id:cz.cesnet.srom", "Martin Srom"));
        resources.add(new Resource("urn:id:cz.cesnet.srom", "Martin Srom"));
        return resources.toArray(new Resource[]{});
    }
}
