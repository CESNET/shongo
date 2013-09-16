package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class SchedulerReportSet extends AbstractReportSet
{
    /**
     * Resource {@link #resource}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceReport")
    public static class ResourceReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.resource.Resource resource;

        public ResourceReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "resource";
        }

        public ResourceReport(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            setResource(resource);
        }

        @javax.persistence.OneToOne(fetch = javax.persistence.FetchType.LAZY)
        @javax.persistence.Access(javax.persistence.AccessType.FIELD)
        @javax.persistence.JoinColumn(name = "resource_id")
        public cz.cesnet.shongo.controller.resource.Resource getResource()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(resource);
        }

        public void setResource(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            this.resource = resource;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource", userType, language, getParameters());
        }
    }

    /**
     * The resource {@link #resource} is disabled for allocation.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceNotAllocatableReport")
    public static class ResourceNotAllocatableReport extends ResourceReport
    {
        public ResourceNotAllocatableReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "resource-not-allocatable";
        }

        public ResourceNotAllocatableReport(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            setResource(resource);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-not-allocatable", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ResourceNotAllocatableReport}.
     */
    public static class ResourceNotAllocatableException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ResourceNotAllocatableException(ResourceNotAllocatableReport report)
        {
            this.report = report;
        }

        public ResourceNotAllocatableException(Throwable throwable, ResourceNotAllocatableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ResourceNotAllocatableException(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            ResourceNotAllocatableReport report = new ResourceNotAllocatableReport();
            report.setResource(resource);
            this.report = report;
        }

        public ResourceNotAllocatableException(Throwable throwable, cz.cesnet.shongo.controller.resource.Resource resource)
        {
            super(throwable);
            ResourceNotAllocatableReport report = new ResourceNotAllocatableReport();
            report.setResource(resource);
            this.report = report;
        }

        @Override
        public ResourceNotAllocatableReport getReport()
        {
            return (ResourceNotAllocatableReport) report;
        }
    }

    /**
     * The resource {@link #resource} is already allocated.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceAlreadyAllocatedReport")
    public static class ResourceAlreadyAllocatedReport extends ResourceReport
    {
        public ResourceAlreadyAllocatedReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "resource-already-allocated";
        }

        public ResourceAlreadyAllocatedReport(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            setResource(resource);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-already-allocated", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ResourceAlreadyAllocatedReport}.
     */
    public static class ResourceAlreadyAllocatedException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ResourceAlreadyAllocatedException(ResourceAlreadyAllocatedReport report)
        {
            this.report = report;
        }

        public ResourceAlreadyAllocatedException(Throwable throwable, ResourceAlreadyAllocatedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ResourceAlreadyAllocatedException(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            ResourceAlreadyAllocatedReport report = new ResourceAlreadyAllocatedReport();
            report.setResource(resource);
            this.report = report;
        }

        public ResourceAlreadyAllocatedException(Throwable throwable, cz.cesnet.shongo.controller.resource.Resource resource)
        {
            super(throwable);
            ResourceAlreadyAllocatedReport report = new ResourceAlreadyAllocatedReport();
            report.setResource(resource);
            this.report = report;
        }

        @Override
        public ResourceAlreadyAllocatedReport getReport()
        {
            return (ResourceAlreadyAllocatedReport) report;
        }
    }

    /**
     * The resource {@link #resource} is not available for the requested time slot. The maximum date/time for which the resource can be allocated is {@link #maxDateTime}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceNotAvailableReport")
    public static class ResourceNotAvailableReport extends ResourceReport
    {
        protected org.joda.time.DateTime maxDateTime;

        public ResourceNotAvailableReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "resource-not-available";
        }

        public ResourceNotAvailableReport(cz.cesnet.shongo.controller.resource.Resource resource, org.joda.time.DateTime maxDateTime)
        {
            setResource(resource);
            setMaxDateTime(maxDateTime);
        }

        @javax.persistence.Column
        @org.hibernate.annotations.Type(type = "DateTime")
        public org.joda.time.DateTime getMaxDateTime()
        {
            return maxDateTime;
        }

        public void setMaxDateTime(org.joda.time.DateTime maxDateTime)
        {
            this.maxDateTime = maxDateTime;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            parameters.put("maxDateTime", maxDateTime);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-not-available", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ResourceNotAvailableReport}.
     */
    public static class ResourceNotAvailableException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ResourceNotAvailableException(ResourceNotAvailableReport report)
        {
            this.report = report;
        }

        public ResourceNotAvailableException(Throwable throwable, ResourceNotAvailableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ResourceNotAvailableException(cz.cesnet.shongo.controller.resource.Resource resource, org.joda.time.DateTime maxDateTime)
        {
            ResourceNotAvailableReport report = new ResourceNotAvailableReport();
            report.setResource(resource);
            report.setMaxDateTime(maxDateTime);
            this.report = report;
        }

        public ResourceNotAvailableException(Throwable throwable, cz.cesnet.shongo.controller.resource.Resource resource, org.joda.time.DateTime maxDateTime)
        {
            super(throwable);
            ResourceNotAvailableReport report = new ResourceNotAvailableReport();
            report.setResource(resource);
            report.setMaxDateTime(maxDateTime);
            this.report = report;
        }

        public org.joda.time.DateTime getMaxDateTime()
        {
            return getReport().getMaxDateTime();
        }

        @Override
        public ResourceNotAvailableReport getReport()
        {
            return (ResourceNotAvailableReport) report;
        }
    }

    /**
     * The resource {@link #resource} is not endpoint.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceNotEndpointReport")
    public static class ResourceNotEndpointReport extends ResourceReport
    {
        public ResourceNotEndpointReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "resource-not-endpoint";
        }

        public ResourceNotEndpointReport(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            setResource(resource);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-not-endpoint", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ResourceNotEndpointReport}.
     */
    public static class ResourceNotEndpointException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ResourceNotEndpointException(ResourceNotEndpointReport report)
        {
            this.report = report;
        }

        public ResourceNotEndpointException(Throwable throwable, ResourceNotEndpointReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ResourceNotEndpointException(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            ResourceNotEndpointReport report = new ResourceNotEndpointReport();
            report.setResource(resource);
            this.report = report;
        }

        public ResourceNotEndpointException(Throwable throwable, cz.cesnet.shongo.controller.resource.Resource resource)
        {
            super(throwable);
            ResourceNotEndpointReport report = new ResourceNotEndpointReport();
            report.setResource(resource);
            this.report = report;
        }

        @Override
        public ResourceNotEndpointReport getReport()
        {
            return (ResourceNotEndpointReport) report;
        }
    }

    /**
     * The resource {@link #resource} is requested multiple times.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceMultipleRequestedReport")
    public static class ResourceMultipleRequestedReport extends ResourceReport
    {
        public ResourceMultipleRequestedReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "resource-multiple-requested";
        }

        public ResourceMultipleRequestedReport(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            setResource(resource);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-multiple-requested", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ResourceMultipleRequestedReport}.
     */
    public static class ResourceMultipleRequestedException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ResourceMultipleRequestedException(ResourceMultipleRequestedReport report)
        {
            this.report = report;
        }

        public ResourceMultipleRequestedException(Throwable throwable, ResourceMultipleRequestedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ResourceMultipleRequestedException(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            ResourceMultipleRequestedReport report = new ResourceMultipleRequestedReport();
            report.setResource(resource);
            this.report = report;
        }

        public ResourceMultipleRequestedException(Throwable throwable, cz.cesnet.shongo.controller.resource.Resource resource)
        {
            super(throwable);
            ResourceMultipleRequestedReport report = new ResourceMultipleRequestedReport();
            report.setResource(resource);
            this.report = report;
        }

        @Override
        public ResourceMultipleRequestedReport getReport()
        {
            return (ResourceMultipleRequestedReport) report;
        }
    }

    /**
     * No available resource was found for the following specification: Technologies: {@link #technologies}
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceNotFoundReport")
    public static class ResourceNotFoundReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected java.util.Set<cz.cesnet.shongo.Technology> technologies;

        public ResourceNotFoundReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "resource-not-found";
        }

        public ResourceNotFoundReport(java.util.Set<cz.cesnet.shongo.Technology> technologies)
        {
            setTechnologies(technologies);
        }

        @javax.persistence.ElementCollection
        public java.util.Set<cz.cesnet.shongo.Technology> getTechnologies()
        {
            return technologies;
        }

        public void setTechnologies(java.util.Set<cz.cesnet.shongo.Technology> technologies)
        {
            this.technologies = technologies;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("technologies", technologies);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-not-found", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ResourceNotFoundReport}.
     */
    public static class ResourceNotFoundException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ResourceNotFoundException(ResourceNotFoundReport report)
        {
            this.report = report;
        }

        public ResourceNotFoundException(Throwable throwable, ResourceNotFoundReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ResourceNotFoundException(java.util.Set<cz.cesnet.shongo.Technology> technologies)
        {
            ResourceNotFoundReport report = new ResourceNotFoundReport();
            report.setTechnologies(technologies);
            this.report = report;
        }

        public ResourceNotFoundException(Throwable throwable, java.util.Set<cz.cesnet.shongo.Technology> technologies)
        {
            super(throwable);
            ResourceNotFoundReport report = new ResourceNotFoundReport();
            report.setTechnologies(technologies);
            this.report = report;
        }

        public java.util.Set<cz.cesnet.shongo.Technology> getTechnologies()
        {
            return getReport().getTechnologies();
        }

        @Override
        public ResourceNotFoundReport getReport()
        {
            return (ResourceNotFoundReport) report;
        }
    }

    /**
     * Reusing existing {@link #executable}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ExecutableReusingReport")
    public static class ExecutableReusingReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.executor.Executable executable;

        public ExecutableReusingReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "executable-reusing";
        }

        public ExecutableReusingReport(cz.cesnet.shongo.controller.executor.Executable executable)
        {
            setExecutable(executable);
        }

        @javax.persistence.OneToOne(fetch = javax.persistence.FetchType.LAZY)
        @javax.persistence.Access(javax.persistence.AccessType.FIELD)
        @javax.persistence.JoinColumn(name = "executable_id")
        public cz.cesnet.shongo.controller.executor.Executable getExecutable()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(executable);
        }

        public void setExecutable(cz.cesnet.shongo.controller.executor.Executable executable)
        {
            this.executable = executable;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("executable", executable);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("executable-reusing", userType, language, getParameters());
        }
    }

    /**
     * Not enough endpoints are requested for the compartment.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("CompartmentNotEnoughEndpointReport")
    public static class CompartmentNotEnoughEndpointReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public CompartmentNotEnoughEndpointReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "compartment-not-enough-endpoint";
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("compartment-not-enough-endpoint", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link CompartmentNotEnoughEndpointReport}.
     */
    public static class CompartmentNotEnoughEndpointException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public CompartmentNotEnoughEndpointException(CompartmentNotEnoughEndpointReport report)
        {
            this.report = report;
        }

        public CompartmentNotEnoughEndpointException(Throwable throwable, CompartmentNotEnoughEndpointReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CompartmentNotEnoughEndpointException()
        {
            CompartmentNotEnoughEndpointReport report = new CompartmentNotEnoughEndpointReport();
            this.report = report;
        }

        public CompartmentNotEnoughEndpointException(Throwable throwable)
        {
            super(throwable);
            CompartmentNotEnoughEndpointReport report = new CompartmentNotEnoughEndpointReport();
            this.report = report;
        }

        @Override
        public CompartmentNotEnoughEndpointReport getReport()
        {
            return (CompartmentNotEnoughEndpointReport) report;
        }
    }

    /**
     * Cannot assign alias to allocated external endpoint.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("CompartmentAssignAliasToExternalEndpointReport")
    public static class CompartmentAssignAliasToExternalEndpointReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public CompartmentAssignAliasToExternalEndpointReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "compartment-assign-alias-to-external-endpoint";
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("compartment-assign-alias-to-external-endpoint", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link CompartmentAssignAliasToExternalEndpointReport}.
     */
    public static class CompartmentAssignAliasToExternalEndpointException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public CompartmentAssignAliasToExternalEndpointException(CompartmentAssignAliasToExternalEndpointReport report)
        {
            this.report = report;
        }

        public CompartmentAssignAliasToExternalEndpointException(Throwable throwable, CompartmentAssignAliasToExternalEndpointReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CompartmentAssignAliasToExternalEndpointException()
        {
            CompartmentAssignAliasToExternalEndpointReport report = new CompartmentAssignAliasToExternalEndpointReport();
            this.report = report;
        }

        public CompartmentAssignAliasToExternalEndpointException(Throwable throwable)
        {
            super(throwable);
            CompartmentAssignAliasToExternalEndpointReport report = new CompartmentAssignAliasToExternalEndpointReport();
            this.report = report;
        }

        @Override
        public CompartmentAssignAliasToExternalEndpointReport getReport()
        {
            return (CompartmentAssignAliasToExternalEndpointReport) report;
        }
    }

    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ConnectionReport")
    public static abstract class ConnectionReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.executor.Endpoint endpointFrom;

        protected cz.cesnet.shongo.controller.executor.Endpoint endpointTo;

        public ConnectionReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "connection";
        }

        public ConnectionReport(cz.cesnet.shongo.controller.executor.Endpoint endpointFrom, cz.cesnet.shongo.controller.executor.Endpoint endpointTo)
        {
            setEndpointFrom(endpointFrom);
            setEndpointTo(endpointTo);
        }

        @javax.persistence.OneToOne(cascade = javax.persistence.CascadeType.PERSIST, fetch = javax.persistence.FetchType.LAZY)
        @javax.persistence.Access(javax.persistence.AccessType.FIELD)
        @javax.persistence.JoinColumn(name = "endpointfrom_id")
        public cz.cesnet.shongo.controller.executor.Endpoint getEndpointFrom()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(endpointFrom);
        }

        public void setEndpointFrom(cz.cesnet.shongo.controller.executor.Endpoint endpointFrom)
        {
            this.endpointFrom = endpointFrom;
        }

        @javax.persistence.OneToOne(cascade = javax.persistence.CascadeType.PERSIST, fetch = javax.persistence.FetchType.LAZY)
        @javax.persistence.Access(javax.persistence.AccessType.FIELD)
        @javax.persistence.JoinColumn(name = "endpointto_id")
        public cz.cesnet.shongo.controller.executor.Endpoint getEndpointTo()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(endpointTo);
        }

        public void setEndpointTo(cz.cesnet.shongo.controller.executor.Endpoint endpointTo)
        {
            this.endpointTo = endpointTo;
        }

        @javax.persistence.PreRemove
        public void preRemove()
        {
            if (endpointFrom.getState() == cz.cesnet.shongo.controller.executor.Executable.State.NOT_ALLOCATED) {
                endpointFrom.setState(cz.cesnet.shongo.controller.executor.Executable.State.TO_DELETE);
            }
            if (endpointTo.getState() == cz.cesnet.shongo.controller.executor.Executable.State.NOT_ALLOCATED) {
                endpointTo.setState(cz.cesnet.shongo.controller.executor.Executable.State.TO_DELETE);
            }
        }
    }

    /**
     * Creating connection between {@link #endpointFrom} and {@link #endpointTo} in technology {@link #technology}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ConnectionBetweenReport")
    public static class ConnectionBetweenReport extends ConnectionReport
    {
        protected cz.cesnet.shongo.Technology technology;

        public ConnectionBetweenReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "connection-between";
        }

        public ConnectionBetweenReport(cz.cesnet.shongo.controller.executor.Endpoint endpointFrom, cz.cesnet.shongo.controller.executor.Endpoint endpointTo, cz.cesnet.shongo.Technology technology)
        {
            setEndpointFrom(endpointFrom);
            setEndpointTo(endpointTo);
            setTechnology(technology);
        }

        @javax.persistence.Column
        @javax.persistence.Enumerated(javax.persistence.EnumType.STRING)
        public cz.cesnet.shongo.Technology getTechnology()
        {
            return technology;
        }

        public void setTechnology(cz.cesnet.shongo.Technology technology)
        {
            this.technology = technology;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("endpointFrom", endpointFrom);
            parameters.put("endpointTo", endpointTo);
            parameters.put("technology", technology);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("connection-between", userType, language, getParameters());
        }
    }

    /**
     * Creating connection from {@link #endpointFrom} to {@link #endpointTo}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ConnectionFromToReport")
    public static class ConnectionFromToReport extends ConnectionReport
    {
        public ConnectionFromToReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "connection-from-to";
        }

        public ConnectionFromToReport(cz.cesnet.shongo.controller.executor.Endpoint endpointFrom, cz.cesnet.shongo.controller.executor.Endpoint endpointTo)
        {
            setEndpointFrom(endpointFrom);
            setEndpointTo(endpointTo);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("endpointFrom", endpointFrom);
            parameters.put("endpointTo", endpointTo);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("connection-from-to", userType, language, getParameters());
        }
    }

    /**
     * Cannot create connection from {@link #endpointFrom} to {@link #endpointTo}, because the target represents multiple endpoints (not supported yet).
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ConnectionToMultipleReport")
    public static class ConnectionToMultipleReport extends ConnectionReport
    {
        public ConnectionToMultipleReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "connection-to-multiple";
        }

        public ConnectionToMultipleReport(cz.cesnet.shongo.controller.executor.Endpoint endpointFrom, cz.cesnet.shongo.controller.executor.Endpoint endpointTo)
        {
            setEndpointFrom(endpointFrom);
            setEndpointTo(endpointTo);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("endpointFrom", endpointFrom);
            parameters.put("endpointTo", endpointTo);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("connection-to-multiple", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ConnectionToMultipleReport}.
     */
    public static class ConnectionToMultipleException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ConnectionToMultipleException(ConnectionToMultipleReport report)
        {
            this.report = report;
        }

        public ConnectionToMultipleException(Throwable throwable, ConnectionToMultipleReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ConnectionToMultipleException(cz.cesnet.shongo.controller.executor.Endpoint endpointFrom, cz.cesnet.shongo.controller.executor.Endpoint endpointTo)
        {
            ConnectionToMultipleReport report = new ConnectionToMultipleReport();
            report.setEndpointFrom(endpointFrom);
            report.setEndpointTo(endpointTo);
            this.report = report;
        }

        public ConnectionToMultipleException(Throwable throwable, cz.cesnet.shongo.controller.executor.Endpoint endpointFrom, cz.cesnet.shongo.controller.executor.Endpoint endpointTo)
        {
            super(throwable);
            ConnectionToMultipleReport report = new ConnectionToMultipleReport();
            report.setEndpointFrom(endpointFrom);
            report.setEndpointTo(endpointTo);
            this.report = report;
        }

        @Override
        public ConnectionToMultipleReport getReport()
        {
            return (ConnectionToMultipleReport) report;
        }
    }

    /**
     * No reservation is allocated for reused {@link #reservationRequest} which can be used in requested time slot.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ReservationRequestNotUsableReport")
    public static class ReservationRequestNotUsableReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest;

        public ReservationRequestNotUsableReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "reservation-request-not-usable";
        }

        public ReservationRequestNotUsableReport(cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest)
        {
            setReservationRequest(reservationRequest);
        }

        @javax.persistence.OneToOne(fetch = javax.persistence.FetchType.LAZY)
        @javax.persistence.Access(javax.persistence.AccessType.FIELD)
        @javax.persistence.JoinColumn(name = "reservationrequest_id")
        public cz.cesnet.shongo.controller.request.AbstractReservationRequest getReservationRequest()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(reservationRequest);
        }

        public void setReservationRequest(cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest)
        {
            this.reservationRequest = reservationRequest;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reservationRequest", reservationRequest);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reservation-request-not-usable", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ReservationRequestNotUsableReport}.
     */
    public static class ReservationRequestNotUsableException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ReservationRequestNotUsableException(ReservationRequestNotUsableReport report)
        {
            this.report = report;
        }

        public ReservationRequestNotUsableException(Throwable throwable, ReservationRequestNotUsableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationRequestNotUsableException(cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest)
        {
            ReservationRequestNotUsableReport report = new ReservationRequestNotUsableReport();
            report.setReservationRequest(reservationRequest);
            this.report = report;
        }

        public ReservationRequestNotUsableException(Throwable throwable, cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest)
        {
            super(throwable);
            ReservationRequestNotUsableReport report = new ReservationRequestNotUsableReport();
            report.setReservationRequest(reservationRequest);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.request.AbstractReservationRequest getReservationRequest()
        {
            return getReport().getReservationRequest();
        }

        @Override
        public ReservationRequestNotUsableReport getReport()
        {
            return (ReservationRequestNotUsableReport) report;
        }
    }

    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ReservationReport")
    public static abstract class ReservationReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.reservation.Reservation reservation;

        public ReservationReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "reservation";
        }

        public ReservationReport(cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            setReservation(reservation);
        }

        @javax.persistence.OneToOne(fetch = javax.persistence.FetchType.LAZY)
        @javax.persistence.Access(javax.persistence.AccessType.FIELD)
        @javax.persistence.JoinColumn(name = "reservation_id")
        public cz.cesnet.shongo.controller.reservation.Reservation getReservation()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(reservation);
        }

        public void setReservation(cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            this.reservation = reservation;
        }
    }

    /**
     * The {@link #reservation} from reused {@link #reusedReservationRequest} is not available because it is already allocated for another reservation request in requested time slot.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ReservationNotAvailableReport")
    public static class ReservationNotAvailableReport extends ReservationReport
    {
        protected cz.cesnet.shongo.controller.request.AbstractReservationRequest reusedReservationRequest;

        public ReservationNotAvailableReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "reservation-not-available";
        }

        public ReservationNotAvailableReport(cz.cesnet.shongo.controller.reservation.Reservation reservation, cz.cesnet.shongo.controller.request.AbstractReservationRequest reusedReservationRequest)
        {
            setReservation(reservation);
            setReusedReservationRequest(reusedReservationRequest);
        }

        @javax.persistence.OneToOne(fetch = javax.persistence.FetchType.LAZY)
        @javax.persistence.Access(javax.persistence.AccessType.FIELD)
        @javax.persistence.JoinColumn(name = "reusedreservationrequest_id")
        public cz.cesnet.shongo.controller.request.AbstractReservationRequest getReusedReservationRequest()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(reusedReservationRequest);
        }

        public void setReusedReservationRequest(cz.cesnet.shongo.controller.request.AbstractReservationRequest reusedReservationRequest)
        {
            this.reusedReservationRequest = reusedReservationRequest;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reservation", reservation);
            parameters.put("reusedReservationRequest", reusedReservationRequest);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reservation-not-available", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ReservationNotAvailableReport}.
     */
    public static class ReservationNotAvailableException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ReservationNotAvailableException(ReservationNotAvailableReport report)
        {
            this.report = report;
        }

        public ReservationNotAvailableException(Throwable throwable, ReservationNotAvailableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationNotAvailableException(cz.cesnet.shongo.controller.reservation.Reservation reservation, cz.cesnet.shongo.controller.request.AbstractReservationRequest reusedReservationRequest)
        {
            ReservationNotAvailableReport report = new ReservationNotAvailableReport();
            report.setReservation(reservation);
            report.setReusedReservationRequest(reusedReservationRequest);
            this.report = report;
        }

        public ReservationNotAvailableException(Throwable throwable, cz.cesnet.shongo.controller.reservation.Reservation reservation, cz.cesnet.shongo.controller.request.AbstractReservationRequest reusedReservationRequest)
        {
            super(throwable);
            ReservationNotAvailableReport report = new ReservationNotAvailableReport();
            report.setReservation(reservation);
            report.setReusedReservationRequest(reusedReservationRequest);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.request.AbstractReservationRequest getReusedReservationRequest()
        {
            return getReport().getReusedReservationRequest();
        }

        @Override
        public ReservationNotAvailableReport getReport()
        {
            return (ReservationNotAvailableReport) report;
        }
    }

    /**
     * Reusing {@link #reservation}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ReservationReusingReport")
    public static class ReservationReusingReport extends ReservationReport
    {
        public ReservationReusingReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "reservation-reusing";
        }

        public ReservationReusingReport(cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            setReservation(reservation);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reservation", reservation);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reservation-reusing", userType, language, getParameters());
        }
    }

    /**
     * Value {@link #value} is already allocated in interval {@link #interval}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ValueAlreadyAllocatedReport")
    public static class ValueAlreadyAllocatedReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected String value;

        protected org.joda.time.Interval interval;

        public ValueAlreadyAllocatedReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "value-already-allocated";
        }

        public ValueAlreadyAllocatedReport(String value, org.joda.time.Interval interval)
        {
            setValue(value);
            setInterval(interval);
        }

        @javax.persistence.Column
        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        @org.hibernate.annotations.Columns(columns={@javax.persistence.Column(name="interval_start"),@javax.persistence.Column(name="interval_end")})
        @org.hibernate.annotations.Type(type = "Interval")
        public org.joda.time.Interval getInterval()
        {
            return interval;
        }

        public void setInterval(org.joda.time.Interval interval)
        {
            this.interval = interval;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("value", value);
            parameters.put("interval", interval);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("value-already-allocated", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ValueAlreadyAllocatedReport}.
     */
    public static class ValueAlreadyAllocatedException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ValueAlreadyAllocatedException(ValueAlreadyAllocatedReport report)
        {
            this.report = report;
        }

        public ValueAlreadyAllocatedException(Throwable throwable, ValueAlreadyAllocatedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ValueAlreadyAllocatedException(String value, org.joda.time.Interval interval)
        {
            ValueAlreadyAllocatedReport report = new ValueAlreadyAllocatedReport();
            report.setValue(value);
            report.setInterval(interval);
            this.report = report;
        }

        public ValueAlreadyAllocatedException(Throwable throwable, String value, org.joda.time.Interval interval)
        {
            super(throwable);
            ValueAlreadyAllocatedReport report = new ValueAlreadyAllocatedReport();
            report.setValue(value);
            report.setInterval(interval);
            this.report = report;
        }

        public String getValue()
        {
            return getReport().getValue();
        }

        public org.joda.time.Interval getInterval()
        {
            return getReport().getInterval();
        }

        @Override
        public ValueAlreadyAllocatedReport getReport()
        {
            return (ValueAlreadyAllocatedReport) report;
        }
    }

    /**
     * Value {@link #value} is invalid.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ValueInvalidReport")
    public static class ValueInvalidReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected String value;

        public ValueInvalidReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "value-invalid";
        }

        public ValueInvalidReport(String value)
        {
            setValue(value);
        }

        @javax.persistence.Column
        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("value", value);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("value-invalid", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ValueInvalidReport}.
     */
    public static class ValueInvalidException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ValueInvalidException(ValueInvalidReport report)
        {
            this.report = report;
        }

        public ValueInvalidException(Throwable throwable, ValueInvalidReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ValueInvalidException(String value)
        {
            ValueInvalidReport report = new ValueInvalidReport();
            report.setValue(value);
            this.report = report;
        }

        public ValueInvalidException(Throwable throwable, String value)
        {
            super(throwable);
            ValueInvalidReport report = new ValueInvalidReport();
            report.setValue(value);
            this.report = report;
        }

        public String getValue()
        {
            return getReport().getValue();
        }

        @Override
        public ValueInvalidReport getReport()
        {
            return (ValueInvalidReport) report;
        }
    }

    /**
     * No value is available.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ValueNotAvailableReport")
    public static class ValueNotAvailableReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected org.joda.time.Interval interval;

        public ValueNotAvailableReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "value-not-available";
        }

        public ValueNotAvailableReport(org.joda.time.Interval interval)
        {
            setInterval(interval);
        }

        @org.hibernate.annotations.Columns(columns={@javax.persistence.Column(name="interval_start"),@javax.persistence.Column(name="interval_end")})
        @org.hibernate.annotations.Type(type = "Interval")
        public org.joda.time.Interval getInterval()
        {
            return interval;
        }

        public void setInterval(org.joda.time.Interval interval)
        {
            this.interval = interval;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("interval", interval);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("value-not-available", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link ValueNotAvailableReport}.
     */
    public static class ValueNotAvailableException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ValueNotAvailableException(ValueNotAvailableReport report)
        {
            this.report = report;
        }

        public ValueNotAvailableException(Throwable throwable, ValueNotAvailableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ValueNotAvailableException(org.joda.time.Interval interval)
        {
            ValueNotAvailableReport report = new ValueNotAvailableReport();
            report.setInterval(interval);
            this.report = report;
        }

        public ValueNotAvailableException(Throwable throwable, org.joda.time.Interval interval)
        {
            super(throwable);
            ValueNotAvailableReport report = new ValueNotAvailableReport();
            report.setInterval(interval);
            this.report = report;
        }

        public org.joda.time.Interval getInterval()
        {
            return getReport().getInterval();
        }

        @Override
        public ValueNotAvailableReport getReport()
        {
            return (ValueNotAvailableReport) report;
        }
    }

    /**
     * Allocating the resource {@link #resource}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("AllocatingResourceReport")
    public static class AllocatingResourceReport extends ResourceReport
    {
        public AllocatingResourceReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-resource";
        }

        public AllocatingResourceReport(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            setResource(resource);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-resource", userType, language, getParameters());
        }
    }

    /**
     * Allocating alias for the following specification: 
     *   Technology: {@link #technologies} 
     *   Alias Type: {@link #aliasTypes} 
     *        Value: {@link #value}
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("AllocatingAliasReport")
    public static class AllocatingAliasReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected java.util.Set<cz.cesnet.shongo.Technology> technologies;

        protected java.util.Set<cz.cesnet.shongo.AliasType> aliasTypes;

        protected String value;

        public AllocatingAliasReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-alias";
        }

        public AllocatingAliasReport(java.util.Set<cz.cesnet.shongo.Technology> technologies, java.util.Set<cz.cesnet.shongo.AliasType> aliasTypes, String value)
        {
            setTechnologies(technologies);
            setAliasTypes(aliasTypes);
            setValue(value);
        }

        @javax.persistence.ElementCollection
        public java.util.Set<cz.cesnet.shongo.Technology> getTechnologies()
        {
            return technologies;
        }

        public void setTechnologies(java.util.Set<cz.cesnet.shongo.Technology> technologies)
        {
            this.technologies = technologies;
        }

        @javax.persistence.ElementCollection
        public java.util.Set<cz.cesnet.shongo.AliasType> getAliasTypes()
        {
            return aliasTypes;
        }

        public void setAliasTypes(java.util.Set<cz.cesnet.shongo.AliasType> aliasTypes)
        {
            this.aliasTypes = aliasTypes;
        }

        @javax.persistence.Column
        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("technologies", technologies);
            parameters.put("aliasTypes", aliasTypes);
            parameters.put("value", value);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-alias", userType, language, getParameters());
        }
    }

    /**
     * Allocating value in the resource {@link #resource}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("AllocatingValueReport")
    public static class AllocatingValueReport extends ResourceReport
    {
        public AllocatingValueReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-value";
        }

        public AllocatingValueReport(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            setResource(resource);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-value", userType, language, getParameters());
        }
    }

    /**
     * Allocating room for the following specification: 
     *     Technology: {@link #technologies} 
     *   Participants: {@link #participantCount}
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("AllocatingRoomReport")
    public static class AllocatingRoomReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected java.util.List<cz.cesnet.shongo.controller.scheduler.TechnologySet> technologies;

        protected Integer participantCount;

        public AllocatingRoomReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-room";
        }

        public AllocatingRoomReport(java.util.List<cz.cesnet.shongo.controller.scheduler.TechnologySet> technologies, Integer participantCount)
        {
            setTechnologies(technologies);
            setParticipantCount(participantCount);
        }

        @javax.persistence.OneToMany(cascade = javax.persistence.CascadeType.ALL, orphanRemoval = true)
        public java.util.List<cz.cesnet.shongo.controller.scheduler.TechnologySet> getTechnologies()
        {
            return technologies;
        }

        public void setTechnologies(java.util.List<cz.cesnet.shongo.controller.scheduler.TechnologySet> technologies)
        {
            this.technologies = technologies;
        }

        @javax.persistence.Column
        public Integer getParticipantCount()
        {
            return participantCount;
        }

        public void setParticipantCount(Integer participantCount)
        {
            this.participantCount = participantCount;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("technologies", technologies);
            parameters.put("participantCount", participantCount);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-room", userType, language, getParameters());
        }
    }

    /**
     * Allocating compartment.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("AllocatingCompartmentReport")
    public static class AllocatingCompartmentReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public AllocatingCompartmentReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-compartment";
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-compartment", userType, language, getParameters());
        }
    }

    /**
     * Allocating executable.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("AllocatingExecutableReport")
    public static class AllocatingExecutableReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public AllocatingExecutableReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-executable";
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-executable", userType, language, getParameters());
        }
    }

    /**
     * Checking specification availability report.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("SpecificationCheckingAvailabilityReport")
    public static class SpecificationCheckingAvailabilityReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public SpecificationCheckingAvailabilityReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "specification-checking-availability";
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("specification-checking-availability", userType, language, getParameters());
        }
    }

    /**
     * Finding available resource.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("FindingAvailableResourceReport")
    public static class FindingAvailableResourceReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public FindingAvailableResourceReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "finding-available-resource";
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("finding-available-resource", userType, language, getParameters());
        }
    }

    /**
     * Sorting resources.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("SortingResourcesReport")
    public static class SortingResourcesReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public SortingResourcesReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "sorting-resources";
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("sorting-resources", userType, language, getParameters());
        }
    }

    /**
     * Specification {@link #specification} is not ready.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("SpecificationNotReadyReport")
    public static class SpecificationNotReadyReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.request.Specification specification;

        public SpecificationNotReadyReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "specification-not-ready";
        }

        public SpecificationNotReadyReport(cz.cesnet.shongo.controller.request.Specification specification)
        {
            setSpecification(specification);
        }

        @javax.persistence.OneToOne(fetch = javax.persistence.FetchType.LAZY)
        @javax.persistence.Access(javax.persistence.AccessType.FIELD)
        @javax.persistence.JoinColumn(name = "specification_id")
        public cz.cesnet.shongo.controller.request.Specification getSpecification()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(specification);
        }

        public void setSpecification(cz.cesnet.shongo.controller.request.Specification specification)
        {
            this.specification = specification;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("specification", specification);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("specification-not-ready", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link SpecificationNotReadyReport}.
     */
    public static class SpecificationNotReadyException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public SpecificationNotReadyException(SpecificationNotReadyReport report)
        {
            this.report = report;
        }

        public SpecificationNotReadyException(Throwable throwable, SpecificationNotReadyReport report)
        {
            super(throwable);
            this.report = report;
        }

        public SpecificationNotReadyException(cz.cesnet.shongo.controller.request.Specification specification)
        {
            SpecificationNotReadyReport report = new SpecificationNotReadyReport();
            report.setSpecification(specification);
            this.report = report;
        }

        public SpecificationNotReadyException(Throwable throwable, cz.cesnet.shongo.controller.request.Specification specification)
        {
            super(throwable);
            SpecificationNotReadyReport report = new SpecificationNotReadyReport();
            report.setSpecification(specification);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.request.Specification getSpecification()
        {
            return getReport().getSpecification();
        }

        @Override
        public SpecificationNotReadyReport getReport()
        {
            return (SpecificationNotReadyReport) report;
        }
    }

    /**
     * Duration {@link #duration} is longer than maximum {@link #maximumDuration}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("DurationLongerThanMaximumReport")
    public static class DurationLongerThanMaximumReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected org.joda.time.Period duration;

        protected org.joda.time.Period maximumDuration;

        public DurationLongerThanMaximumReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "duration-longer-than-maximum";
        }

        public DurationLongerThanMaximumReport(org.joda.time.Period duration, org.joda.time.Period maximumDuration)
        {
            setDuration(duration);
            setMaximumDuration(maximumDuration);
        }

        @javax.persistence.Column
        @org.hibernate.annotations.Type(type = "Period")
        public org.joda.time.Period getDuration()
        {
            return duration;
        }

        public void setDuration(org.joda.time.Period duration)
        {
            this.duration = duration;
        }

        @javax.persistence.Column
        @org.hibernate.annotations.Type(type = "Period")
        public org.joda.time.Period getMaximumDuration()
        {
            return maximumDuration;
        }

        public void setMaximumDuration(org.joda.time.Period maximumDuration)
        {
            this.maximumDuration = maximumDuration;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("duration", duration);
            parameters.put("maximumDuration", maximumDuration);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("duration-longer-than-maximum", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link DurationLongerThanMaximumReport}.
     */
    public static class DurationLongerThanMaximumException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public DurationLongerThanMaximumException(DurationLongerThanMaximumReport report)
        {
            this.report = report;
        }

        public DurationLongerThanMaximumException(Throwable throwable, DurationLongerThanMaximumReport report)
        {
            super(throwable);
            this.report = report;
        }

        public DurationLongerThanMaximumException(org.joda.time.Period duration, org.joda.time.Period maximumDuration)
        {
            DurationLongerThanMaximumReport report = new DurationLongerThanMaximumReport();
            report.setDuration(duration);
            report.setMaximumDuration(maximumDuration);
            this.report = report;
        }

        public DurationLongerThanMaximumException(Throwable throwable, org.joda.time.Period duration, org.joda.time.Period maximumDuration)
        {
            super(throwable);
            DurationLongerThanMaximumReport report = new DurationLongerThanMaximumReport();
            report.setDuration(duration);
            report.setMaximumDuration(maximumDuration);
            this.report = report;
        }

        public org.joda.time.Period getDuration()
        {
            return getReport().getDuration();
        }

        public org.joda.time.Period getMaximumDuration()
        {
            return getReport().getMaximumDuration();
        }

        @Override
        public DurationLongerThanMaximumReport getReport()
        {
            return (DurationLongerThanMaximumReport) report;
        }
    }

    /**
     * The specification {@link #specification} is not supposed to be allocated.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("SpecificationNotAllocatableReport")
    public static class SpecificationNotAllocatableReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.request.Specification specification;

        public SpecificationNotAllocatableReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "specification-not-allocatable";
        }

        public SpecificationNotAllocatableReport(cz.cesnet.shongo.controller.request.Specification specification)
        {
            setSpecification(specification);
        }

        @javax.persistence.OneToOne(fetch = javax.persistence.FetchType.LAZY)
        @javax.persistence.Access(javax.persistence.AccessType.FIELD)
        @javax.persistence.JoinColumn(name = "specification_id")
        public cz.cesnet.shongo.controller.request.Specification getSpecification()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(specification);
        }

        public void setSpecification(cz.cesnet.shongo.controller.request.Specification specification)
        {
            this.specification = specification;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("specification", specification);
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("specification-not-allocatable", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link SpecificationNotAllocatableReport}.
     */
    public static class SpecificationNotAllocatableException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public SpecificationNotAllocatableException(SpecificationNotAllocatableReport report)
        {
            this.report = report;
        }

        public SpecificationNotAllocatableException(Throwable throwable, SpecificationNotAllocatableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public SpecificationNotAllocatableException(cz.cesnet.shongo.controller.request.Specification specification)
        {
            SpecificationNotAllocatableReport report = new SpecificationNotAllocatableReport();
            report.setSpecification(specification);
            this.report = report;
        }

        public SpecificationNotAllocatableException(Throwable throwable, cz.cesnet.shongo.controller.request.Specification specification)
        {
            super(throwable);
            SpecificationNotAllocatableReport report = new SpecificationNotAllocatableReport();
            report.setSpecification(specification);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.request.Specification getSpecification()
        {
            return getReport().getSpecification();
        }

        @Override
        public SpecificationNotAllocatableReport getReport()
        {
            return (SpecificationNotAllocatableReport) report;
        }
    }

    /**
     * User is not resource owner.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("UserNotOwnerReport")
    public static class UserNotOwnerReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public UserNotOwnerReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public String getUniqueId()
        {
            return "user-not-owner";
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @javax.persistence.Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(UserType userType, Language language)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("user-not-owner", userType, language, getParameters());
        }
    }

    /**
     * Exception for {@link UserNotOwnerReport}.
     */
    public static class UserNotOwnerException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public UserNotOwnerException(UserNotOwnerReport report)
        {
            this.report = report;
        }

        public UserNotOwnerException(Throwable throwable, UserNotOwnerReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UserNotOwnerException()
        {
            UserNotOwnerReport report = new UserNotOwnerReport();
            this.report = report;
        }

        public UserNotOwnerException(Throwable throwable)
        {
            super(throwable);
            UserNotOwnerReport report = new UserNotOwnerReport();
            this.report = report;
        }

        @Override
        public UserNotOwnerReport getReport()
        {
            return (UserNotOwnerReport) report;
        }
    }

    @Override
    protected void fillReportClasses()
    {
        addReportClass(ResourceReport.class);
        addReportClass(ResourceNotAllocatableReport.class);
        addReportClass(ResourceAlreadyAllocatedReport.class);
        addReportClass(ResourceNotAvailableReport.class);
        addReportClass(ResourceNotEndpointReport.class);
        addReportClass(ResourceMultipleRequestedReport.class);
        addReportClass(ResourceNotFoundReport.class);
        addReportClass(ExecutableReusingReport.class);
        addReportClass(CompartmentNotEnoughEndpointReport.class);
        addReportClass(CompartmentAssignAliasToExternalEndpointReport.class);
        addReportClass(ConnectionReport.class);
        addReportClass(ConnectionBetweenReport.class);
        addReportClass(ConnectionFromToReport.class);
        addReportClass(ConnectionToMultipleReport.class);
        addReportClass(ReservationRequestNotUsableReport.class);
        addReportClass(ReservationReport.class);
        addReportClass(ReservationNotAvailableReport.class);
        addReportClass(ReservationReusingReport.class);
        addReportClass(ValueAlreadyAllocatedReport.class);
        addReportClass(ValueInvalidReport.class);
        addReportClass(ValueNotAvailableReport.class);
        addReportClass(AllocatingResourceReport.class);
        addReportClass(AllocatingAliasReport.class);
        addReportClass(AllocatingValueReport.class);
        addReportClass(AllocatingRoomReport.class);
        addReportClass(AllocatingCompartmentReport.class);
        addReportClass(AllocatingExecutableReport.class);
        addReportClass(SpecificationCheckingAvailabilityReport.class);
        addReportClass(FindingAvailableResourceReport.class);
        addReportClass(SortingResourcesReport.class);
        addReportClass(SpecificationNotReadyReport.class);
        addReportClass(DurationLongerThanMaximumReport.class);
        addReportClass(SpecificationNotAllocatableReport.class);
        addReportClass(UserNotOwnerReport.class);
    }
}
