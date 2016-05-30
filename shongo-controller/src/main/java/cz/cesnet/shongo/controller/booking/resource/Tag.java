package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
@Entity
public class Tag extends SimplePersistentObject {
    private String name;

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH, unique = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return tag converted capability to API
     */
    public final cz.cesnet.shongo.controller.api.Tag toApi()
    {
        cz.cesnet.shongo.controller.api.Tag api = new cz.cesnet.shongo.controller.api.Tag();
        toApi(api);
        return api;
    }

    public void toApi(cz.cesnet.shongo.controller.api.Tag tagApi)
    {
        tagApi.setId(ObjectIdentifier.formatId(this));
        tagApi.setName(name);
    }

    /**
     * @param tagApi
     * @return tag converted from API
     */
    public static Tag createFromApi(cz.cesnet.shongo.controller.api.Tag tagApi)
    {
        Tag tag = new Tag();
        tag.fromApi(tagApi);
        return tag;
    }

    public void fromApi(cz.cesnet.shongo.controller.api.Tag tagApi)
    {
        this.setName(tagApi.getName());
    }

}
