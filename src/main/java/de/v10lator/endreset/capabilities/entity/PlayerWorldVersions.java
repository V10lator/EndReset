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

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

public class PlayerWorldVersions implements IPlayerWorldVersions, Callable<IPlayerWorldVersions> {
	private final HashMap<Integer, Long> versions = new HashMap<Integer, Long>();
	
	@Override
	public IPlayerWorldVersions call() throws Exception {
		return new PlayerWorldVersions();
	}

	@Override
	public long get(int dimension) {
		return has(dimension) ? versions.get(dimension) : -1L;
	}

	@Override
	public void set(int dimension, long version) {
		versions.put(dimension, version);
	}

	@Override
	public boolean has(int dimension) {
		return versions.containsKey(dimension);
	}

	@Override
	public HashMap<Integer, Long> getInternalMap() {
		return this.versions;
	}

	@Override
	public void integrate(IPlayerWorldVersions versions) {
		for(Entry<Integer, Long> entry: versions.getInternalMap().entrySet())
			this.versions.put(entry.getKey(), entry.getValue());
	}
}
