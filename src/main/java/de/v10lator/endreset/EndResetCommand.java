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

import java.util.ArrayList;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.Teleporter;
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
		boolean canReset, canAddRemove;
		if(sender instanceof EntityPlayer)
		{
			EntityPlayer p = (EntityPlayer)sender;
			canReset = PermissionAPI.hasPermission(p, mod.permResetNode);
			canAddRemove = PermissionAPI.hasPermission(p, mod.permAddRemoveNode);
		}
		else
			canReset = canAddRemove = true;
		if(canReset)
		{
			if(canAddRemove)
				return "/endreset <reset|add|remove> ID";
			return "/endreset reset ID";
		}
		if(canAddRemove)
			return "/endreset <add|remove> ID";
		return "/endreset";
	}
	
	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		if(sender instanceof EntityPlayer)
		{
			EntityPlayer p = (EntityPlayer)sender;
			return PermissionAPI.hasPermission(p, mod.permResetNode) || PermissionAPI.hasPermission(p, mod.permAddRemoveNode);
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
			default:
				sender.sendMessage(mod.makeMessage(TextFormatting.RED, getUsage(sender)));
				break;
		}
	}
	
	private World getWorldFromArgs(MinecraftServer server, ICommandSender sender, String[] args)
	{
		World ret = null;
		if(args.length < 2)
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
				ret = server.getWorld(Integer.parseInt(args[1]));
			}
			catch(NumberFormatException e)
			{
				sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Invalid dimension: " + args[1]));
				return null;
			}
		}
		
		if(ret == null)
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Invalid dimension!"));
		return ret;
	}
	
	private void removeCommand(MinecraftServer server, ICommandSender sender, String[] args)
	{
		if(sender instanceof EntityPlayer && !PermissionAPI.hasPermission((EntityPlayer)sender, mod.permAddRemoveNode))
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "No permission to use the add command!"));
			return;
		}
		World tr = getWorldFromArgs(server, sender, args);
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
		World tr = getWorldFromArgs(server, sender, args);
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
		World tr = getWorldFromArgs(server, sender, args);
		if(tr == null)
			return;
		
		int id = tr.provider.getDimension();
		if(id == 0)
		{
			sender.sendMessage(mod.makeMessage(TextFormatting.RED, "Can't reset the overworld!"));
			return;
		}
		
		PlayerList pl = server.getPlayerList();
		Teleporter tp = new Teleporter(server.getWorld(0))
		{
			@Override
		    public void placeEntity(World world, Entity entity, float yaw)
		    {
				BlockPos to = world.getSpawnPoint();
		        entity.setPosition(to.getX(), to.getY(), to.getZ());
			}
		};
		for(EntityPlayer p: new ArrayList<EntityPlayer>(tr.playerEntities))
			pl.transferPlayerToDimension((EntityPlayerMP)p, 0, tp);
		mod.reset(tr);
		sender.sendMessage(mod.makeMessage(TextFormatting.GREEN, "DIM" + id + " resetted!"));
	}
}
