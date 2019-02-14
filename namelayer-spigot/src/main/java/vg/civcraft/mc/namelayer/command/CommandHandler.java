package vg.civcraft.mc.namelayer.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import vg.civcraft.mc.civmodcore.command.Command;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.command.commands.AcceptInvite;
import vg.civcraft.mc.namelayer.command.commands.AddBlacklist;
import vg.civcraft.mc.namelayer.command.commands.LinkGroups;
import vg.civcraft.mc.namelayer.command.commands.ChangePlayerName;
import vg.civcraft.mc.namelayer.command.commands.CreateGroup;
import vg.civcraft.mc.namelayer.command.commands.DeleteGroup;
import vg.civcraft.mc.namelayer.command.commands.DisciplineGroup;
import vg.civcraft.mc.namelayer.command.commands.GlobalStats;
import vg.civcraft.mc.namelayer.command.commands.GroupStats;
import vg.civcraft.mc.namelayer.command.commands.InfoDump;
import vg.civcraft.mc.namelayer.command.commands.InvitePlayer;
import vg.civcraft.mc.namelayer.command.commands.JoinGroup;
import vg.civcraft.mc.namelayer.command.commands.LeaveGroup;
import vg.civcraft.mc.namelayer.command.commands.NameLayerGroupGui;
import vg.civcraft.mc.namelayer.command.commands.ShowBlacklist;
import vg.civcraft.mc.namelayer.command.commands.ListCurrentInvites;
import vg.civcraft.mc.namelayer.command.commands.ListGroups;
import vg.civcraft.mc.namelayer.command.commands.ListMembers;
import vg.civcraft.mc.namelayer.command.commands.ListPermissions;
import vg.civcraft.mc.namelayer.command.commands.ListPlayerTypes;
import vg.civcraft.mc.namelayer.command.commands.ListSubGroups;
import vg.civcraft.mc.namelayer.command.commands.MergeGroups;
import vg.civcraft.mc.namelayer.command.commands.ModifyPermissions;
import vg.civcraft.mc.namelayer.command.commands.RemoveBlacklist;
import vg.civcraft.mc.namelayer.command.commands.RemoveMember;
import vg.civcraft.mc.namelayer.command.commands.UnlinkGroups;
import vg.civcraft.mc.namelayer.command.commands.SetPassword;
import vg.civcraft.mc.namelayer.command.commands.ToggleAutoAcceptInvites;
import vg.civcraft.mc.namelayer.command.commands.TransferGroup;
import vg.civcraft.mc.namelayer.command.commands.PromotePlayer;
import vg.civcraft.mc.namelayer.command.commands.RevokeInvite;
import vg.civcraft.mc.namelayer.command.commands.SetDefaultGroup;
import vg.civcraft.mc.namelayer.command.commands.GetDefaultGroup;
import vg.civcraft.mc.namelayer.command.commands.UpdateName;

public class CommandHandler {
	public Map<String, Command> commands = new HashMap<String, Command>();
	
	public void registerCommands(){
		addCommands(new AcceptInvite("AcceptInvite"));
		//addCommands(new LinkGroups("LinkGroups"));
		//addCommands(new UnlinkGroups("UnlinkGroups"));
		//addCommands(new ListSubGroups("ListSubGroups"));
		addCommands(new CreateGroup("CreateGroup"));
		addCommands(new DeleteGroup("DeleteGroup"));
		addCommands(new DisciplineGroup("DisiplineGroup"));
		addCommands(new GlobalStats("GlobalStats"));
		addCommands(new GroupStats("GroupStats"));
		addCommands(new InfoDump("InfoDump"));
		addCommands(new InvitePlayer("InvitePlayer"));
		addCommands(new JoinGroup("JoinGroup"));
		addCommands(new ListGroups("ListGroups"));
		addCommands(new ListMembers("ListMembers"));
		addCommands(new ListPermissions("ListPermissions"));
		addCommands(new MergeGroups("MergeGroups"));
		addCommands(new ModifyPermissions("ModifyPermissions"));
		addCommands(new RemoveMember("RemoveMember"));
		addCommands(new SetPassword("SetPassword"));
		addCommands(new TransferGroup("TransferGroup"));
		addCommands(new LeaveGroup("LeaveGroup"));
		addCommands(new ListPlayerTypes("ListPlayerTypes"));
		addCommands(new ListCurrentInvites("ListCurrentInvites"));
		addCommands(new ToggleAutoAcceptInvites("AutoAcceptInvites"));
		addCommands(new PromotePlayer("PromotePlayer"));
		addCommands(new RevokeInvite("RevokeInvite"));
		addCommands(new ChangePlayerName("ChangePlayerName"));
		addCommands(new SetDefaultGroup("SetDefaultGroup"));
		addCommands(new GetDefaultGroup("GetDefaultGroup"));
		addCommands(new UpdateName("UpdateName"));
		addCommands(new AddBlacklist("AddBlacklist"));
		addCommands(new RemoveBlacklist("RemoveBlacklist"));
		addCommands(new ShowBlacklist("ShowBlacklist"));
		addCommands(new NameLayerGroupGui("OpenGUI"));
	}
	
	public void addCommands(Command command){
			commands.put(command.getIdentifier().toLowerCase(), command);
	}
	
	public boolean execute(CommandSender sender, org.bukkit.command.Command cmd, String[] args){
		String cmdName = cmd.getName().toLowerCase();
		if (commands.containsKey(cmdName)){
			Command command = commands.get(cmdName);
			if (args.length < command.getMinArguments() || args.length > command.getMaxArguments()){
				helpPlayer(command, sender);
				return true;
			}
			if (!NameLayerPlugin.rateLimit(((OfflinePlayer) sender).getUniqueId(), cmdName, false)) {
				command.execute(sender, args);
			} else {
				limitPlayer(cmdName, sender, false);
			}
		}
		return true;
	}

	public List<String> complete(CommandSender sender, org.bukkit.command.Command cmd, String[] args){
		String cmdName = cmd.getName().toLowerCase();
		if (commands.containsKey(cmdName)){
			Command command = commands.get(cmdName);
			if (!NameLayerPlugin.rateLimit(((OfflinePlayer) sender).getUniqueId(), cmdName, true)) {
				return command.tabComplete(sender, args);
			} else {
				limitPlayer(cmdName, sender, true);
				return null;
			}
		}
		return null;
	}

	
	public void helpPlayer(Command command, CommandSender sender){
		sender.sendMessage(new StringBuilder().append(ChatColor.RED + "Command: " ).append(command.getName()).toString());
		sender.sendMessage(new StringBuilder().append(ChatColor.RED + "Description: " ).append(command.getDescription()).toString());
		sender.sendMessage(new StringBuilder().append(ChatColor.RED + "Usage: ").append(command.getUsage()).toString());
	}

	public void limitPlayer(String cmd, CommandSender sender, boolean isTab) {
		String limitMsg = NameLayerPlugin.rateLimitMessage(cmd, isTab);
		if (limitMsg != null && !"".equals(limitMsg.trim())) {
			sender.sendMessage(new StringBuilder().append(ChatColor.RED).append(limitMsg).toString());
		}
	}
}
