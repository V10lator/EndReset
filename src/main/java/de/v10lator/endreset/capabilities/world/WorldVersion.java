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

import java.util.concurrent.Callable;

public class WorldVersion implements IWorldVersion, Callable<IWorldVersion> {
	private int version = 0;

	@Override
	public int get() {
		return this.version;
	}

	@Override
	public void set(int version) {
		this.version = version;
	}

	@Override
	public IWorldVersion call() throws Exception {
		return new WorldVersion();
	}
}
