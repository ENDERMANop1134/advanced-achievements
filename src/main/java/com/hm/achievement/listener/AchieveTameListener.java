package com.hm.achievement.listener;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTameEvent;

import com.hm.achievement.AdvancedAchievements;
import com.hm.achievement.category.NormalAchievements;

/**
 * Listener class to deal with Taming achievements.
 * 
 * @author Pyves
 *
 */
public class AchieveTameListener implements Listener {

	private AdvancedAchievements plugin;

	public AchieveTameListener(AdvancedAchievements plugin) {

		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTame(EntityTameEvent event) {

		if (!(event.getOwner() instanceof Player))
			return;
		Player player = (Player) event.getOwner();
		if (!player.hasPermission("achievement.count.taming")
				|| plugin.isRestrictCreative() && player.getGameMode() == GameMode.CREATIVE
				|| plugin.isInExludedWorld(player))
			return;

		int tames = plugin.getPoolsManager().getPlayerTameAmount(player) + 1;

		plugin.getPoolsManager().getTameHashMap().put(player.getUniqueId().toString(), tames);

		String configAchievement = NormalAchievements.TAMES + "." + tames;
		if (plugin.getPluginConfig().getString(configAchievement + ".Message", null) != null) {

			plugin.getAchievementDisplay().displayAchievement(player, configAchievement);
			plugin.getDb().registerAchievement(player, plugin.getPluginConfig().getString(configAchievement + ".Name"),
					plugin.getPluginConfig().getString(configAchievement + ".Message"));

			plugin.getReward().checkConfig(player, configAchievement);
		}
	}
}
