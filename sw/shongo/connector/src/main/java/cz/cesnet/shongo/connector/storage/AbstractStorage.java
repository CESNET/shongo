package cz.cesnet.shongo.connector.storage;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;

import java.io.InputStream;

/**
 * Abstract {@link Storage} implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractStorage implements Storage
{
    /**
     * URL of the {@link AbstractStorage}.
     */
    private String url;

    /**
     *
     */
    private String downloadableUrlBase;

    /**
     * @see UserInformationProvider
     */
    private UserInformationProvider userInformationProvider;

    /**
     * Constructor.
     *
     * @param url sets the {@link #url}
     * @param userInformationProvider sets the {@link #userInformationProvider}
     */
    protected AbstractStorage(String url, String downloadableUrlBase, UserInformationProvider userInformationProvider)
    {
        this.url = url;
        this.userInformationProvider = userInformationProvider;

        if (downloadableUrlBase.endsWith("/")) {
            downloadableUrlBase = downloadableUrlBase.substring(0,downloadableUrlBase.length() - 1);
        }
        this.downloadableUrlBase = downloadableUrlBase;

    }

    /**
     * @return {@link #url}
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @return {@link #downloadableUrlBase}
     */
    public String getDownloadableUrlBase()
    {
        return downloadableUrlBase;
    }

    /**
     * Create a new file in existing folder in the storage.
     *
     * @param file        information about the new file
     * @param fileContent input stream from which the file content can be read
     */
    public final void createFile(File file, InputStream fileContent)
    {
        createFile(file, fileContent, null);
    }

    /**
     * @param userId of user
     * @return {@link UserInformation} for user with given {@code userId}
     */
    protected UserInformation getUserInformation(String userId) throws CommandException
    {
        return userInformationProvider.getUserInformation(userId);
    }

    /**
     * Provider for {@link UserInformation}s by user-ids.
     */
    public static interface UserInformationProvider
    {
        /**
         * @param userId of user
         * @return {@link UserInformation} for user with given {@code userId}
         */
        public UserInformation getUserInformation(String userId) throws CommandException;
    }
}
