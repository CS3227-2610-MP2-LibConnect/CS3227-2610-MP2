package libconnect.storage.file;

import java.util.Objects;

/**
 * Provides shared low-level parsing for the limited JSON structures used by repositories.
 */
class JsonReader {
    protected final String json;
    protected int position;
    private final String documentName;

    /**
     * Creates a reader for JSON content.
     *
     * @param json the JSON content to parse.
     * @param documentName the document name used in error messages.
     */
    protected JsonReader(String json, String documentName) {
        this.json = Objects.requireNonNull(json, "json cannot be null");
        this.documentName = Objects.requireNonNull(documentName, "documentName cannot be null");
    }

    /**
     * Parses one JSON string value.
     *
     * @return the unescaped string value.
     * @throws IllegalArgumentException if the value is not a valid JSON string.
     */
    protected String parseString() {
        skipWhitespace();
        expect('"');
        StringBuilder value = new StringBuilder();

        while (position < json.length()) {
            char character = json.charAt(position++);
            if (character == '"') {
                return value.toString();
            }
            if (character == '\\') {
                value.append(parseEscape());
            } else if (character < 0x20) {
                throw new IllegalArgumentException("Control character in JSON string");
            } else {
                value.append(character);
            }
        }

        throw new IllegalArgumentException("Unterminated JSON string");
    }

    /**
     * Parses a JSON string or null value.
     *
     * @return the parsed string, or null for a JSON null value.
     * @throws IllegalArgumentException if the value is invalid.
     */
    protected String parseNullableString() {
        skipWhitespace();
        if (consumeLiteral("null")) {
            return null;
        }

        return parseString();
    }

    /**
     * Parses a JSON integer value.
     *
     * @return the parsed integer.
     * @throws IllegalArgumentException if the value is not a valid integer.
     */
    protected int parseInteger() {
        skipWhitespace();
        int start = position;

        if (position < json.length() && json.charAt(position) == '-') {
            position++;
        }
        while (position < json.length() && Character.isDigit(json.charAt(position))) {
            position++;
        }

        if (start == position || (json.charAt(start) == '-' && start + 1 == position)) {
            throw new IllegalArgumentException("Invalid integer in " + documentName + " JSON");
        }

        try {
            return Integer.parseInt(json.substring(start, position));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid integer in " + documentName + " JSON",
                    exception);
        }
    }

    /**
     * Parses a JSON boolean value.
     *
     * @return the parsed boolean.
     * @throws IllegalArgumentException if the value is not a boolean.
     */
    protected boolean parseBoolean() {
        skipWhitespace();
        if (consumeLiteral("true")) {
            return true;
        }
        if (consumeLiteral("false")) {
            return false;
        }

        throw new IllegalArgumentException("Invalid boolean in " + documentName + " JSON");
    }

    /**
     * Parses an escaped JSON string character.
     *
     * @return the unescaped character.
     * @throws IllegalArgumentException if the escape sequence is invalid.
     */
    protected char parseEscape() {
        if (position >= json.length()) {
            throw new IllegalArgumentException("Unterminated JSON escape sequence");
        }

        char escape = json.charAt(position++);
        return switch (escape) {
        case '"' -> '"';
        case '\\' -> '\\';
        case '/' -> '/';
        case 'b' -> '\b';
        case 'f' -> '\f';
        case 'n' -> '\n';
        case 'r' -> '\r';
        case 't' -> '\t';
        case 'u' -> parseUnicodeEscape();
        default -> throw new IllegalArgumentException("Invalid JSON escape sequence");
        };
    }

    /**
     * Parses a four-digit Unicode escape sequence.
     *
     * @return the decoded character.
     * @throws IllegalArgumentException if the sequence is invalid.
     */
    protected char parseUnicodeEscape() {
        if (position + 4 > json.length()) {
            throw new IllegalArgumentException("Incomplete JSON Unicode escape sequence");
        }

        String hexadecimalValue = json.substring(position, position + 4);
        position += 4;

        try {
            return (char) Integer.parseInt(hexadecimalValue, 16);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid JSON Unicode escape sequence", exception);
        }
    }

    /**
     * Skips one JSON value that is not needed by the repository.
     */
    protected void skipValue() {
        skipWhitespace();
        if (position >= json.length()) {
            throw new IllegalArgumentException("Missing JSON value in " + documentName + " record");
        }

        char nextCharacter = json.charAt(position);
        if (nextCharacter == '"') {
            parseString();
        } else {
            while (position < json.length() && ",}".indexOf(json.charAt(position)) < 0) {
                position++;
            }
        }
    }

    /**
     * Consumes a literal when it is next in the input.
     *
     * @param literal the literal to consume.
     * @return true if the literal was consumed.
     */
    protected boolean consumeLiteral(String literal) {
        if (json.startsWith(literal, position)) {
            position += literal.length();
            return true;
        }

        return false;
    }

    /**
     * Consumes a specific character when it is next in the input.
     *
     * @param expectedCharacter the character to consume.
     * @return true if the character was consumed.
     */
    protected boolean consume(char expectedCharacter) {
        if (position < json.length() && json.charAt(position) == expectedCharacter) {
            position++;
            return true;
        }

        return false;
    }

    /**
     * Requires a specific character at the current input position.
     *
     * @param expectedCharacter the character expected.
     * @throws IllegalArgumentException if the expected character is absent.
     */
    protected void expect(char expectedCharacter) {
        if (!consume(expectedCharacter)) {
            throw new IllegalArgumentException("Expected '" + expectedCharacter
                    + "' in " + documentName + " JSON");
        }
    }

    /**
     * Skips JSON whitespace characters.
     */
    protected void skipWhitespace() {
        while (position < json.length() && Character.isWhitespace(json.charAt(position))) {
            position++;
        }
    }

    /**
     * Ensures that no non-whitespace content remains after the root value.
     *
     * @throws IllegalArgumentException if trailing content exists.
     */
    protected void ensureEnd() {
        skipWhitespace();
        if (position != json.length()) {
            throw new IllegalArgumentException("Unexpected content after " + documentName + " JSON");
        }
    }
}
