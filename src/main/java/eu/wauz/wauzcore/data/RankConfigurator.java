package eu.wauz.wauzcore.data;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;

import eu.wauz.wauzcore.data.api.GlobalConfigurationUtils;
import eu.wauz.wauzcore.system.WauzPermission;

/**
 * Configurator to fetch or modify data from the Ranks.yml.
 * 
 * @author Wauzmons
 */
public class RankConfigurator extends GlobalConfigurationUtils {

// General Parameters

	/**
	 * @return The keys of all ranks.
	 */
	public static List<String> getAllRankKeys() {
		return new ArrayList<>(mainConfigGetKeys("Ranks", null));
	}
	
	/**
	 * @param rank The name of the rank.
	 * 
	 * @return The player name prefix of the rank.
	 */
	public static String getRankPrefix(String rank) {
		return mainConfigGetString("Ranks", rank + ".prefix");
	}
	
	/**
	 * @param rank The name of the rank.
	 * 
	 * @return The player name color of the rank.
	 */
	public static ChatColor getRankColor(String rank) {
		return ChatColor.valueOf(mainConfigGetString("Ranks", rank + ".color"));
	}
	
	/**
	 * @param rank The name of the rank.
	 * 
	 * @return The player permission of the rank.
	 */
	public static WauzPermission getRankPermission(String rank) {
		return WauzPermission.valueOf(mainConfigGetString("Ranks", rank + ".permission"));
	}
	
// Daily Rewards
	
	/**
	 * @param rank The name of the rank.
	 * 
	 * @return The daily coin reward of the rank.
	 */
	public static int getRankRewardCoins(String rank) {
		return mainConfigGetInt("Ranks", rank + ".rewards.coins");
	}
	
	/**
	 * @param rank The name of the rank.
	 * 
	 * @return The daily soulstone reward of the rank.
	 */
	public static int getRankRewardSoulstones(String rank) {
		return mainConfigGetInt("Ranks", rank + ".rewards.soulstones");
	}
	
}