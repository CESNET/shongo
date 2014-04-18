package cz.cesnet.shongo.connector.api;

/**
 * A compression algorithm used to compress data files.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public enum CompressionAlgorithm
{
    /**
     * Zip compression, as specified by the application/zip MIME type.
     */
    ZIP,

    /**
     * A rar archive.
     */
    RAR,

    /**
     * A gzip-compressed tar archive.
     */
    TAR_GZIP,

    /**
     * A bzip2-compressed tar archive.
     */
    TAR_BZIP2,
}
