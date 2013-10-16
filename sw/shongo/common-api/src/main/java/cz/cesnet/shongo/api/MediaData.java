package cz.cesnet.shongo.api;

import cz.cesnet.shongo.AliasType;
import jade.content.Concept;
import org.apache.tika.mime.MediaType;

/**
 * Custom media data.
 * <p/>
 * Typically used for uploading or downloading some content (images, documents, etc.) with content type information.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class MediaData extends AbstractComplexType implements Concept
{
    /**
     * Type of the data.
     */
    private MediaType type;

    /**
     * The content. To be interpreted according to the content type.
     */
    private byte[] data;

    /**
     * Constructor.
     */
    public MediaData()
    {
    }

    /**
     * Constructor.
     *
     * @param type sets the {@link #type}
     * @param data sets the {@link #data}
     */
    public MediaData(MediaType type, byte[] data)
    {
        this.type = type;
        this.data = data;
    }

    /**
     * @return {@link #type}
     */
    public MediaType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(MediaType type)
    {
        this.type = type;
    }

    /**
     * @return {@link #data}
     */
    public byte[] getData()
    {
        return data;
    }

    /**
     * @param data sets the {@link #data}
     */
    public void setData(byte[] data)
    {
        this.data = data;
    }

    private static final String TYPE = "type";
    private static final String DATA = "data";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TYPE, type.toString());
        dataMap.set(DATA, data);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        type = MediaType.parse(dataMap.getString(TYPE));
        data = dataMap.getByteArray(DATA);
    }
}
