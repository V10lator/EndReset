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

import java.io.File;
import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.end.DragonFightManager;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

@Mod(modid = "##MODID##", name = "##NAME##", version = "##VERSION##", acceptedMinecraftVersions = "1.12.2", acceptableRemoteVersions = "*")
public class EndReset {
	private File configFile;
	private Field dragonKilled, delegate;
	final String permResetNode = "##MODID##.command.reset";
	final String permAddRemoveNode = "##MODID##.command.addRemove";
	EndResetConfigHandler configHandler;
	
	@Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
		configFile = new File(event.getModConfigurationDirectory(), "##NAME##.cfg");
	}
	
	@Mod.EventHandler
	public void onServerStart(FMLServerStartingEvent event) {
		if(FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER)
		{
			configFile = null;
			return;
		}
		dragonKilled = ReflectionHelper.findField(DragonFightManager.class, "field_186117_k", "dragonKilled");
		delegate = ReflectionHelper.findField(WorldServerMulti.class, "field_175743_a", "delegate");
		configHandler = new EndResetConfigHandler(this, new Configuration(configFile, "1.0"));
		PermissionAPI.registerNode(permResetNode, DefaultPermissionLevel.OP, "Use the /endreset reset command");
		PermissionAPI.registerNode(permAddRemoveNode, DefaultPermissionLevel.OP, "Use the /endreset <add|remove> commands");
		event.registerServerCommand(new EndResetCommand(this));
		MinecraftForge.EVENT_BUS.register(this);
		configHandler.start();
	}
	
	@Mod.EventHandler
	public void onServerStop(FMLServerStoppingEvent event) {
		if(FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER)
			return;
		MinecraftForge.EVENT_BUS.unregister(this);
		dragonKilled = delegate = null;
		configHandler.die();
		configHandler = null;
	}
	
	private void removeRecursive(File file)
	{
		if(file.isDirectory())
			for(String f: file.list())
				removeRecursive(new File(file, f));
		file.delete();
	}
	
	World reset(World world)
	{
		int id = world.provider.getDimension();
		MinecraftServer server = world.getMinecraftServer();
		// Remove NBT data
		NBTTagCompound nbt = world.getWorldInfo().getDimensionData(id);
		for(String key: nbt.getKeySet())
			nbt.removeTag(key);
		// Force-unload world
		ISaveHandler sh = world.getSaveHandler();
		DimensionManager.setWorld(id, null, server);
		// Remove region (anvil) data on disk
		removeRecursive(new File(sh.getWorldDirectory(), world.provider.getSaveFolder()));
		// Create new world (will recycle old NBT data, that's why we removed it above)
		try
		{
			world = new WorldServerMulti(server, sh, id, (WorldServer)delegate.get(world), server.profiler).init();
		}
		catch (Exception e)
		{
			Logger logger = LogManager.getLogger("##NAME##");
			logger.error("Internal (reflection) error!");
			logger.catching(e);
			DimensionManager.setWorld(id, (WorldServer)world, server);
		}
		return world;
	}
	
	private void checkDim(World world, EntityPlayer ignore)
	{
		if(world.provider.getDimensionType() != DimensionType.THE_END)
			return;
		ConfigCategory cat = configHandler.getLockedConfig().getCategory("worlds");
		if(cat == null || !cat.containsKey(Integer.toString(world.provider.getDimension())))
		{
			configHandler.releaseLock();
			return;
		}
		configHandler.releaseLock();
		try
		{
			if(!dragonKilled.getBoolean(((WorldProviderEnd)world.provider).getDragonFightManager()))
				return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
		if(!world.playerEntities.isEmpty())
			for(EntityPlayer player: world.playerEntities)
				if(player != ignore)
					return;
		reset(world);
	}
	
	// PlayerChangeDimensionEvent seems bugged, use EntityTravelToDimensionEvent instead
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onDimensionChange(EntityTravelToDimensionEvent event) {
		if(!event.isCanceled() && event.getEntity() instanceof EntityPlayer)
			checkDim(event.getEntity().world, (EntityPlayer)event.getEntity());
	}
	
	@SubscribeEvent
	public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		checkDim(event.player.world, event.player);
	}
	
	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		if(event.getEntity() instanceof EntityPlayer)
			checkDim(event.getEntity().world, (EntityPlayer)event.getEntity());
	}
	
	TextComponentString makeMessage(TextFormatting color, String message) {
		TextComponentString ret = new TextComponentString(message);
		ret.setStyle((new Style()).setColor(color));
		return ret;
	}
}
