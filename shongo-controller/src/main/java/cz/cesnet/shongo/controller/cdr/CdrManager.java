package cz.cesnet.shongo.controller.cdr;

import cz.cesnet.shongo.AbstractManager;
import cz.cesnet.shongo.controller.Controller;

import javax.persistence.EntityManager;

public class CdrManager extends AbstractManager {

    /**
     * Constructor.
     *
     * @param entityManager
     */
    public CdrManager(EntityManager entityManager) {
        super(entityManager);
    }

    public void createEntry(CdrEntry cdrEntry) {
        super.create(cdrEntry);
        Controller.loggerCdr.info("Created CDR entry (id: {}, start: {}, end: {})",
                new Object[]{cdrEntry.getId(), cdrEntry.getSlotStart(), cdrEntry.getSlotEnd()});
    }

    public void update(CdrEntry cdrEntry) {
        super.update(cdrEntry);
    }

    public void delete(CdrEntry cdrEntry)
    {
        super.delete(cdrEntry);
    }
}
