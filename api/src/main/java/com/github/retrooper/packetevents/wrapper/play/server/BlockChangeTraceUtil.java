/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2026 retrooper and contributors
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

package com.github.retrooper.packetevents.wrapper.play.server;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

import java.util.logging.Level;

final class BlockChangeTraceUtil {
    private static final boolean DEBUG_BLOCK_TRACE = Boolean.getBoolean("packetevents.debug.block-change.trace");

    private BlockChangeTraceUtil() {
    }

    static boolean shouldDebugTraceBlockUpdates() {
        if (PacketEvents.getAPI() == null) {
            return false;
        }
        return DEBUG_BLOCK_TRACE
                && PacketEvents.getAPI().getSettings().isDebugEnabled()
                && PacketEvents.getAPI().getLogger().isLoggable(Level.FINE);
    }

    static String getStateNameSafe(ServerVersion serverVersion, int blockId) {
        try {
            return WrappedBlockState.getByGlobalId(serverVersion.toClientVersion(), blockId).getType().getName();
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    static boolean isSulfurFamily(String stateName) {
        String normalized = stateName.toLowerCase();
        return normalized.contains("sulfur") || normalized.contains("cinnabar");
    }
}
