/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2022 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.retrooper.packetevents.protocol.world.chunk;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_16.Chunk_v1_9;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_7.Chunk_v1_7;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v1_8.Chunk_v1_8;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.ListPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import com.github.retrooper.packetevents.protocol.world.chunk.storage.LegacyFlexibleStorage;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateValue;

public interface BaseChunk {
    int getBlockId(int x, int y, int z);

    default WrappedBlockState get(ClientVersion version, int x, int y, int z) {
        return get(version, x, y, z, true);
    }

    default WrappedBlockState get(ClientVersion version, int x, int y, int z, boolean clone) {
        return WrappedBlockState.getByGlobalId(version, getBlockId(x, y, z), clone);
    }

    default WrappedBlockState get(int x, int y, int z) {
        return get(x, y, z, true);
    }

    default WrappedBlockState get(int x, int y, int z, boolean clone) {
        return get(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion(), x, y, z, clone);
    }

    default void set(int x, int y, int z, WrappedBlockState state) {
        set(x, y, z, state.getGlobalId());
    }

    void set(int x, int y, int z, int combinedID);

    // We don't use ClientVersion, but it's here to maintain backwards compatibility.
    default void set(ClientVersion version, int x, int y, int z, int combinedID) {
        set(x, y, z, combinedID);
    }

    boolean isEmpty();

    default boolean hasFluid() {
        ClientVersion version = PacketEvents.getAPI().getServerManager().getVersion().toClientVersion();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int blockId = getBlockId(x, y, z);
                    if (blockId == 0) {
                        continue;
                    }
                    WrappedBlockState state = WrappedBlockState.getByGlobalId(version, blockId);
                    if (state.getType() == StateTypes.WATER || state.getType() == StateTypes.LAVA) {
                        return true;
                    }
                    if (state.hasProperty(StateValue.WATERLOGGED) && state.isWaterlogged()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static BaseChunk create() {
        ServerVersion version = PacketEvents.getAPI().getServerManager().getVersion();
        if (version.isNewerThanOrEquals(ServerVersion.V_1_18)) {
            return new Chunk_v1_18();
        } else if (version.isNewerThanOrEquals(ServerVersion.V_1_16)) {
            return new Chunk_v1_9(0, PaletteType.CHUNK.create());
        } else if (version.isNewerThanOrEquals(ServerVersion.V_1_9)) {
            return new Chunk_v1_9(0, new DataPalette(new ListPalette(4), new LegacyFlexibleStorage(4, 4096), PaletteType.CHUNK));
        } else if (version.isNewerThanOrEquals(ServerVersion.V_1_8)) {
            return new Chunk_v1_8(new ShortArray3d(4096), null, null);
        }
        return new Chunk_v1_7(false, true);
    }
}
