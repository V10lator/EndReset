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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.FMLCommonHandler;

public class EndResetConfigHandler extends Thread {
	private boolean running = true;
	private final EndReset mod;
	private final Configuration config;
	private final AtomicBoolean lock = new AtomicBoolean(false);
	
	EndResetConfigHandler(EndReset mod, Configuration config)
	{
		this.mod = mod;
		this.config = config;
		reloadConfig();
	}
	
	void die()
	{
		running = false;
		this.interrupt();
		while(!lock.compareAndSet(false, true))
		{
			try
			{
				Thread.sleep(1L);
			}
			catch (InterruptedException e) {}
		}
		if(config.hasChanged())
			config.save();
	}
	
	@Override
	public void run()
	{
		while(running)
		{
			while(!lock.compareAndSet(false, true))
			{
				try
				{
					Thread.sleep(5L);
				}
				catch (InterruptedException e) {
					if(!running)
						return;
				}
			}
			if(config.hasChanged())
				config.save();
			releaseLock();
			try
			{
				Thread.sleep(300000L);
			}
			catch (InterruptedException e) {
			}
		}
	}
	
	void reloadConfig()
	{
		getLockedConfig();
		config.load();
		int c = 0;
		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		int id;
		World world;
		Entry<String, Property> entry;
		if(config.hasCategory("worlds"))
		{
			for(Iterator<Entry<String, Property>> iter = config.getCategory("worlds").entrySet().iterator(); iter.hasNext(); )
			{
				entry = iter.next();
				if(!entry.getValue().getBoolean())
				{
					iter.remove();
					c++;
					continue;
				}
				try
				{
					id = Integer.parseInt(entry.getKey());
				}
				catch(NumberFormatException e)
				{
					iter.remove();
					c++;
					continue;
				}
				world = server.getWorld(id);
				if(world == null || world.provider.getDimensionType() != DimensionType.THE_END)
				{
					iter.remove();
					c++;
				}
			}
		}
		mod.scheduler.clear();
		long secs;
		if(config.hasCategory("schedulers"))
		{
			for(Iterator<Entry<String, Property>> iter = config.getCategory("schedulers").entrySet().iterator(); iter.hasNext(); )
			{
				entry = iter.next();
				secs = entry.getValue().getLong();
				if(secs < 1)
				{
					iter.remove();
					c++;
					continue;
				}
				try
				{
					id = Integer.parseInt(entry.getKey());
				}
				catch(NumberFormatException e)
				{
					iter.remove();
					c++;
					continue;
				}
				world = server.getWorld(id);
				if(world == null)
				{
					iter.remove();
					c++;
					continue;
				}
				mod.scheduler.set(id, secs * 20L);
			}
		}
		releaseLock();
		if(c > 0)
			server.sendMessage(mod.makeMessage(TextFormatting.RED, c + " invalid config entries deleted!"));
	}
	
	Configuration getLockedConfig()
	{
		while(!lock.compareAndSet(false, true))
		{
			try {
				Thread.sleep(1);
			}
			catch (InterruptedException e) {}
		}
		return config;
	}
	
	void releaseLock()
	{
		lock.set(false);
	}
}
