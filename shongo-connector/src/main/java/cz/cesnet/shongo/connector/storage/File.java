package cz.cesnet.shongo.connector.storage;

/**
 * Represents information about a single file in the storage.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class File
{
    /**
     * Id of the folder in which the file is located.
     */
    private String folderId;

    /**
     * Name of the file, which is unique in folder.
     */
    private String fileName;

    /**
     * Constructor.
     */
    public File()
    {
    }

    /**
     * Constructor.
     *
     * @param folderId sets the {@link #folderId}
     * @param fileName sets the {@link #fileName}
     */
    public File(String folderId, String fileName)
    {
        this.folderId = folderId;
        this.fileName = fileName;
    }

    /**
     * @return {@link #fileName}
     */
    public String getFileName()
    {
        return fileName;
    }

    /**
     * @param fileName sets the {@link #fileName}
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    /**
     * @return {@link #folderId}
     */
    public String getFolderId()
    {
        return folderId;
    }

    /**
     * @param folderId sets the {@link #folderId}
     */
    public void setFolderId(String folderId)
    {
        this.folderId = folderId;
    }
}
