package cz.cesnet.shongo.oldapi.util;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.oldapi.annotation.Required;
import cz.cesnet.shongo.oldapi.annotation.Transient;

import java.util.Set;

/**
 * Represents a type for a API that can be serialized
 * to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class IdentifiedChangeableObject extends IdentifiedObject
        implements ChangesTracking.Changeable
{
    /**
     * Storage for properties.
     */
    private PropertyStorage propertyStorage;

    /**
     * @see ChangesTracking
     */
    private ChangesTracking changesTracking;

    /**
     * @return {@link #propertyStorage}
     */
    protected PropertyStorage getPropertyStorage()
    {
        if (propertyStorage == null) {
            propertyStorage = new PropertyStorage(getChangesTracking());
        }
        return propertyStorage;
    }

    @Override
    public void setId(String id)
    {
        super.setId(id);
        getChangesTracking().markPropertyAsFilled("id");
    }

    @Override
    @Transient
    public ChangesTracking getChangesTracking()
    {
        if (changesTracking == null) {
            changesTracking = new ChangesTracking();
        }
        return changesTracking;
    }

    /**
     * @param changesTracking from which should be {@link #changesTracking} filled
     */
    public void setChangesTracking(ChangesTracking changesTracking)
    {
        getChangesTracking().fill(changesTracking);
    }

    /**
     * Checks whether all properties with {@link Required} annotation are marked as filled and
     * sets the {@link ChangesTracking#collectionItemIsByDefaultNew} to true (recursive).
     *
     * @throws CommonReportSet.ClassCollectionRequiredException,CommonReportSet.ClassAttributeRequiredException
     *          when some required field isn't filled
     */
    public void setupNewEntity()
    {
        ChangesTracking.setupNewEntity(this);
    }

    /**
     * Remove all filled/changed marks for properties/collections. It is useful when you don't want to send all
     * data but only the modified (marked as filled).
     */
    public void startModification()
    {
        ChangesTracking changesTracking = getChangesTracking();
        if (changesTracking.isPropertyFilled("id")) {
            changesTracking.clearMarks();
            changesTracking.markPropertyAsFilled("id");
        }
        else {
            changesTracking.clearMarks();
        }
    }

    /**
     * @return {@link ChangesTracking#getFilledProperties()}
     */
    @Transient
    public Set<String> getFilledProperties()
    {
        return getChangesTracking().getFilledProperties();
    }

    /**
     * @see ChangesTracking#isPropertyFilled(String)
     */
    public boolean isPropertyFilled(String property)
    {
        return getChangesTracking().isPropertyFilled(property);
    }

    /**
     * @see ChangesTracking#isPropertyItemMarkedAsNew(String, Object)
     */
    public boolean isPropertyItemMarkedAsNew(String property, Object item)
    {
        return getChangesTracking().isPropertyItemMarkedAsNew(property, item);
    }

    /**
     * @see ChangesTracking#getPropertyItemsMarkedAsDeleted(String)
     */
    public <T> Set<T> getPropertyItemsMarkedAsDeleted(String property)
    {
        return getChangesTracking().getPropertyItemsMarkedAsDeleted(property);
    }
}
