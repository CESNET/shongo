package cz.cesnet.shongo.connector.storage;

import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * {@link AbstractStorage} which is implemented by local folder which is published by Apache.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ApacheStorage extends AbstractStorage
{
    private static Logger logger = LoggerFactory.getLogger(ApacheStorage.class);

    /**
     * Maximum number of consequent resumings.
     */
    private final int MAX_RESUME_COUNT = 5;

    /**
     * Duration in milliseconds to sleep before resuming.
     */
    private final int BEFORE_RESUME_SLEEP = 100;

    /**
     * List of folders and delete (request from another Thread)
     */
    private List<String> foldersBeingDeleted = new CopyOnWriteArrayList<String>();

    /**
     * Map of files to be created or that are being copied now (entry key represents a fileId and entry value
     * represents a recordingFolderId of folder for the file to be created in)
     */
    private Map<String,String> filesBeingCreated = new ConcurrentHashMap<String, String>();

    /**
     * Constructor.
     *
     * @param url                     sets the {@link #url}
     * @param userInformationProvider sets the {@link #userInformationProvider}
     */
    public ApacheStorage(String url, String downloadableUrlBase, UserInformationProvider userInformationProvider)
    {
        super(url, downloadableUrlBase, userInformationProvider);
    }

    @Override
    public String createFolder(Folder folder)
    {
        String folderId = getChildId(folder.getParentFolderId(), folder.getFolderName());
        String folderUrl = getUrlFromId(folderId);
        java.io.File file = new java.io.File(folderUrl);
        if (file.exists()) {
            throw new RuntimeException("Directory '" + folderUrl + "' already exists.");
        }
        if (!file.mkdir()) {
            throw new RuntimeException("Directory '" + folderUrl + "' can't be created.");
        }
        return folderId;
    }

    @Override
    public void deleteFolder(String folderId)
    {
        foldersBeingDeleted.add(folderId);

        while(filesBeingCreated.containsValue(folderId)) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                break;
            }
        }

        String folderUrl = getUrlFromId(folderId);
        if (!deleteRecursive(new java.io.File(folderUrl))) {
            throw new RuntimeException("Directory '" + folderUrl + "' couldn't be deleted.");
        }

        foldersBeingDeleted.remove(folderId);
    }

    @Override
    public List<Folder> listFolders(String folderId, final String folderName)
    {
        String folderUrl = getUrlFromId(folderId);
        java.io.File ioParentFolder = new java.io.File(folderUrl);
        String[] ioFolders = ioParentFolder.list(new java.io.FilenameFilter()
        {
            @Override
            public boolean accept(java.io.File directory, String fileName)
            {
                java.io.File file = new java.io.File(directory, fileName);
                if (!file.isDirectory() || fileName.equals(".") || fileName.equals("..")) {
                    return false;
                }
                return folderName == null || StringUtils.containsIgnoreCase(fileName, folderName);
            }
        });
        List<Folder> folders = new LinkedList<Folder>();
        for (String ioFolder : ioFolders) {
            Folder folder = new Folder();
            folder.setFolderId(getChildId(folderId, ioFolder));
            folder.setParentFolderId(folderId);
            folder.setFolderName(ioFolder);
            folders.add(folder);
        }
        return folders;
    }

    @Override
    public void setFolderPermissions(String folderId, Map<String, RecordingFolder.UserPermission> userPermissions)
            throws CommandException
    {
        String folderUrl = getUrlFromId(folderId);
        String permissionFileUrl = getChildUrl(folderUrl, ".htaccess");

        StringBuilder permissionFileContent = new StringBuilder();
        permissionFileContent.append("require user");
        for (String userId : new TreeSet<String>(userPermissions.keySet())) {
            UserInformation userInformation = getUserInformation(userId);
            for (String principalName : new TreeSet<String>(userInformation.getPrincipalNames())) {
                permissionFileContent.append(" ");
                permissionFileContent.append(principalName);
            }
        }
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(permissionFileUrl));
            out.print(permissionFileContent);
        }
        catch (FileNotFoundException exception) {
            throw new RuntimeException("File '" + permissionFileUrl + "' couldn't be written.", exception);
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    public void createFile(File file, InputStream fileContent, ResumeSupport resumeSupport)
    {
        String folderId = file.getFolderId();
        String folderUrl = getUrlFromId(folderId);
        String fileName = file.getFileName();
        String fileUrl = getChildUrl(folderUrl, fileName);
        if (new java.io.File(fileUrl).exists()) {
            throw new RuntimeException("File '" + fileUrl + "' already exists.");
        }

        if (folderId != null) {
            filesBeingCreated.put(fileName, folderId);
        }
        try {
            int fileContentIndex = 0;
            OutputStream fileOutputStream = new FileOutputStream(fileUrl);
            byte[] buffer = new byte[4096];
            while (true) {
                // Read next bytes into buffer
                int readResumeCount = MAX_RESUME_COUNT;
                int bytesRead;
                while (true) {
                    try {
                        bytesRead = fileContent.read(buffer, 0, buffer.length);
                        readResumeCount = MAX_RESUME_COUNT;
                        break;
                    }
                    catch (IOException exception) {
                        // Check if resume isn't available
                        if (resumeSupport == null || --readResumeCount <= 0) {
                            throw exception;
                        }

                        String message = "Creation of file " + folderUrl + "/" + fileName + ": ";
                        logger.warn(message + "Reading data failed at " + fileContentIndex + ".", exception);

                        // Wait before resuming
                        try {
                            Thread.sleep(BEFORE_RESUME_SLEEP);
                        }
                        catch (InterruptedException sleepException) {
                            logger.warn("Thread.sleep", sleepException);
                        }

                        // Reopen file content stream
                        logger.info(message + "Trying to resume the reading the data at {}...", fileContentIndex);
                        try {
                            fileContent = resumeSupport.reopenInputStream(fileContent, fileContentIndex);
                            logger.info(message + "Resume succeeded, continuing in file creation...");
                        }
                        catch (Exception resumeException) {
                            throw new RuntimeException("Reopening input stream failed for creation of file " +
                                    folderUrl + "/" + fileName + ".", resumeException);
                        }
                    }
                    catch (Exception exception) {
                        throw new RuntimeException("Reading input stream failed for creation of file " +
                                folderUrl + "/" + fileName + " at " + fileContentIndex + ".", exception);
                    }
                }

                // Check for end of file content
                if (bytesRead == -1) {
                    break;
                }

                // Check if folder isn't already deleted
                if (foldersBeingDeleted.contains(file.getFolderId())) {
                    logger.warn("Creation of file " + folderUrl + "/" + fileName +
                            " has been stopped because folder " + folderId + " is being deleted.");
                    break;
                }

                // Write bytes from buffer
                int writeResumeCount = MAX_RESUME_COUNT;
                while (true) {
                    try {
                        fileOutputStream.write(buffer, 0, bytesRead);
                        writeResumeCount = MAX_RESUME_COUNT;
                        break;
                    }
                    catch (IOException exception) {
                        // Close output stream
                        fileOutputStream.close();

                        // Check if resume isn't available
                        if (--writeResumeCount <= 0) {
                            throw exception;
                        }

                        // Resume writing
                        try {
                            fileOutputStream = new FileOutputStream(fileUrl);
                        }
                        catch (Exception resumeException) {
                            throw new RuntimeException("Reopening output stream failed for creation of file " +
                                    file.getFolderId() + ":" + file.getFileName() + ".", resumeException);
                        }
                        logger.warn("Writing file {}:{} failed at {}, resuming...", new Object[]{
                                file.getFolderId(), file.getFileName(), fileContentIndex
                        });
                        try {
                            Thread.sleep(BEFORE_RESUME_SLEEP);
                        }
                        catch (InterruptedException sleepException) {
                            logger.warn("Thread.sleep", sleepException);
                        }
                    }
                }

                // Move file content
                fileContentIndex += bytesRead;
            }
            fileContent.close();
            fileOutputStream.close();
        }
        catch (IOException exception) {
            throw new RuntimeException("File '" + fileUrl + "' couldn't be created.", exception);
        }
        finally {
            filesBeingCreated.remove(file.getFileName());
        }
    }

    @Override
    public void deleteFile(String folderId, String fileName)
    {
        String folderUrl = getUrlFromId(folderId);
        String fileUrl = getChildUrl(folderUrl, fileName);
        java.io.File file = new java.io.File(fileUrl);
        if (!file.delete()) {
            throw new RuntimeException("File'" + fileUrl + "' couldn't be deleted.");
        }
    }

    @Override
    public List<File> listFiles(String folderId, String fileName)
    {
        String folderUrl = getUrlFromId(folderId);
        java.io.File ioFolder = new java.io.File(folderUrl);
        if (!ioFolder.exists()) {
            throw new RuntimeException("Folder '" + folderUrl + "' doesn't exist.");
        }
        String[] ioFiles = ioFolder.list(new java.io.FilenameFilter()
        {
            @Override
            public boolean accept(java.io.File directory, String fileName)
            {
                java.io.File file = new java.io.File(directory, fileName);
                if (!file.isFile()) {
                    return false;
                }
                return fileName == null || StringUtils.containsIgnoreCase(file.getName(), fileName);
            }
        });
        List<File> files = new LinkedList<File>();
        for (String ioFile : ioFiles) {
            File file = new File();
            file.setFolderId(folderId);
            file.setFileName(ioFile);
            files.add(file);
        }
        return files;
    }

    @Override
    public InputStream getFileContent(String folderId, String fileName)
    {
        String folderUrl = getUrlFromId(folderId);
        String fileUrl = getChildUrl(folderUrl, fileName);

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(fileUrl);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("File " + fileUrl + " doesn't exist.");
        }

        return inputStream;
    }

    @Override
    public String getFileDownloadableUrl(String folderId, String fileName)
    {
        String fileUrl = getChildUrl(folderId, fileName);
        String downloadableUrl = this.getDownloadableUrlBase() + "/" + fileUrl;
        return downloadableUrl;
    }

    private String getUrlFromId(String id)
    {
        if (id == null) {
            return getUrl();
        }
        else {
            return getChildUrl(getUrl(), id);
        }
    }

    private static String getChildId(String parentId, String childName)
    {
        if (parentId == null) {
            return childName;
        }
        else {
            return parentId + "/" + childName;
        }
    }


    static String getChildUrl(String parentUrl, String childName)
    {
        StringBuilder childUrl = new StringBuilder();
        childUrl.append(parentUrl);
        if (childUrl.charAt(childUrl.length() - 1) != '/') {
            childUrl.append("/");
        }
        childUrl.append(childName);
        return childUrl.toString();
    }

    /**
     * Delete file or folder recursive.
     *
     * @param file file or folder to be deleted
     * @return true whether deletion succeeded, false otherwise
     */
    public static boolean deleteRecursive(java.io.File file)
    {
        deleteContentRecursive(file);
        return file.delete();
    }

    /**
     * Delete file or folder recursive.
     *
     * @param file file or folder to be deleted
     * @return true whether deletion succeeded, false otherwise
     */
    public static boolean deleteContentRecursive(java.io.File file)
    {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (String child : children) {
                boolean success = deleteRecursive(new java.io.File(file, child));
                if (!success) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean fileExists(File file)
    {
        String folderUrl = getUrlFromId(file.getFolderId());
        String fileUrl = getChildUrl(folderUrl, file.getFileName());
        return new java.io.File(fileUrl).exists();
    }
}
