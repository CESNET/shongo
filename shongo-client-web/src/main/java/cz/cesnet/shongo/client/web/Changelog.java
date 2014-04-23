package cz.cesnet.shongo.client.web;

import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Changelog extends LinkedList<Changelog.Version>
{
    private static Pattern PATTERN_VERSION = Pattern.compile("(\\d+\\.\\d+\\.\\d+) \\((.+)\\)");
    private static Pattern PATTERN_CHANGE = Pattern.compile("\\s*\\*\\s*(.+)");

    private Changelog()
    {
        File changelogFile = new File("../CHANGELOG");
        if (!changelogFile.exists()) {
            return;
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(changelogFile));
            try {
                String line = bufferedReader.readLine();
                Version currentVersion = null;
                StringBuilder currentChange = null;
                while (line != null) {
                    Matcher versionMatcher = PATTERN_VERSION.matcher(line);
                    if (versionMatcher.matches()) {
                        if (currentChange != null) {
                            currentVersion.addChange(currentChange.toString());
                            currentChange = null;
                        }
                        currentVersion = new Version();
                        currentVersion.setName(versionMatcher.group(1));
                        currentVersion.setDateTime(DateTime.parse(versionMatcher.group(2)));
                        add(currentVersion);
                    }
                    else {
                        Matcher changeMatcher = PATTERN_CHANGE.matcher(line);
                        if (changeMatcher.matches() && currentVersion != null) {
                            if (currentChange != null) {
                                currentVersion.addChange(currentChange.toString());
                            }
                            currentChange = new StringBuilder();
                            currentChange.append(changeMatcher.group(1).trim());
                        }
                        else {
                            if (currentChange != null) {
                                currentChange.append(" ");
                                currentChange.append(line.trim());
                            }
                        }
                    }
                    line = bufferedReader.readLine();
                }
                if (currentChange != null) {
                    currentVersion.addChange(currentChange.toString());
                }
            }
            finally {
                bufferedReader.close();
            }
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static Changelog instance;

    public static Changelog getInstance()
    {
        if (instance == null) {
            instance = new Changelog();
        }
        return instance;
    }

    public static class Version
    {
        private String name;

        private DateTime dateTime;

        private List<String> changes = new LinkedList<String>();

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public DateTime getDateTime()
        {
            return dateTime;
        }

        public void setDateTime(DateTime dateTime)
        {
            this.dateTime = dateTime;
        }

        public List<String> getChanges()
        {
            return changes;
        }

        public void addChange(String change)
        {
            changes.add(change);
        }
    }
}
