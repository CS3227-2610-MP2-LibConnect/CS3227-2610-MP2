package libconnect.storage.repositories;

/** Provides the stable identifier required by repository implementations. */
public interface Identifiable {
    /** Returns the stable identifier for this entity. */
    String getId();
}