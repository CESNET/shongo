package cz.cesnet.shongo.connector.storage;

import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.jade.CommandException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Interface for Storage
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Storage
{
    /**
     * Create a new folder in the storage.
     *
     * @param folder information about the new folder, if parentFolderId is {@coe null}, folder is created in /
     * @return id of the new folder
     */
    String createFolder(Folder folder);

    /**
     * Delete existing folder in the storage.
     *
     * @param folderId id of existing folder which should be deleted
     */
    void deleteFolder(String folderId);

    /**
     * List folders in specified parent folder or root folders in the storage.
     *
     * @param folderId   id of the parent folder which contains requested folders which or {@code null}
     * @param folderName substring which must be contained in {@link Folder#folderName} or {@code null}
     * @return list of {@link Folder}s
     */
    List<Folder> listFolders(String folderId, String folderName);

    /**
     * Set user permissions for existing folder and all its' files in the storage.
     *
     * @param folderId        id of existing folder for which the user permissions should be set
     * @param userPermissions map of {@link RecordingFolder.UserPermission} by user-id
     */
    void setFolderPermissions(String folderId, Map<String, RecordingFolder.UserPermission> userPermissions)
            throws CommandException;

    /**
     * Create a new file in existing folder in the storage.
     *
     * @param file        information about the new file
     * @param fileContent input stream from which the file content can be read
     * @return id of the new file
     */
    String createFile(File file, InputStream fileContent);

    /**
     * Delete existing file in the storage.
     *
     * @param folderId
     * @param fileId
     */
    void deleteFile(String folderId, String fileId);

    /**
     * List files in specified folder in the storage.
     *
     * @param folderId id of the folder which contains requested files
     * @param fileName substring which must be contained in {@link File#fileName} or {@code null}
     * @return list of {@link File}s
     */
    List<File> listFiles(String folderId, String fileName);

    /**
     * Download content of existing file in the storage.
     *
     * @param folderId id of the folder in which the file is located
     * @param fileId   id of the file in the folder
     * @return {@link InputStream} with the file content
     */
    InputStream getFileContent(String folderId, String fileId);

    /**
     * Returns downloadable url of file
     *
     * @return file downloadableUrl
     */
    String getFileDownloadableUrl(String folderId, String fileId);

    /**
     * Represents information about a single folder in the storage.
     */
    class Folder
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

    /**
     * Represents information about a single file in the storage.
     */
    class File
    {
        /**
         * Id of the file or {@code null} if the id doesn't exist yet.
         */
        private String fileId;

        /**
         * Id of the folder in which the file is located.
         */
        private String folderId;

        /**
         * Name of the file.
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
         * @return {@link #fileId}
         */
        public String getFileId()
        {
            return fileId;
        }

        /**
         * @param fileId sets the {@link #fileId}
         */
        public void setFileId(String fileId)
        {
            this.fileId = fileId;
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
    }

    /**
     * Available permissions which the user can have to a folder and all its' files.
     *
    enum UserPermission
    {
        /**
         * User can list files in folder and read the files.
         *
        READ,

        /**
         * User can do everything like {@link #READ} and modify and delete the files.
         *
        WRITE
    }    */
}