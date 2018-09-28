package de.v10lator.endreset;

import de.v10lator.endreset.capabilities.entity.IPlayerWorldVersions;
import de.v10lator.endreset.capabilities.entity.PlayerWorldVersions;
import de.v10lator.endreset.capabilities.entity.PlayerWorldVersionsProvider;
import de.v10lator.endreset.capabilities.entity.PlayerWorldVersionsStorage;
import de.v10lator.endreset.capabilities.world.IWorldVersion;
import de.v10lator.endreset.capabilities.world.WorldVersion;
import de.v10lator.endreset.capabilities.world.WorldVersionProvider;
import de.v10lator.endreset.capabilities.world.WorldVersionStorage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

public class EndResetCapabilityAttacher {
	private final ResourceLocation worldVersionCap = new ResourceLocation("##MODID##", "WorldVersion");
	private final ResourceLocation playerWorldVersionsCap = new ResourceLocation("##MODID##", "PlayerWorldVersions");
	private boolean worldRegistered = false;
	private boolean playerRegistered = false;
	
	@SubscribeEvent
	public void onWorldAttachCap(AttachCapabilitiesEvent<World> event)
	{
		if(FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER)
		{
			if(!worldRegistered)
			{
				CapabilityManager.INSTANCE.register(IWorldVersion.class, new WorldVersionStorage(), new WorldVersion());
				worldRegistered = true;
			}
			event.addCapability(worldVersionCap, new WorldVersionProvider());
		}
	}
	
	@SubscribeEvent
	public void onEntityAttachCap(AttachCapabilitiesEvent<Entity> event)
	{
		if(FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER && event.getObject() instanceof EntityPlayer)
		{
			if(!playerRegistered)
			{
				CapabilityManager.INSTANCE.register(IPlayerWorldVersions.class, new PlayerWorldVersionsStorage(), new PlayerWorldVersions());
				playerRegistered = true;
			}
			event.addCapability(playerWorldVersionsCap, new PlayerWorldVersionsProvider());
		}
	}
}
