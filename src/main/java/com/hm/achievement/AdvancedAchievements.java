package com.hm.achievement;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.mcstats.MetricsLite;

import com.hm.achievement.category.MultipleAchievements;
import com.hm.achievement.category.NormalAchievements;
import com.hm.achievement.command.BookCommand;
import com.hm.achievement.command.CheckCommand;
import com.hm.achievement.command.DeleteCommand;
import com.hm.achievement.command.GiveCommand;
import com.hm.achievement.command.HelpCommand;
import com.hm.achievement.command.InfoCommand;
import com.hm.achievement.command.ListCommand;
import com.hm.achievement.command.StatsCommand;
import com.hm.achievement.command.TopCommand;
import com.hm.achievement.db.DatabasePoolsManager;
import com.hm.achievement.db.PooledRequestsSender;
import com.hm.achievement.db.SQLDatabaseManager;
import com.hm.achievement.listener.AchieveArrowListener;
import com.hm.achievement.listener.AchieveBedListener;
import com.hm.achievement.listener.AchieveBlockBreakListener;
import com.hm.achievement.listener.AchieveBlockPlaceListener;
import com.hm.achievement.listener.AchieveConnectionListener;
import com.hm.achievement.listener.AchieveConsumeListener;
import com.hm.achievement.listener.AchieveCraftListener;
import com.hm.achievement.listener.AchieveDeathListener;
import com.hm.achievement.listener.AchieveDropListener;
import com.hm.achievement.listener.AchieveEnchantListener;
import com.hm.achievement.listener.AchieveFishListener;
import com.hm.achievement.listener.AchieveHoeFertiliseFireworkMusicListener;
import com.hm.achievement.listener.AchieveItemBreakListener;
import com.hm.achievement.listener.AchieveKillListener;
import com.hm.achievement.listener.AchieveMilkListener;
import com.hm.achievement.listener.AchieveQuitListener;
import com.hm.achievement.listener.AchieveShearListener;
import com.hm.achievement.listener.AchieveSnowballEggListener;
import com.hm.achievement.listener.AchieveTameListener;
import com.hm.achievement.listener.AchieveTeleportRespawnListener;
import com.hm.achievement.listener.AchieveTradeAnvilBrewListener;
import com.hm.achievement.listener.AchieveXPListener;
import com.hm.achievement.listener.ListGUIListener;
import com.hm.achievement.runnable.AchieveDistanceRunnable;
import com.hm.achievement.runnable.AchievePlayTimeRunnable;
import com.hm.achievement.utils.FileManager;
import com.hm.achievement.utils.FileUpdater;
import com.hm.achievement.utils.UpdateChecker;
import com.hm.achievement.utils.YamlManager;

import net.milkbowl.vault.economy.Economy;

/**
 * Advanced Achievements enables unique and challenging achievements on your server. Try to collect as many as you can,
 * earn rewards, climb the rankings and receive RP books!
 * 
 * Some minor parts of the code and ideas are based on Achievement plugin by Death_marine and captainawesome7, under
 * Federation of Lost Lawn Chairs license (http://dev.bukkit.org/licenses/1332-federation-of-lost-lawn-chairs).
 * 
 * AdvancedAchievements is under GNU General Public License version 3. Please visit the plugin's GitHub for more
 * information : https://github.com/PyvesB/AdvancedAchievements
 * 
 * Official plugin's server: hellominecraft.fr
 * 
 * Bukkit project page: dev.bukkit.org/bukkit-plugins/advanced-achievements
 * 
 * Spigot project page: spigotmc.org/resources/advanced-achievements.6239
 * 
 * @since April 2015
 * @version 3.0.5
 * @author Pyves
 */
public class AdvancedAchievements extends JavaPlugin {

	// Used for Vault plugin integration.
	private Economy economy;

	// Listeners, to monitor events and manage stats.
	private AchieveConnectionListener connectionListener;
	private AchieveDeathListener deathListener;
	private AchieveArrowListener arrowListener;
	private AchieveSnowballEggListener snowballEggListener;
	private AchieveFishListener fishListener;
	private AchieveItemBreakListener itemBreakListener;
	private AchieveConsumeListener consumeListener;
	private AchieveShearListener shearListener;
	private AchieveMilkListener milkListener;
	private AchieveTradeAnvilBrewListener inventoryClickListener;
	private AchieveEnchantListener enchantmentListener;
	private AchieveBedListener bedListener;
	private AchieveXPListener xpListener;
	private AchieveDropListener dropListener;
	private AchieveHoeFertiliseFireworkMusicListener hoeFertiliseFireworkMusicListener;
	private AchieveTameListener tameListener;
	private AchieveBlockPlaceListener blockPlaceListener;
	private AchieveBlockBreakListener blockBreakListener;
	private AchieveKillListener killListener;
	private AchieveCraftListener craftListener;
	private AchieveQuitListener quitListener;
	private AchieveTeleportRespawnListener teleportRespawnListener;

	private ListGUIListener listGUIListener;

	// Additional classes related to plugin modules and commands.
	private AchievementRewards reward;
	private AchievementDisplay achievementDisplay;
	private GiveCommand giveCommand;
	private BookCommand bookCommand;
	private TopCommand topCommand;
	private ListCommand listCommand;
	private StatsCommand statsCommand;
	private InfoCommand infoCommand;
	private HelpCommand helpCommand;
	private CheckCommand checkCommand;
	private DeleteCommand deleteCommand;
	private UpdateChecker updateChecker;

	private YamlManager config;
	private YamlManager lang;
	private final FileManager fileManager;
	private final FileUpdater fileUpdater;

	// Database related.
	private final SQLDatabaseManager db;
	private final DatabasePoolsManager poolsManager;
	private PooledRequestsSender pooledRequestsSender;
	private int pooledRequestsTaskInterval;
	private boolean databaseBackup;
	private boolean asyncPooledRequestsSender;

	// Plugin options and various parameters.
	private String icon;
	private ChatColor color;
	private String chatHeader;
	private boolean restrictCreative;
	private Set<String> excludedWorldSet;
	private Set<String> disabledCategorySet;
	private boolean successfulLoad;
	private boolean overrideDisable;
	private int playtimeTaskInterval;
	private int distanceTaskInterval;

	// Plugin runnable classes.
	private AchieveDistanceRunnable achieveDistanceRunnable;
	private AchievePlayTimeRunnable achievePlayTimeRunnable;

	// Bukkit scheduler tasks.
	private BukkitTask pooledRequestsSenderTask;
	private BukkitTask playedTimeTask;
	private BukkitTask distanceTask;

	/**
	 * Constructor.
	 */
	public AdvancedAchievements() {

		overrideDisable = false;
		excludedWorldSet = new HashSet<>();
		disabledCategorySet = new HashSet<>();
		fileManager = new FileManager(this);
		db = new SQLDatabaseManager(this);
		poolsManager = new DatabasePoolsManager(this);
		fileUpdater = new FileUpdater(this);
	}

	/**
	 * Called when server is launched or reloaded.
	 */
	@Override
	public void onEnable() {

		// Start enabling plugin.
		long startTime = System.currentTimeMillis();

		configurationLoad(true);

		// Error while loading .yml files; do not do any further work.
		if (overrideDisable) {
			overrideDisable = false;
			return;
		}

		// Load Metrics Lite.
		try {
			MetricsLite metrics = new MetricsLite(this);
			metrics.start();
		} catch (IOException e) {
			this.getLogger().severe("Error while sending Metrics statistics.");
			successfulLoad = false;
		}

		if (databaseBackup && (!"mysql".equalsIgnoreCase(config.getString("DatabaseType", "sqlite"))
				|| !"postgresql".equalsIgnoreCase(config.getString("DatabaseType", "sqlite")))) {
			File backup = new File(this.getDataFolder(), "achievements.db.bak");
			// Only do a daily backup for the .db file.
			if (System.currentTimeMillis() - backup.lastModified() > 86400000 || backup.length() == 0) {
				this.getLogger().info("Backing up database file...");
				try {
					fileManager.backupFile("achievements.db");
				} catch (IOException e) {
					this.getLogger().log(Level.SEVERE, "Error while backing up database file: ", e);
					successfulLoad = false;
				}
			}
		}

		// Check for available plugin update.
		if (config.getBoolean("CheckForUpdate", true))
			updateChecker = new UpdateChecker(this);

		this.getLogger().info("Registering listeners...");

		// Register listeners so they can monitor server events; if there are no
		// config related achievements, listeners aren't registered.
		PluginManager pm = getServer().getPluginManager();
		if (!disabledCategorySet.contains(MultipleAchievements.PLACES.toString())) {
			blockPlaceListener = new AchieveBlockPlaceListener(this);
			pm.registerEvents(blockPlaceListener, this);
		}

		if (!disabledCategorySet.contains(MultipleAchievements.BREAKS.toString())) {
			blockBreakListener = new AchieveBlockBreakListener(this);
			pm.registerEvents(blockBreakListener, this);
		}

		if (!disabledCategorySet.contains(MultipleAchievements.KILLS.toString())) {
			killListener = new AchieveKillListener(this);
			pm.registerEvents(killListener, this);
		}

		if (!disabledCategorySet.contains(MultipleAchievements.CRAFTS.toString())) {
			craftListener = new AchieveCraftListener(this);
			pm.registerEvents(craftListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.DEATHS.toString())) {
			deathListener = new AchieveDeathListener(this);
			pm.registerEvents(deathListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.ARROWS.toString())) {
			arrowListener = new AchieveArrowListener(this);
			pm.registerEvents(arrowListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.SNOWBALLS.toString())
				|| !disabledCategorySet.contains(NormalAchievements.EGGS.toString())) {
			snowballEggListener = new AchieveSnowballEggListener(this);
			pm.registerEvents(snowballEggListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.FISH.toString())) {
			fishListener = new AchieveFishListener(this);
			pm.registerEvents(fishListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.ITEMBREAKS.toString())) {
			itemBreakListener = new AchieveItemBreakListener(this);
			pm.registerEvents(itemBreakListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.CONSUMEDPOTIONS.toString())
				|| !disabledCategorySet.contains(NormalAchievements.EATENITEMS.toString())) {
			consumeListener = new AchieveConsumeListener(this);
			pm.registerEvents(consumeListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.SHEARS.toString())) {
			shearListener = new AchieveShearListener(this);
			pm.registerEvents(shearListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.MILKS.toString())) {
			milkListener = new AchieveMilkListener(this);
			pm.registerEvents(milkListener, this);
		}

		if (config.getBoolean("CheckForUpdate", true)
				|| !disabledCategorySet.contains(NormalAchievements.CONNECTIONS.toString())) {
			connectionListener = new AchieveConnectionListener(this);
			pm.registerEvents(connectionListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.TRADES.toString())
				|| !disabledCategorySet.contains(NormalAchievements.ANVILS.toString())
				|| !disabledCategorySet.contains(NormalAchievements.BREWING.toString())) {
			inventoryClickListener = new AchieveTradeAnvilBrewListener(this);
			pm.registerEvents(inventoryClickListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.ENCHANTMENTS.toString())) {
			enchantmentListener = new AchieveEnchantListener(this);
			pm.registerEvents(enchantmentListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.LEVELS.toString())) {
			xpListener = new AchieveXPListener(this);
			pm.registerEvents(xpListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.BEDS.toString())) {
			bedListener = new AchieveBedListener(this);
			pm.registerEvents(bedListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.DROPS.toString())) {
			dropListener = new AchieveDropListener(this);
			pm.registerEvents(dropListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.TAMES.toString())) {
			tameListener = new AchieveTameListener(this);
			pm.registerEvents(tameListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.HOEPLOWING.toString())
				|| !disabledCategorySet.contains(NormalAchievements.FERTILISING.toString())
				|| !disabledCategorySet.contains(NormalAchievements.FIREWORKS.toString())
				|| !disabledCategorySet.contains(NormalAchievements.MUSICDISCS.toString())) {
			hoeFertiliseFireworkMusicListener = new AchieveHoeFertiliseFireworkMusicListener(this);
			pm.registerEvents(hoeFertiliseFireworkMusicListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.LEVELS.toString())
				|| !disabledCategorySet.contains(NormalAchievements.PLAYEDTIME.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEFOOT.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEPIG.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEHORSE.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEMINECART.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEBOAT.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEGLIDING.toString())) {
			quitListener = new AchieveQuitListener(this);
			pm.registerEvents(quitListener, this);
		}

		if (!disabledCategorySet.contains(NormalAchievements.DISTANCEFOOT.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEPIG.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEHORSE.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEMINECART.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEBOAT.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEGLIDING.toString())
				|| !disabledCategorySet.contains(NormalAchievements.ENDERPEARLS.toString())) {
			teleportRespawnListener = new AchieveTeleportRespawnListener(this);
			pm.registerEvents(teleportRespawnListener, this);
		}

		listGUIListener = new ListGUIListener(this);
		pm.registerEvents(listGUIListener, this);

		this.getLogger().info("Initialising database and launching scheduled tasks...");

		// Initialise the SQLite/MySQL/PostgreSQL database.
		db.initialise();

		// Error while loading database do not do any further work.
		if (overrideDisable) {
			overrideDisable = false;
			return;
		}

		pooledRequestsSender = new PooledRequestsSender(this);
		// Schedule a repeating task to group database queries for some frequent
		// events.
		pooledRequestsSenderTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(
				Bukkit.getPluginManager().getPlugin("AdvancedAchievements"), pooledRequestsSender,
				pooledRequestsTaskInterval * 40L, pooledRequestsTaskInterval * 20L);

		// Schedule a repeating task to monitor played time for each player (not
		// directly related to an event).
		if (!disabledCategorySet.contains(NormalAchievements.PLAYEDTIME.toString())) {
			achievePlayTimeRunnable = new AchievePlayTimeRunnable(this);
			playedTimeTask = Bukkit.getServer().getScheduler().runTaskTimer(
					Bukkit.getPluginManager().getPlugin("AdvancedAchievements"), achievePlayTimeRunnable,
					playtimeTaskInterval * 10L, playtimeTaskInterval * 20L);
		}

		// Schedule a repeating task to monitor distances travelled by each
		// player (not directly related to an event).
		if (!disabledCategorySet.contains(NormalAchievements.DISTANCEFOOT.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEPIG.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEHORSE.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEMINECART.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEBOAT.toString())
				|| !disabledCategorySet.contains(NormalAchievements.DISTANCEGLIDING.toString())) {
			achieveDistanceRunnable = new AchieveDistanceRunnable(this);
			distanceTask = Bukkit.getServer().getScheduler().runTaskTimer(
					Bukkit.getPluginManager().getPlugin("AdvancedAchievements"), achieveDistanceRunnable,
					distanceTaskInterval * 40L, distanceTaskInterval * 20L);
		}

		if (successfulLoad)
			this.getLogger().info("Plugin successfully enabled and ready to run! Took "
					+ (System.currentTimeMillis() - startTime) + "ms.");
		else
			this.getLogger().severe("Error(s) while loading plugin. Please view previous logs for more information.");
	}

	/**
	 * Load plugin configuration and set values to different parameters; load language file and backup configuration
	 * files. Register permissions. Initialise command modules.
	 */
	private void configurationLoad(boolean attemptUpdate) {

		successfulLoad = true;
		Logger logger = this.getLogger();

		logger.info("Backing up and loading configuration files...");

		try {
			config = fileManager.getNewConfig("config.yml");
		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, "Error while loading configuration file: ", e);
			successfulLoad = false;
		} catch (InvalidConfigurationException e) {
			logger.severe("Error while loading configuration file, disabling plugin.");
			logger.log(Level.SEVERE,
					"Verify your syntax by visiting yaml-online-parser.appspot.com and using the following logs: ", e);
			successfulLoad = false;
			overrideDisable = true;
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}

		try {
			lang = fileManager.getNewConfig(config.getString("LanguageFileName", "lang.yml"));
		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, "Error while loading language file: ", e);
			successfulLoad = false;
		} catch (InvalidConfigurationException e) {
			logger.severe("Error while loading language file, disabling plugin.");
			this.getLogger().log(Level.SEVERE,
					"Verify your syntax by visiting yaml-online-parser.appspot.com and using the following logs: ", e);
			successfulLoad = false;
			overrideDisable = true;
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}

		backupFiles();

		// Update configurations from previous versions of the plugin if server reload or restart.
		if (attemptUpdate) {
			fileUpdater.updateOldConfiguration();
			fileUpdater.updateOldLanguage();
		}

		logger.info("Loading configs, registering permissions and initialising command modules...");

		extractParameters();

		registerPermissions();

		initialiseCommands();

		// Reload achievements in distance, max level and play time runnables on plugin reload (when objects are null).
		if (achieveDistanceRunnable != null)
			achieveDistanceRunnable.extractAchievementsFromConfig();
		if (achievePlayTimeRunnable != null)
			achievePlayTimeRunnable.extractAchievementsFromConfig();
		if (xpListener != null)
			xpListener.extractAchievementsFromConfig();

		// Set to null in case user changed the option and did an /aach reload. We do not recheck for update on /aach
		// reload.
		if (!config.getBoolean("CheckForUpdate", true)) {
			updateChecker = null;
		}

		logAchievementStats();
	}

	/**
	 * Performs a backup of the language and configuration files.
	 */
	private void backupFiles() {

		try {
			fileManager.backupFile("config.yml");
		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, "Error while backing up configuration file: ", e);
			successfulLoad = false;
		}

		try {
			fileManager.backupFile(config.getString("LanguageFileName", "lang.yml"));
		} catch (IOException e) {
			this.getLogger().log(Level.SEVERE, "Error while backing up language file: ", e);
			successfulLoad = false;
		}
	}

	/**
	 * Extracts plugin parameters from the configuration file.
	 */
	@SuppressWarnings("unchecked")
	private void extractParameters() {

		icon = StringEscapeUtils.unescapeJava(config.getString("Icon", "\u2618"));
		color = ChatColor.getByChar(config.getString("Color", "5").toCharArray()[0]);
		chatHeader = ChatColor.GRAY + "[" + color + icon + ChatColor.GRAY + "] ";
		restrictCreative = config.getBoolean("RestrictCreative", false);
		databaseBackup = config.getBoolean("DatabaseBackup", true);
		for (String world : (List<String>) config.getList("ExcludedWorlds"))
			excludedWorldSet.add(world);
		for (String category : (List<String>) config.getList("DisabledCategories"))
			disabledCategorySet.add(category);
		playtimeTaskInterval = config.getInt("PlaytimeTaskInterval", 60);
		distanceTaskInterval = config.getInt("DistanceTaskInterval", 5);
		pooledRequestsTaskInterval = config.getInt("PooledRequestsTaskInterval", 60);
		asyncPooledRequestsSender = config.getBoolean("AsyncPooledRequestsSender", true);
	}

	/**
	 * Initialises the command modules.
	 */
	private void initialiseCommands() {

		reward = new AchievementRewards(this);
		achievementDisplay = new AchievementDisplay(this);
		giveCommand = new GiveCommand(this);
		bookCommand = new BookCommand(this);
		topCommand = new TopCommand(this);
		statsCommand = new StatsCommand(this);
		infoCommand = new InfoCommand(this);
		listCommand = new ListCommand(this);
		helpCommand = new HelpCommand(this);
		checkCommand = new CheckCommand(this);
		deleteCommand = new DeleteCommand(this);
	}

	/**
	 * Log number of achievements and disabled categories.
	 */
	private void logAchievementStats() {

		int totalAchievements = 0;
		int categoriesInUse = 0;

		// Enumerate Commands achievements
		if (!disabledCategorySet.contains("Commands")) {
			ConfigurationSection categoryConfig = config.getConfigurationSection("Commands");
			int keyCount = categoryConfig.getKeys(false).size();
			if (keyCount > 0) {
				categoriesInUse += 1;
				totalAchievements += keyCount;
			}
		}

		// Enumerate the normal achievements
		for (NormalAchievements category : NormalAchievements.values()) {
			if (disabledCategorySet.contains(category.toString()))
				continue;

			ConfigurationSection categoryConfig = config.getConfigurationSection(category.toString());
			int keyCount = categoryConfig.getKeys(false).size();
			if (keyCount > 0) {
				categoriesInUse += 1;
				totalAchievements += keyCount;
			}
		}

		// Enumerate the achievements with multiple categories
		for (MultipleAchievements category : MultipleAchievements.values()) {
			if (disabledCategorySet.contains(category.toString()))
				continue;

			ConfigurationSection categoryConfig = config.getConfigurationSection(category.toString());
			Set<String> categorySections = categoryConfig.getKeys(false);

			if (categorySections.isEmpty())
				continue;

			categoriesInUse += 1;

			// Enumerate the sub-categories
			for (String section : categorySections) {
				ConfigurationSection subcategoryConfig = config.getConfigurationSection(category + "." + section);
				int achievementCount = subcategoryConfig.getKeys(false).size();
				if (achievementCount > 0) {
					totalAchievements += achievementCount;
				}
			}
		}

		this.getLogger().info("Loaded " + totalAchievements + " achievements in " + categoriesInUse + " categories.");

		if (!disabledCategorySet.isEmpty()) {
			StringBuilder disabledCategories = new StringBuilder();

			if (disabledCategorySet.size() == 1)
				disabledCategories.append(disabledCategorySet.size() + " disabled category: ");
			else
				disabledCategories.append(disabledCategorySet.size() + " disabled categories: ");

			for (String category : disabledCategorySet) {
				disabledCategories.append(category + ", ");
			}

			// Remove the trailing comma and space
			disabledCategories.deleteCharAt(disabledCategories.length() - 1);
			disabledCategories.deleteCharAt(disabledCategories.length() - 1);

			this.getLogger().info(disabledCategories.toString());
		}
	}

	/**
	 * Register permissions that depend on the user's configuration file (based on multiple type achievements; for
	 * instance for stone breaks, achievement.count.breaks.stone will be registered).
	 */
	private void registerPermissions() {

		for (MultipleAchievements category : MultipleAchievements.values())
			for (String section : config.getConfigurationSection(category.toString()).getKeys(false)) {
				// Bukkit only allows permissions to be set once, so must do
				// additional check for /aach reload correctness.
				if (this.getServer().getPluginManager().getPermission(
						"achievement.count." + category.toString().toLowerCase() + "." + section) == null)
					this.getServer().getPluginManager()
							.addPermission(new Permission(
									"achievement.count." + category.toString().toLowerCase() + "." + section,
									PermissionDefault.TRUE));
			}
	}

	/**
	 * Called when server is stopped or reloaded.
	 */
	@Override
	public void onDisable() {

		// Error while loading .yml files or database; do not do any further
		// work.
		if (overrideDisable)
			return;

		// Cancel scheduled tasks.
		if (pooledRequestsSenderTask != null)
			pooledRequestsSenderTask.cancel();
		if (playedTimeTask != null)
			playedTimeTask.cancel();
		if (distanceTask != null)
			distanceTask.cancel();

		// Send remaining stats for pooled events to the database.
		pooledRequestsSender.sendRequests();

		// Send played time stats to the database, forcing synchronous writes.
		if (achievePlayTimeRunnable != null)
			for (Entry<String, Long> entry : poolsManager.getPlayedTimeHashMap().entrySet())
				this.getDb().updatePlaytime(entry.getKey(), entry.getValue());

		// Send traveled distance stats to the database, forcing synchronous
		// writes.
		if (achieveDistanceRunnable != null) {
			for (Entry<String, Integer> entry : poolsManager.getDistanceFootHashMap().entrySet())
				this.getDb().updateDistance(entry.getKey(), entry.getValue(),
						NormalAchievements.DISTANCEFOOT.toDBName());

			for (Entry<String, Integer> entry : poolsManager.getDistancePigHashMap().entrySet())
				this.getDb().updateDistance(entry.getKey(), entry.getValue(),
						NormalAchievements.DISTANCEPIG.toDBName());

			for (Entry<String, Integer> entry : poolsManager.getDistanceHorseHashMap().entrySet())
				this.getDb().updateDistance(entry.getKey(), entry.getValue(),
						NormalAchievements.DISTANCEHORSE.toDBName());

			for (Entry<String, Integer> entry : poolsManager.getDistanceBoatHashMap().entrySet())
				this.getDb().updateDistance(entry.getKey(), entry.getValue(),
						NormalAchievements.DISTANCEBOAT.toDBName());

			for (Entry<String, Integer> entry : poolsManager.getDistanceMinecartHashMap().entrySet())
				this.getDb().updateDistance(entry.getKey(), entry.getValue(),
						NormalAchievements.DISTANCEMINECART.toDBName());

			for (Entry<String, Integer> entry : poolsManager.getDistanceGlidingHashMap().entrySet())
				this.getDb().updateDistance(entry.getKey(), entry.getValue(),
						NormalAchievements.DISTANCEGLIDING.toDBName());
		}

		try {
			if (this.getDb().getSQLConnection() != null)
				this.getDb().getSQLConnection().close();
		} catch (SQLException e) {
			this.getLogger().log(Level.SEVERE, "Error while closing connection to database: ", e);
		}

		this.getLogger().info("Remaining requests sent to database, plugin disabled.");
	}

	/**
	 * Check if player is in a world in which achievements must not be received.
	 * 
	 * @param player
	 * @return true if player is in excluded world, false otherwise
	 */
	public boolean isInExludedWorld(Player player) {

		if (excludedWorldSet.isEmpty())
			return false;

		return excludedWorldSet.contains(player.getWorld().getName());
	}

	/**
	 * Try to hook up with Vault, and log if this is called on plugin initialisation.
	 * 
	 * @param log
	 * @return true if Vault available, false otherwise
	 */
	public boolean setUpEconomy(boolean log) {

		if (economy != null)
			return true;

		try {
			RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager()
					.getRegistration(net.milkbowl.vault.economy.Economy.class);
			if (economyProvider != null) {
				economy = economyProvider.getProvider();
			}

			return (economy != null);
		} catch (NoClassDefFoundError e) {
			if (log)
				this.getLogger().warning("Attempt to hook up with Vault failed. Money reward ignored.");
			return false;
		}
	}

	/**
	 * Called when a player or the console enters a command. Handles command directly or dispatches to one of the
	 * command modules.
	 */
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String args[]) {

		if (!"aach".equalsIgnoreCase(cmd.getName()))
			return false;

		if ((args.length == 1) && !"help".equalsIgnoreCase(args[0])) {

			if ("book".equalsIgnoreCase(args[0]) && sender.hasPermission("achievement.book")
					&& sender instanceof Player) {

				bookCommand.giveBook(((Player) sender));

			} else if ("hcaa".equalsIgnoreCase(args[0]) && sender.hasPermission("achievement.easteregg")) {

				displayEasterEgg(sender);

			} else if ("reload".equalsIgnoreCase(args[0])) {

				if (sender.hasPermission("achievement.reload")) {

					this.reloadConfig();
					configurationLoad(false);
					if (successfulLoad) {
						if (sender instanceof Player)
							sender.sendMessage(chatHeader + lang.getString("configuration-successfully-reloaded",
									"Configuration successfully reloaded."));
						this.getLogger().info("Configuration successfully reloaded.");
					} else {
						sender.sendMessage(chatHeader + lang.getString("configuration-reload-failed",
								"Errors while reloading configuration. Please view logs for more details."));
						this.getLogger()
								.severe("Errors while reloading configuration. Please view logs for more details.");
					}

				} else {

					sender.sendMessage(chatHeader
							+ lang.getString("no-permissions", "You do not have the permission to do this."));
				}
			} else if ("stats".equalsIgnoreCase(args[0]) && sender instanceof Player) {

				if (sender.hasPermission("achievement.stats")) {
					statsCommand.getStats((Player) sender);
				} else {

					sender.sendMessage(chatHeader
							+ lang.getString("no-permissions", "You do not have the permission to do this."));
				}
			} else if ("list".equalsIgnoreCase(args[0]) && sender instanceof Player) {

				if (sender.hasPermission("achievement.list")) {
					listCommand.createMainGUI((Player) sender);
				} else {

					sender.sendMessage(chatHeader
							+ lang.getString("no-permissions", "You do not have the permission to do this."));
				}
			} else if ("top".equalsIgnoreCase(args[0])) {

				if (sender.hasPermission("achievement.top")) {
					topCommand.getTop(sender);
				} else {

					sender.sendMessage(chatHeader
							+ lang.getString("no-permissions", "You do not have the permission to do this."));
				}
			} else if ("week".equalsIgnoreCase(args[0])) {

				if (sender.hasPermission("achievement.week")) {
					topCommand.getWeek(sender);
				} else {

					sender.sendMessage(chatHeader
							+ lang.getString("no-permissions", "You do not have the permission to do this."));
				}
			} else if ("month".equalsIgnoreCase(args[0])) {

				if (sender.hasPermission("achievement.month")) {
					topCommand.getMonth(sender);
				} else {

					sender.sendMessage(chatHeader
							+ lang.getString("no-permissions", "You do not have the permission to do this."));
				}
			} else if ("info".equalsIgnoreCase(args[0])) {

				infoCommand.getInfo(sender);
			} else {

				helpCommand.getHelp(sender);
			}
		} else if ((args.length == 3) && "give".equalsIgnoreCase(args[0])) {

			if (sender.hasPermission("achievement.give")) {

				giveCommand.achievementGive(sender, args);

			} else {

				sender.sendMessage(
						chatHeader + lang.getString("no-permissions", "You do not have the permission to do this."));
			}

		} else if ((args.length >= 3) && "check".equalsIgnoreCase(args[0])) {

			if (sender.hasPermission("achievement.check")) {

				checkCommand.achievementCheck(sender, args);

			} else {

				sender.sendMessage(
						chatHeader + lang.getString("no-permissions", "You do not have the permission to do this."));
			}

		} else if ((args.length >= 3) && "delete".equalsIgnoreCase(args[0])) {

			if (sender.hasPermission("achievement.delete")) {

				deleteCommand.achievementDelete(sender, args);

			} else {

				sender.sendMessage(
						chatHeader + lang.getString("no-permissions", "You do not have the permission to do this."));
			}
		} else {

			helpCommand.getHelp(sender);
		}

		return true;

	}

	/**
	 * Easter egg; run it and you'll see what all this mess is about!
	 * 
	 * @param sender
	 */
	private void displayEasterEgg(CommandSender sender) {

		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§0\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§0\u2592§8\u2592§8\u2592§8\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§0\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§0\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§0\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§4\u2592§4\u2592§c\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§4\u2592§4\u2592§4\u2592§c\u2592§c\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§0\u2592§8\u2592§8\u2592§4\u2592§4\u2592§4\u2592§c\u2592§4\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§0\u2592§0\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§4\u2592§4\u2592§4\u2592§4\u2592§4\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§7\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§0\u2592§0\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§4\u2592§4\u2592§4\u2592§4\u2592§4\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§0\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§4\u2592§4\u2592§4\u2592§4\u2592§4\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§0\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§4\u2592§4\u2592§4\u2592§4\u2592§4\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§0\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§0\u2592§8\u2592§f\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§4\u2592§4\u2592§4\u2592§4\u2592§4\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§0\u2592§7\u2592§0\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§0\u2592§8\u2592§7\u2592§f\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§4\u2592§4\u2592§4\u2592§4\u2592§4\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§0\u2592§7\u2592§7\u2592§7\u2592§8\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§0\u2592§8\u2592§f\u2592§7\u2592§f\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§4\u2592§4\u2592§4\u2592§4\u2592§4\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§8\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§0\u2592§8\u2592§7\u2592§f\u2592§7\u2592§f\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§4\u2592§4\u2592§4\u2592§6\u2592§6\u2592§8\u2592§0\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§8\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§0\u2592§8\u2592§f\u2592§7\u2592§f\u2592§7\u2592§f\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§4\u2592§6\u2592§6\u2592§6\u2592§6\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§8\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§8\u2592§8\u2592§8\u2592§f\u2592§7\u2592§f\u2592§7\u2592§f\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§6\u2592§6\u2592§6\u2592§6\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§0\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§0\u2592§8\u2592§8\u2592§f\u2592§7\u2592§f\u2592§7\u2592§f\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§0\u2592§7\u2592§6\u2592§6\u2592§4\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§0\u2592§0\u2592§0\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§0\u2592§8\u2592§8\u2592§f\u2592§7\u2592§f\u2592§7\u2592§f\u2592§8\u2592§8\u2592§8\u2592§8\u2592§0\u2592§0\u2592§7\u2592§7\u2592§7\u2592§4\u2592§4\u2592§4\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§0\u2592§0\u2592§0\u2592§0\u2592§0\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§7\u2592§0\u2592§8\u2592§8\u2592§f\u2592§7\u2592§f\u2592§7\u2592§f\u2592§8\u2592§8\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§4\u2592§4\u2592§4\u2592§7\u2592§7\u2592§0\u2592§0\u2592§0\u2592§0\u2592§0\u2592§0\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§8\u2592§8\u2592§f\u2592§7\u2592§f\u2592§7\u2592§f\u2592§f\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§4\u2592§4\u2592§4\u2592§0\u2592§0\u2592§0\u2592§0\u2592§0\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§4\u2592§4\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§f\u2592§f\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§4\u2592§4\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§0\u2592§0\u2592§0\u2592§4\u2592§4\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§f\u2592§f\u2592§7\u2592§0\u2592§0\u2592§0\u2592§0\u2592§0\u2592§4\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§8\u2592§8\u2592§0\u2592§0\u2592§0\u2592§0\u2592§0\u2592§7\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§0\u2592§0\u2592§0\u2592§0\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§0\u2592§0\u2592§0\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§r"));
		sender.sendMessage(StringEscapeUtils.unescapeJava(
				"§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§7\u2592§5\u2592§5\u2592§5\u2592§5\u2592§7\u2592§r"));
	}

	// Various getters and setters. Names are self-explanatory.

	public Economy getEconomy() {

		return economy;
	}

	public SQLDatabaseManager getDb() {

		return db;
	}

	public DatabasePoolsManager getPoolsManager() {

		return poolsManager;
	}

	public AchievementRewards getReward() {

		return reward;
	}

	public AchievementDisplay getAchievementDisplay() {

		return achievementDisplay;
	}

	public UpdateChecker getUpdateChecker() {

		return updateChecker;
	}

	public String getChatHeader() {

		return chatHeader;
	}

	public boolean isRestrictCreative() {

		return restrictCreative;
	}

	public boolean isSuccessfulLoad() {

		return successfulLoad;
	}

	public Set<String> getDisabledCategorySet() {

		return disabledCategorySet;
	}

	public void setOverrideDisable(boolean overrideDisable) {

		this.overrideDisable = overrideDisable;
	}

	public void setSuccessfulLoad(boolean successfulLoad) {

		this.successfulLoad = successfulLoad;
	}

	public BookCommand getAchievementBookCommand() {

		return bookCommand;
	}

	public ListCommand getAchievementListCommand() {

		return listCommand;
	}

	public AchieveDistanceRunnable getAchieveDistanceRunnable() {

		return achieveDistanceRunnable;
	}

	public AchievePlayTimeRunnable getAchievePlayTimeRunnable() {

		return achievePlayTimeRunnable;
	}

	public AchieveConnectionListener getConnectionListener() {

		return connectionListener;
	}

	public AchieveXPListener getXpListener() {

		return xpListener;
	}

	/**
	 * Return a map from achievement name (as stored in the database) to DisplayName. If multiple achievements have the
	 * same achievement name, only the first DisplayName will be tracked. If DisplayName for an achievement is empty or
	 * undefined, the value in the returned map will be an empty string.
	 * 
	 * @return Map from achievement name to user-friendly display name
	 */
	public Map<String, String> getAchievementsAndDisplayNames() {

		Map<String, String> achievementsAndDisplayNames = new HashMap<>();

		// Enumerate Commands achievements
		for (String ach : config.getConfigurationSection("Commands").getKeys(false)) {

			String achName = config.getString("Commands." + ach + ".Name", "");
			String displayName = config.getString("Commands." + ach + ".DisplayName", "");

			if (!achievementsAndDisplayNames.containsKey(achName)) {
				achievementsAndDisplayNames.put(achName, displayName);
			}
		}

		// Enumerate the normal achievements
		for (NormalAchievements category : NormalAchievements.values()) {
			ConfigurationSection categoryConfig = config.getConfigurationSection(category.toString());
			for (String ach : categoryConfig.getKeys(false)) {

				String achName = config.getString(category + "." + ach + ".Name", "");
				String displayName = config.getString(category + "." + ach + ".DisplayName", "");

				if (!achievementsAndDisplayNames.containsKey(achName)) {
					achievementsAndDisplayNames.put(achName, displayName);
				}
			}
		}

		// Enumerate the achievements with multiple categories
		for (MultipleAchievements category : MultipleAchievements.values()) {
			ConfigurationSection categoryConfig = config.getConfigurationSection(category.toString());
			for (String section : categoryConfig.getKeys(false)) {
				ConfigurationSection subcategoryConfig = config.getConfigurationSection(category + "." + section);
				for (String level : subcategoryConfig.getKeys(false)) {

					String achName = config.getString(category + "." + section + '.' + level + ".Name", "");
					String displayName = config.getString(category + "." + section + '.' + level + ".DisplayName", "");

					if (!achievementsAndDisplayNames.containsKey(achName)) {
						achievementsAndDisplayNames.put(achName, displayName);
					}
				}
			}
		}

		return achievementsAndDisplayNames;
	}

	public String getIcon() {

		return icon;
	}

	public ChatColor getColor() {

		return color;
	}

	public boolean isAsyncPooledRequestsSender() {

		return asyncPooledRequestsSender;
	}

	public YamlManager getPluginConfig() {

		return config;
	}

	public YamlManager getPluginLang() {

		return lang;
	}
}
