package cz.cesnet.shongo.connector.storage;

/**
 * Represents information about a single folder in the storage.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Folder
{
    /**
     * Id of the folder or {@code null} if the id doesn't exist yet.
     */
    private String folderId;

    /**
     * Id of the parent folder.
     */
    private String parentFolderId;

    /**
     * Name of the new folder.
     */
    private String folderName;

    /**
     * Constructor.
     */
    public Folder()
    {
    }

    /**
     * Constructor.
     *
     * @param parentFolderId sets the {@link #parentFolderId}
     * @param folderName     sets the {@link #folderName}
     */
    public Folder(String parentFolderId, String folderName)
    {
        this.parentFolderId = parentFolderId;
        this.folderName = folderName;
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

    /**
     * @return {@link #parentFolderId}
     */
    public String getParentFolderId()
    {
        return parentFolderId;
    }

    /**
     * @param parentFolderId sets the {@link #parentFolderId}
     */
    public void setParentFolderId(String parentFolderId)
    {
        this.parentFolderId = parentFolderId;
    }

    /**
     * @return {@link #folderName}
     */
    public String getFolderName()
    {
        return folderName;
    }

    /**
     * @param folderName sets the {@link #folderName}
     */
    public void setFolderName(String folderName)
    {
        this.folderName = folderName;
    }
}
