package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.report.*;
import jakarta.persistence.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class SchedulerReportSet extends AbstractReportSet
{
    /**
     * User does not have permissions for the resource {@link #resource}.
     */
    @Entity
    @DiscriminatorValue("UserNotAllowedReport")
    public static class UserNotAllowedReport extends ResourceReport
    {
        public UserNotAllowedReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "user-not-allowed";
        }

        public UserNotAllowedReport(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            setResource(resource);
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("user-not-allowed", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link UserNotAllowedReport}.
     */
    public static class UserNotAllowedException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public UserNotAllowedException(UserNotAllowedReport report)
        {
            this.report = report;
        }

        public UserNotAllowedException(Throwable throwable, UserNotAllowedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UserNotAllowedException(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            UserNotAllowedReport report = new UserNotAllowedReport();
            report.setResource(resource);
            this.report = report;
        }

        public UserNotAllowedException(Throwable throwable, cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            super(throwable);
            UserNotAllowedReport report = new UserNotAllowedReport();
            report.setResource(resource);
            this.report = report;
        }

        @Override
        public UserNotAllowedReport getReport()
        {
            return (UserNotAllowedReport) report;
        }
    }

    /**
     * No resource was found.
     */
    @Entity
    @DiscriminatorValue("ResourceNotFoundReport")
    public static class ResourceNotFoundReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public ResourceNotFoundReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource-not-found";
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-not-found", userType, language, timeZone, getParameters());
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

        public ResourceNotFoundException()
        {
            ResourceNotFoundReport report = new ResourceNotFoundReport();
            this.report = report;
        }

        public ResourceNotFoundException(Throwable throwable)
        {
            super(throwable);
            ResourceNotFoundReport report = new ResourceNotFoundReport();
            this.report = report;
        }

        @Override
        public ResourceNotFoundReport getReport()
        {
            return (ResourceNotFoundReport) report;
        }
    }

    /**
     * Resource {@link #resource}.
     */
    @Entity
    @DiscriminatorValue("ResourceReport")
    public static class ResourceReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.resource.Resource resource;

        public ResourceReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource";
        }

        public ResourceReport(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            setResource(resource);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "resource_id")
        public cz.cesnet.shongo.controller.booking.resource.Resource getResource()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(resource);
        }

        public void setResource(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            this.resource = resource;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource", userType, language, timeZone, getParameters());
        }
    }

    /**
     * The resource {@link #resource} is disabled for allocation.
     */
    @Entity
    @DiscriminatorValue("ResourceNotAllocatableReport")
    public static class ResourceNotAllocatableReport extends ResourceReport
    {
        public ResourceNotAllocatableReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource-not-allocatable";
        }

        public ResourceNotAllocatableReport(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            setResource(resource);
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-not-allocatable", userType, language, timeZone, getParameters());
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

        public ResourceNotAllocatableException(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            ResourceNotAllocatableReport report = new ResourceNotAllocatableReport();
            report.setResource(resource);
            this.report = report;
        }

        public ResourceNotAllocatableException(Throwable throwable, cz.cesnet.shongo.controller.booking.resource.Resource resource)
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
     * The resource {@link #resource} is already allocated in the time slot {@link #interval}.
     */
    @Entity
    @DiscriminatorValue("ResourceAlreadyAllocatedReport")
    public static class ResourceAlreadyAllocatedReport extends ResourceReport
    {
        protected org.joda.time.Interval interval;

        public ResourceAlreadyAllocatedReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource-already-allocated";
        }

        public ResourceAlreadyAllocatedReport(cz.cesnet.shongo.controller.booking.resource.Resource resource, org.joda.time.Interval interval)
        {
            setResource(resource);
            setInterval(interval);
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="interval_start"),@jakarta.persistence.Column(name="interval_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getInterval()
        {
            return interval;
        }

        public void setInterval(org.joda.time.Interval interval)
        {
            this.interval = interval;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            parameters.put("interval", interval);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-already-allocated", userType, language, timeZone, getParameters());
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

        public ResourceAlreadyAllocatedException(cz.cesnet.shongo.controller.booking.resource.Resource resource, org.joda.time.Interval interval)
        {
            ResourceAlreadyAllocatedReport report = new ResourceAlreadyAllocatedReport();
            report.setResource(resource);
            report.setInterval(interval);
            this.report = report;
        }

        public ResourceAlreadyAllocatedException(Throwable throwable, cz.cesnet.shongo.controller.booking.resource.Resource resource, org.joda.time.Interval interval)
        {
            super(throwable);
            ResourceAlreadyAllocatedReport report = new ResourceAlreadyAllocatedReport();
            report.setResource(resource);
            report.setInterval(interval);
            this.report = report;
        }

        public org.joda.time.Interval getInterval()
        {
            return getReport().getInterval();
        }

        @Override
        public ResourceAlreadyAllocatedReport getReport()
        {
            return (ResourceAlreadyAllocatedReport) report;
        }
    }

    /**
     * There is no available capacity due to maintenance in the time slot {@link #interval}.
     */
    @Entity
    @DiscriminatorValue("ResourceUnderMaintenanceReport")
    public static class ResourceUnderMaintenanceReport extends ResourceReport
    {
        protected org.joda.time.Interval interval;

        public ResourceUnderMaintenanceReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource-under-maintenance";
        }

        public ResourceUnderMaintenanceReport(cz.cesnet.shongo.controller.booking.resource.Resource resource, org.joda.time.Interval interval)
        {
            setResource(resource);
            setInterval(interval);
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="interval_start"),@jakarta.persistence.Column(name="interval_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getInterval()
        {
            return interval;
        }

        public void setInterval(org.joda.time.Interval interval)
        {
            this.interval = interval;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            parameters.put("interval", interval);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-under-maintenance", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ResourceUnderMaintenanceReport}.
     */
    public static class ResourceUnderMaintenanceException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ResourceUnderMaintenanceException(ResourceUnderMaintenanceReport report)
        {
            this.report = report;
        }

        public ResourceUnderMaintenanceException(Throwable throwable, ResourceUnderMaintenanceReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ResourceUnderMaintenanceException(cz.cesnet.shongo.controller.booking.resource.Resource resource, org.joda.time.Interval interval)
        {
            ResourceUnderMaintenanceReport report = new ResourceUnderMaintenanceReport();
            report.setResource(resource);
            report.setInterval(interval);
            this.report = report;
        }

        public ResourceUnderMaintenanceException(Throwable throwable, cz.cesnet.shongo.controller.booking.resource.Resource resource, org.joda.time.Interval interval)
        {
            super(throwable);
            ResourceUnderMaintenanceReport report = new ResourceUnderMaintenanceReport();
            report.setResource(resource);
            report.setInterval(interval);
            this.report = report;
        }

        public org.joda.time.Interval getInterval()
        {
            return getReport().getInterval();
        }

        @Override
        public ResourceUnderMaintenanceReport getReport()
        {
            return (ResourceUnderMaintenanceReport) report;
        }
    }

    /**
     * The resource {@link #resource} is not available for the requested time slot. The maximum date/time for which the resource can be allocated is {@link #maxDateTime}.
     */
    @Entity
    @DiscriminatorValue("ResourceNotAvailableReport")
    public static class ResourceNotAvailableReport extends ResourceReport
    {
        protected org.joda.time.DateTime maxDateTime;

        public ResourceNotAvailableReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource-not-available";
        }

        public ResourceNotAvailableReport(cz.cesnet.shongo.controller.booking.resource.Resource resource, org.joda.time.DateTime maxDateTime)
        {
            setResource(resource);
            setMaxDateTime(maxDateTime);
        }

        @Column
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentDateTime.class)
        public org.joda.time.DateTime getMaxDateTime()
        {
            return maxDateTime;
        }

        public void setMaxDateTime(org.joda.time.DateTime maxDateTime)
        {
            this.maxDateTime = maxDateTime;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            parameters.put("maxDateTime", maxDateTime);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-not-available", userType, language, timeZone, getParameters());
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

        public ResourceNotAvailableException(cz.cesnet.shongo.controller.booking.resource.Resource resource, org.joda.time.DateTime maxDateTime)
        {
            ResourceNotAvailableReport report = new ResourceNotAvailableReport();
            report.setResource(resource);
            report.setMaxDateTime(maxDateTime);
            this.report = report;
        }

        public ResourceNotAvailableException(Throwable throwable, cz.cesnet.shongo.controller.booking.resource.Resource resource, org.joda.time.DateTime maxDateTime)
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
     * The resource {@link #resource} has available only {@link #availableLicenseCount} from {@link #maxLicenseCount} licenses.
     */
    @Entity
    @DiscriminatorValue("ResourceRoomCapacityExceededReport")
    public static class ResourceRoomCapacityExceededReport extends ResourceReport
    {
        protected Integer availableLicenseCount;

        protected Integer maxLicenseCount;

        public ResourceRoomCapacityExceededReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource-room-capacity-exceeded";
        }

        public ResourceRoomCapacityExceededReport(cz.cesnet.shongo.controller.booking.resource.Resource resource, Integer availableLicenseCount, Integer maxLicenseCount)
        {
            setResource(resource);
            setAvailableLicenseCount(availableLicenseCount);
            setMaxLicenseCount(maxLicenseCount);
        }

        @Column
        public Integer getAvailableLicenseCount()
        {
            return availableLicenseCount;
        }

        public void setAvailableLicenseCount(Integer availableLicenseCount)
        {
            this.availableLicenseCount = availableLicenseCount;
        }

        @Column
        public Integer getMaxLicenseCount()
        {
            return maxLicenseCount;
        }

        public void setMaxLicenseCount(Integer maxLicenseCount)
        {
            this.maxLicenseCount = maxLicenseCount;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            parameters.put("availableLicenseCount", availableLicenseCount);
            parameters.put("maxLicenseCount", maxLicenseCount);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-room-capacity-exceeded", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ResourceRoomCapacityExceededReport}.
     */
    public static class ResourceRoomCapacityExceededException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ResourceRoomCapacityExceededException(ResourceRoomCapacityExceededReport report)
        {
            this.report = report;
        }

        public ResourceRoomCapacityExceededException(Throwable throwable, ResourceRoomCapacityExceededReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ResourceRoomCapacityExceededException(cz.cesnet.shongo.controller.booking.resource.Resource resource, Integer availableLicenseCount, Integer maxLicenseCount)
        {
            ResourceRoomCapacityExceededReport report = new ResourceRoomCapacityExceededReport();
            report.setResource(resource);
            report.setAvailableLicenseCount(availableLicenseCount);
            report.setMaxLicenseCount(maxLicenseCount);
            this.report = report;
        }

        public ResourceRoomCapacityExceededException(Throwable throwable, cz.cesnet.shongo.controller.booking.resource.Resource resource, Integer availableLicenseCount, Integer maxLicenseCount)
        {
            super(throwable);
            ResourceRoomCapacityExceededReport report = new ResourceRoomCapacityExceededReport();
            report.setResource(resource);
            report.setAvailableLicenseCount(availableLicenseCount);
            report.setMaxLicenseCount(maxLicenseCount);
            this.report = report;
        }

        public Integer getAvailableLicenseCount()
        {
            return getReport().getAvailableLicenseCount();
        }

        public Integer getMaxLicenseCount()
        {
            return getReport().getMaxLicenseCount();
        }

        @Override
        public ResourceRoomCapacityExceededReport getReport()
        {
            return (ResourceRoomCapacityExceededReport) report;
        }
    }

    /**
     * The resource {@link #resource} doesn't have any available licenses for recording.
     */
    @Entity
    @DiscriminatorValue("ResourceRecordingCapacityExceededReport")
    public static class ResourceRecordingCapacityExceededReport extends ResourceReport
    {
        public ResourceRecordingCapacityExceededReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource-recording-capacity-exceeded";
        }

        public ResourceRecordingCapacityExceededReport(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            setResource(resource);
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-recording-capacity-exceeded", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ResourceRecordingCapacityExceededReport}.
     */
    public static class ResourceRecordingCapacityExceededException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ResourceRecordingCapacityExceededException(ResourceRecordingCapacityExceededReport report)
        {
            this.report = report;
        }

        public ResourceRecordingCapacityExceededException(Throwable throwable, ResourceRecordingCapacityExceededReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ResourceRecordingCapacityExceededException(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            ResourceRecordingCapacityExceededReport report = new ResourceRecordingCapacityExceededReport();
            report.setResource(resource);
            this.report = report;
        }

        public ResourceRecordingCapacityExceededException(Throwable throwable, cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            super(throwable);
            ResourceRecordingCapacityExceededReport report = new ResourceRecordingCapacityExceededReport();
            report.setResource(resource);
            this.report = report;
        }

        @Override
        public ResourceRecordingCapacityExceededReport getReport()
        {
            return (ResourceRecordingCapacityExceededReport) report;
        }
    }

    /**
     * The resource {@link #resource} is not endpoint.
     */
    @Entity
    @DiscriminatorValue("ResourceNotEndpointReport")
    public static class ResourceNotEndpointReport extends ResourceReport
    {
        public ResourceNotEndpointReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource-not-endpoint";
        }

        public ResourceNotEndpointReport(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            setResource(resource);
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-not-endpoint", userType, language, timeZone, getParameters());
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

        public ResourceNotEndpointException(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            ResourceNotEndpointReport report = new ResourceNotEndpointReport();
            report.setResource(resource);
            this.report = report;
        }

        public ResourceNotEndpointException(Throwable throwable, cz.cesnet.shongo.controller.booking.resource.Resource resource)
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
    @Entity
    @DiscriminatorValue("ResourceMultipleRequestedReport")
    public static class ResourceMultipleRequestedReport extends ResourceReport
    {
        public ResourceMultipleRequestedReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource-multiple-requested";
        }

        public ResourceMultipleRequestedReport(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            setResource(resource);
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-multiple-requested", userType, language, timeZone, getParameters());
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

        public ResourceMultipleRequestedException(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            ResourceMultipleRequestedReport report = new ResourceMultipleRequestedReport();
            report.setResource(resource);
            this.report = report;
        }

        public ResourceMultipleRequestedException(Throwable throwable, cz.cesnet.shongo.controller.booking.resource.Resource resource)
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
     * No available endpoint was found for the following specification: Technologies: {@link #technologies}
     */
    @Entity
    @DiscriminatorValue("EndpointNotFoundReport")
    public static class EndpointNotFoundReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected java.util.Set<cz.cesnet.shongo.Technology> technologies;

        public EndpointNotFoundReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "endpoint-not-found";
        }

        public EndpointNotFoundReport(java.util.Set<cz.cesnet.shongo.Technology> technologies)
        {
            setTechnologies(technologies);
        }

        @CollectionTable(name = "scheduler_report_technologies", joinColumns = @JoinColumn(name = "scheduler_report_id"))
        @ElementCollection
        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.ENUM_COLUMN_LENGTH)
        @Enumerated(EnumType.STRING)
        public java.util.Set<cz.cesnet.shongo.Technology> getTechnologies()
        {
            return technologies;
        }

        public void setTechnologies(java.util.Set<cz.cesnet.shongo.Technology> technologies)
        {
            this.technologies = technologies;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("technologies", technologies);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("endpoint-not-found", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link EndpointNotFoundReport}.
     */
    public static class EndpointNotFoundException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public EndpointNotFoundException(EndpointNotFoundReport report)
        {
            this.report = report;
        }

        public EndpointNotFoundException(Throwable throwable, EndpointNotFoundReport report)
        {
            super(throwable);
            this.report = report;
        }

        public EndpointNotFoundException(java.util.Set<cz.cesnet.shongo.Technology> technologies)
        {
            EndpointNotFoundReport report = new EndpointNotFoundReport();
            report.setTechnologies(technologies);
            this.report = report;
        }

        public EndpointNotFoundException(Throwable throwable, java.util.Set<cz.cesnet.shongo.Technology> technologies)
        {
            super(throwable);
            EndpointNotFoundReport report = new EndpointNotFoundReport();
            report.setTechnologies(technologies);
            this.report = report;
        }

        public java.util.Set<cz.cesnet.shongo.Technology> getTechnologies()
        {
            return getReport().getTechnologies();
        }

        @Override
        public EndpointNotFoundReport getReport()
        {
            return (EndpointNotFoundReport) report;
        }
    }

    /**
     * Reusing existing {@link #executable}.
     */
    @Entity
    @DiscriminatorValue("ExecutableReusingReport")
    public static class ExecutableReusingReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.executable.Executable executable;

        public ExecutableReusingReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "executable-reusing";
        }

        public ExecutableReusingReport(cz.cesnet.shongo.controller.booking.executable.Executable executable)
        {
            setExecutable(executable);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "executable_id")
        public cz.cesnet.shongo.controller.booking.executable.Executable getExecutable()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(executable);
        }

        public void setExecutable(cz.cesnet.shongo.controller.booking.executable.Executable executable)
        {
            this.executable = executable;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("executable", executable);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("executable-reusing", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Room executable doesn't exist.
     */
    @Entity
    @DiscriminatorValue("RoomExecutableNotExistsReport")
    public static class RoomExecutableNotExistsReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public RoomExecutableNotExistsReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "room-executable-not-exists";
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("room-executable-not-exists", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link RoomExecutableNotExistsReport}.
     */
    public static class RoomExecutableNotExistsException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public RoomExecutableNotExistsException(RoomExecutableNotExistsReport report)
        {
            this.report = report;
        }

        public RoomExecutableNotExistsException(Throwable throwable, RoomExecutableNotExistsReport report)
        {
            super(throwable);
            this.report = report;
        }

        public RoomExecutableNotExistsException()
        {
            RoomExecutableNotExistsReport report = new RoomExecutableNotExistsReport();
            this.report = report;
        }

        public RoomExecutableNotExistsException(Throwable throwable)
        {
            super(throwable);
            RoomExecutableNotExistsReport report = new RoomExecutableNotExistsReport();
            this.report = report;
        }

        @Override
        public RoomExecutableNotExistsReport getReport()
        {
            return (RoomExecutableNotExistsReport) report;
        }
    }

    /**
     * Requested time slot doesn't correspond to {@link #interval} from reused executable {@link #executable}.
     */
    @Entity
    @DiscriminatorValue("ExecutableInvalidSlotReport")
    public static class ExecutableInvalidSlotReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.executable.Executable executable;

        protected org.joda.time.Interval interval;

        public ExecutableInvalidSlotReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "executable-invalid-slot";
        }

        public ExecutableInvalidSlotReport(cz.cesnet.shongo.controller.booking.executable.Executable executable, org.joda.time.Interval interval)
        {
            setExecutable(executable);
            setInterval(interval);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "executable_id")
        public cz.cesnet.shongo.controller.booking.executable.Executable getExecutable()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(executable);
        }

        public void setExecutable(cz.cesnet.shongo.controller.booking.executable.Executable executable)
        {
            this.executable = executable;
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="interval_start"),@jakarta.persistence.Column(name="interval_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getInterval()
        {
            return interval;
        }

        public void setInterval(org.joda.time.Interval interval)
        {
            this.interval = interval;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("executable", executable);
            parameters.put("interval", interval);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("executable-invalid-slot", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ExecutableInvalidSlotReport}.
     */
    public static class ExecutableInvalidSlotException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ExecutableInvalidSlotException(ExecutableInvalidSlotReport report)
        {
            this.report = report;
        }

        public ExecutableInvalidSlotException(Throwable throwable, ExecutableInvalidSlotReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ExecutableInvalidSlotException(cz.cesnet.shongo.controller.booking.executable.Executable executable, org.joda.time.Interval interval)
        {
            ExecutableInvalidSlotReport report = new ExecutableInvalidSlotReport();
            report.setExecutable(executable);
            report.setInterval(interval);
            this.report = report;
        }

        public ExecutableInvalidSlotException(Throwable throwable, cz.cesnet.shongo.controller.booking.executable.Executable executable, org.joda.time.Interval interval)
        {
            super(throwable);
            ExecutableInvalidSlotReport report = new ExecutableInvalidSlotReport();
            report.setExecutable(executable);
            report.setInterval(interval);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.booking.executable.Executable getExecutable()
        {
            return getReport().getExecutable();
        }

        public org.joda.time.Interval getInterval()
        {
            return getReport().getInterval();
        }

        @Override
        public ExecutableInvalidSlotReport getReport()
        {
            return (ExecutableInvalidSlotReport) report;
        }
    }

    /**
     * Reused executable {@link #executable} is not available because it's already used in reservation request {@link #usageReservationRequest} for {@link #usageInterval}.
     */
    @Entity
    @DiscriminatorValue("ExecutableAlreadyUsedReport")
    public static class ExecutableAlreadyUsedReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.executable.Executable executable;

        protected cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest usageReservationRequest;

        protected org.joda.time.Interval usageInterval;

        public ExecutableAlreadyUsedReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "executable-already-used";
        }

        public ExecutableAlreadyUsedReport(cz.cesnet.shongo.controller.booking.executable.Executable executable, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest usageReservationRequest, org.joda.time.Interval usageInterval)
        {
            setExecutable(executable);
            setUsageReservationRequest(usageReservationRequest);
            setUsageInterval(usageInterval);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "executable_id")
        public cz.cesnet.shongo.controller.booking.executable.Executable getExecutable()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(executable);
        }

        public void setExecutable(cz.cesnet.shongo.controller.booking.executable.Executable executable)
        {
            this.executable = executable;
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "usage_reservation_request_id")
        public cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest getUsageReservationRequest()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(usageReservationRequest);
        }

        public void setUsageReservationRequest(cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest usageReservationRequest)
        {
            this.usageReservationRequest = usageReservationRequest;
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="usage_interval_start"),@jakarta.persistence.Column(name="usage_interval_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getUsageInterval()
        {
            return usageInterval;
        }

        public void setUsageInterval(org.joda.time.Interval usageInterval)
        {
            this.usageInterval = usageInterval;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("executable", executable);
            parameters.put("usageReservationRequest", usageReservationRequest);
            parameters.put("usageInterval", usageInterval);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("executable-already-used", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ExecutableAlreadyUsedReport}.
     */
    public static class ExecutableAlreadyUsedException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ExecutableAlreadyUsedException(ExecutableAlreadyUsedReport report)
        {
            this.report = report;
        }

        public ExecutableAlreadyUsedException(Throwable throwable, ExecutableAlreadyUsedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ExecutableAlreadyUsedException(cz.cesnet.shongo.controller.booking.executable.Executable executable, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest usageReservationRequest, org.joda.time.Interval usageInterval)
        {
            ExecutableAlreadyUsedReport report = new ExecutableAlreadyUsedReport();
            report.setExecutable(executable);
            report.setUsageReservationRequest(usageReservationRequest);
            report.setUsageInterval(usageInterval);
            this.report = report;
        }

        public ExecutableAlreadyUsedException(Throwable throwable, cz.cesnet.shongo.controller.booking.executable.Executable executable, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest usageReservationRequest, org.joda.time.Interval usageInterval)
        {
            super(throwable);
            ExecutableAlreadyUsedReport report = new ExecutableAlreadyUsedReport();
            report.setExecutable(executable);
            report.setUsageReservationRequest(usageReservationRequest);
            report.setUsageInterval(usageInterval);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.booking.executable.Executable getExecutable()
        {
            return getReport().getExecutable();
        }

        public cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest getUsageReservationRequest()
        {
            return getReport().getUsageReservationRequest();
        }

        public org.joda.time.Interval getUsageInterval()
        {
            return getReport().getUsageInterval();
        }

        @Override
        public ExecutableAlreadyUsedReport getReport()
        {
            return (ExecutableAlreadyUsedReport) report;
        }
    }

    /**
     * Not enough endpoints are requested for the compartment.
     */
    @Entity
    @DiscriminatorValue("CompartmentNotEnoughEndpointReport")
    public static class CompartmentNotEnoughEndpointReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public CompartmentNotEnoughEndpointReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "compartment-not-enough-endpoint";
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("compartment-not-enough-endpoint", userType, language, timeZone, getParameters());
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
    @Entity
    @DiscriminatorValue("CompartmentAssignAliasToExternalEndpointReport")
    public static class CompartmentAssignAliasToExternalEndpointReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public CompartmentAssignAliasToExternalEndpointReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "compartment-assign-alias-to-external-endpoint";
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("compartment-assign-alias-to-external-endpoint", userType, language, timeZone, getParameters());
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

    @Entity
    @DiscriminatorValue("ConnectionReport")
    public static abstract class ConnectionReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.executable.Endpoint endpointFrom;

        protected cz.cesnet.shongo.controller.booking.executable.Endpoint endpointTo;

        public ConnectionReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "connection";
        }

        public ConnectionReport(cz.cesnet.shongo.controller.booking.executable.Endpoint endpointFrom, cz.cesnet.shongo.controller.booking.executable.Endpoint endpointTo)
        {
            setEndpointFrom(endpointFrom);
            setEndpointTo(endpointTo);
        }

        @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "endpoint_from_id")
        public cz.cesnet.shongo.controller.booking.executable.Endpoint getEndpointFrom()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(endpointFrom);
        }

        public void setEndpointFrom(cz.cesnet.shongo.controller.booking.executable.Endpoint endpointFrom)
        {
            this.endpointFrom = endpointFrom;
        }

        @OneToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "endpoint_to_id")
        public cz.cesnet.shongo.controller.booking.executable.Endpoint getEndpointTo()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(endpointTo);
        }

        public void setEndpointTo(cz.cesnet.shongo.controller.booking.executable.Endpoint endpointTo)
        {
            this.endpointTo = endpointTo;
        }

        @PreRemove
        public void preRemove()
        {
            if (endpointFrom.getState() == cz.cesnet.shongo.controller.booking.executable.Executable.State.NOT_ALLOCATED) {
                endpointFrom.setState(cz.cesnet.shongo.controller.booking.executable.Executable.State.TO_DELETE);
            }
            if (endpointTo.getState() == cz.cesnet.shongo.controller.booking.executable.Executable.State.NOT_ALLOCATED) {
                endpointTo.setState(cz.cesnet.shongo.controller.booking.executable.Executable.State.TO_DELETE);
            }
        }
    }

    /**
     * Creating connection between {@link #endpointFrom} and {@link #endpointTo} in technology {@link #technology}.
     */
    @Entity
    @DiscriminatorValue("ConnectionBetweenReport")
    public static class ConnectionBetweenReport extends ConnectionReport
    {
        protected cz.cesnet.shongo.Technology technology;

        public ConnectionBetweenReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "connection-between";
        }

        public ConnectionBetweenReport(cz.cesnet.shongo.controller.booking.executable.Endpoint endpointFrom, cz.cesnet.shongo.controller.booking.executable.Endpoint endpointTo, cz.cesnet.shongo.Technology technology)
        {
            setEndpointFrom(endpointFrom);
            setEndpointTo(endpointTo);
            setTechnology(technology);
        }

        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.ENUM_COLUMN_LENGTH)
        @Enumerated(EnumType.STRING)
        public cz.cesnet.shongo.Technology getTechnology()
        {
            return technology;
        }

        public void setTechnology(cz.cesnet.shongo.Technology technology)
        {
            this.technology = technology;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("endpointFrom", endpointFrom);
            parameters.put("endpointTo", endpointTo);
            parameters.put("technology", technology);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("connection-between", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Creating connection from {@link #endpointFrom} to {@link #endpointTo}.
     */
    @Entity
    @DiscriminatorValue("ConnectionFromToReport")
    public static class ConnectionFromToReport extends ConnectionReport
    {
        public ConnectionFromToReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "connection-from-to";
        }

        public ConnectionFromToReport(cz.cesnet.shongo.controller.booking.executable.Endpoint endpointFrom, cz.cesnet.shongo.controller.booking.executable.Endpoint endpointTo)
        {
            setEndpointFrom(endpointFrom);
            setEndpointTo(endpointTo);
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("endpointFrom", endpointFrom);
            parameters.put("endpointTo", endpointTo);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("connection-from-to", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Cannot create connection from {@link #endpointFrom} to {@link #endpointTo}, because the target represents multiple endpoints (not supported yet).
     */
    @Entity
    @DiscriminatorValue("ConnectionToMultipleReport")
    public static class ConnectionToMultipleReport extends ConnectionReport
    {
        public ConnectionToMultipleReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "connection-to-multiple";
        }

        public ConnectionToMultipleReport(cz.cesnet.shongo.controller.booking.executable.Endpoint endpointFrom, cz.cesnet.shongo.controller.booking.executable.Endpoint endpointTo)
        {
            setEndpointFrom(endpointFrom);
            setEndpointTo(endpointTo);
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("endpointFrom", endpointFrom);
            parameters.put("endpointTo", endpointTo);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("connection-to-multiple", userType, language, timeZone, getParameters());
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

        public ConnectionToMultipleException(cz.cesnet.shongo.controller.booking.executable.Endpoint endpointFrom, cz.cesnet.shongo.controller.booking.executable.Endpoint endpointTo)
        {
            ConnectionToMultipleReport report = new ConnectionToMultipleReport();
            report.setEndpointFrom(endpointFrom);
            report.setEndpointTo(endpointTo);
            this.report = report;
        }

        public ConnectionToMultipleException(Throwable throwable, cz.cesnet.shongo.controller.booking.executable.Endpoint endpointFrom, cz.cesnet.shongo.controller.booking.executable.Endpoint endpointTo)
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
     * Requested time slot doesn't correspond to {@link #interval} from reused reservation request {@link #reservationRequest}.
     */
    @Entity
    @DiscriminatorValue("ReservationRequestInvalidSlotReport")
    public static class ReservationRequestInvalidSlotReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest;

        protected org.joda.time.Interval interval;

        public ReservationRequestInvalidSlotReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "reservation-request-invalid-slot";
        }

        public ReservationRequestInvalidSlotReport(cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest, org.joda.time.Interval interval)
        {
            setReservationRequest(reservationRequest);
            setInterval(interval);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "reservation_request_id")
        public cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest getReservationRequest()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(reservationRequest);
        }

        public void setReservationRequest(cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest)
        {
            this.reservationRequest = reservationRequest;
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="interval_start"),@jakarta.persistence.Column(name="interval_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getInterval()
        {
            return interval;
        }

        public void setInterval(org.joda.time.Interval interval)
        {
            this.interval = interval;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reservationRequest", reservationRequest);
            parameters.put("interval", interval);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reservation-request-invalid-slot", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ReservationRequestInvalidSlotReport}.
     */
    public static class ReservationRequestInvalidSlotException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ReservationRequestInvalidSlotException(ReservationRequestInvalidSlotReport report)
        {
            this.report = report;
        }

        public ReservationRequestInvalidSlotException(Throwable throwable, ReservationRequestInvalidSlotReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationRequestInvalidSlotException(cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest, org.joda.time.Interval interval)
        {
            ReservationRequestInvalidSlotReport report = new ReservationRequestInvalidSlotReport();
            report.setReservationRequest(reservationRequest);
            report.setInterval(interval);
            this.report = report;
        }

        public ReservationRequestInvalidSlotException(Throwable throwable, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest, org.joda.time.Interval interval)
        {
            super(throwable);
            ReservationRequestInvalidSlotReport report = new ReservationRequestInvalidSlotReport();
            report.setReservationRequest(reservationRequest);
            report.setInterval(interval);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest getReservationRequest()
        {
            return getReport().getReservationRequest();
        }

        public org.joda.time.Interval getInterval()
        {
            return getReport().getInterval();
        }

        @Override
        public ReservationRequestInvalidSlotReport getReport()
        {
            return (ReservationRequestInvalidSlotReport) report;
        }
    }

    /**
     * The reservation request has been denied by resource owner {@link #deniedBy}. Reason: {@link #reason}
     */
    @Entity
    @DiscriminatorValue("ReservationRequestDeniedReport")
    public static class ReservationRequestDeniedReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected String deniedBy;

        protected String reason;

        public ReservationRequestDeniedReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "reservation-request-denied";
        }

        public ReservationRequestDeniedReport(String deniedBy, String reason)
        {
            setDeniedBy(deniedBy);
            setReason(reason);
        }

        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
        public String getDeniedBy()
        {
            return deniedBy;
        }

        public void setDeniedBy(String deniedBy)
        {
            this.deniedBy = deniedBy;
        }

        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
        public String getReason()
        {
            return reason;
        }

        public void setReason(String reason)
        {
            this.reason = reason;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("deniedBy", deniedBy);
            parameters.put("reason", reason);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reservation-request-denied", userType, language, timeZone, getParameters());
        }
    }

    /**
     * The reservation request has been denied. Reason: The resource {@link #resource} is already allocated in interval {@link #interval}.
     */
    @Entity
    @DiscriminatorValue("ReservationRequestDeniedAlreadyAllocatedReport")
    public static class ReservationRequestDeniedAlreadyAllocatedReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.resource.Resource resource;

        protected org.joda.time.Interval interval;

        public ReservationRequestDeniedAlreadyAllocatedReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "reservation-request-denied-already-allocated";
        }

        public ReservationRequestDeniedAlreadyAllocatedReport(cz.cesnet.shongo.controller.booking.resource.Resource resource, org.joda.time.Interval interval)
        {
            setResource(resource);
            setInterval(interval);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "resource_id")
        public cz.cesnet.shongo.controller.booking.resource.Resource getResource()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(resource);
        }

        public void setResource(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            this.resource = resource;
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="interval_start"),@jakarta.persistence.Column(name="interval_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getInterval()
        {
            return interval;
        }

        public void setInterval(org.joda.time.Interval interval)
        {
            this.interval = interval;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            parameters.put("interval", interval);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reservation-request-denied-already-allocated", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Reused reservation request {@link #reservationRequest} is mandatory but wasn't used.
     */
    @Entity
    @DiscriminatorValue("ReservationWithoutMandatoryUsageReport")
    public static class ReservationWithoutMandatoryUsageReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest;

        public ReservationWithoutMandatoryUsageReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "reservation-without-mandatory-usage";
        }

        public ReservationWithoutMandatoryUsageReport(cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest)
        {
            setReservationRequest(reservationRequest);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "reservation_request_id")
        public cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest getReservationRequest()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(reservationRequest);
        }

        public void setReservationRequest(cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest)
        {
            this.reservationRequest = reservationRequest;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reservationRequest", reservationRequest);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reservation-without-mandatory-usage", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ReservationWithoutMandatoryUsageReport}.
     */
    public static class ReservationWithoutMandatoryUsageException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ReservationWithoutMandatoryUsageException(ReservationWithoutMandatoryUsageReport report)
        {
            this.report = report;
        }

        public ReservationWithoutMandatoryUsageException(Throwable throwable, ReservationWithoutMandatoryUsageReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationWithoutMandatoryUsageException(cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest)
        {
            ReservationWithoutMandatoryUsageReport report = new ReservationWithoutMandatoryUsageReport();
            report.setReservationRequest(reservationRequest);
            this.report = report;
        }

        public ReservationWithoutMandatoryUsageException(Throwable throwable, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest)
        {
            super(throwable);
            ReservationWithoutMandatoryUsageReport report = new ReservationWithoutMandatoryUsageReport();
            report.setReservationRequest(reservationRequest);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest getReservationRequest()
        {
            return getReport().getReservationRequest();
        }

        @Override
        public ReservationWithoutMandatoryUsageReport getReport()
        {
            return (ReservationWithoutMandatoryUsageReport) report;
        }
    }

    @Entity
    @DiscriminatorValue("ReservationReport")
    public static abstract class ReservationReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.reservation.Reservation reservation;

        public ReservationReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "reservation";
        }

        public ReservationReport(cz.cesnet.shongo.controller.booking.reservation.Reservation reservation)
        {
            setReservation(reservation);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "reservation_id")
        public cz.cesnet.shongo.controller.booking.reservation.Reservation getReservation()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(reservation);
        }

        public void setReservation(cz.cesnet.shongo.controller.booking.reservation.Reservation reservation)
        {
            this.reservation = reservation;
        }
    }

    /**
     * Reused reservation request {@link #reservationRequest} is not available because it's reservation {@link #reservation} is already used in reservation request {@link #usageReservationRequest} for {@link #usageInterval}.
     */
    @Entity
    @DiscriminatorValue("ReservationAlreadyUsedReport")
    public static class ReservationAlreadyUsedReport extends ReservationReport
    {
        protected cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest;

        protected cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest usageReservationRequest;

        protected org.joda.time.Interval usageInterval;

        public ReservationAlreadyUsedReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "reservation-already-used";
        }

        public ReservationAlreadyUsedReport(cz.cesnet.shongo.controller.booking.reservation.Reservation reservation, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest usageReservationRequest, org.joda.time.Interval usageInterval)
        {
            setReservation(reservation);
            setReservationRequest(reservationRequest);
            setUsageReservationRequest(usageReservationRequest);
            setUsageInterval(usageInterval);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "reservation_request_id")
        public cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest getReservationRequest()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(reservationRequest);
        }

        public void setReservationRequest(cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest)
        {
            this.reservationRequest = reservationRequest;
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "usage_reservation_request_id")
        public cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest getUsageReservationRequest()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(usageReservationRequest);
        }

        public void setUsageReservationRequest(cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest usageReservationRequest)
        {
            this.usageReservationRequest = usageReservationRequest;
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="usage_interval_start"),@jakarta.persistence.Column(name="usage_interval_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getUsageInterval()
        {
            return usageInterval;
        }

        public void setUsageInterval(org.joda.time.Interval usageInterval)
        {
            this.usageInterval = usageInterval;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reservation", reservation);
            parameters.put("reservationRequest", reservationRequest);
            parameters.put("usageReservationRequest", usageReservationRequest);
            parameters.put("usageInterval", usageInterval);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reservation-already-used", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ReservationAlreadyUsedReport}.
     */
    public static class ReservationAlreadyUsedException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ReservationAlreadyUsedException(ReservationAlreadyUsedReport report)
        {
            this.report = report;
        }

        public ReservationAlreadyUsedException(Throwable throwable, ReservationAlreadyUsedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ReservationAlreadyUsedException(cz.cesnet.shongo.controller.booking.reservation.Reservation reservation, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest usageReservationRequest, org.joda.time.Interval usageInterval)
        {
            ReservationAlreadyUsedReport report = new ReservationAlreadyUsedReport();
            report.setReservation(reservation);
            report.setReservationRequest(reservationRequest);
            report.setUsageReservationRequest(usageReservationRequest);
            report.setUsageInterval(usageInterval);
            this.report = report;
        }

        public ReservationAlreadyUsedException(Throwable throwable, cz.cesnet.shongo.controller.booking.reservation.Reservation reservation, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest, cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest usageReservationRequest, org.joda.time.Interval usageInterval)
        {
            super(throwable);
            ReservationAlreadyUsedReport report = new ReservationAlreadyUsedReport();
            report.setReservation(reservation);
            report.setReservationRequest(reservationRequest);
            report.setUsageReservationRequest(usageReservationRequest);
            report.setUsageInterval(usageInterval);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest getReservationRequest()
        {
            return getReport().getReservationRequest();
        }

        public cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest getUsageReservationRequest()
        {
            return getReport().getUsageReservationRequest();
        }

        public org.joda.time.Interval getUsageInterval()
        {
            return getReport().getUsageInterval();
        }

        @Override
        public ReservationAlreadyUsedReport getReport()
        {
            return (ReservationAlreadyUsedReport) report;
        }
    }

    /**
     * Reusing {@link #reservation}.
     */
    @Entity
    @DiscriminatorValue("ReservationReusingReport")
    public static class ReservationReusingReport extends ReservationReport
    {
        public ReservationReusingReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "reservation-reusing";
        }

        public ReservationReusingReport(cz.cesnet.shongo.controller.booking.reservation.Reservation reservation)
        {
            setReservation(reservation);
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reservation", reservation);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reservation-reusing", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Value {@link #value} is already allocated in interval {@link #interval}.
     */
    @Entity
    @DiscriminatorValue("ValueAlreadyAllocatedReport")
    public static class ValueAlreadyAllocatedReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected String value;

        protected org.joda.time.Interval interval;

        public ValueAlreadyAllocatedReport()
        {
        }

        @Transient
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

        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="interval_start"),@jakarta.persistence.Column(name="interval_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getInterval()
        {
            return interval;
        }

        public void setInterval(org.joda.time.Interval interval)
        {
            this.interval = interval;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("value", value);
            parameters.put("interval", interval);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("value-already-allocated", userType, language, timeZone, getParameters());
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
    @Entity
    @DiscriminatorValue("ValueInvalidReport")
    public static class ValueInvalidReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected String value;

        public ValueInvalidReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "value-invalid";
        }

        public ValueInvalidReport(String value)
        {
            setValue(value);
        }

        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("value", value);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("value-invalid", userType, language, timeZone, getParameters());
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
    @Entity
    @DiscriminatorValue("ValueNotAvailableReport")
    public static class ValueNotAvailableReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected org.joda.time.Interval interval;

        public ValueNotAvailableReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "value-not-available";
        }

        public ValueNotAvailableReport(org.joda.time.Interval interval)
        {
            setInterval(interval);
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="interval_start"),@jakarta.persistence.Column(name="interval_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getInterval()
        {
            return interval;
        }

        public void setInterval(org.joda.time.Interval interval)
        {
            this.interval = interval;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("interval", interval);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("value-not-available", userType, language, timeZone, getParameters());
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
     * Requested service slot {@link #serviceSlot} is outside the executable slot {@link #executableSlot}.
     */
    @Entity
    @DiscriminatorValue("ExecutableServiceInvalidSlotReport")
    public static class ExecutableServiceInvalidSlotReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected org.joda.time.Interval executableSlot;

        protected org.joda.time.Interval serviceSlot;

        public ExecutableServiceInvalidSlotReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "executable-service-invalid-slot";
        }

        public ExecutableServiceInvalidSlotReport(org.joda.time.Interval executableSlot, org.joda.time.Interval serviceSlot)
        {
            setExecutableSlot(executableSlot);
            setServiceSlot(serviceSlot);
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="executable_slot_start"),@jakarta.persistence.Column(name="executable_slot_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getExecutableSlot()
        {
            return executableSlot;
        }

        public void setExecutableSlot(org.joda.time.Interval executableSlot)
        {
            this.executableSlot = executableSlot;
        }

        @org.hibernate.annotations.Columns(columns={@jakarta.persistence.Column(name="service_slot_start"),@jakarta.persistence.Column(name="service_slot_end")})
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentInterval.class)
        public org.joda.time.Interval getServiceSlot()
        {
            return serviceSlot;
        }

        public void setServiceSlot(org.joda.time.Interval serviceSlot)
        {
            this.serviceSlot = serviceSlot;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("executableSlot", executableSlot);
            parameters.put("serviceSlot", serviceSlot);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("executable-service-invalid-slot", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ExecutableServiceInvalidSlotReport}.
     */
    public static class ExecutableServiceInvalidSlotException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ExecutableServiceInvalidSlotException(ExecutableServiceInvalidSlotReport report)
        {
            this.report = report;
        }

        public ExecutableServiceInvalidSlotException(Throwable throwable, ExecutableServiceInvalidSlotReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ExecutableServiceInvalidSlotException(org.joda.time.Interval executableSlot, org.joda.time.Interval serviceSlot)
        {
            ExecutableServiceInvalidSlotReport report = new ExecutableServiceInvalidSlotReport();
            report.setExecutableSlot(executableSlot);
            report.setServiceSlot(serviceSlot);
            this.report = report;
        }

        public ExecutableServiceInvalidSlotException(Throwable throwable, org.joda.time.Interval executableSlot, org.joda.time.Interval serviceSlot)
        {
            super(throwable);
            ExecutableServiceInvalidSlotReport report = new ExecutableServiceInvalidSlotReport();
            report.setExecutableSlot(executableSlot);
            report.setServiceSlot(serviceSlot);
            this.report = report;
        }

        public org.joda.time.Interval getExecutableSlot()
        {
            return getReport().getExecutableSlot();
        }

        public org.joda.time.Interval getServiceSlot()
        {
            return getReport().getServiceSlot();
        }

        @Override
        public ExecutableServiceInvalidSlotReport getReport()
        {
            return (ExecutableServiceInvalidSlotReport) report;
        }
    }

    /**
     * Recording service cannot be allocated for the room endpoint {@link #roomEndpointId} because it is always recordable.
     */
    @Entity
    @DiscriminatorValue("RoomEndpointAlwaysRecordableReport")
    public static class RoomEndpointAlwaysRecordableReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected String roomEndpointId;

        public RoomEndpointAlwaysRecordableReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "room-endpoint-always-recordable";
        }

        public RoomEndpointAlwaysRecordableReport(String roomEndpointId)
        {
            setRoomEndpointId(roomEndpointId);
        }

        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
        public String getRoomEndpointId()
        {
            return roomEndpointId;
        }

        public void setRoomEndpointId(String roomEndpointId)
        {
            this.roomEndpointId = roomEndpointId;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("roomEndpointId", roomEndpointId);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("room-endpoint-always-recordable", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link RoomEndpointAlwaysRecordableReport}.
     */
    public static class RoomEndpointAlwaysRecordableException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public RoomEndpointAlwaysRecordableException(RoomEndpointAlwaysRecordableReport report)
        {
            this.report = report;
        }

        public RoomEndpointAlwaysRecordableException(Throwable throwable, RoomEndpointAlwaysRecordableReport report)
        {
            super(throwable);
            this.report = report;
        }

        public RoomEndpointAlwaysRecordableException(String roomEndpointId)
        {
            RoomEndpointAlwaysRecordableReport report = new RoomEndpointAlwaysRecordableReport();
            report.setRoomEndpointId(roomEndpointId);
            this.report = report;
        }

        public RoomEndpointAlwaysRecordableException(Throwable throwable, String roomEndpointId)
        {
            super(throwable);
            RoomEndpointAlwaysRecordableReport report = new RoomEndpointAlwaysRecordableReport();
            report.setRoomEndpointId(roomEndpointId);
            this.report = report;
        }

        public String getRoomEndpointId()
        {
            return getReport().getRoomEndpointId();
        }

        @Override
        public RoomEndpointAlwaysRecordableReport getReport()
        {
            return (RoomEndpointAlwaysRecordableReport) report;
        }
    }

    /**
     * Allocating the resource {@link #resource}.
     */
    @Entity
    @DiscriminatorValue("AllocatingResourceReport")
    public static class AllocatingResourceReport extends ResourceReport
    {
        public AllocatingResourceReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-resource";
        }

        public AllocatingResourceReport(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            setResource(resource);
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-resource", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Allocating alias for the following specification: 
     *   Technology: {@link #technologies} 
     *   Alias Type: {@link #aliasTypes} 
     *        Value: {@link #value}
     */
    @Entity
    @DiscriminatorValue("AllocatingAliasReport")
    public static class AllocatingAliasReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected java.util.Set<cz.cesnet.shongo.Technology> technologies;

        protected java.util.Set<cz.cesnet.shongo.AliasType> aliasTypes;

        protected String value;

        public AllocatingAliasReport()
        {
        }

        @Transient
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

        @CollectionTable(name = "scheduler_report_technologies", joinColumns = @JoinColumn(name = "scheduler_report_id"))
        @ElementCollection
        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.ENUM_COLUMN_LENGTH)
        @Enumerated(EnumType.STRING)
        public java.util.Set<cz.cesnet.shongo.Technology> getTechnologies()
        {
            return technologies;
        }

        public void setTechnologies(java.util.Set<cz.cesnet.shongo.Technology> technologies)
        {
            this.technologies = technologies;
        }

        @CollectionTable(name = "scheduler_report_alias_types", joinColumns = @JoinColumn(name = "scheduler_report_id"))
        @ElementCollection
        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.ENUM_COLUMN_LENGTH)
        @Enumerated(EnumType.STRING)
        public java.util.Set<cz.cesnet.shongo.AliasType> getAliasTypes()
        {
            return aliasTypes;
        }

        public void setAliasTypes(java.util.Set<cz.cesnet.shongo.AliasType> aliasTypes)
        {
            this.aliasTypes = aliasTypes;
        }

        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
        public String getValue()
        {
            return value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("technologies", technologies);
            parameters.put("aliasTypes", aliasTypes);
            parameters.put("value", value);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-alias", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Allocating value in the resource {@link #resource}.
     */
    @Entity
    @DiscriminatorValue("AllocatingValueReport")
    public static class AllocatingValueReport extends ResourceReport
    {
        public AllocatingValueReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-value";
        }

        public AllocatingValueReport(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            setResource(resource);
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-value", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Allocating room for the following specification: 
     *     Technology: {@link #technologySet}
     *   Participants: {@link #participantCount} 
     *       Resource: {@link #resource}
     */
    @Entity
    @DiscriminatorValue("AllocatingRoomReport")
    public static class AllocatingRoomReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected java.util.List<cz.cesnet.shongo.controller.booking.TechnologySet> technologySet;

        protected Integer participantCount;

        protected cz.cesnet.shongo.controller.booking.resource.Resource resource;

        public AllocatingRoomReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-room";
        }

        public AllocatingRoomReport(java.util.List<cz.cesnet.shongo.controller.booking.TechnologySet> technologySet, Integer participantCount, cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            setTechnologySet(technologySet);
            setParticipantCount(participantCount);
            setResource(resource);
        }

        @JoinTable(name = "scheduler_report_technology_sets", joinColumns = @JoinColumn(name = "scheduler_report_id"))
        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        public java.util.List<cz.cesnet.shongo.controller.booking.TechnologySet> getTechnologySet()
        {
            return technologySet;
        }

        public void setTechnologySet(java.util.List<cz.cesnet.shongo.controller.booking.TechnologySet> technologySet)
        {
            this.technologySet = technologySet;
        }

        @Column
        public Integer getParticipantCount()
        {
            return participantCount;
        }

        public void setParticipantCount(Integer participantCount)
        {
            this.participantCount = participantCount;
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "resource_id")
        public cz.cesnet.shongo.controller.booking.resource.Resource getResource()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(resource);
        }

        public void setResource(cz.cesnet.shongo.controller.booking.resource.Resource resource)
        {
            this.resource = resource;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("technologySets", technologySet);
            parameters.put("participantCount", participantCount);
            parameters.put("resource", resource);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-room", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Allocating recording service for the following specification: 
     *     Enabled: {@link #enabled}
     */
    @Entity
    @DiscriminatorValue("AllocatingRecordingServiceReport")
    public static class AllocatingRecordingServiceReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected Boolean enabled;

        public AllocatingRecordingServiceReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-recording-service";
        }

        public AllocatingRecordingServiceReport(Boolean enabled)
        {
            setEnabled(enabled);
        }

        @Column
        public Boolean getEnabled()
        {
            return enabled;
        }

        public void setEnabled(Boolean enabled)
        {
            this.enabled = enabled;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("enabled", enabled);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-recording-service", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Allocating compartment.
     */
    @Entity
    @DiscriminatorValue("AllocatingCompartmentReport")
    public static class AllocatingCompartmentReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public AllocatingCompartmentReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-compartment";
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-compartment", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Allocating executable.
     */
    @Entity
    @DiscriminatorValue("AllocatingExecutableReport")
    public static class AllocatingExecutableReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public AllocatingExecutableReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "allocating-executable";
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("allocating-executable", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Checking specification availability report.
     */
    @Entity
    @DiscriminatorValue("SpecificationCheckingAvailabilityReport")
    public static class SpecificationCheckingAvailabilityReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public SpecificationCheckingAvailabilityReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "specification-checking-availability";
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("specification-checking-availability", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Finding available resource.
     */
    @Entity
    @DiscriminatorValue("FindingAvailableResourceReport")
    public static class FindingAvailableResourceReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public FindingAvailableResourceReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "finding-available-resource";
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("finding-available-resource", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Sorting resources.
     */
    @Entity
    @DiscriminatorValue("SortingResourcesReport")
    public static class SortingResourcesReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public SortingResourcesReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "sorting-resources";
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("sorting-resources", userType, language, timeZone, getParameters());
        }
    }

    /**
     * The following reservations are colliding, trying to reallocate them: {@link #reservations}
     */
    @Entity
    @DiscriminatorValue("CollidingReservationsReport")
    public static class CollidingReservationsReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected java.util.Map<String, String> reservations;

        public CollidingReservationsReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "colliding-reservations";
        }

        public CollidingReservationsReport(java.util.Map<String, String> reservations)
        {
            setReservations(reservations);
        }

        @CollectionTable(name = "scheduler_report_reservations", joinColumns = @JoinColumn(name = "scheduler_report_id"))
        @ElementCollection
        public java.util.Map<String, String> getReservations()
        {
            return reservations;
        }

        public void setReservations(java.util.Map<String, String> reservations)
        {
            this.reservations = reservations;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reservations", reservations);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("colliding-reservations", userType, language, timeZone, getParameters());
        }
    }

    /**
     * The following reservation requests will be reallocated: {@link #reservationRequests}
     */
    @Entity
    @DiscriminatorValue("ReallocatingReservationRequestsReport")
    public static class ReallocatingReservationRequestsReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected java.util.List<String> reservationRequests;

        public ReallocatingReservationRequestsReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "reallocating-reservation-requests";
        }

        public ReallocatingReservationRequestsReport(java.util.List<String> reservationRequests)
        {
            setReservationRequests(reservationRequests);
        }

        @CollectionTable(name = "scheduler_report_reservation_requests", joinColumns = @JoinColumn(name = "scheduler_report_id"))
        @ElementCollection
        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
        public java.util.List<String> getReservationRequests()
        {
            return reservationRequests;
        }

        public void setReservationRequests(java.util.List<String> reservationRequests)
        {
            this.reservationRequests = reservationRequests;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reservationRequests", reservationRequests);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reallocating-reservation-requests", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Reallocating reservation request {@link #reservationRequest}.
     */
    @Entity
    @DiscriminatorValue("ReallocatingReservationRequestReport")
    public static class ReallocatingReservationRequestReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected String reservationRequest;

        public ReallocatingReservationRequestReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "reallocating-reservation-request";
        }

        public ReallocatingReservationRequestReport(String reservationRequest)
        {
            setReservationRequest(reservationRequest);
        }

        @Column(length = cz.cesnet.shongo.api.AbstractComplexType.DEFAULT_COLUMN_LENGTH)
        public String getReservationRequest()
        {
            return reservationRequest;
        }

        public void setReservationRequest(String reservationRequest)
        {
            this.reservationRequest = reservationRequest;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.INFORMATION;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("reservationRequest", reservationRequest);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("reallocating-reservation-request", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Specification {@link #specification} is not ready.
     */
    @Entity
    @DiscriminatorValue("SpecificationNotReadyReport")
    public static class SpecificationNotReadyReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.specification.Specification specification;

        public SpecificationNotReadyReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "specification-not-ready";
        }

        public SpecificationNotReadyReport(cz.cesnet.shongo.controller.booking.specification.Specification specification)
        {
            setSpecification(specification);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "specification_id")
        public cz.cesnet.shongo.controller.booking.specification.Specification getSpecification()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(specification);
        }

        public void setSpecification(cz.cesnet.shongo.controller.booking.specification.Specification specification)
        {
            this.specification = specification;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("specification", specification);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("specification-not-ready", userType, language, timeZone, getParameters());
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

        public SpecificationNotReadyException(cz.cesnet.shongo.controller.booking.specification.Specification specification)
        {
            SpecificationNotReadyReport report = new SpecificationNotReadyReport();
            report.setSpecification(specification);
            this.report = report;
        }

        public SpecificationNotReadyException(Throwable throwable, cz.cesnet.shongo.controller.booking.specification.Specification specification)
        {
            super(throwable);
            SpecificationNotReadyReport report = new SpecificationNotReadyReport();
            report.setSpecification(specification);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.booking.specification.Specification getSpecification()
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
     * The specification {@link #specification} is not supposed to be allocated.
     */
    @Entity
    @DiscriminatorValue("SpecificationNotAllocatableReport")
    public static class SpecificationNotAllocatableReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected cz.cesnet.shongo.controller.booking.specification.Specification specification;

        public SpecificationNotAllocatableReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "specification-not-allocatable";
        }

        public SpecificationNotAllocatableReport(cz.cesnet.shongo.controller.booking.specification.Specification specification)
        {
            setSpecification(specification);
        }

        @OneToOne(fetch = FetchType.LAZY)
        @Access(AccessType.FIELD)
        @JoinColumn(name = "specification_id")
        public cz.cesnet.shongo.controller.booking.specification.Specification getSpecification()
        {
            return cz.cesnet.shongo.PersistentObject.getLazyImplementation(specification);
        }

        public void setSpecification(cz.cesnet.shongo.controller.booking.specification.Specification specification)
        {
            this.specification = specification;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("specification", specification);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("specification-not-allocatable", userType, language, timeZone, getParameters());
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

        public SpecificationNotAllocatableException(cz.cesnet.shongo.controller.booking.specification.Specification specification)
        {
            SpecificationNotAllocatableReport report = new SpecificationNotAllocatableReport();
            report.setSpecification(specification);
            this.report = report;
        }

        public SpecificationNotAllocatableException(Throwable throwable, cz.cesnet.shongo.controller.booking.specification.Specification specification)
        {
            super(throwable);
            SpecificationNotAllocatableReport report = new SpecificationNotAllocatableReport();
            report.setSpecification(specification);
            this.report = report;
        }

        public cz.cesnet.shongo.controller.booking.specification.Specification getSpecification()
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
     * Duration {@link #duration} is longer than maximum {@link #maxDuration}.
     */
    @Entity
    @DiscriminatorValue("MaximumDurationExceededReport")
    public static class MaximumDurationExceededReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        protected org.joda.time.Period duration;

        protected org.joda.time.Period maxDuration;

        public MaximumDurationExceededReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "maximum-duration-exceeded";
        }

        public MaximumDurationExceededReport(org.joda.time.Period duration, org.joda.time.Period maxDuration)
        {
            setDuration(duration);
            setMaxDuration(maxDuration);
        }

        @Column(length = cz.cesnet.shongo.hibernate.PersistentPeriod.LENGTH)
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentPeriod.class)
        public org.joda.time.Period getDuration()
        {
            return duration;
        }

        public void setDuration(org.joda.time.Period duration)
        {
            this.duration = duration;
        }

        @Column(length = cz.cesnet.shongo.hibernate.PersistentPeriod.LENGTH)
        @org.hibernate.annotations.Type(value = cz.cesnet.shongo.hibernate.PersistentPeriod.class)
        public org.joda.time.Period getMaxDuration()
        {
            return maxDuration;
        }

        public void setMaxDuration(org.joda.time.Period maxDuration)
        {
            this.maxDuration = maxDuration;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("duration", duration);
            parameters.put("maxDuration", maxDuration);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("maximum-duration-exceeded", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link MaximumDurationExceededReport}.
     */
    public static class MaximumDurationExceededException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public MaximumDurationExceededException(MaximumDurationExceededReport report)
        {
            this.report = report;
        }

        public MaximumDurationExceededException(Throwable throwable, MaximumDurationExceededReport report)
        {
            super(throwable);
            this.report = report;
        }

        public MaximumDurationExceededException(org.joda.time.Period duration, org.joda.time.Period maxDuration)
        {
            MaximumDurationExceededReport report = new MaximumDurationExceededReport();
            report.setDuration(duration);
            report.setMaxDuration(maxDuration);
            this.report = report;
        }

        public MaximumDurationExceededException(Throwable throwable, org.joda.time.Period duration, org.joda.time.Period maxDuration)
        {
            super(throwable);
            MaximumDurationExceededReport report = new MaximumDurationExceededReport();
            report.setDuration(duration);
            report.setMaxDuration(maxDuration);
            this.report = report;
        }

        public org.joda.time.Period getDuration()
        {
            return getReport().getDuration();
        }

        public org.joda.time.Period getMaxDuration()
        {
            return getReport().getMaxDuration();
        }

        @Override
        public MaximumDurationExceededReport getReport()
        {
            return (MaximumDurationExceededReport) report;
        }
    }

    /**
     * User is not resource owner.
     */
    @Entity
    @DiscriminatorValue("UserNotOwnerReport")
    public static class UserNotOwnerReport extends cz.cesnet.shongo.controller.scheduler.SchedulerReport
    {
        public UserNotOwnerReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "user-not-owner";
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("user-not-owner", userType, language, timeZone, getParameters());
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


    /**
     * The resource {@link #resource} has limit of {@link maxLicencesPerRoom}.
     */
    @Entity
    @DiscriminatorValue("ResourceSingleRoomLimitExceededReport")
    public static class ResourceSingleRoomLimitExceededReport extends ResourceReport
    {
        protected Integer maxLicencesPerRoom;

        public ResourceSingleRoomLimitExceededReport()
        {
        }

        @Transient
        @Override
        public String getUniqueId()
        {
            return "resource-single-room-limit-exceeded";
        }

        public ResourceSingleRoomLimitExceededReport(cz.cesnet.shongo.controller.booking.resource.Resource resource, Integer maxLicencesPerRoom)
        {
            setResource(resource);
            setMaxLicencesPerRoom(maxLicencesPerRoom);
        }

        @Column
        public Integer getMaxLicencesPerRoom()
        {
            return maxLicencesPerRoom;
        }

        public void setMaxLicencesPerRoom(Integer maxLicencesPerRoom)
        {
            this.maxLicencesPerRoom = maxLicencesPerRoom;
        }

        @Transient
        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Transient
        @Override
        public int getVisibleFlags()
        {
            return VISIBLE_TO_USER | VISIBLE_TO_DOMAIN_ADMIN;
        }

        @Transient
        @Override
        public java.util.Map<String, Object> getParameters()
        {
            java.util.Map<String, Object> parameters = new java.util.HashMap<String, Object>();
            parameters.put("resource", resource);
            parameters.put("maxLicencesPerRoom", maxLicencesPerRoom);
            return parameters;
        }

        @Transient
        @Override
        public String getMessage(UserType userType, Language language, org.joda.time.DateTimeZone timeZone)
        {
            return cz.cesnet.shongo.controller.AllocationStateReportMessages.getMessage("resource-single-room-limit-exceeded", userType, language, timeZone, getParameters());
        }
    }

    /**
     * Exception for {@link ResourceSingleRoomLimitExceededReport}.
     */
    public static class ResourceSingleRoomLimitExceededException extends cz.cesnet.shongo.controller.scheduler.SchedulerException
    {
        public ResourceSingleRoomLimitExceededException(ResourceSingleRoomLimitExceededReport report)
        {
            this.report = report;
        }

        public ResourceSingleRoomLimitExceededException(Throwable throwable, ResourceSingleRoomLimitExceededReport report)
        {
            super(throwable);
            this.report = report;
        }

        public ResourceSingleRoomLimitExceededException(cz.cesnet.shongo.controller.booking.resource.Resource resource, Integer maxLicencesPerRoom)
        {
            ResourceSingleRoomLimitExceededReport report = new ResourceSingleRoomLimitExceededReport();
            report.setResource(resource);
            report.setMaxLicencesPerRoom(maxLicencesPerRoom);
            this.report = report;
        }

        public ResourceSingleRoomLimitExceededException(Throwable throwable, cz.cesnet.shongo.controller.booking.resource.Resource resource, Integer maxLicencesPerRoom)
        {
            super(throwable);
            ResourceSingleRoomLimitExceededReport report = new ResourceSingleRoomLimitExceededReport();
            report.setResource(resource);
            report.setMaxLicencesPerRoom(maxLicencesPerRoom);
            this.report = report;
        }

        public Integer getMaxLicencesPerRoom()
        {
            return getReport().getMaxLicencesPerRoom();
        }


        @Override
        public ResourceSingleRoomLimitExceededReport getReport()
        {
            return (ResourceSingleRoomLimitExceededReport) report;
        }
    }


    @Override
    protected void fillReportClasses()
    {
        addReportClass(UserNotAllowedReport.class);
        addReportClass(ResourceNotFoundReport.class);
        addReportClass(ResourceReport.class);
        addReportClass(ResourceNotAllocatableReport.class);
        addReportClass(ResourceAlreadyAllocatedReport.class);
        addReportClass(ResourceUnderMaintenanceReport.class);
        addReportClass(ResourceNotAvailableReport.class);
        addReportClass(ResourceRoomCapacityExceededReport.class);
        addReportClass(ResourceRecordingCapacityExceededReport.class);
        addReportClass(ResourceNotEndpointReport.class);
        addReportClass(ResourceMultipleRequestedReport.class);
        addReportClass(EndpointNotFoundReport.class);
        addReportClass(ExecutableReusingReport.class);
        addReportClass(RoomExecutableNotExistsReport.class);
        addReportClass(ExecutableInvalidSlotReport.class);
        addReportClass(ExecutableAlreadyUsedReport.class);
        addReportClass(CompartmentNotEnoughEndpointReport.class);
        addReportClass(CompartmentAssignAliasToExternalEndpointReport.class);
        addReportClass(ConnectionReport.class);
        addReportClass(ConnectionBetweenReport.class);
        addReportClass(ConnectionFromToReport.class);
        addReportClass(ConnectionToMultipleReport.class);
        addReportClass(ReservationRequestInvalidSlotReport.class);
        addReportClass(ReservationRequestDeniedReport.class);
        addReportClass(ReservationRequestDeniedAlreadyAllocatedReport.class);
        addReportClass(ReservationWithoutMandatoryUsageReport.class);
        addReportClass(ReservationReport.class);
        addReportClass(ReservationAlreadyUsedReport.class);
        addReportClass(ReservationReusingReport.class);
        addReportClass(ValueAlreadyAllocatedReport.class);
        addReportClass(ValueInvalidReport.class);
        addReportClass(ValueNotAvailableReport.class);
        addReportClass(ExecutableServiceInvalidSlotReport.class);
        addReportClass(RoomEndpointAlwaysRecordableReport.class);
        addReportClass(AllocatingResourceReport.class);
        addReportClass(AllocatingAliasReport.class);
        addReportClass(AllocatingValueReport.class);
        addReportClass(AllocatingRoomReport.class);
        addReportClass(AllocatingRecordingServiceReport.class);
        addReportClass(AllocatingCompartmentReport.class);
        addReportClass(AllocatingExecutableReport.class);
        addReportClass(SpecificationCheckingAvailabilityReport.class);
        addReportClass(FindingAvailableResourceReport.class);
        addReportClass(SortingResourcesReport.class);
        addReportClass(CollidingReservationsReport.class);
        addReportClass(ReallocatingReservationRequestsReport.class);
        addReportClass(ReallocatingReservationRequestReport.class);
        addReportClass(SpecificationNotReadyReport.class);
        addReportClass(SpecificationNotAllocatableReport.class);
        addReportClass(MaximumDurationExceededReport.class);
        addReportClass(UserNotOwnerReport.class);
    }
}
