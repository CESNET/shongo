package cz.cesnet.shongo.connector.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface which can be implemented to allow resuming of downloading by {@link java.io.InputStream}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ResumeSupport
{
    /**
     * @param oldInputStream which can be closed
     * @param offset         at which the {@link java.io.InputStream} should be reopened
     * @return newly opened {@link java.io.InputStream}
     */
    InputStream reopenInputStream(InputStream oldInputStream, int offset) throws IOException;
}
