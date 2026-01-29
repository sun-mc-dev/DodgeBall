package me.sunmc.dodgeball.listeners;

import me.sunmc.dodgeball.DodgeballPlugin;
import me.sunmc.dodgeball.api.events.GameStateChangeEvent;
import me.sunmc.dodgeball.game.*;
import me.sunmc.dodgeball.game.enums.GameState;
import me.sunmc.dodgeball.game.enums.GameTeam;
import me.sunmc.dodgeball.storage.GameFileStorage;
import me.sunmc.dodgeball.storage.user.User;
import me.sunmc.dodgeball.storage.user.storage.IUserStorage;
import me.sunmc.dodgeball.utility.DefaultSound;
import me.sunmc.dodgeball.utility.SoundHelper;
import me.sunmc.dodgeball.utility.TextureManager;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegister;
import me.sunmc.dodgeball.utility.autoregistry.AutoRegistry;
import me.sunmc.dodgeball.utility.messaging.MessageUtils;
import me.sunmc.dodgeball.utility.messaging.lang.MessageHelper;
import me.sunmc.dodgeball.utility.messaging.lang.MsgReplace;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Game-specific event listeners.
 * Modernized for Paper 1.21.11 with Adventure API.
 */
@AutoRegister(type = AutoRegistry.Type.LISTENER)
public class GameListener implements Listener {

    private final DodgeballPlugin plugin;
    private final GameFileStorage config;
    private final Map<UUID, Location> lastLocation;

    public GameListener(@NotNull DodgeballPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        this.lastLocation = new HashMap<>();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();

        Game game = GameHelper.getGameFromPlayer(player);
        if (game == null) {
            return;
        }

        this.callGameLeaveActions(player, game);
    }

    @EventHandler
    public void onWorldChangeEvent(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            Game game = GameHelper.getGameFromPlayer(player);
            if (game == null) {
                return;
            }

            MsgReplace[] replacements = {
                    new MsgReplace("player", player.getName()),
                    new MsgReplace("players_joined", game.getPlayers().size())
            };

            World world = player.getWorld();
            List<Player> players = world.getPlayers();

            GameState gameState = game.getGameState();
            if (event.getFrom().getName().equals(game.getWorldName())) {
                this.callGameLeaveActions(player, game);
                return;
            }

            // Check if we can start countdown
            int playersSize = game.getPlayers().size();
            if (gameState == GameState.PRE_WAITING && playersSize >= this.config.getInt("game.players-to-start-timer")) {
                game.setDelayedGameState(GameState.WAITING, 2);
            }

            // Player joined the game
            if (gameState.isWaiting()) {
                if (playersSize >= this.config.getInt("game.max-players")) {
                    game.setDelayedGameState(GameState.ACTIVE, 2);
                }

                for (Player target : players) {
                    MessageHelper.sendMessage(target, "join-game.announce-join", replacements);
                }
            }
            SoundHelper.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING);
        }, 4);
    }

    private void callGameLeaveActions(@NotNull Player player, @NotNull Game game) {
        MsgReplace[] replacements = {
                new MsgReplace("player", player.getName()),
                new MsgReplace("players_joined", game.getPlayers().size() - 1)
        };

        GamePlayer gamePlayer = game.getGamePlayer(player);
        if (gamePlayer != null) {
            gamePlayer.getTeam().removeAlivePlayer(player.getUniqueId());
        }

        GameState gameState = game.getGameState();
        for (GamePlayer targetGamePlayer : game.getPlayers()) {
            Player targetPlayer = targetGamePlayer.toPlayer();
            if (targetPlayer == null) {
                continue;
            }

            if (gameState.isWaiting()) {
                MessageHelper.sendMessage(targetPlayer, "join-game.announce-quit", replacements);
            }
        }

        game.removePlayer(player);

        // Check if one team has no alive players left
        if (gameState == GameState.ACTIVE) {
            for (GameTeam team : game.getPlayableTeams()) {
                if (team.getAlivePlayers().isEmpty()) {
                    game.setDelayedGameState(GameState.END, 3);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        final Game game = event.getAffectedGame();
        final GameState newState = event.getNewState();

        if (newState == GameState.ACTIVE) {
            this.callActiveActions(game);
        }

        if (newState == GameState.END) {
            this.callEndActions(game);
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof Snowball snowball) {
            if (snowball.getShooter() instanceof Player player) {
                Game game = GameHelper.getGameFromPlayer(player);
                if (game == null) {
                    return;
                }

                GamePlayer gamePlayer = game.getGamePlayer(player);
                if (gamePlayer == null) {
                    return;
                }

                gamePlayer.incrementBallsThrown();
            }
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        final Item item = event.getItem();
        if (event.getEntity() instanceof Player player) {
            ItemStack itemStack = item.getItemStack();
            if (itemStack.getType() == Material.SNOWBALL) {
                Game game = GameHelper.getGameFromPlayer(player);
                if (game == null) {
                    return;
                }

                game.removeSnowball(item);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }

        if (!(snowball.getShooter() instanceof Player shooter)) {
            return;
        }

        Game game = GameHelper.getGameFromPlayer(shooter);
        if (game == null) {
            return;
        }

        // Snowball hit a block
        if (event.getHitBlock() != null) {
            ItemStack snowballItem = TextureManager.getTexturedItem("snowball_default", 1);

            Item itemEntity = snowball.getWorld().dropItemNaturally(snowball.getLocation(), snowballItem);
            game.addSnowball(itemEntity);
            snowball.remove();
            return;
        }

        // Snowball hit a player
        if (event.getHitEntity() instanceof Player hit) {
            GamePlayer gamePlayer = game.getGamePlayer(shooter);
            if (gamePlayer == null) {
                return;
            }
            gamePlayer.incrementHits();

            // Notify players
            SoundHelper.playSound(shooter, Sound.BLOCK_NOTE_BLOCK_PLING);
            SoundHelper.playDefaultSound(hit, DefaultSound.ERROR);
            MessageHelper.sendMessage(shooter, "game.player-hit", new MsgReplace("hit", hit.getName()));
            MessageHelper.sendMessage(hit, "game.died", new MsgReplace("killer", shooter.getName()));

            // Update statistics
            this.incrementDeath(hit);
            this.incrementKill(shooter);

            // Give coins
            int coins = this.config.getInt("game.coins-on-kill");
            this.incrementCoins(shooter, coins);

            Component actionBar = MessageUtils.colorMessage("&6+" + coins + " coins");
            shooter.sendActionBar(actionBar);

            // Check if player's team is eliminated
            GamePlayer gamePlayerHit = game.getGamePlayer(hit);
            if (gamePlayerHit == null) {
                return;
            }

            GameTeam hitTeam = gamePlayerHit.getTeam();
            hitTeam.removeAlivePlayer(gamePlayerHit.getUuid());
            hit.setGameMode(GameMode.SPECTATOR);
            gamePlayerHit.setTeamId("spectator");

            if (hitTeam.getAlivePlayers().isEmpty()) {
                game.setDelayedGameState(GameState.END, 3);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework firework) {
            if (firework.hasMetadata("victory_nodamage")) {
                event.setCancelled(true);
            }
        }
    }

    private void incrementCoins(@NotNull Player player, int coins) {
        IUserStorage userStorage = this.plugin.getUserStorage();
        User user = userStorage.getCachedUser(player.getUniqueId());

        if (user != null) {
            user.setCoins(user.getCoins() + coins);
        }
    }

    private void incrementKill(@NotNull Player player) {
        IUserStorage userStorage = this.plugin.getUserStorage();
        User user = userStorage.getCachedUser(player.getUniqueId());

        if (user != null) {
            user.setLifetimeKills(user.getLifetimeKills() + 1);
        }
    }

    private void incrementDeath(@NotNull Player player) {
        IUserStorage userStorage = this.plugin.getUserStorage();
        User user = userStorage.getCachedUser(player.getUniqueId());

        if (user != null) {
            user.setLifetimeDeaths(user.getLifetimeDeaths() + 1);
        }
    }

    private void callEndActions(@NotNull Game game) {
        GameTeam winningTeam = null;

        for (GameTeam target : game.getPlayableTeams()) {
            if (!target.getAlivePlayers().isEmpty()) {
                winningTeam = target;
                break;
            }
        }

        if (winningTeam == null) {
            return;
        }

        // Send messages and clear inventory
        List<GamePlayer> players = game.getPlayers();
        for (GamePlayer gamePlayer : players) {
            Player player = gamePlayer.toPlayer();
            if (player == null) {
                continue;
            }

            player.getInventory().clear();
            SoundHelper.playSound(player, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE);
            MessageHelper.sendTitle(player, "game.victory-title", "game.victory-subtitle",
                    new MsgReplace("winning_team", winningTeam.getDisplayName()));
        }

        // Spawn visual effects
        for (UUID uuid : winningTeam.getAlivePlayers()) {
            Player target = Bukkit.getPlayer(uuid);
            if (target == null) {
                continue;
            }

            Location location = target.getLocation();
            this.spawnVictoryFireworks(location);
            this.showVictoryParticles(location);
        }

        // Execute commands
        ConsoleCommandSender sender = Bukkit.getConsoleSender();
        for (String command : this.config.getStringList("game.victory.commands")) {
            if (!command.isEmpty()) {
                Bukkit.dispatchCommand(sender, command.replace("/", ""));
            }
        }

        // Clean up snowballs
        game.getSnowballs().forEach(Item::remove);

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            for (GamePlayer gamePlayer : players) {
                Player player = gamePlayer.toPlayer();
                if (player == null) {
                    continue;
                }

                Location location = this.plugin.getLobbyConfig().getLocation("environment.lobby", false);
                if (location == null) {
                    player.kick(Component.text("Game Over"));
                } else {
                    player.teleport(location);
                }

                game.resetValues();
            }
        }, 20 * 6);
    }

    private void spawnVictoryFireworks(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        Firework firework = world.spawn(location, Firework.class);
        firework.setMetadata("victory_nodamage", new FixedMetadataValue(this.plugin, true));

        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        fireworkMeta.addEffect(FireworkEffect.builder()
                .withColor(Color.GREEN)
                .flicker(true)
                .build());
        firework.setFireworkMeta(fireworkMeta);

        new BukkitRunnable() {
            @Override
            public void run() {
                firework.detonate();
            }
        }.runTaskLater(this.plugin, 40);
    }

    private void showVictoryParticles(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.FIREWORK, location, 100, 1, 1, 1, 0.1);
    }

    private void callActiveActions(@NotNull Game game) {
        final List<GamePlayer> players = game.getPlayers();
        int size = players.size();

        // Split players into teams
        int teamOneIndex = 0;
        int teamTwoIndex = size / 2 + size % 2;
        Collections.shuffle(players);

        List<GameTeam> playableTeams = game.getPlayableTeams();
        GameTeam teamOne = playableTeams.get(0);
        GameTeam teamTwo = playableTeams.get(1);

        for (int i = 0; i < players.size(); i++) {
            if (i % 2 == 0) {
                GamePlayer player = players.get(teamOneIndex++);
                player.setTeamId(teamOne.getId());
                teamOne.addAlivePlayer(player.getUuid());
            } else {
                GamePlayer player = players.get(teamTwoIndex++);
                player.setTeamId(teamTwo.getId());
                teamTwo.addAlivePlayer(player.getUuid());
            }
        }

        // Spawn players in their areas
        for (GameTeam team : playableTeams) {
            BlockLocationPair playableTeamArea = team.getPlayableTeamArea();
            Location positionOne = playableTeamArea.getPositionOne();
            Location positionTwo = playableTeamArea.getPositionTwo();

            if (positionOne == null || positionTwo == null) {
                continue;
            }

            PlayerSpawner spawner = new PlayerSpawner(game.getWorldName(),
                    positionOne.clone().add(0, 1.5, 0), positionTwo);

            List<Player> spawnPlayers = team.getAlivePlayers()
                    .stream()
                    .map(Bukkit::getPlayer)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            spawner.spawnPlayers(spawnPlayers);
        }

        // Give items and effects
        ItemStack snowball = TextureManager.getTexturedItem("snowball_default",
                this.config.getInt("game.snowballs-start-amount"));

        int teamOneSize = teamOne.getAlivePlayers().size();
        int teamTwoSize = teamTwo.getAlivePlayers().size();
        PotionEffect speedEffect = new PotionEffect(PotionEffectType.SPEED,
                PotionEffect.INFINITE_DURATION, 0);

        for (GamePlayer gamePlayer : players) {
            Player player = gamePlayer.toPlayer();
            if (player == null) {
                return;
            }

            // Give speed to smaller team
            String gameTeamId = gamePlayer.getTeam().getId();
            if (teamOneSize < teamTwoSize && gameTeamId.equals(teamOne.getId())) {
                player.addPotionEffect(speedEffect);
            } else if (teamTwoSize < teamOneSize && gameTeamId.equals(teamTwo.getId())) {
                player.addPotionEffect(speedEffect);
            }

            player.getInventory().addItem(snowball);

            MessageHelper.sendMessage(player, "game.game-started.message");
            MessageHelper.sendTitle(player, "game.game-started.title", "game.game-started.subtitle");
            SoundHelper.playSound(player, Sound.ENTITY_ENDER_DRAGON_SHOOT);
            SoundHelper.playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        Game game = GameHelper.getGameFromPlayer(player);
        if (game == null) {
            return;
        }

        GamePlayer gamePlayer = game.getGamePlayer(player);
        if (gamePlayer == null) {
            return;
        }

        // Check if player is in their team area
        GameTeam team = gamePlayer.getTeam();
        BlockLocationPair playableArea = team.getPlayableTeamArea();
        Location location = player.getLocation();
        Location positionOne = playableArea.getPositionOne();
        Location positionTwo = playableArea.getPositionTwo();

        if (positionOne != null && positionTwo != null && !this.isInsideCuboid(location, positionOne, positionTwo)) {
            Location lastLoc = this.lastLocation.get(uuid);
            if (lastLoc != null) {
                player.teleport(lastLoc);
            }
            return;
        }

        this.lastLocation.put(uuid, location.clone());
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (this.isGameWorld(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        World world = event.getWorld();

        for (Game game : this.plugin.getGames().values()) {
            if (game.getWorldName().equals(world.getName())) {
                event.setCancelled(true);
                break;
            }
        }
    }

    private boolean isInsideCuboid(@NotNull Location base, @NotNull Location loc1, @NotNull Location loc2) {
        int x1 = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int y1 = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int z1 = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int x2 = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int y2 = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int z2 = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        return (base.getBlockX() >= x1 && base.getBlockX() <= x2 &&
                base.getBlockY() >= y1 && base.getBlockY() <= y2 &&
                base.getBlockZ() >= z1 && base.getBlockZ() <= z2);
    }

    private boolean isGameWorld(@NotNull Player player) {
        Game game = GameHelper.getGameFromPlayer(player);
        return game != null && player.getWorld().getName().equalsIgnoreCase(game.getWorldName());
    }
}