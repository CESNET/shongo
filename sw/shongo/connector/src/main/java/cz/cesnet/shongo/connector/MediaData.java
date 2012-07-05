package cz.cesnet.shongo.connector;

/**
 * Custom media data.
 *
 * Typically used for uploading or downloading some content (images, documents, etc.) with content type information.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class MediaData
{
    private ContentType contentType;
    private byte[] data;
    private CompressionAlgorithm compressionAlgorithm;

    /**
     * @return Algorithm used to compress data.
     */
    public CompressionAlgorithm getCompressionAlgorithm()
    {
        return compressionAlgorithm;
    }

    /**
     * @param compressionAlgorithm    Algorithm used to compress data.
     */
    public void setCompressionAlgorithm(CompressionAlgorithm compressionAlgorithm)
    {
        this.compressionAlgorithm = compressionAlgorithm;
    }

    /**
     * @return Type of the data.
     */
    public ContentType getContentType()
    {
        return contentType;
    }

    /**
     * @param contentType    Type of the data.
     */
    public void setContentType(ContentType contentType)
    {
        this.contentType = contentType;
    }

    /**
     * @return The content. To be interpreted according to the content type.
     */
    public byte[] getData()
    {
        return data;
    }

    /**
     * @param data    The content. To be interpreted according to the content type.
     */
    public void setData(byte[] data)
    {
        this.data = data;
    }
}
