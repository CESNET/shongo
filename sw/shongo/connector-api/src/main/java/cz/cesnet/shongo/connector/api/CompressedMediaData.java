package cz.cesnet.shongo.connector.api;

/**
 * {@link MediaData} which are compressed by any {@link CompressionAlgorithm}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompressedMediaData extends MediaData
{
    /**
     * Algorithm used to compress data.
     */
    private CompressionAlgorithm compressionAlgorithm;

    /**
     * @return {@link #compressionAlgorithm}
     */
    public CompressionAlgorithm getCompressionAlgorithm()
    {
        return compressionAlgorithm;
    }

    /**
     * @param compressionAlgorithm sets the {@link #compressionAlgorithm}
     */
    public void setCompressionAlgorithm(CompressionAlgorithm compressionAlgorithm)
    {
        this.compressionAlgorithm = compressionAlgorithm;
    }
}
