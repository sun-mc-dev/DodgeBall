package me.sunmc.dodgeball.storage.user;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.game.GamePlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds information about a User. In a bigger network,
 * this user to the difference from {@link GamePlayer}, does not
 * need to be specific to the game.
 */
public class User {

    private final @NonNull UUID uuid;
    private final @NonNull MetadataValues metadataValues;
    private @NonNull Map<String, Object> values;

    public User(@NonNull UUID uuid) {
        this.uuid = uuid;
        this.values = new HashMap<>();
        this.metadataValues = new MetadataValues(DodgeballPlugin.getInstance());
    }

    public int getLevel() {
        return this.getInt("level");
    }

    public void setLevel(int level) {
        this.set("level", level);
    }

    public int getCoins() {
        return this.getInt("coins");
    }

    public void setCoins(int level) {
        this.set("coins", level);
    }

    public int getLifetimeKills() {
        return this.getInt("lifetimeKills");
    }

    public void setLifetimeKills(int kills) {
        this.set("lifetimeKills", kills);
    }

    public int getLifetimeDeaths() {
        return this.getInt("lifetimeDeaths");
    }

    public void setLifetimeDeaths(int lifetimeDeaths) {
        this.set("lifetimeDeaths", lifetimeDeaths);
    }

    public void set(@NonNull String key, Object value) {
        this.values.put(key, value);
    }

    public String getString(@NonNull String key) {
        Object object = this.get(key);
        return object == null ? "" : String.valueOf(object);
    }

    public int getInt(@NonNull String key) {
        Object object = this.get(key);
        return object == null ? 0 : Integer.parseInt(String.valueOf(object));
    }

    public short getShort(@NonNull String key) {
        Object object = this.get(key);
        return object == null ? 0 : Short.parseShort(String.valueOf(object));
    }

    public double getDouble(@NonNull String key) {
        Object object = this.get(key);
        return object == null ? 0 : Double.parseDouble(String.valueOf(object));
    }

    public float getFloat(@NonNull String key) {
        Object object = this.get(key);
        return object == null ? 0 : Float.parseFloat(String.valueOf(object));
    }

    public long getLong(@NonNull String key) {
        Object object = this.get(key);
        return object == null ? 0 : Long.parseLong(String.valueOf(object));
    }

    public boolean getBoolean(@NonNull String key) {
        Object object = this.get(key);
        return object != null && Boolean.parseBoolean(String.valueOf(object));
    }

    private @Nullable Object get(@NonNull String key) {
        if (this.values.containsKey(key)) {
            return this.values.get(key);
        }

        return null;
    }

    public @NonNull Map<String, Object> getValues() {
        return this.values;
    }

    public void setValues(@NonNull Map<String, Object> values) {
        this.values = values;
    }

    public @NonNull MetadataValues getMetadataValues() {
        return this.metadataValues;
    }

    public @NonNull UUID getUuid() {
        return this.uuid;
    }
}
