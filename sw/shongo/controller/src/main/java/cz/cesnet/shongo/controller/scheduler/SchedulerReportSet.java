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

        public ResourceReport(cz.cesnet.shongo.controller.resource.Resource resource)
        {
            setResource(resource);
        }

        @javax.persistence.OneToOne
        @javax.persistence.JoinColumn(name = "resource_id")
        public cz.cesnet.shongo.controller.resource.Resource getResource()
        {
            return resource;
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
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Resource ");
                    message.append((resource == null ? "null" : resource.getReportDescription(messageType)));
                    message.append(".");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Resource {@link #resource} is not allocatable.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceNotAllocatableReport")
    public static class ResourceNotAllocatableReport extends ResourceReport
    {
        public ResourceNotAllocatableReport()
        {
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Resource ");
                    message.append((resource == null ? "null" : resource.getReportDescription(messageType)));
                    message.append(" is not allocatable.");
                    break;
            }
            return message.toString();
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
     * Resource {@link #resource} is already allocated.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceAlreadyAllocatedReport")
    public static class ResourceAlreadyAllocatedReport extends ResourceReport
    {
        public ResourceAlreadyAllocatedReport()
        {
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Resource ");
                    message.append((resource == null ? "null" : resource.getReportDescription(messageType)));
                    message.append(" is already allocated.");
                    break;
            }
            return message.toString();
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
     * Resource {@link #resource} is not available for the requested time slot. The maximum date/time for which the resource can be allocated is {@link #maxDateTime}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceNotAvailableReport")
    public static class ResourceNotAvailableReport extends ResourceReport
    {
        protected org.joda.time.DateTime maxDateTime;

        public ResourceNotAvailableReport()
        {
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Resource ");
                    message.append((resource == null ? "null" : resource.getReportDescription(messageType)));
                    message.append(" is not available for the requested time slot. The maximum date/time for which the resource can be allocated is ");
                    message.append((maxDateTime == null ? "null" : maxDateTime.toString()));
                    message.append(".");
                    break;
            }
            return message.toString();
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
     * Resource {@link #resource} is not endpoint.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceNotEndpointReport")
    public static class ResourceNotEndpointReport extends ResourceReport
    {
        public ResourceNotEndpointReport()
        {
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Resource ");
                    message.append((resource == null ? "null" : resource.getReportDescription(messageType)));
                    message.append(" is not endpoint.");
                    break;
            }
            return message.toString();
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
     * Resource {@link #resource} is requested multiple times.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ResourceMultipleRequestedReport")
    public static class ResourceMultipleRequestedReport extends ResourceReport
    {
        public ResourceMultipleRequestedReport()
        {
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Resource ");
                    message.append((resource == null ? "null" : resource.getReportDescription(messageType)));
                    message.append(" is requested multiple times.");
                    break;
            }
            return message.toString();
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("No available resource was found for the following specification: Technologies: ");
                    message.append((technologies == null ? "null" : technologies.toString()));
                    break;
            }
            return message.toString();
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

        public ExecutableReusingReport(cz.cesnet.shongo.controller.executor.Executable executable)
        {
            setExecutable(executable);
        }

        @javax.persistence.OneToOne
        @javax.persistence.JoinColumn(name = "executable_id")
        public cz.cesnet.shongo.controller.executor.Executable getExecutable()
        {
            return executable;
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
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Reusing existing ");
                    message.append((executable == null ? "null" : executable.getReportDescription(messageType)));
                    message.append(".");
                    break;
            }
            return message.toString();
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
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Not enough endpoints are requested for the compartment.");
                    break;
            }
            return message.toString();
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
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Cannot assign alias to allocated external endpoint.");
                    break;
            }
            return message.toString();
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

        public ConnectionReport(cz.cesnet.shongo.controller.executor.Endpoint endpointFrom, cz.cesnet.shongo.controller.executor.Endpoint endpointTo)
        {
            setEndpointFrom(endpointFrom);
            setEndpointTo(endpointTo);
        }

        @javax.persistence.OneToOne(cascade = javax.persistence.CascadeType.PERSIST)
        @javax.persistence.JoinColumn(name = "endpointfrom_id")
        public cz.cesnet.shongo.controller.executor.Endpoint getEndpointFrom()
        {
            return endpointFrom;
        }

        public void setEndpointFrom(cz.cesnet.shongo.controller.executor.Endpoint endpointFrom)
        {
            this.endpointFrom = endpointFrom;
        }

        @javax.persistence.OneToOne(cascade = javax.persistence.CascadeType.PERSIST)
        @javax.persistence.JoinColumn(name = "endpointto_id")
        public cz.cesnet.shongo.controller.executor.Endpoint getEndpointTo()
        {
            return endpointTo;
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
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Creating connection between ");
                    message.append((endpointFrom == null ? "null" : endpointFrom.getReportDescription(messageType)));
                    message.append(" and ");
                    message.append((endpointTo == null ? "null" : endpointTo.getReportDescription(messageType)));
                    message.append(" in technology ");
                    message.append((technology == null ? "null" : technology.toString()));
                    message.append(".");
                    break;
            }
            return message.toString();
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
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Creating connection from ");
                    message.append((endpointFrom == null ? "null" : endpointFrom.getReportDescription(messageType)));
                    message.append(" to ");
                    message.append((endpointTo == null ? "null" : endpointTo.getReportDescription(messageType)));
                    message.append(".");
                    break;
            }
            return message.toString();
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
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Cannot create connection from ");
                    message.append((endpointFrom == null ? "null" : endpointFrom.getReportDescription(messageType)));
                    message.append(" to ");
                    message.append((endpointTo == null ? "null" : endpointTo.getReportDescription(messageType)));
                    message.append(", because the target represents multiple endpoints (not supported yet).");
                    break;
            }
            return message.toString();
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

    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ReservationReport")
    public static abstract class ReservationReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.reservation.Reservation reservation;

        public ReservationReport()
        {
        }

        public ReservationReport(cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            setReservation(reservation);
        }

        @javax.persistence.OneToOne
        @javax.persistence.JoinColumn(name = "reservation_id")
        public cz.cesnet.shongo.controller.reservation.Reservation getReservation()
        {
            return reservation;
        }

        public void setReservation(cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            this.reservation = reservation;
        }
    }

    /**
     * Provided reservation {@link #reservation} is not available.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ReservationNotAvailableReport")
    public static class ReservationNotAvailableReport extends ReservationReport
    {
        public ReservationNotAvailableReport()
        {
        }

        public ReservationNotAvailableReport(cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            setReservation(reservation);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Provided reservation ");
                    message.append((reservation == null ? "null" : reservation.getReportDescription(messageType)));
                    message.append(" is not available.");
                    break;
            }
            return message.toString();
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

        public ReservationNotAvailableException(cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            ReservationNotAvailableReport report = new ReservationNotAvailableReport();
            report.setReservation(reservation);
            this.report = report;
        }

        public ReservationNotAvailableException(Throwable throwable, cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            super(throwable);
            ReservationNotAvailableReport report = new ReservationNotAvailableReport();
            report.setReservation(reservation);
            this.report = report;
        }

        @Override
        public ReservationNotAvailableReport getReport()
        {
            return (ReservationNotAvailableReport) report;
        }
    }

    /**
     * Provided reservation {@link #reservation} is not usable, because provided date/time slot doesn't contain the requested.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ReservationNotUsableReport")
    public static class ReservationNotUsableReport extends ReservationReport
    {
        public ReservationNotUsableReport()
        {
        }

        public ReservationNotUsableReport(cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            setReservation(reservation);
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Provided reservation ");
                    message.append((reservation == null ? "null" : reservation.getReportDescription(messageType)));
                    message.append(" is not usable, because provided date/time slot doesn't contain the requested.");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Exception for {@link ReservationNotUsableReport}.
     */
    public static class ReservationNotUsableException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ReservationNotUsableException(ReservationNotUsableReport report)
        {
            this.report = report;
        }

        public ReservationNotUsableException(Throwable throwable, ReservationNotUsableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationNotUsableException(cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            ReservationNotUsableReport report = new ReservationNotUsableReport();
            report.setReservation(reservation);
            this.report = report;
        }

        public ReservationNotUsableException(Throwable throwable, cz.cesnet.shongo.controller.reservation.Reservation reservation)
        {
            super(throwable);
            ReservationNotUsableReport report = new ReservationNotUsableReport();
            report.setReservation(reservation);
            this.report = report;
        }

        @Override
        public ReservationNotUsableReport getReport()
        {
            return (ReservationNotUsableReport) report;
        }
    }

    /**
     * Reusing reservation {@link #reservation}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ReservationReusingReport")
    public static class ReservationReusingReport extends ReservationReport
    {
        public ReservationReusingReport()
        {
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
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Reusing reservation ");
                    message.append((reservation == null ? "null" : reservation.getReportDescription(messageType)));
                    message.append(".");
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Value {@link #value} is already allocated.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("ValueAlreadyAllocatedReport")
    public static class ValueAlreadyAllocatedReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected String value;

        public ValueAlreadyAllocatedReport()
        {
        }

        public ValueAlreadyAllocatedReport(String value)
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Value ");
                    message.append((value == null ? "null" : value));
                    message.append(" is already allocated.");
                    break;
            }
            return message.toString();
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

        public ValueAlreadyAllocatedException(String value)
        {
            ValueAlreadyAllocatedReport report = new ValueAlreadyAllocatedReport();
            report.setValue(value);
            this.report = report;
        }

        public ValueAlreadyAllocatedException(Throwable throwable, String value)
        {
            super(throwable);
            ValueAlreadyAllocatedReport report = new ValueAlreadyAllocatedReport();
            report.setValue(value);
            this.report = report;
        }

        public String getValue()
        {
            return getReport().getValue();
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Value ");
                    message.append((value == null ? "null" : value));
                    message.append(" is invalid.");
                    break;
            }
            return message.toString();
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
        public ValueNotAvailableReport()
        {
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("No value is available.");
                    break;
            }
            return message.toString();
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

        public ValueNotAvailableException()
        {
            ValueNotAvailableReport report = new ValueNotAvailableReport();
            this.report = report;
        }

        public ValueNotAvailableException(Throwable throwable)
        {
            super(throwable);
            ValueNotAvailableReport report = new ValueNotAvailableReport();
            this.report = report;
        }

        @Override
        public ValueNotAvailableReport getReport()
        {
            return (ValueNotAvailableReport) report;
        }
    }

    /**
     * Allocating resource {@link #resource}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("AllocatingResourceReport")
    public static class AllocatingResourceReport extends ResourceReport
    {
        public AllocatingResourceReport()
        {
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
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Allocating resource ");
                    message.append((resource == null ? "null" : resource.getReportDescription(messageType)));
                    message.append(".");
                    break;
            }
            return message.toString();
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Allocating alias for the following specification: \n  Technology: ");
                    message.append(((technologies == null || technologies.isEmpty()) ? "Any" : technologies.toString()));
                    message.append(" \n  Alias Type: ");
                    message.append(((aliasTypes == null || aliasTypes.isEmpty()) ? "Any" : aliasTypes.toString()));
                    message.append(" \n       Value: ");
                    message.append((value == null ? "Any" : value));
                    break;
            }
            return message.toString();
        }
    }

    /**
     * Allocating value in resource {@link #resource}.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("AllocatingValueReport")
    public static class AllocatingValueReport extends ResourceReport
    {
        public AllocatingValueReport()
        {
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
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Allocating value in resource ");
                    message.append((resource == null ? "null" : resource.getReportDescription(messageType)));
                    message.append(".");
                    break;
            }
            return message.toString();
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Allocating room for the following specification: \n    Technology: ");
                    message.append((technologies == null ? "null" : technologies.toString()));
                    message.append(" \n  Participants: ");
                    message.append((participantCount == null ? "null" : participantCount.toString()));
                    break;
            }
            return message.toString();
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
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Allocating compartment.");
                    break;
            }
            return message.toString();
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
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Allocating executable.");
                    break;
            }
            return message.toString();
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
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Checking specification availability report.");
                    break;
            }
            return message.toString();
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
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Finding available resource.");
                    break;
            }
            return message.toString();
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
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Sorting resources.");
                    break;
            }
            return message.toString();
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

        public SpecificationNotReadyReport(cz.cesnet.shongo.controller.request.Specification specification)
        {
            setSpecification(specification);
        }

        @javax.persistence.OneToOne
        @javax.persistence.JoinColumn(name = "specification_id")
        public cz.cesnet.shongo.controller.request.Specification getSpecification()
        {
            return specification;
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
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Specification ");
                    message.append((specification == null ? "null" : specification.getReportDescription(messageType)));
                    message.append(" is not ready.");
                    break;
            }
            return message.toString();
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
        protected int getVisibleFlags()
        {
            return VISIBLE_TO_USER;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("Duration ");
                    message.append((duration == null ? "null" : duration.toString()));
                    message.append(" is longer than maximum ");
                    message.append((maximumDuration == null ? "null" : maximumDuration.toString()));
                    message.append(".");
                    break;
            }
            return message.toString();
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
     * The specification of class {@link #specification} is not supposed to be allocated.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("SpecificationNotAllocatableReport")
    public static class SpecificationNotAllocatableReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.request.Specification specification;

        public SpecificationNotAllocatableReport()
        {
        }

        public SpecificationNotAllocatableReport(cz.cesnet.shongo.controller.request.Specification specification)
        {
            setSpecification(specification);
        }

        @javax.persistence.OneToOne
        @javax.persistence.JoinColumn(name = "specification_id")
        public cz.cesnet.shongo.controller.request.Specification getSpecification()
        {
            return specification;
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
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("The specification ");
                    message.append((specification == null ? "null" : specification.getClass().getSimpleName()));
                    message.append(" is not supposed to be allocated.");
                    break;
            }
            return message.toString();
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
     * User {@link #userId} is not resource owner.
     */
    @javax.persistence.Entity
    @javax.persistence.DiscriminatorValue("UserNotOwnerReport")
    public static class UserNotOwnerReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected String userId;

        public UserNotOwnerReport()
        {
        }

        public UserNotOwnerReport(String userId)
        {
            setUserId(userId);
        }

        @javax.persistence.Column
        public String getUserId()
        {
            return userId;
        }

        public void setUserId(String userId)
        {
            this.userId = userId;
        }

        @javax.persistence.Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @javax.persistence.Transient
        @Override
        public String getMessage(MessageType messageType)
        {
            StringBuilder message = new StringBuilder();
            switch (messageType) {
                default:
                    message.append("User ");
                    message.append((userId == null ? "null" : cz.cesnet.shongo.PersonInformation.Formatter.format(cz.cesnet.shongo.controller.authorization.Authorization.getInstance().getUserInformation(userId))));
                    message.append(" is not resource owner.");
                    break;
            }
            return message.toString();
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

        public UserNotOwnerException(String userId)
        {
            UserNotOwnerReport report = new UserNotOwnerReport();
            report.setUserId(userId);
            this.report = report;
        }

        public UserNotOwnerException(Throwable throwable, String userId)
        {
            super(throwable);
            UserNotOwnerReport report = new UserNotOwnerReport();
            report.setUserId(userId);
            this.report = report;
        }

        public String getUserId()
        {
            return getReport().getUserId();
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
        addReportClass(ReservationReport.class);
        addReportClass(ReservationNotAvailableReport.class);
        addReportClass(ReservationNotUsableReport.class);
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
