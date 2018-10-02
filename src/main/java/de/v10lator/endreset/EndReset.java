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
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;

import de.v10lator.endreset.capabilities.entity.IPlayerWorldVersions;
import de.v10lator.endreset.capabilities.entity.PlayerWorldVersionsProvider;
import de.v10lator.endreset.capabilities.world.WorldVersionProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUnloadChunk;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.end.DragonFightManager;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

@Mod(modid = "##MODID##", name = "##NAME##", version = "##VERSION##", acceptedMinecraftVersions = "1.12.2", acceptableRemoteVersions = "*", updateJSON="http://forge.home.v10lator.de/update.json?id=##MODID##&v=##VERSION##")
public class EndReset {
	private File configFile;
	private Field dragonKilled;
	final String permResetNode = "##MODID##.command.reset";
	final String permAddRemoveNode = "##MODID##.command.addRemove";
	final String permScheduleNode = "##MODID##.command.scheduler";
	EndResetConfigHandler configHandler;
	EndResetScheduler scheduler;
	private final HashSet<Integer> unloadingDims = new HashSet<Integer>();
	
	@Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
		configFile = new File(event.getModConfigurationDirectory(), "##NAME##.cfg");
		MinecraftForge.EVENT_BUS.register(new EndResetCapabilityAttacher());
	}
	
	@Mod.EventHandler
	public void onServerStart(FMLServerStartingEvent event) {
		if(FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER)
		{
			configFile = null;
			return;
		}
		dragonKilled = ReflectionHelper.findField(DragonFightManager.class, "field_186117_k", "dragonKilled");
		scheduler = new EndResetScheduler(this);
		configHandler = new EndResetConfigHandler(this, new Configuration(configFile, "1.0"));
		PermissionAPI.registerNode(permResetNode, DefaultPermissionLevel.OP, "Use the /endreset reset command");
		PermissionAPI.registerNode(permAddRemoveNode, DefaultPermissionLevel.OP, "Use the /endreset <add|remove> commands");
		PermissionAPI.registerNode(permScheduleNode, DefaultPermissionLevel.OP, "Use the /endreset scheduler <add|remove> commands");
		event.registerServerCommand(new EndResetCommand(this));
		MinecraftForge.EVENT_BUS.register(this);
		configHandler.start();
	}
	
	@Mod.EventHandler
	public void onServerStop(FMLServerStoppingEvent event) {
		if(FMLCommonHandler.instance().getEffectiveSide() != Side.SERVER)
			return;
		MinecraftForge.EVENT_BUS.unregister(this);
		dragonKilled = null;
		configHandler.die();
		configHandler = null;
		scheduler = null;
	}
	
	private void removeRecursive(File file)
	{
		if(file.isDirectory())
			for(String f: file.list())
				removeRecursive(new File(file, f));
		file.delete();
	}
	
	void reset(World world, boolean updatePlayers)
	{
		int id = world.provider.getDimension();
		MinecraftServer server = world.getMinecraftServer();
		EntityPlayerMP[] playerList;
		PlayerList pl;
		if(updatePlayers)
		{
			// Remove players from dimension and create an array containing all EntityPlayerMP objects for this world
			pl = server.getPlayerList();
			playerList = new EntityPlayerMP[world.playerEntities.size()];
			EntityPlayer player;
			EntityPlayerMP playerMp;
			for(int i = 0; i < world.playerEntities.size(); i++)
			{
				player = world.playerEntities.get(i);
				world.onEntityRemoved(player);
				player.setWorld(null);
				playerMp = (EntityPlayerMP)player;
				playerMp.interactionManager.setWorld(null);
				playerList[i] = playerMp;
			}
		}
		else
		{
			playerList = null;
			pl = null;
		}
		// Remove NBT data
		int version = world.getCapability(WorldVersionProvider.VERSION_CAP, null).get();
		version = version == Integer.MAX_VALUE ? 0 : version + 1;
		NBTTagCompound nbt = world.getWorldInfo().getDimensionData(id);
		for(String key: nbt.getKeySet())
			nbt.removeTag(key);
		// Force-unload world
		ISaveHandler sh = world.getSaveHandler();
		File folder = new File(sh.getWorldDirectory(), world.provider.getSaveFolder());
		unloadingDims.add(id);
		world = null;
		DimensionManager.setWorld(id, null, server);
		// Remove region (anvil) data on disk
		removeRecursive(folder);
		sh.flush();
		// Create new world (will recycle old NBT data, that's why we removed it above)
		unloadingDims.remove(id);
		DimensionManager.initDimension(id);
		WorldServer newWorld = DimensionManager.getWorld(id);
		newWorld.getCapability(WorldVersionProvider.VERSION_CAP, null).set(version);
		
		if(updatePlayers)
		{
			// Add players to the new dimension and inform them about the reset
			boolean oldForceSpawn;
			for(EntityPlayerMP player: playerList)
			{
				player.setWorld(newWorld);
				oldForceSpawn = player.forceSpawn;
				player.forceSpawn = true;
				newWorld.spawnEntity(player);
				player.forceSpawn = oldForceSpawn;
                
				pl.preparePlayer(player, null);
				player.interactionManager.setWorld(newWorld);
				pl.updateTimeAndWeatherForPlayer(player, newWorld);
				
				player.getCapability(PlayerWorldVersionsProvider.VERSION_CAP, null).set(id, version);
				player.sendMessage(makeMessage(TextFormatting.YELLOW, "The world resetted!"));
			}
		}
		// Log the resetting
		LogManager.getLogger("##NAME##").info("DIM" + id + " resetted!");
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
		reset(world, false);
	}
	
	private void checkPlayerTp(World to, EntityPlayer player)
	{
		IPlayerWorldVersions versions = player.getCapability(PlayerWorldVersionsProvider.VERSION_CAP, null);
		int id = to.provider.getDimension();
		int pv = versions.get(id);
		int version = to.getCapability(WorldVersionProvider.VERSION_CAP, null).get();
		if(pv != version)
		{
			versions.set(id, version);
			if(pv > -1)
				player.sendMessage(makeMessage(TextFormatting.YELLOW, "This world has been resetted since your last visit"));
		}
	}
	
	@SubscribeEvent
	public void onTick(ServerTickEvent event)
	{
		if(event.phase == Phase.END)
			scheduler.tick();
	}
	
	@SubscribeEvent
	public void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event)
	{
		if(event.isWasDeath())
			event.getEntity().getCapability(PlayerWorldVersionsProvider.VERSION_CAP, null).integrate(event.getOriginal().getCapability(PlayerWorldVersionsProvider.VERSION_CAP, null));
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if(!event.isCanceled())
			checkPlayerTp(event.player.world, event.player);
	}
	
	// PlayerChangeDimensionEvent seems bugged, use EntityTravelToDimensionEvent instead
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onEntityDimensionChange(EntityTravelToDimensionEvent event) {
		if(!event.isCanceled() && event.getEntity() instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer)event.getEntity();
			checkDim(player.world, player);
			checkPlayerTp(event.getEntity().getServer().getWorld(event.getDimension()), player);
		}
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onPlayerDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		if(!event.isCanceled())
		{
			MinecraftServer server = event.player.getServer();
			checkDim(server.getWorld(event.fromDim), event.player);
			checkPlayerTp(server.getWorld(event.toDim), event.player);
		}
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
	
	/*
	 * The server doesn't send chunk unload packets for chunks out of the players view-distance
	 * but the client might have cached chunks farer away, so send unload packets to force
	 * the player to reload the chunk when it comes into view-distance again.
	 * TODO: We're a bit lazy on this and send packets also for chunks in view. So there might
	 * be a small optimization potential here
	 */
	@SubscribeEvent
	public void onChunkUnload(ChunkEvent.Unload event)
	{
		World world = event.getWorld();
		if(!unloadingDims.contains(world.provider.getDimension()))
			return;
		Chunk chunk = event.getChunk();
		SPacketUnloadChunk packet = new SPacketUnloadChunk(chunk.x, chunk.z);
		for(EntityPlayer player: world.playerEntities)
			((EntityPlayerMP)player).connection.sendPacket(packet);
	}
}
