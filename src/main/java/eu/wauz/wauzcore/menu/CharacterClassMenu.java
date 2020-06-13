package eu.wauz.wauzcore.menu;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import eu.wauz.wauzcore.menu.util.HeadUtils;
import eu.wauz.wauzcore.menu.util.MenuUtils;
import eu.wauz.wauzcore.menu.util.WauzInventory;
import eu.wauz.wauzcore.menu.util.WauzInventoryHolder;
import eu.wauz.wauzcore.players.CharacterManager;
import eu.wauz.wauzcore.players.WauzPlayerData;
import eu.wauz.wauzcore.players.WauzPlayerDataPool;
import eu.wauz.wauzcore.system.util.WauzMode;

/**
 * An inventory that can be used as menu or for other custom interaction mechanics.
 * A character creation menu, that will let the player select their class.
 * 
 * @author Wauzmons
 * 
 * @see CharacterSlotMenu
 * @see CharacterWorldMenu
 */
public class CharacterClassMenu implements WauzInventory {
	
	/**
	 * @return The id of the inventory.
	 */
	@Override
	public String getInventoryId() {
		return "classes";
	}
	
	/**
	 * Opens a new inventory of this type for the given player.
	 * 
	 * @param player The player that should view the inventory.
	 */
	@Override
	public void openInstance(Player player) {
		CharacterClassMenu.open(player);
	}
	
	/**
	 * Opens the menu for the given player.
	 * Shows three hardcoded classes to choose: "Nephilim", "Crusader", "Assassin".
	 * 
	 * @param player The player that should view the inventory.
	 * 
	 * @see MenuUtils#setBorders(Inventory)
	 */
	public static void open(Player player) {
		WauzInventoryHolder holder = new WauzInventoryHolder(new CharacterClassMenu());
		Inventory menu = Bukkit.createInventory(holder, 9, ChatColor.BLACK + "" + ChatColor.BOLD + "Choose your Class!");
		
		ItemStack class1 = HeadUtils.getNephilimItem();
		ItemMeta im1 = class1.getItemMeta();
		im1.setDisplayName(ChatColor.RED + "Nephilim");
		List<String> lores1 = new ArrayList<String>();
		lores1.add(ChatColor.WHITE + "Preferred Weapons (+35% Atk):" + ChatColor.LIGHT_PURPLE + " Staves");
		lores1.add(ChatColor.WHITE + "Armor Category:" + ChatColor.AQUA + " Medium");
		lores1.add("");
		lores1.add(ChatColor.GRAY + "Children of fallen Angels,");
		lores1.add(ChatColor.GRAY + "who fight to gain more power.");
		im1.setLore(lores1);
		class1.setItemMeta(im1);
		menu.setItem(2, class1);
		
		ItemStack class2 = HeadUtils.getCrusaderItem();
		ItemMeta im2 = class2.getItemMeta();
		im2.setDisplayName(ChatColor.GOLD + "Crusader");
		List<String> lores2 = new ArrayList<String>();
		lores2.add(ChatColor.WHITE + "Preferred Weapons (+35% Atk):" + ChatColor.LIGHT_PURPLE + " Axes");
		lores2.add(ChatColor.WHITE + "Armor Category:" + ChatColor.AQUA + " Heavy");
		lores2.add("");
		lores2.add(ChatColor.GRAY + "Holy Warriors,");
		lores2.add(ChatColor.GRAY + "who fight in the name of their god.");
		im2.setLore(lores2);
		class2.setItemMeta(im2);
		menu.setItem(4, class2);
		
		ItemStack class3 = HeadUtils.getAssassinItem();
		ItemMeta im3 = class3.getItemMeta();
		im3.setDisplayName(ChatColor.GREEN + "Assassin");
		List<String> lores3 = new ArrayList<String>();
		lores3.add(ChatColor.WHITE + "Preferred Weapons (+35% Atk):" + ChatColor.LIGHT_PURPLE + " Swords");
		lores3.add(ChatColor.WHITE + "Armor Category:" + ChatColor.AQUA + " Light");
		lores3.add("");
		lores3.add(ChatColor.GRAY + "Ruthless Mercenaries,");
		lores3.add(ChatColor.GRAY + "who fight to fill their wallets.");
		im3.setLore(lores3);
		class3.setItemMeta(im3);
		menu.setItem(6, class3);
		
		MenuUtils.setBorders(menu);
		player.openInventory(menu);
	}

	/**
	 * Checks if an event in this inventory was triggered by a player click.
	 * The default event will be automatically canceled.
	 * If the clicked item is a class selection, it will be cached in the player data.
	 * Next the character creation will be triggered.
	 * 
	 * @param event The inventory click event.
	 * 
	 * @see WauzPlayerData#setSelectedCharacterClass(String)
	 * @see CharacterManager#createCharacter(Player, WauzMode)
	 */
	@Override
	public void selectMenuPoint(InventoryClickEvent event) {
		event.setCancelled(true);
		ItemStack clicked = event.getCurrentItem();
		final Player player = (Player) event.getWhoClicked();
		WauzPlayerData playerData = WauzPlayerDataPool.getPlayer(player);
		
		if(playerData == null || clicked == null) {
			return;
		}
		else if(HeadUtils.isHeadMenuItem(clicked, "Nephilim")) {
			playerData.setSelectedCharacterClass("Nephilim");
			CharacterManager.createCharacter(player, WauzMode.MMORPG);
			player.closeInventory();
		}
		else if(HeadUtils.isHeadMenuItem(clicked, "Crusader")) {
			playerData.setSelectedCharacterClass("Crusader");
			CharacterManager.createCharacter(player, WauzMode.MMORPG);
			player.closeInventory();
		}
		else if(HeadUtils.isHeadMenuItem(clicked, "Assassin")) {
			playerData.setSelectedCharacterClass("Assassin");
			CharacterManager.createCharacter(player, WauzMode.MMORPG);
			player.closeInventory();
		}
	}

}
