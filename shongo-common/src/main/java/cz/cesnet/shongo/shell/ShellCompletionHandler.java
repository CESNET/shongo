package cz.cesnet.shongo.shell;

import jline.console.ConsoleReader;
import jline.console.completer.CandidateListCompletionHandler;

import java.io.IOException;
import java.util.List;

/**
 * Shell completition handler.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
class ShellCompletionHandler extends CandidateListCompletionHandler
{
    /**
     * Perform completion by given candidates.
     *
     * @param reader
     * @param candidates
     * @param pos
     * @return true if completion was performed,
     *         false otherwise
     * @throws IOException
     */
    @Override
    public boolean complete(ConsoleReader reader, List<CharSequence> candidates, int pos) throws IOException
    {
        if (candidates.size() == 0) {
            return false;
        }

        String buffer = reader.getCursorBuffer().toString();
        String newBuffer;
        boolean showCandidates = false;

        // If there is only one completion, then fill in the buffer
        if (candidates.size() == 1) {
            newBuffer = candidates.get(0).toString();

            // fail if the only candidate is the same as the current buffer
            if (newBuffer.equals(buffer)) {
                return false;
            }
        }
        // Else get common prefix
        else {
            newBuffer = getUnambiguousCompletions(candidates);
            if (newBuffer == null) {
                newBuffer = buffer;
            }
            showCandidates = true;
        }

        // If new buffer is option append it to previous buffer content
        if (newBuffer.startsWith("-")) {
            // We must append right after the last space
            int position = buffer.lastIndexOf(" ");
            if (position == -1) {
                return true;
            }
            // If no character was typed for option, we don't want to auto-start the option
            // (user must type at least one character "-" to start auto completion)
            if (position < (buffer.length() - 1)) {
                newBuffer = buffer.substring(0, position + 1) + newBuffer;
            }
            else {
                newBuffer = buffer;
            }
        }

        // Set new buffer
        setBuffer(reader, newBuffer, pos);

        if (showCandidates) {
            // Print candidates
            printCandidates(reader, candidates);

            // Redraw the current console buffer
            reader.drawLine();
        }

        return true;
    }

    /**
     * Returns a root that matches all the {@link String} elements of the specified {@link List},
     * or null if there are no commonalities. For example, if the list contains
     * <i>foobar</i>, <i>foobaz</i>, <i>foobuz</i>, the method will return <i>foob</i>.
     */
    private String getUnambiguousCompletions(final List<CharSequence> candidates)
    {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        // convert to an array for speed
        String[] strings = candidates.toArray(new String[candidates.size()]);

        String first = strings[0];
        StringBuilder candidate = new StringBuilder();

        for (int i = 0; i < first.length(); i++) {
            if (startsWith(first.substring(0, i + 1), strings)) {
                candidate.append(first.charAt(i));
            }
            else {
                break;
            }
        }

        return candidate.toString();
    }

    /**
     * @return true is all the elements of <i>candidates</i> start with <i>starts</i>
     */
    private boolean startsWith(final String starts, final String[] candidates)
    {
        for (String candidate : candidates) {
            if (!candidate.startsWith(starts)) {
                return false;
            }
        }

        return true;
    }

}
