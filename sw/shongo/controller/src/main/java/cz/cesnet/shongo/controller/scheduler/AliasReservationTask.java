package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.cache.AvailableAlias;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.AliasSpecification;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.scheduler.report.NoAvailableAliasReport;
import cz.cesnet.shongo.fault.TodoImplementException;

import java.util.List;

/**
 * Represents {@link ReservationTask} for a {@link AliasSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AliasReservationTask extends ReservationTask<AliasSpecification, AliasReservation>
{
    /**
     * Constructor.
     *
     * @param specification sets the {@link #specification}
     * @param context       sets the {@link #context}
     */
    public AliasReservationTask(AliasSpecification specification, Context context)
    {
        super(specification, context);
    }

    @Override
    protected AliasReservation createReservation(AliasSpecification specification) throws ReportException
    {
        if (specification.getAliasType() != null) {
            throw new TodoImplementException("Allocating alias by alias type!");
        }

        AvailableAlias availableAlias = null;
        // First try to allocate alias from a resource capabilities
        if (specification.getResource() != null) {
            Resource resource = specification.getResource();
            List<AliasProviderCapability> aliasProviderCapabilities =
                    resource.getCapabilities(AliasProviderCapability.class);

            for (AliasProviderCapability aliasProviderCapability : aliasProviderCapabilities) {
                if (aliasProviderCapability.getTechnology().equals(specification.getTechnology())) {
                    availableAlias = getCache().getAvailableAlias(
                            aliasProviderCapability, getInterval(), getCacheTransaction());
                }
            }
        }
        // Allocate alias from all resources in the cache
        if (availableAlias == null) {
            availableAlias = getCache().getAvailableAlias(
                    getCacheTransaction(), specification.getTechnology(), getInterval());
        }
        if (availableAlias == null) {
            throw new NoAvailableAliasReport(specification.getTechnology()).exception();
        }
        AliasReservation aliasReservation = new AliasReservation();
        aliasReservation.setSlot(getInterval());
        aliasReservation.setAliasProviderCapability(availableAlias.getAliasProviderCapability());
        aliasReservation.setAlias(availableAlias.getAlias());
        return aliasReservation;
    }
}
