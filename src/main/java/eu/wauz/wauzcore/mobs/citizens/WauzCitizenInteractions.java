package eu.wauz.wauzcore.mobs.citizens;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import eu.wauz.wauzcore.data.CitizenConfigurator;
import eu.wauz.wauzcore.data.players.PlayerRelationConfigurator;
import eu.wauz.wauzcore.events.WauzPlayerEvent;
import eu.wauz.wauzcore.events.WauzPlayerEventCitizenCommand;
import eu.wauz.wauzcore.events.WauzPlayerEventCitizenInn;
import eu.wauz.wauzcore.events.WauzPlayerEventCitizenQuest;
import eu.wauz.wauzcore.events.WauzPlayerEventCitizenShop;
import eu.wauz.wauzcore.events.WauzPlayerEventCitizenTalk;
import eu.wauz.wauzcore.items.util.ItemUtils;
import eu.wauz.wauzcore.menu.util.HeadUtils;
import eu.wauz.wauzcore.menu.util.MenuUtils;
import eu.wauz.wauzcore.system.WauzDebugger;
import eu.wauz.wauzcore.system.economy.WauzShop;
import eu.wauz.wauzcore.system.quests.WauzQuest;

/**
 * The interaction options of a citizen.
 * 
 * @author Wauzmons
 *
 * @see WauzCitizen
 */
public class WauzCitizenInteractions {
	
	/**
	 * The name of the citizen, as shown in chat.
	 */
	private String displayName;
	
	/**
	 * The mode that should be selected from the hub on interaction.
	 */
	private String modeSelection;
	
	/**
	 * A map of interaction events, indexed by the triggering item stacks.
	 */
	private Map<ItemStack, WauzPlayerEvent> interactionEventMap = new HashMap<>();
	
	/**
	 * Constructs a set of interactions for the citizen with the given name.
	 * 
	 * @param citizenName The canonical name of the citizen.
	 * 
	 * @see CitizenConfigurator#getModeSelection(String)
	 * @see WauzCitizenInteractions#createInteractionItemStack(String, String)
	 */
	public WauzCitizenInteractions(String citizenName, String displayName) {
		this.displayName = displayName;
		modeSelection = CitizenConfigurator.getModeSelection(citizenName);
		if(StringUtils.isNotBlank(modeSelection)) {
			return;
		}
		Set<String> interactionKeys = CitizenConfigurator.getInteractionKeys(citizenName);
		for(String interactionKey : interactionKeys) {
			createInteractionItemStack(citizenName, interactionKey);
		}
	}
	
	/**
	 * Triggers the corresponding event, if the player clicked an interaction item stack.
	 * 
	 * @param player The player who chose the interaction.
	 * @param clickedItemStack The chosen interaction item stack.
	 * 
	 * @return If a successful interaction was made.
	 */
	public boolean checkForValidInteractions(Player player, ItemStack clickedItemStack) {
		WauzPlayerEvent event = null;
		for(ItemStack interactionItemStack : interactionEventMap.keySet()) {
			if(ItemUtils.isSpecificItem(clickedItemStack, interactionItemStack.getItemMeta().getDisplayName())) {
				int relationLevel = RelationLevel.getRelationLevel(PlayerRelationConfigurator.getRelationProgress(player, displayName)).getRelationTier();
				int requiredLevel = Integer.parseInt(ItemUtils.getStringFromLore(interactionItemStack, "Required Relation Level", 3));
				if(relationLevel < requiredLevel) {
					player.sendMessage(ChatColor.RED + "Your relation with this citizen is not good enough to do that!");
					player.closeInventory();
					return false;
				}
				event = interactionEventMap.get(interactionItemStack);
				break;
			}
		}
		if(event != null) {
			player.closeInventory();
			return event.execute(player);
		}
		return false;
	}
	
	/**
	 * Creates an inventory menu, containing all interaction item stacks.
	 * 
	 * @param holder The holder of the inventory menu.
	 * @param title The title of the menu.
	 * 
	 * @return The created inventory menu.
	 */
	public Inventory createInteractionMenuBase(InventoryHolder holder, String title) {
		int size = (int) Math.ceil((double) interactionEventMap.size() / (double) 5) * 9;
		Inventory menu = Bukkit.createInventory(holder, size, title);
		int row = 0;
		int column = 0;
		for(ItemStack interactionItemStack : interactionEventMap.keySet()) {
			if(column >= 5) {
				row++;
				column = 0;
			}
			int index = column + 2 + (row * 9);
			menu.setItem(index, interactionItemStack);
			column++;
		}
		return menu;
	}
	
	/**
	 * Creates an interaction item stack from the given values.
	 * Also creates an event entry in the interaction map.
	 * 
	 * @param citizenName The name of the citizen.
	 * @param interactionKey The key of the citizen interaction.
	 */
	private void createInteractionItemStack(String citizenName, String interactionKey) {
		ItemStack interactionItemStack;
		WauzPlayerEvent event;
		String type = CitizenConfigurator.getInteractionType(citizenName, interactionKey);
		String interactionName = CitizenConfigurator.getInteractionName(citizenName, interactionKey);
		int level = CitizenConfigurator.getInteractionLevel(citizenName, interactionKey);
		
		switch (type) {
		case "talk":
			interactionItemStack = HeadUtils.getCitizenTalkItem();
			MenuUtils.setItemDisplayName(interactionItemStack, ChatColor.AQUA + "Talk: " + interactionName);
			List<String> messages = CitizenConfigurator.getInteractionMessages(citizenName, interactionKey);
			event = new WauzPlayerEventCitizenTalk(displayName, messages);
			break;
		case "shop":
			interactionItemStack = HeadUtils.getCitizenShopItem();
			WauzShop shop = WauzShop.getShop(interactionName);
			MenuUtils.setItemDisplayName(interactionItemStack, ChatColor.GREEN + "Shop: " + shop.getShopDisplayName());
			event = new WauzPlayerEventCitizenShop(displayName, interactionName);
			break;
		case "quest":
			interactionItemStack = HeadUtils.getCitizenQuestItem();
			WauzQuest quest = WauzQuest.getQuest(interactionName);
			MenuUtils.setItemDisplayName(interactionItemStack, ChatColor.GOLD + "Quest: " + quest.getDisplayName());
			event = new WauzPlayerEventCitizenQuest(displayName, interactionName);
			break;
		case "inn":
			interactionItemStack = HeadUtils.getCitizenInnItem();
			MenuUtils.setItemDisplayName(interactionItemStack, ChatColor.RED + "Inn: Set as New Home");
			Location location = CitizenConfigurator.getLocation(citizenName);
			event = new WauzPlayerEventCitizenInn(displayName, location);
			break;
		case "command":
			interactionItemStack = HeadUtils.getCitizenCommandItem();
			MenuUtils.setItemDisplayName(interactionItemStack, ChatColor.BLUE + "Action: " + interactionName);
			String command = CitizenConfigurator.getInteractionCommand(citizenName, interactionKey);
			event = new WauzPlayerEventCitizenCommand(displayName, command);
			break;
		default:
			WauzDebugger.log("Invalid Citizen Interaction Type: " + type);
			return;
		}
		MenuUtils.addItemLore(interactionItemStack, ChatColor.GRAY + "Required Relation Level:" + ChatColor.YELLOW + " " + level, false);
		interactionEventMap.put(interactionItemStack, event);
	}

	/**
	 * @return The mode that should be selected from the hub on interaction.
	 */
	public String getModeSelection() {
		return modeSelection;
	}
	
}
