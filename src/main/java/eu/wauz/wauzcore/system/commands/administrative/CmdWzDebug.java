package eu.wauz.wauzcore.system.commands.administrative;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import eu.wauz.wauzcore.system.WauzDebugger;
import eu.wauz.wauzcore.system.commands.execution.WauzCommand;
import net.md_5.bungee.api.ChatColor;

public class CmdWzDebug implements WauzCommand {

	@Override
	public String getCommandId() {
		return "wzDebug";
	}

	@Override
	public boolean executeCommand(CommandSender sender, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Only players can execute this command!");
			return true;
		}
		// TODO Player from parameter.
		return WauzDebugger.toggleDebugMode((Player) sender);
	}

}