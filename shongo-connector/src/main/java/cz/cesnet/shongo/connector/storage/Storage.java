package cz.cesnet.shongo.connector.storage;

import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.jade.CommandException;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
    String createFolder(Folder folder) throws FileNotFoundException;

    /**
     * Delete existing folder in the storage.
     *
     * @param folderId id of existing folder which should be deleted
     */
    void deleteFolder(String folderId);

    /**
     * Test if folder already exists.
     *
     * @param folderId folder to be tested
     * @return {@value true} if folder exists, {@value false} otherwise
     */
    public boolean folderExists(String folderId);

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
     * @param file          information about the new file
     * @param fileContent   input stream from which the file content can be read
     * @param resumeSupport to be used for re-opening given {@code fileContent}
     */
    void createFile(File file, InputStream fileContent, ResumeSupport resumeSupport);

    /**
     * Delete existing file in the storage.
     *
     * @param folderId
     * @param fileName
     */
    void deleteFile(String folderId, String fileName);

    /**
     * Test if file already exists.
     *
     * @param file file to be tested
     * @return {@value true} if file exists, {@value false} otherwise
     */
    public boolean fileExists(File file);

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
     * @param fileName id of the file in the folder
     * @return {@link InputStream} with the file content
     */
    InputStream getFileContent(String folderId, String fileName);

    /**
     * Returns downloadable url of file
     *
     * @return file downloadableUrl
     */
    String getFileDownloadableUrl(String folderId, String fileName) throws MalformedURLException;

    /**
     * Validate if file has given size in Bytes.
     *
     * @param file to be verified
     * @param expectedSize expected file size in Bytes
     * @return
     */
    boolean validateFile(File file, long expectedSize);

    /**
     *
     * @param file
     * @param fileId
     * @return
     */
    boolean filenameEqualsFileId(File file, String fileId);
}