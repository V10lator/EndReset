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

package de.v10lator.endreset.capabilities.world;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.Capability.IStorage;

public class WorldVersionStorage implements IStorage<IWorldVersion>{

	@Override
	public NBTBase writeNBT(Capability<IWorldVersion> capability, IWorldVersion instance, EnumFacing side) {
		return new NBTTagInt(instance.get());
	}

	@Override
	public void readNBT(Capability<IWorldVersion> capability, IWorldVersion instance, EnumFacing side, NBTBase nbt) {
		instance.set(((NBTPrimitive)nbt).getInt());
	}
}
