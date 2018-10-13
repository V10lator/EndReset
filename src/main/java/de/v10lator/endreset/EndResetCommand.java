/*
 * This file is part of EndReset.
 *
 * EndReset is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * EndReset is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with EndReset.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package de.v10lator.endreset;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.server.permission.PermissionAPI;

public class EndResetCommand extends CommandBase {
	private final EndReset mod;
	
	EndResetCommand(EndReset mod)
	{
		this.mod = mod;
	}
	
	@Override
	public String getName() {
		return "endreset";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		boolean canReset, canAddRemove, canSchedule;
		if(sender instanceof EntityPlayer)
		{
			EntityPlayer p = (EntityPlayer)sender;
			canReset = PermissionAPI.hasPermission(p, mod.permResetNode);
			canAddRemove = PermissionAPI.hasPermission(p, mod.permAddRemoveNode);
			canSchedule = PermissionAPI.hasPermission(p, mod.permScheduleNode);
		}
		else
			canReset = canAddRemove = canSchedule = true;
		
		if(canReset)
		{
			if(canAddRemove)
				return canSchedule ? "/endreset <reset|add|remove|schedule>" : "/endreset <reset|add|remove> ID";
			else 
				return canSchedule ? "/endreset <reset|schedule>" : "/endreset reset ID";
		}
		return canAddRemove ? canSchedule ? "/endreset <add|remove|schedule>" : "/endreset <add|remove> ID" : "/endreset";
	}
	
	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		if(sender instanceof EntityPlayer)
		{
			EntityPlayer p = (EntityPlayer)sender;
			return PermissionAPI.hasPermission(p, mod.permResetNode) || PermissionAPI.hasPermission(p, mod.permAddRemoveNode) || PermissionAPI.hasPermission(p, mod.permScheduleNode);
		}
		return true;
	}
	
	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if(args.length < 1)
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, getUsage(sender)));
			return;
		}
		args[0] = args[0].toLowerCase();
		switch(args[0])
		{
			case "reset":
				resetCommand(server, sender, args);
				break;
			case "add":
				addCommand(server, sender, args);
				break;
			case "remove":
				removeCommand(server, sender, args);
				break;
			case "scheduler":
				schedulerCommand(server, sender, args);
				break;
			case "reload":
				reloadCommand(server, sender, args);
				break;
			default:
				sender.sendMessage(mod.makeMessage(TextFormatting.RED, getUsage(sender)));
				break;
		}
	}
	
	private World getWorldFromArgs(MinecraftServer server, ICommandSender sender, String[] args, int i)
	{
		World ret = null;
		if(args.length < i + 1)
		{
			if(!(sender instanceof EntityPlayer))
			{
				sender.sendMessage(mod.makeMessage(TextFormatting.RED, getUsage(sender)));
				return null;
			}
			ret = ((EntityPlayer)sender).world;
		}
		else
		{
			try
			{
				ret = server.getWorld(Integer.parseInt(args[i]));
			}
			catch(NumberFormatException e)
			{
				sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Invalid dimension: " + args[i]));
				return null;
			}
		}
		
		if(ret == null)
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Invalid dimension!"));
		return ret;
	}
	
	private void reloadCommand(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if(sender instanceof EntityPlayer && !PermissionAPI.hasPermission((EntityPlayer)sender, mod.permReloadNode))
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "No permission to use the scheduler command!"));
			return;
		}
		
		mod.configHandler.reloadConfig();
		sender.sendMessage(mod.makeMessage(TextFormatting.GREEN, "Configuration reloaded!"));
	}
	
	private void schedulerAddCommand(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if(args.length < 4)
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "/endreset scheduler add ID seconds"));
			return;
		}
		World tr = getWorldFromArgs(server, sender, args, 2);
		if(tr == null)
			return;
		int seconds;
		try
		{
			seconds = Integer.parseInt(args[3]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Invalid seconds: " + args[3]));
			return;
		}
		if(seconds < 1)
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Invalid seconds: " + seconds));
			return;
		}
		int id = tr.provider.getDimension();
		mod.configHandler.getLockedConfig().get("schedulers", Integer.toString(id), seconds).set(seconds);
		mod.configHandler.releaseLock();
		mod.scheduler.set(id, ((long)seconds) * 20L);
		sender.sendMessage(mod.makeMessage(TextFormatting.GREEN, "DIM" + id + " scheduled each " + seconds + " seconds!"));
	}
	
	private void schedulerDeleteCommand(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if(args.length < 3)
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "/endreset scheduler remove ID"));
			return;
		}
		int id;
		try
		{
			id = Integer.parseInt(args[2]);
		}
		catch(NumberFormatException e)
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Invalid ID: " + args[2]));
			return;
		}
		if(!mod.scheduler.isScheduled(id))
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Invalid ID: " + id));
			return;
		}
		mod.scheduler.remove(id);
		mod.configHandler.getLockedConfig().getCategory("schedulers").remove(Integer.toString(id));
		mod.configHandler.releaseLock();
		sender.sendMessage(mod.makeMessage(TextFormatting.GREEN, "DIM" + id + " removed from scheduler!"));
	}
	
	private void schedulerCommand(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if(sender instanceof EntityPlayer && !PermissionAPI.hasPermission((EntityPlayer)sender, mod.permScheduleNode))
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "No permission to use the scheduler command!"));
			return;
		}
		if(args.length < 2)
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "/endreset scheduler <add|remove>"));
			return;
		}
		args[1] = args[1].toLowerCase();
		switch(args[1])
		{
			case "add":
				schedulerAddCommand(server, sender, args);
				break;
			case "remove":
				schedulerDeleteCommand(server, sender, args);
				break;
			default:
				sender.sendMessage(mod.makeMessage(TextFormatting.RED, "/endreset scheduler <add|remove>"));
				break;
		}
			
	}
	
	private void removeCommand(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if(sender instanceof EntityPlayer && !PermissionAPI.hasPermission((EntityPlayer)sender, mod.permAddRemoveNode))
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "No permission to use the add command!"));
			return;
		}
		World tr = getWorldFromArgs(server, sender, args, 1);
		if(tr == null)
			return;
		String id = Integer.toString(tr.provider.getDimension());
		
		ConfigCategory cat = mod.configHandler.getLockedConfig().getCategory("worlds");
		if(cat == null || !cat.containsKey(id))
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Can't remove DIM" + id + " (try adding it first)"));
			mod.configHandler.releaseLock();
			return;
		}
		cat.remove(id);
		mod.configHandler.releaseLock();
		sender.sendMessage(mod.makeMessage(TextFormatting.GREEN, "DIM" + id + " added!"));
	}
	
	private void addCommand(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if(sender instanceof EntityPlayer && !PermissionAPI.hasPermission((EntityPlayer)sender, mod.permAddRemoveNode))
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "No permission to use the add command!"));
			return;
		}
		World tr = getWorldFromArgs(server, sender, args, 1);
		if(tr == null)
			return;
		String id = Integer.toString(tr.provider.getDimension());
		mod.configHandler.getLockedConfig().get("worlds", id, true);
		mod.configHandler.releaseLock();
		sender.sendMessage(mod.makeMessage(TextFormatting.GREEN, "DIM" + id + " added!"));
	}
	
	private void resetCommand(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if(sender instanceof EntityPlayer && !PermissionAPI.hasPermission((EntityPlayer)sender, mod.permResetNode))
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "No permission to use the reset command!"));
			return;
		}
		World tr = getWorldFromArgs(server, sender, args, 1);
		if(tr == null)
			return;
		
		int id = tr.provider.getDimension();
		if(id == 0)
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Can't reset the overworld!"));
			return;
		}
		
		mod.reset(tr, true);
		sender.sendMessage(mod.makeMessage(TextFormatting.GREEN, "DIM" + id + " resetted!"));
	}
}
