package eu.wauz.wauzcore.players;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import eu.wauz.wauzcore.WauzCore;
import eu.wauz.wauzcore.data.players.PlayerConfigurator;
import eu.wauz.wauzcore.data.players.PlayerPassiveSkillConfigurator;
import eu.wauz.wauzcore.items.InventoryStringConverter;
import eu.wauz.wauzcore.items.WauzRewards;
import eu.wauz.wauzcore.items.runes.RuneHardening;
import eu.wauz.wauzcore.menu.collection.PetOverviewMenu;
import eu.wauz.wauzcore.menu.social.TabardMenu;
import eu.wauz.wauzcore.menu.util.MenuUtils;
import eu.wauz.wauzcore.oneblock.OnePlotManager;
import eu.wauz.wauzcore.players.calc.DamageCalculator;
import eu.wauz.wauzcore.players.classes.WauzPlayerClass;
import eu.wauz.wauzcore.players.classes.WauzPlayerClassPool;
import eu.wauz.wauzcore.players.classes.WauzPlayerClassStats;
import eu.wauz.wauzcore.system.WauzDebugger;
import eu.wauz.wauzcore.system.achievements.WauzAchievementType;
import eu.wauz.wauzcore.system.nms.WauzNmsMinimap;
import eu.wauz.wauzcore.system.quests.QuestProcessor;
import eu.wauz.wauzcore.system.quests.QuestSlot;
import eu.wauz.wauzcore.system.util.WauzFileUtils;
import eu.wauz.wauzcore.system.util.WauzMode;

/**
 * The character manager is used to login/out characters and manage their data.
 * 
 * @author Wauzmons
 */
public class CharacterManager {
	
	/**
	 * The current schema version of character files.
	 */
	public static final int SCHEMA_VERSION = 1;
	
	/**
	 * A direct reference to the main class.
	 */
	private static WauzCore core = WauzCore.getInstance();
	
	/**
	 * Logs in the character specified in the player data.
	 * Sets gamemode, level, health, mana, spawn, current location and inventory.
	 * Also shows guild motd and triggers daily rewards.
	 * 
	 * @param player The player that selected the character.
	 * @param wauzMode The mode of the character.
	 * 
	 * @see WauzPlayerData#getSelectedCharacterSlot()
	 * @see InventoryStringConverter#loadInventory(Player)
	 * @see CharacterManager#equipCharacterItems(Player)
	 */
	public static void loginCharacter(final Player player, WauzMode wauzMode) {
		WauzPlayerData playerData = WauzPlayerDataPool.getPlayer(player);
		if(playerData == null) {
			return;
		}

		player.setGameMode(wauzMode.equals(WauzMode.SURVIVAL) ? GameMode.SURVIVAL : GameMode.ADVENTURE);
		
		playerData.setMaxHealth(PlayerPassiveSkillConfigurator.getHealth(player));
		if(wauzMode.equals(WauzMode.MMORPG)) {
			playerData.setMaxMana(PlayerPassiveSkillConfigurator.getMana(player));
		}
		
		Location spawn = PlayerConfigurator.getCharacterSpawn(player);
		Location destination = PlayerConfigurator.getCharacterLocation(player);
		if(wauzMode.equals(WauzMode.MMORPG)) {
			PlayerConfigurator.setTrackerDestination(player, spawn, "Spawn");
		}
		
		player.setCompassTarget(spawn);
		player.setBedSpawnLocation(spawn, true);
		player.teleport(destination);

		player.getInventory().clear();
		InventoryStringConverter.loadInventory(player);
		
		if(wauzMode.equals(WauzMode.MMORPG)) {
			equipCharacterItems(player);
			
			WauzRewards.earnDailyReward(player);
			
			WauzPlayerGuild guild = PlayerConfigurator.getGuild(player);
			if(guild != null) {
				player.sendMessage(
						ChatColor.WHITE + "[" + ChatColor.GREEN + guild.getGuildName() + ChatColor.WHITE + "] " +
						ChatColor.GRAY + guild.getGuildDescription());
			}
		}
	}
	
	/**
	 * Logs out the current chracter of the player.
	 * Resets gamemode, pet, group, potions, character selection, level, health, mana, saturation, inventory, spawn, location.
	 * 
	 * @param player The player that is logging out their character.
	 * 
	 * @see CharacterManager#saveCharacter(Player)
	 * @see CharacterManager#equipHubItems(Player)
	 */
	public static void logoutCharacter(final Player player) {
		player.setGameMode(GameMode.ADVENTURE);
		WauzPlayerData playerData = WauzPlayerDataPool.getPlayer(player);
		
		if(WauzMode.isMMORPG(player)) {
			PetOverviewMenu.unsummon(player);
		}
		
		if(playerData.isInGroup()) {
			WauzPlayerGroupPool.getGroup(playerData.getGroupUuidString()).removePlayer(player);
			playerData.setGroupUuidString(null);
		}
		
		for(PotionEffect potionEffect : player.getActivePotionEffects()) {
			player.removePotionEffect(potionEffect.getType());
		}
		
		saveCharacter(player);
		
		playerData.setSelectedCharacterSlot(null);
		playerData.setSelectedCharacterWorld(null);
		playerData.setSelectedCharacterClass(null);
		
	    player.setExp(0);
		player.setLevel(0);

		playerData.setMaxHealth(20);
		DamageCalculator.setHealth(player, 20);
		playerData.setMaxMana(0);
		playerData.setMana(0);
		
		player.setFoodLevel(20);
		player.setSaturation(20);
		
		player.getInventory().clear();
		equipHubItems(player);
		
		player.setCompassTarget(WauzCore.getHubLocation());
		player.setBedSpawnLocation(WauzCore.getHubLocation(), true);
		player.teleport(WauzCore.getHubLocation());
	}
	
	/**
	 * Saves the character specified in the player data.
	 * 
	 * @param player The player that selected the character.
	 * 
	 * @see InventoryStringConverter#saveInventory(Player)
	 * @see PlayerConfigurator#setCharacterLocation(Player, Location)
	 */
	public static void saveCharacter(final Player player) {
		if(WauzPlayerDataPool.isCharacterSelected(player)) {
			InventoryStringConverter.saveInventory(player);
			if(!StringUtils.startsWith(player.getWorld().getName(), "WzInstance")) {
				PlayerConfigurator.setCharacterLocation(player, player.getLocation());
			}
			WauzDebugger.log(player, ChatColor.GREEN + "Saving... Character-Data saved!");
		}
		else {
			WauzDebugger.log(player, ChatColor.GREEN + "Saving... No Character-Data selected!");
		}
	}
	
	/**
	 * Creates and logs in the new character specified in the player data.
	 * Sets player data file content, gamemode, level, health, mana, saturation, start-equip and quest.
	 * Also triggers first daily reward.
	 * 
	 * @param player The player that selected the new character.
	 * @param wauzMode The mode of the new character.
	 */
	public static void createCharacter(final Player player, WauzMode wauzMode) {
		WauzPlayerData playerData = WauzPlayerDataPool.getPlayer(player);
		if(playerData == null) {
			return;
		}
		
		String characterSlot = playerData.getSelectedCharacterSlot();
		String characterWorldString = playerData.getSelectedCharacterWorld();
		String characterClassString = playerData.getSelectedCharacterClass();
		if(characterSlot == null || characterWorldString == null || characterClassString == null) {
			return;
		}
		
		File playerDataFile = new File(core.getDataFolder(), "PlayerData/" + player.getUniqueId() + "/" + characterSlot + ".yml");
		FileConfiguration playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
		
		String characterPosition = null;
		if(characterSlot.contains("OneBlock")) {
			Location oneBlockLocation = OnePlotManager.getNextFreePlotLocation();
			oneBlockLocation.getBlock().setType(Material.GRASS_BLOCK);
			characterPosition = (oneBlockLocation.getX() + 0.5) + " " + (oneBlockLocation.getY() + 1) + " " + (oneBlockLocation.getZ() + 0.5);
		}
		else {
			Location spawnLocation = core.getServer().getWorld(characterWorldString).getSpawnLocation();
			characterPosition = (spawnLocation.getX() + 0.5) + " " + spawnLocation.getY() + " " + (spawnLocation.getZ() + 0.5);
		}
		
		player.getInventory().clear();
		player.setFoodLevel(20);
		player.setSaturation(10);
		player.setExp(0);
		
		playerDataConfig.set("exists", true);
		playerDataConfig.set("schemaversion", SCHEMA_VERSION);
		playerDataConfig.set("lastplayed", System.currentTimeMillis());
		playerDataConfig.set("class", characterClassString);
		playerDataConfig.set("level", wauzMode.equals(WauzMode.MMORPG) ? 1 : 0);
		playerDataConfig.set("exp", 0);
		playerDataConfig.set("pos.world", characterWorldString);
		playerDataConfig.set("pos.spawn", characterPosition);
		playerDataConfig.set("pos.location", characterPosition);
	
		playerDataConfig.set("stats.current.health", 10);
		playerDataConfig.set("stats.current.hunger", 20);
		playerDataConfig.set("stats.current.saturation", 10);
		
		if(wauzMode.equals(WauzMode.MMORPG)) {
			player.setGameMode(GameMode.ADVENTURE);
			player.setLevel(1);
			
			playerData.setMaxHealth(10);
			playerData.setHealth(10);
			playerData.setMaxMana(10);
			playerData.setMana(10);
			
			WauzPlayerClass characterClass = WauzPlayerClassPool.getClass(characterClassString);
			WauzPlayerClassStats startingStats = characterClass.getStartingStats();
			
			playerDataConfig.set("tracker.coords", characterPosition);
			playerDataConfig.set("tracker.name", "Spawn");
			
			playerDataConfig.set("arrows.selected", "normal");
			playerDataConfig.set("arrows.amount.reinforced", 0);
			playerDataConfig.set("arrows.amount.fire", 0);
			playerDataConfig.set("arrows.amount.ice", 0);
			playerDataConfig.set("arrows.amount.shock", 0);
			playerDataConfig.set("arrows.amount.bomb", 0);

			playerDataConfig.set("stats.current.mana", 10);
			playerDataConfig.set("stats.points.spent", 0);
			playerDataConfig.set("stats.points.total", 0);
			playerDataConfig.set("stats.health", 10);
			playerDataConfig.set("stats.healthpts", 0);
			playerDataConfig.set("stats.trading", 100);
			playerDataConfig.set("stats.tradingpts", 0);
			playerDataConfig.set("stats.luck", 10);
			playerDataConfig.set("stats.luckpts", 0);
			playerDataConfig.set("stats.mana", 10);
			playerDataConfig.set("stats.manapts", 0);
			playerDataConfig.set("stats.strength", 100);
			playerDataConfig.set("stats.strengthpts", 0);
			playerDataConfig.set("stats.agility", 0);
			playerDataConfig.set("stats.agilitypts", 0);
			
			playerDataConfig.set("skills.crafting", 1);
			playerDataConfig.set("skills.sword", startingStats.getSwordSkill());
			playerDataConfig.set("skills.swordmax", startingStats.getSwordSkillMax());
			playerDataConfig.set("skills.axe", startingStats.getAxeSkill());
			playerDataConfig.set("skills.axemax", startingStats.getAxeSkillMax());
			playerDataConfig.set("skills.staff", startingStats.getStaffSkill());
			playerDataConfig.set("skills.staffmax", startingStats.getStaffSkillMax());
				
			playerDataConfig.set("curenncies", new ArrayList<>());
			playerDataConfig.set("materials", new ArrayList<>());
			
			playerDataConfig.set("options.hideSpecialQuests", 0);
			playerDataConfig.set("options.hideCompletedQuests", 0);
			playerDataConfig.set("options.tabard", "No Tabard");
			playerDataConfig.set("options.title", "default");
			playerDataConfig.set("options.titlelist", new ArrayList<>());
			
			playerDataConfig.set("cooldown.reward", 0);
			
			playerDataConfig.set("pets.active.id", "none");
			playerDataConfig.set("pets.active.slot", -1);
			playerDataConfig.set("pets.slot0.type", "none");
			playerDataConfig.set("pets.slot1.type", "none");
			playerDataConfig.set("pets.slot2.type", "none");
			playerDataConfig.set("pets.slot3.type", "none");
			playerDataConfig.set("pets.slot4.type", "none");
			playerDataConfig.set("pets.slot6.type", "none");
			playerDataConfig.set("pets.slot8.type", "none");
			playerDataConfig.set("pets.egg.time", 0);
			
			playerDataConfig.set(QuestSlot.MAIN.getConfigKey(), "none");
			playerDataConfig.set(QuestSlot.CAMPAIGN1.getConfigKey(), "none");
			playerDataConfig.set(QuestSlot.CAMPAIGN2.getConfigKey(), "none");
			playerDataConfig.set(QuestSlot.DAILY1.getConfigKey(), "none");
			playerDataConfig.set(QuestSlot.DAILY2.getConfigKey(), "none");
			playerDataConfig.set(QuestSlot.DAILY3.getConfigKey(), "none");
			
			playerDataConfig.set("achievements.completed", 0);
			playerDataConfig.set("achievements.generic." + WauzAchievementType.KILL_ENEMIES.getKey(), 0);
			playerDataConfig.set("achievements.generic." + WauzAchievementType.IDENTIFY_ITEMS.getKey(), 0);
			playerDataConfig.set("achievements.generic." + WauzAchievementType.USE_MANA.getKey(), 0);
			playerDataConfig.set("achievements.generic." + WauzAchievementType.COMPLETE_QUESTS.getKey(), 0);
			playerDataConfig.set("achievements.generic." + WauzAchievementType.CRAFT_ITEMS.getKey(), 0);
			playerDataConfig.set("achievements.generic." + WauzAchievementType.COLLECT_PETS.getKey(), 0);
			playerDataConfig.set("achievements.generic." + WauzAchievementType.EARN_COINS.getKey(), 0);
			playerDataConfig.set("achievements.generic." + WauzAchievementType.PLAY_HOURS.getKey(), 0);
			playerDataConfig.set("achievements.generic." + WauzAchievementType.GAIN_LEVELS.getKey(), 1);
			
			player.getInventory().addItem(characterClass.getStartingWeapon());
			player.getInventory().addItem(WauzDebugger.getRune(RuneHardening.RUNE_NAME, false));
			equipCharacterItems(player);
			WauzRewards.earnDailyReward(player);
			
			if(characterWorldString.equals("Wauzland")) {
				QuestProcessor.processQuest(player, "CalamityBeneathWauzland");
			}
		}
		else if(wauzMode.equals(WauzMode.SURVIVAL)) {
			player.setGameMode(GameMode.SURVIVAL);
			player.setLevel(0);
			
			playerDataConfig.set("pvp.resticks", 720);
			playerData.setResistancePvP((short) 720);
		}
		
		try {
			playerDataConfig.save(playerDataFile);
		}
		catch(IOException e) {
			e.printStackTrace();
		}
				
		Location spawn = PlayerConfigurator.getCharacterSpawn(player);
		player.setCompassTarget(spawn);
		player.setBedSpawnLocation(spawn, true);
		player.teleport(spawn);
	}
	
	/**
	 * Deletes the given character permanently.
	 * 
	 * @param player The player who owns the character.
	 * @param characterSlot The slot of the character.
	 */
	public static void deleteCharacter(Player player, String characterSlot) {
		String basePath = core.getDataFolder().getAbsolutePath() + "/PlayerData/" + player.getUniqueId() + "/" + characterSlot;
		new File(basePath + ".yml").delete();
		WauzFileUtils.removeFilesRecursive(new File(basePath + "-quests"));
		WauzFileUtils.removeFilesRecursive(new File(basePath + "-relations"));
	}
	
	/**
	 * Equips a player with default hub items.
	 * Currently only the mode selection menu.
	 * 
	 * @param player The player that should receive the items.
	 * 
	 * @see MenuUtils#setMainMenuOpener(org.bukkit.inventory.Inventory, int)
	 */
	public static void equipHubItems(Player player) {
		player.getInventory().clear();
		MenuUtils.setMainMenuOpener(player.getInventory(), 4);
	}
	
	/**
	 * Equips a player with default mmorpg character items.
	 * Contains quest tracker, main menu, minimap, trashcan and selected tabard.
	 * 
	 * @param player The player that should receive the items.
	 * 
	 * @see MenuUtils#setMainMenuOpener(org.bukkit.inventory.Inventory, int)
	 * @see MenuUtils#setTrashcan(org.bukkit.inventory.Inventory, int...)
	 * @see TabardMenu#equipSelectedTabard(Player)
	 */
	public static void equipCharacterItems(Player player) {
		ItemStack trackerItemStack = new ItemStack(Material.COMPASS);
		ItemMeta trackerItemMeta = trackerItemStack.getItemMeta();
		trackerItemMeta.setDisplayName(ChatColor.DARK_AQUA + "Tracked: " + PlayerConfigurator.getTrackerDestinationName(player));
		trackerItemStack.setItemMeta(trackerItemMeta);
		player.getInventory().setItem(7, trackerItemStack);
		MenuUtils.setMainMenuOpener(player.getInventory(), 8);
		
		ItemStack mapItemStack = new ItemStack(Material.FILLED_MAP);
		ItemMeta mapItemMeta = mapItemStack.getItemMeta();
		mapItemMeta.setDisplayName(ChatColor.GOLD + "Explorer Map");
		mapItemStack.setItemMeta(mapItemMeta);
		player.getEquipment().setItemInOffHand(mapItemStack);
		WauzNmsMinimap.init(player);
		
		MenuUtils.setTrashcan(player.getInventory(), 35);
		
		TabardMenu.equipSelectedTabard(player);
	}

}
