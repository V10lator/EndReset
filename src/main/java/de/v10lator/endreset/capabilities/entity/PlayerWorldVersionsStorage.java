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

package de.v10lator.endreset.capabilities.entity;

import java.util.Map.Entry;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class PlayerWorldVersionsStorage implements IStorage<IPlayerWorldVersions>{

	@Override
	public NBTBase writeNBT(Capability<IPlayerWorldVersions> capability, IPlayerWorldVersions instance, EnumFacing side) {
		NBTTagCompound tag = new NBTTagCompound();
		for(Entry<Integer, Integer> entry: instance.getInternalMap().entrySet())
			tag.setInteger(Integer.toString(entry.getKey()), entry.getValue());
		return tag;
	}

	@Override
	public void readNBT(Capability<IPlayerWorldVersions> capability, IPlayerWorldVersions instance, EnumFacing side, NBTBase nbt) {
		NBTTagCompound tag = (NBTTagCompound)nbt;
		for(String key: tag.getKeySet())
			instance.set(Integer.parseInt(key), tag.getInteger(key));
	}
}
