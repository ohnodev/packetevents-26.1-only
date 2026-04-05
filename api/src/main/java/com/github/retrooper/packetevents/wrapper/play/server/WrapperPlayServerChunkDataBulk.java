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

package com.github.retrooper.packetevents.wrapper.play.server;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

public class WrapperPlayServerChunkDataBulk extends PacketWrapper<WrapperPlayServerChunkDataBulk> {
    private int[] x;
    private int[] z;
    private BaseChunk[][] chunks;
    private byte[][] biomeData;

    public WrapperPlayServerChunkDataBulk(PacketSendEvent event) {
        super(event);
    }

    @Override
    public void read() {
        throw new UnsupportedOperationException("MAP_CHUNK_BULK is not supported in this 26.1-only fork");
    }

    @Override
    public void write() {
        throw new UnsupportedOperationException("MAP_CHUNK_BULK is not supported in this 26.1-only fork");
    }

    @Override
    public void copy(WrapperPlayServerChunkDataBulk wrapper) {
        this.x = wrapper.x;
        this.z = wrapper.z;
        this.chunks = wrapper.chunks;
        this.biomeData = wrapper.biomeData;
    }

    public int[] getX() {
        return x;
    }

    public int[] getZ() {
        return z;
    }

    public BaseChunk[][] getChunks() {
        return chunks;
    }

    public byte[][] getBiomeData() {
        return biomeData;
    }
}
