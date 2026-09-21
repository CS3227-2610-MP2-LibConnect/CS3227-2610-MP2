package libconnect.storage.file.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import libconnect.storage.file.JsonReader;

/** Tests unit-level JSON parsing behavior used by file repositories. */
class JsonReaderTest {

    @Test
    void constructor_nullJson_throwsException() {
        assertThrows(NullPointerException.class, () -> new ExposedJsonReader(null, "test"));
    }

    @Test
    void constructor_nullDocumentName_throwsException() {
        assertThrows(NullPointerException.class, () -> new ExposedJsonReader("[]", null));
    }

    @Test
    void constructor_emptyDocumentName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new ExposedJsonReader("[]", ""));
    }

    @Test
    void constructor_whitespaceOnlyDocumentName_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> new ExposedJsonReader("[]", " \t\n"));
    }

    @Test
    void constructor_nonBlankDocumentName_constructsReader() {
        new ExposedJsonReader("[]", "test");
    }

    @Test
    void parseString_surroundingWhitespace_returnsString() {
        ExposedJsonReader reader = new ExposedJsonReader("  \"value\"  ", "test");

        assertEquals("value", reader.readString());
    }

    @Test
    void parseString_emptyString_returnsEmptyString() {
        ExposedJsonReader reader = new ExposedJsonReader("\"\"", "test");

        assertEquals("", reader.readString());
    }

    @Test
    void parseString_supportedEscapes_returnsUnescapedCharacters() {
        assertEquals("\"", new ExposedJsonReader("\"\\\"\"", "test").readString());
        assertEquals("\\", new ExposedJsonReader("\"\\\\\"", "test").readString());
        assertEquals("/", new ExposedJsonReader("\"\\/\"", "test").readString());
        assertEquals("\b", new ExposedJsonReader("\"\\b\"", "test").readString());
        assertEquals("\f", new ExposedJsonReader("\"\\f\"", "test").readString());
        assertEquals("\n", new ExposedJsonReader("\"\\n\"", "test").readString());
        assertEquals("\r", new ExposedJsonReader("\"\\r\"", "test").readString());
        assertEquals("\t", new ExposedJsonReader("\"\\t\"", "test").readString());
    }

    @Test
    void parseString_unicodeEscape_returnsDecodedCharacter() {
        ExposedJsonReader reader = new ExposedJsonReader("\"\\u0041\"", "test");

        assertEquals("A", reader.readString());
    }

    @Test
    void parseString_unterminatedString_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("\"unterminated", "test").readString());
    }

    @Test
    void parseString_controlCharacter_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("\"line\nbreak\"", "test").readString());
    }

    @Test
    void parseString_invalidEscape_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("\"\\q\"", "test").readString());
    }

    @Test
    void parseString_incompleteUnicodeEscape_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("\"\\u12\"", "test").readString());
    }

    @Test
    void parseString_nonHexadecimalUnicodeEscape_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("\"\\uZZZZ\"", "test").readString());
    }

    @Test
    void parseNullableString_nullValue_returnsNull() {
        assertNull(new ExposedJsonReader("null", "test").readNullableString());
    }

    @Test
    void parseNullableString_stringValue_returnsString() {
        assertEquals("value", new ExposedJsonReader("\"value\"", "test").readNullableString());
    }

    @Test
    void parseInteger_validValues_returnsIntegers() {
        assertEquals(0, new ExposedJsonReader("0", "test").readInteger());
        assertEquals(42, new ExposedJsonReader("42", "test").readInteger());
        assertEquals(-7, new ExposedJsonReader("-7", "test").readInteger());
    }

    @Test
    void parseInteger_missingDigits_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("-", "test").readInteger());
    }

    @Test
    void parseInteger_overflow_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("2147483648", "test").readInteger());
    }

    @Test
    void parseBoolean_validValues_returnsBooleans() {
        assertTrue(new ExposedJsonReader("true", "test").readBoolean());
        assertFalse(new ExposedJsonReader("false", "test").readBoolean());
    }

    @Test
    void parseBoolean_invalidValue_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("yes", "test").readBoolean());
    }

    @Test
    void skipValue_quotedValue_continuesToNextValue() {
        assertEquals("next", new ExposedJsonReader("\"ignored\",\"next\"", "test")
                .skipAndReadNextString());
    }

    @Test
    void skipValue_scalarValues_continuesToNextValue() {
        assertEquals("next", new ExposedJsonReader("123,\"next\"", "test")
                .skipAndReadNextString());
        assertEquals("next", new ExposedJsonReader("true,\"next\"", "test")
                .skipAndReadNextString());
        assertEquals("next", new ExposedJsonReader("null,\"next\"", "test")
                .skipAndReadNextString());
    }

    @Test
    void parseArray_emptyArray_returnsEmptyList() {
        assertTrue(new ExposedJsonReader("[]", "test").readIntegers().isEmpty());
    }

    @Test
    void parseArray_multipleValues_returnsAllValues() {
        assertEquals(List.of(1, 2, 3), new ExposedJsonReader("[1, 2, 3]", "test").readIntegers());
    }

    @Test
    void parseArray_whitespace_returnsAllValues() {
        assertEquals(List.of(1, 2), new ExposedJsonReader("[\n 1,\n 2 ]", "test").readIntegers());
    }

    @Test
    void parseArray_malformedElement_skipsElementAndReportsException() {
        ExposedJsonReader reader = new ExposedJsonReader("[1, invalid, 3]", "test");

        List<Integer> values = reader.readIntegers();

        assertEquals(List.of(1, 3), values);
        assertEquals(1, reader.getMalformedExceptions().size());
    }

    @Test
    void parseArray_malformedNestedElement_recoversAtNextElement() {
        ExposedJsonReader reader = new ExposedJsonReader("[\"valid\", {\"bad\": \"a,b\"}, \"later\"]",
                "test");

        assertEquals(List.of("valid", "later"), reader.readStrings());
        assertEquals(1, reader.getMalformedExceptions().size());
    }

    @Test
    void parseArray_nonArrayRoot_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("{}", "test").readIntegers());
    }

    @Test
    void parseArray_missingComma_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("[1 2]", "test").readIntegers());
    }

    @Test
    void parseArray_trailingComma_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("[1,]", "test").readIntegers());
    }

    @Test
    void parseArray_trailingContent_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ExposedJsonReader("[] extra", "test").readIntegers());
    }

    private static final class ExposedJsonReader extends JsonReader {
        private final List<IllegalArgumentException> malformedExceptions = new ArrayList<>();

        private ExposedJsonReader(String json, String documentName) {
            super(json, documentName);
        }

        private String readString() {
            return parseString();
        }

        private String readNullableString() {
            return parseNullableString();
        }

        private int readInteger() {
            return parseInteger();
        }

        private boolean readBoolean() {
            return parseBoolean();
        }

        private String skipAndReadNextString() {
            skipValue();
            expect(',');
            return parseString();
        }

        private List<Integer> readIntegers() {
            return parseArray(this::parseInteger, malformedExceptions::add);
        }

        private List<String> readStrings() {
            return parseArray(this::parseString, malformedExceptions::add);
        }

        private List<IllegalArgumentException> getMalformedExceptions() {
            return malformedExceptions;
        }
    }
}
