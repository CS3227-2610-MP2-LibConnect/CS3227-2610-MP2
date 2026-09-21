package libconnect.storage.file;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provides shared JSON serialization helpers for file-backed repositories.
 */
final class JsonWriter {
    private JsonWriter() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Serializes records as a readable JSON array.
     *
     * @param records the records to serialize.
     * @param objectSerializer the serializer for one record.
     * @param <T> the record type.
     * @return the JSON array representation.
     */
    static <T> String toJsonArray(List<T> records, Function<T, String> objectSerializer) {
        return records.stream()
                .map(objectSerializer)
                .collect(Collectors.joining(",\n", "[\n", "\n]\n"));
    }

    /**
     * Escapes a string for use as a JSON string value.
     *
     * @param value the string to escape.
     * @return the quoted JSON string.
     */
    static String quote(String value) {
        StringBuilder escapedValue = new StringBuilder(value.length() + 2);
        escapedValue.append('"');

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
            case '"' -> escapedValue.append("\\\"");
            case '\\' -> escapedValue.append("\\\\");
            case '\b' -> escapedValue.append("\\b");
            case '\f' -> escapedValue.append("\\f");
            case '\n' -> escapedValue.append("\\n");
            case '\r' -> escapedValue.append("\\r");
            case '\t' -> escapedValue.append("\\t");
            default -> {
                if (character < 0x20) {
                    escapedValue.append(String.format("\\u%04x", (int) character));
                } else {
                    escapedValue.append(character);
                }
            }
            }
        }

        return escapedValue.append('"').toString();
    }
}
