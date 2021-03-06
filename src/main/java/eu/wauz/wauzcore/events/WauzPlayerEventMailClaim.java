package eu.wauz.wauzcore.events;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.text.WordUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import eu.wauz.wauzcore.data.players.PlayerConfigurator;
import eu.wauz.wauzcore.data.players.PlayerMailConfigurator;
import eu.wauz.wauzcore.items.util.ItemUtils;
import eu.wauz.wauzcore.menu.LootContainer;
import eu.wauz.wauzcore.system.util.Formatters;
import eu.wauz.wauzcore.system.util.UnicodeUtils;
import eu.wauz.wauzcore.system.util.WauzDateUtils;

/**
 * An event to let a player claim their mail from the mailbox.
 * 
 * @author Wauzmons
 */
public class WauzPlayerEventMailClaim implements WauzPlayerEvent {
	
	/**
	 * The name of the mail.
	 */
	private String mailName;
	
	/**
	 * The item stack, representing the mail content.
	 */
	private ItemStack mailItemStack;
	
	/**
	 * The coin attachment of the mail.
	 */
	private long coinAttachment;
	
	/**
	 * The item attachment of the mail.
	 */
	private ItemStack itemAttachment;
	
	/**
	 * Creates an event to claim the given mail.
	 * 
	 * @param player The player who owns the mail.
	 * @param mailName The name of the mail.
	 */
	public WauzPlayerEventMailClaim(Player player, String mailName) {
		this.mailName = mailName;
		
		String sender = PlayerMailConfigurator.getMailSender(player, mailName);
		String text = PlayerMailConfigurator.getMailText(player, mailName);
		long time =  PlayerMailConfigurator.getMailTime(player, mailName);
		coinAttachment = PlayerMailConfigurator.getMailCoins(player, mailName);
		itemAttachment = PlayerMailConfigurator.getMailItem(player, mailName);
		
		mailItemStack = new ItemStack(Material.GLOBE_BANNER_PATTERN);
		ItemMeta mailItemMeta = mailItemStack.getItemMeta();
		mailItemMeta.setDisplayName(ChatColor.YELLOW + "Mail from " + sender);
		List<String> mailLores = new ArrayList<>();
		mailLores.add(ChatColor.GRAY + "Received " + WauzDateUtils.formatTime(time));
		if(coinAttachment > 0) {
			mailLores.add(ChatColor.GRAY + "Coins: " + Formatters.formatCoins(coinAttachment));
		}
		if(itemAttachment != null) {
			String attachmentName = ItemUtils.hasDisplayName(itemAttachment)
					? itemAttachment.getItemMeta().getDisplayName()
					: itemAttachment.getI18NDisplayName();
			mailLores.add(ChatColor.GRAY + "Item: " + attachmentName);
		}
		mailLores.add("");
		String doubleParagraph = UnicodeUtils.ICON_PARAGRAPH + UnicodeUtils.ICON_PARAGRAPH;
		String[] textParts = WordUtils.wrap(text, 42, doubleParagraph, true).split(doubleParagraph);
		for(String textPart : textParts) {
			mailLores.add(ChatColor.GRAY + textPart);
		}
		mailItemMeta.setLore(mailLores);
		mailItemStack.setItemMeta(mailItemMeta);
	}
	
	/**
	 * Executes the event for the given player.
	 * 
	 * @param player The player for the execution.
	 * 
	 * @return If the event was executed successfully.
	 * 
	 * @see PlayerMailConfigurator#deletePlayerMail(Player, String)
	 */
	@Override
	public boolean execute(Player player) {
		try {
			List<ItemStack> lootItemStacks = new ArrayList<>();
			lootItemStacks.add(mailItemStack);
			if(coinAttachment > 0) {
				long coins = PlayerConfigurator.getCharacterCoins(player);
				PlayerConfigurator.setCharacterCoins(player, coins + coinAttachment);
			}
			if(itemAttachment != null) {
				lootItemStacks.add(itemAttachment);
			}
			LootContainer.open(player, lootItemStacks);
			PlayerMailConfigurator.deletePlayerMail(player, mailName);
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			player.sendMessage(ChatColor.RED + "An Error occurred while claiming the mail!");
			player.closeInventory();
			return false;
		}
	}

	/**
	 * @return The item stack, representing the mail content.
	 */
	public ItemStack getMailItemStack() {
		return mailItemStack;
	}
	
}
