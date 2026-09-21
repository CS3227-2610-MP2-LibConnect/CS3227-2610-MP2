package libconnect.util;

import java.util.Objects;

/**
 * Provides validation helpers shared by domain and persistence code.
 */
public final class ValidationUtils {
    private ValidationUtils() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Validates and trims a required text value.
     *
     * @param value the value to validate.
     * @param fieldName the field name used in the exception message.
     * @return the trimmed value.
     * @throws IllegalArgumentException if the value is null or blank.
     * @throws NullPointerException if {@code fieldName} is null.
     */
    public static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }

        return value.trim();
    }
}
