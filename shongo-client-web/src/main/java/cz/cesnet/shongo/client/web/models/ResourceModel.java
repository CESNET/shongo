package cz.cesnet.shongo.client.web.models;


import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.api.Capability;
import cz.cesnet.shongo.controller.api.DeviceResource;
import cz.cesnet.shongo.controller.api.Resource;
import org.joda.time.Period;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Marek Perichta.
 */
public class ResourceModel
{

    public ResourceModel()
    {
    }

    public ResourceModel(Resource resource)
    {
        if (resource instanceof DeviceResource) {
            this.setType(ResourceType.DEVICE_RESOURCE);
            DeviceResource deviceResource = (DeviceResource) resource;
            this.technologies = new LinkedList<String>();
            this.technologies.add(TechnologyModel.find(deviceResource.getTechnologies()).getTitle());
        } else {
            this.setType(ResourceType.RESOURCE);
        }

        this.id = resource.getId();
        this.name = resource.getName();
        this.description = resource.getDescription();
        this.allocatable = resource.getAllocatable();
        this.calendarPublic = resource.isCalendarPublic();
        this.confirmByOwner = resource.isConfirmByOwner();
        this.administratorEmails = resource.getAdministratorEmails();
        this.capabilities = resource.getCapabilities();
        if (resource.getMaximumFuture() instanceof Period) {
            Period maxFuturePeriod = (Period) resource.getMaximumFuture();
            this.maximumFuture = maxFuturePeriod.getMonths();
        }
    }

    private String id;

    private String name;

    private String description;

    private ResourceType type;

    private boolean allocatable;

    private boolean calendarPublic;

    private boolean confirmByOwner;

    private Integer maximumFuture;

    private List<String> technologies;

    private List<String> administratorEmails = new LinkedList<String>();

    private List<Capability> capabilities = new LinkedList<Capability>();


    public List<Capability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<Capability> capabilities) {
        this.capabilities = capabilities;
    }

    public void addCapability (Capability capability) {
        this.capabilities.add(capability);
    }

    public List<String> getAdministratorEmails() {
        return administratorEmails;
    }

    public void setAdministratorEmails(List<String> administratorEmails) {
        this.administratorEmails = administratorEmails;
    }

    public void addAdministratorEmail(String administratorEmail) {
        this.administratorEmails.add(administratorEmail);
    }

    public List<String> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies;
    }

    public Integer getMaximumFuture() {
        return maximumFuture;
    }

    public void setMaximumFuture(Integer maximumFuture) {
        this.maximumFuture = maximumFuture;
    }

    public boolean isConfirmByOwner() {
        return confirmByOwner;
    }

    public void setConfirmByOwner(boolean confirmByOwner) {
        this.confirmByOwner = confirmByOwner;
    }

    public boolean isCalendarPublic() {
        return calendarPublic;
    }

    public void setCalendarPublic(boolean calendarPublic) {
        this.calendarPublic = calendarPublic;
    }

    public boolean isAllocatable() {
        return allocatable;
    }

    public void setAllocatable(boolean allocatable) {
        this.allocatable = allocatable;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Resource toApi () {
        Resource res;
        if (this.type == ResourceType.DEVICE_RESOURCE) {
            res = new DeviceResource();
        } else {
            res = new Resource();
        }

        res.setId(id);
        res.setName(name);
        res.setAllocatable(allocatable);
        res.setDescription(description);
        res.setCalendarPublic(calendarPublic);
        res.setConfirmByOwner(confirmByOwner);
        for (String email : getAdministratorEmails()) {
            res.addAdministratorEmail(email);
        }
        if (maximumFuture != null) {
            Period maxFuturePeriod = new Period().withMonths(maximumFuture);
            res.setMaximumFuture(maxFuturePeriod);
        }

        if (this.type == ResourceType.DEVICE_RESOURCE) {
            if (getTechnologies() != null) {
                DeviceResource deviceResource = (DeviceResource) res;
                Set<Technology> technologySet = new HashSet<Technology>();

                for (String technologySetString : getTechnologies()) {
                    technologySet.addAll(TechnologyModel.getTechnologies(technologySetString));
                }
                for (Technology technology : technologySet) {
                    deviceResource.addTechnology(technology);
                }
            }
        }

        return res;
    }
}
