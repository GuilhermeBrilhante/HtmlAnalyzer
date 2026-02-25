import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Analyzes HTML content from a URL and finds the text at the deepest nesting level.
 * 
 * This implementation follows Clean Code principles and Domain-Driven Design (DDD)
 * practices as used at Axur, separating infrastructure concerns (HTTP fetching)
 * from domain logic (HTML parsing)
 */
public class HtmlAnalyzer {

    // Infrastructure layer constants
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 299;

    // Domain layer constants
    private static final String URL_CONNECTION_ERROR = "URL connection error";
    private static final String MALFORMED_HTML = "malformed HTML";
    private static final int MIN_TAG_LENGTH = 3; // Minimum valid tag: <x>

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(URL_CONNECTION_ERROR);
            return;
        }

        HtmlAnalyzer analyzer = new HtmlAnalyzer();
        String result = analyzer.analyze(args[0]);
        System.out.println(result);
    }

    /**
     * Analyzes the HTML content from the given URL and returns the deepest text
     * 
     * @param urlString the URL to fetch and analyze
     * @return the deepest text found or an error message
     */
    public String analyze(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return URL_CONNECTION_ERROR;
        }

        HttpURLConnection connection = null;
        try {
            connection = createConnection(urlString);
            try (BufferedReader reader = createReader(connection)) {
                return findDeepestText(reader);
            }
        } catch (IOException e) {
            return URL_CONNECTION_ERROR;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Creates and configures an HTTP connection
     * Infrastructure layer handles external communication
     */
    private HttpURLConnection createConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);

        int responseCode = connection.getResponseCode();
        if (!isSuccessfulResponse(responseCode)) {
            throw new IOException("HTTP error: " + responseCode);
        }

        return connection;
    }

    /**
     * Creates a BufferedReader from the connection with proper charset handling.
     */
    private BufferedReader createReader(HttpURLConnection connection) throws IOException {
        return new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
        );
    }

    private boolean isSuccessfulResponse(int responseCode) {
        return responseCode >= HTTP_SUCCESS_MIN && responseCode <= HTTP_SUCCESS_MAX;
    }

    /**
     * Parses HTML content and finds the deepest text node
     * Domain layer  core business logic
     */
    private String findDeepestText(BufferedReader reader) throws IOException {
        HtmlParsingState state = new HtmlParsingState();

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty()) {
                continue;
            }

            LineType lineType = classifyLine(trimmedLine);
            
            if (!processLine(trimmedLine, lineType, state)) {
                return MALFORMED_HTML;
            }
        }

        return getResult(state);
    }

    /**
     * Classifies a line of HTML into one of the valid types
     * Application layer orchestrates domain operations
     */
    private LineType classifyLine(String line) {
        if (line.startsWith("</") && line.endsWith(">")) {
            return LineType.CLOSING_TAG;
        }
        if (line.startsWith("<") && line.endsWith(">")) {
            return LineType.OPENING_TAG;
        }
        if (!line.startsWith("<")) {
            return LineType.TEXT;
        }
        return LineType.INVALID;
    }

    /**
     * Processes a single line based on its type
     * Delegates to specialized handlers following Single Responsibility Principle
     */
    private boolean processLine(String line, LineType type, HtmlParsingState state) {
        switch (type) {
            case OPENING_TAG:
                return processOpeningTag(line, state);
            case CLOSING_TAG:
                return processClosingTag(line, state);
            case TEXT:
                processText(line, state);
                return true;
            default:
                return false;
        }
    }

    private boolean processOpeningTag(String line, HtmlParsingState state) {
        String tagName = extractTagName(line);
        if (tagName.isEmpty()) {
            return false;
        }
        state.pushTag(tagName);
        return true;
    }

    private boolean processClosingTag(String line, HtmlParsingState state) {
        if (line.length() <= MIN_TAG_LENGTH) {
            return false;
        }

        String tagName = extractClosingTagName(line);
        if (tagName.isEmpty()) {
            return false;
        }
        
        if (!state.canCloseTag(tagName)) {
            return false;
        }
        
        state.popTag();
        return true;
    }

    private void processText(String text, HtmlParsingState state) {
        state.updateDeepestText(text);
    }

    /**
     * Extracts tag name from opening tag (e.g., "<div>" -> "div")
     */
    private String extractTagName(String openingTag) {
        if (openingTag.length() <= 2) { // "<>" has length 2
            return "";
        }
        return openingTag.substring(1, openingTag.length() - 1);
    }

    /**
     * Extracts tag name from closing tag (e.g., "</div>" -> "div")
     */
    private String extractClosingTagName(String closingTag) {
        if (closingTag.length() <= MIN_TAG_LENGTH) { // "</>" has length 3
            return "";
        }
        return closingTag.substring(2, closingTag.length() - 1);
    }

    /**
     * Validates parsing state and returns the final result
     */
    private String getResult(HtmlParsingState state) {
        if (state.hasUnclosedTags()) {
            return MALFORMED_HTML;
        }
        if (!state.hasFoundText()) {
            return MALFORMED_HTML;
        }
        return state.getDeepestText();
    }

    /**
     * Represents the type of content in an HTML line.
     */
    private enum LineType {
        OPENING_TAG,
        CLOSING_TAG,
        TEXT,
        INVALID
    }

    /**
     * Encapsulates the state during HTML parsing
     * Domain model  represents the core concept of parsing state
     * 
     * Tracks open tags and identifies the deepest text node following
     * the Single Responsibility Principle
     */
    private static class HtmlParsingState {
        private final Deque<String> openTags = new ArrayDeque<>();
        private int currentDepth = 0;
        private int maxDepth = -1;
        private String deepestText = null;

        /**
         * Pushes a new tag into the stack and increments depth
         */
        void pushTag(String tagName) {
            openTags.push(tagName);
            currentDepth++;
        }

        /**
         * Pops a tag from the stack and decrements depth
         */
        void popTag() {
            openTags.pop();
            currentDepth--;
        }

        /**
         * Checks if a closing tag matches the most recent opening tag
         */
        boolean canCloseTag(String tagName) {
            return !openTags.isEmpty() && openTags.peek().equals(tagName);
        }

        /**
         * Updates the deepest text if current depth exceeds previous maximum
         * return text at deepest level
         */
        void updateDeepestText(String text) {
            if (currentDepth > maxDepth) {
                maxDepth = currentDepth;
                deepestText = text;
            }
        }

        /**
         * Checks if there are unclosed tags, indicates malformed HTML
         */
        boolean hasUnclosedTags() {
            return !openTags.isEmpty();
        }

        /**
         * Checks if any text was found during parsing
         */
        boolean hasFoundText() {
            return deepestText != null;
        }

        /**
         * Returns the deepest text found
         */
        String getDeepestText() {
            return deepestText;
        }
    }
    }
