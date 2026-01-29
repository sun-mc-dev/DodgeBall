package me.sunmc.dodgeball;

import me.sunmc.dodgeball.api.DodgeballAPI;
import me.sunmc.dodgeball.api.DodgeballAPIImpl;
import me.sunmc.dodgeball.game.Game;
import me.sunmc.dodgeball.game.enums.GameState;
import me.sunmc.dodgeball.managers.ScoreboardManager;
import me.sunmc.dodgeball.managers.world.WorldManager;
import me.sunmc.dodgeball.storage.GameFileStorage;
import me.sunmc.dodgeball.storage.user.storage.IUserStorage;
import me.sunmc.dodgeball.storage.user.storage.LocalUserStorage;
import me.sunmc.dodgeball.storage.user.storage.MongoCredentials;
import me.sunmc.dodgeball.storage.user.storage.MongoUserStorage;
import me.sunmc.dodgeball.tickhandler.RunnableManager;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegistry;
import me.sunmc.dodgeball.utility.location.InvalidLocationParseException;
import me.sunmc.dodgeball.utility.location.LocationHelper;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Main plugin class for Dodgeball minigame.
 * Modernized for Paper 1.21.11+ with Adventure API support.
 */
public final class DodgeballPlugin extends JavaPlugin {

    private static DodgeballPlugin INSTANCE;
    private DodgeballAPIImpl api;

    private Map<String, Game> games;
    private FileConfiguration messagesConfig;
    private GameFileStorage pluginConfig;
    private GameFileStorage playerDataFileStorage;
    private GameFileStorage lobbyConfig;
    private GameFileStorage gameStorage;
    private IUserStorage userStorage;
    private RunnableManager runnableManager;
    private ScoreboardManager scoreboardManager;
    private WorldManager worldManager;

    public static DodgeballPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        final long time = System.currentTimeMillis();
        Logger log = this.getLogger();
        log.info(this.getPluginMeta().getName() + " is starting up...");

        INSTANCE = this;

        // 1. Load configs FIRST — most important (prevents NPEs on early abort)
        boolean freshSetup = setupConfigurations();
        if (freshSetup) {
            log.info("Detected fresh plugin start-up! All files were created from default configurations.");
        }

        // 2. Now safe to abort early if needed
        PluginManager pluginManager = this.getServer().getPluginManager();

        // Check PlaceholderAPI
        log.info("Hooking into PlaceholderAPI...");
        if (!pluginManager.isPluginEnabled("PlaceholderAPI")) {
            abort(pluginManager,
                    "*** PlaceholderAPI is not installed or not enabled! ***",
                    "*** The plugin will now be disabled, please install PlaceholderAPI! ***"
            );
            return;
        }

        new PlaceholderAPIExtension(this).register();
        log.info("Hooked extension into PlaceholderAPI!");

        // Enable user storage
        String storageType = this.pluginConfig.getString("storage").toLowerCase();
        if (storageType.isEmpty()) {
            storageType = "local";
        }

        if (storageType.equals("mongo")) {
            MongoCredentials credentials = new MongoCredentials(
                    this.pluginConfig.getString("mongo.ip"),
                    this.pluginConfig.getString("mongo.port"),
                    this.pluginConfig.getString("mongo.user"),
                    this.pluginConfig.getString("mongo.database"),
                    this.pluginConfig.getString("mongo.password"),
                    this.pluginConfig.getString("mongo.users-collection")
            );

            this.userStorage = new MongoUserStorage(this, credentials);
            log.info("MongoDB storage was selected for user storage.");
        } else {
            this.userStorage = new LocalUserStorage(this);
            log.info("Local storage was selected for user storage.");
        }

        // Initialize managers
        this.scoreboardManager = new ScoreboardManager(this);
        this.runnableManager = new RunnableManager(this);
        this.runnableManager.registerRunnable();
        this.worldManager = new WorldManager(this);

        // Load registered games
        this.games = new HashMap<>();
        ConfigurationSection gamesSection = this.gameStorage.getSection("registeredGames");
        if (gamesSection == null) {
            log.warning("The game_data.yml file is missing the 'registeredGames' section! Games could not be loaded in.");
        } else {
            for (String gameId : gamesSection.getKeys(false)) {
                Game game = this.loadInGame(gameId, gamesSection);
                if (game == null) {
                    continue;
                }
                this.games.put(gameId, game);
            }
        }

        int gamesSize = this.games.size();
        if (gamesSize == 0) {
            log.info("No registered games exist. View the plugin documentation on how to set up a new game.");
        } else {
            log.info(gamesSize + " game(s) were successfully registered.");
        }

        // Register commands and listeners
        log.info("Registering commands and listeners...");
        this.registerCommands();
        this.registerListeners(pluginManager);
        log.info("Commands and listeners were successfully registered!");

        // Start global runnables
        this.runnableManager.start("scoreboard", false);
        this.runnableManager.start("timer", false);

        log.info("Plugin has successfully loaded in " + (System.currentTimeMillis() - time) + "ms!");
    }

    @Override
    public void onDisable() {
        long time = System.currentTimeMillis();
        Logger log = this.getLogger();

        // Save all users asynchronously
        if (this.userStorage != null) {
            CompletableFuture<?>[] futures = this.userStorage.getUsers().values().stream()
                    .map(user -> this.userStorage.saveUserToStorage(user.getUuid()))
                    .toArray(CompletableFuture[]::new);

            CompletableFuture.allOf(futures).join();
            this.userStorage.handleShutdown();
            log.info("All users have been saved to storage.");
        }

        // Save all games
        if (this.games != null) {
            for (Game game : this.games.values()) {
                game.clearSnowballs();
                this.saveGameToStorage(game);
            }
        }

        // Safe save of game storage
        if (this.gameStorage != null) {
            this.gameStorage.save();
        }

        INSTANCE = null;
        log.info("The plugin was successfully shutdown in " + (System.currentTimeMillis() - time) + "ms!");
    }

    /**
     * Saves a game to storage.
     */
    private void saveGameToStorage(@NotNull Game game) {
        final String path = "registeredGames." + game.getGameId() + ".";
        this.gameStorage.setCache(path + "enabled", game.isEnabled());
        this.gameStorage.setCache(path + "worldName", game.getWorldName());
        this.gameStorage.setCache(path + "gameId", game.getGameId());
        this.gameStorage.setCache(path + "waitingLobbySpawn", LocationHelper.writeLocation(game.getWaitingLobbySpawn()));

        // Save teams
        game.getPlayableTeams().forEach(team -> {
            final String teamPath = path + "team." + team.getId() + ".";
            this.gameStorage.setCache(teamPath + "displayName", team.getDisplayName());
            this.gameStorage.setCache(teamPath + "prefix", team.getPrefix());
            this.gameStorage.setCache(teamPath + "color", team.getColor().toString());
            this.gameStorage.setCache(teamPath + "playable", team.isPlayable());

            var locationPair = team.getPlayableTeamArea();
            this.gameStorage.setCache(teamPath + "locationPair.one", LocationHelper.writeLocation(locationPair.getPositionOne()));
            this.gameStorage.setCache(teamPath + "locationPair.two", LocationHelper.writeLocation(locationPair.getPositionTwo()));
        });
    }

    /**
     * Loads a game from configuration.
     */
    private @Nullable Game loadInGame(@NotNull String gameId, @NotNull ConfigurationSection gamesSection) {
        final String path = gameId + ".";
        Logger logger = this.getLogger();

        boolean enabled = gamesSection.getBoolean(path + "enabled");
        String worldName = gamesSection.getString(path + "worldName", "");

        // Create and load the world
        this.worldManager.setupModifiedWorld(worldName);

        Location waitingLobbySpawn;
        try {
            waitingLobbySpawn = LocationHelper.parseLocation(gamesSection.getString(path + "waitingLobbySpawn", ""));
        } catch (InvalidLocationParseException exception) {
            logger.severe("The waiting lobby spawn location is broken in game " + gameId);
            exception.printStackTrace();
            return null;
        }

        // Create game
        Game game = new Game(this, gameId, worldName);
        game.setEnabled(enabled);
        game.setGameState(GameState.PRE_WAITING);
        game.setWaitingLobbySpawn(waitingLobbySpawn);

        // Load teams
        ConfigurationSection teamsSection = gamesSection.getConfigurationSection(path + "team");
        if (teamsSection != null) {
            game.loadTeamsFromConfig(teamsSection);
        }

        return game;
    }

    private void registerCommands() {
        for (Class<?> clazz : AutoRegistry.getClassesWithRegisterType(AutoRegistry.Type.COMMAND)) {
            AutoRegistry.register(clazz, DodgeballPlugin.class, this);
        }
    }

    private void registerListeners(@NotNull PluginManager pluginManager) {
        for (Class<?> clazz : AutoRegistry.getClassesWithRegisterType(AutoRegistry.Type.LISTENER)) {
            if (clazz.getInterfaces().length == 0) continue;

            try {
                Listener listener = (Listener) clazz.getConstructor(DodgeballPlugin.class).newInstance(this);
                pluginManager.registerEvents(listener, this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean setupConfigurations() {
        File configFile = new File(this.getDataFolder(), "config.yml");
        boolean fresh = !configFile.exists();

        if (fresh && !this.getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder!");
        }

        this.pluginConfig = loadOrCreate("config.yml");
        this.messagesConfig = loadOrCreateConfig("messages.yml");
        this.playerDataFileStorage = loadOrCreate("player_data.yml");
        this.gameStorage = loadOrCreate("game_data.yml");
        this.lobbyConfig = loadOrCreate("lobby.yml");

        return fresh;
    }

    private GameFileStorage loadOrCreate(String filename) {
        File file = setupFile(filename);
        return new GameFileStorage(file, YamlConfiguration.loadConfiguration(file));
    }

    private FileConfiguration loadOrCreateConfig(String filename) {
        File file = setupFile(filename);
        return YamlConfiguration.loadConfiguration(file);
    }

    private File setupFile(@NotNull String fileName) {
        File file = new File(this.getDataFolder(), fileName);
        if (file.exists()) return file;

        try (InputStream in = getResource(fileName)) {
            if (in == null) {
                getLogger().severe("Default resource not found in jar: " + fileName);
                file.createNewFile();
                return file;
            }

            if (file.createNewFile()) {
                try (OutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    // ────────────────────────────────────────────────
    // Getters (unchanged except minor formatting)
    // ────────────────────────────────────────────────

    private void abort(@NotNull PluginManager pm, @NotNull String... messages) {
        Logger log = getLogger();
        for (String msg : messages) {
            log.warning(msg);
        }

        // Try to save whatever we managed to load
        if (gameStorage != null) {
            try {
                gameStorage.save();
            } catch (Exception ignored) {
            }
        }

        pm.disablePlugin(this);
    }

    public DodgeballAPI getAPI() {
        return this.api;
    }

    public IUserStorage getUserStorage() {
        return this.userStorage;
    }

    public ScoreboardManager getScoreboardManager() {
        return this.scoreboardManager;
    }

    public RunnableManager getRunnableManager() {
        return this.runnableManager;
    }

    public WorldManager getWorldManager() {
        return this.worldManager;
    }

    public FileConfiguration getMessagesConfig() {
        return this.messagesConfig;
    }

    @NotNull
    @Override
    @Deprecated
    public FileConfiguration getConfig() {
        throw new UnsupportedOperationException("Use getPluginConfig() instead");
    }

    public GameFileStorage getPluginConfig() {
        return this.pluginConfig;
    }

    public GameFileStorage getLobbyConfig() {
        return this.lobbyConfig;
    }

    public GameFileStorage getGameStore() {
        return this.gameStorage;
    }

    public GameFileStorage getPlayerDataFileStorage() {
        return this.playerDataFileStorage;
    }

    public Game getGameById(String id) {
        return this.games != null ? this.games.get(id) : null;
    }

    public void addGame(@NotNull Game game) {
        if (this.games == null) this.games = new HashMap<>();
        this.games.putIfAbsent(game.getGameId(), game);
    }

    public void removeGame(@NotNull String gameId) {
        if (this.games != null) {
            this.games.remove(gameId);
        }
        if (this.gameStorage != null) {
            this.gameStorage.set("registeredGames." + gameId, null);
        }
    }

    public Map<String, Game> getGames() {
        return this.games != null ? this.games : Map.of();
    }
}