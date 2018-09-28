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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;

class EndResetScheduler {
	private final HashMap<Integer, Long> worlds = new HashMap<Integer, Long>();
	private final EndReset mod;
	private final MinecraftServer server;
	private final ArrayList<Integer> dims = new ArrayList<Integer>(); // We need this to prevent double-locking of the config
	
	EndResetScheduler(EndReset mod)
	{
		this.mod = mod;
		server = FMLCommonHandler.instance().getMinecraftServerInstance();
	}
	
	void tick()
	{
		Entry<Integer, Long> entry;
		long ticks;
		int dim;
		Configuration config = mod.configHandler.getLockedConfig();
		for(Iterator<Entry<Integer, Long>> iter = worlds.entrySet().iterator(); iter.hasNext();)
		{
			entry = iter.next();
			ticks = entry.getValue() - 1L;
			if(ticks == 0L)
			{
				dim = entry.getKey();
				dims.add(dim);
				ticks = config.get("schedulers", Integer.toString(dim), 0).getLong() * 20L;
			}
			entry.setValue(ticks);
		}
		mod.configHandler.releaseLock();
		if(!dims.isEmpty())
		{
			for(int id: dims)
				mod.reset(server.getWorld(id), true);
			dims.clear();
		}
	}
	
	void set(int dim, long ticks)
	{
		worlds.put(dim, ticks);
	}
	
	void remove(int dim)
	{
		worlds.remove(dim);
	}
	
	boolean isScheduled(int dim)
	{
		return worlds.containsKey(dim);
	}
	
	void clear()
	{
		worlds.clear();
	}
}
