package cz.cesnet.shongo.client.web.models;

import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.ObjectRole;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Marek Perichta.
 */
public class ResourceModel {

    protected String id;

    protected String name;

    protected List<UserRoleModel> userRoles = new LinkedList<UserRoleModel>();

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
