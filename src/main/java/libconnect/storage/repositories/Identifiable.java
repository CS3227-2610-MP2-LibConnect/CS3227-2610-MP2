package libconnect.storage.repositories;

/** Defines the stable identifier required by repository implementations. */
public interface Identifiable {
    /**
     * Returns the stable identifier for this entity.
     *
     * @return the entity's stable identifier.
     */
    String getId();
}
