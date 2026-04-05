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
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.HeightmapType;
import com.github.retrooper.packetevents.protocol.world.chunk.LightData;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.reader.impl.ChunkReader_v1_18;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.util.Map;

public class WrapperPlayServerChunkData extends PacketWrapper<WrapperPlayServerChunkData> {
    private static final ChunkReader_v1_18 CHUNK_READER_V1_18 = new ChunkReader_v1_18();

    private Column column;
    private LightData lightData;
    // Kept for API compatibility with existing constructors/getters.
    private boolean ignoreOldData;

    public WrapperPlayServerChunkData(PacketSendEvent event) {
        super(event);
    }

    public WrapperPlayServerChunkData(Column column) {
        this(column, null, false);
    }

    public WrapperPlayServerChunkData(Column column, LightData lightData) {
        this(column, lightData, false);
    }

    public WrapperPlayServerChunkData(Column column, LightData lightData, boolean ignoreOldData) {
        super(PacketType.Play.Server.CHUNK_DATA);
        this.column = column;
        this.lightData = lightData;
        this.ignoreOldData = ignoreOldData;
    }

    @Override
    public void read() {
        int chunkX = readInt();
        int chunkZ = readInt();
        int chunkSize = user.getTotalWorldHeight() >> 4;
        int dataLength = this.readVarInt();

        NBTCompound heightmapsNbt = null;
        Map<HeightmapType, long[]> modernHeightmaps = null;
        if (this.serverVersion.isNewerThanOrEquals(ServerVersion.V_1_21_5)) {
            modernHeightmaps = this.readMap(HeightmapType::read, PacketWrapper::readLongArray);
        } else {
            heightmapsNbt = this.readNBT();
        }

        int expectedReaderIndex = ByteBufHelper.readerIndex(this.buffer) + dataLength;
        BaseChunk[] chunks = CHUNK_READER_V1_18.read(
                this.user.getDimensionType(),
                null,
                null,
                true,
                false,
                false,
                chunkSize,
                dataLength,
                this
        );

        int readerIndex = ByteBufHelper.readerIndex(this.buffer);
        if (expectedReaderIndex != readerIndex) {
            if (expectedReaderIndex < readerIndex) {
                throw new RuntimeException(
                        "Error while decoding chunk at " + chunkX + " " + chunkZ
                                + "; expected reader index " + expectedReaderIndex + ", got " + readerIndex
                );
            }
            ByteBufHelper.readerIndex(this.buffer, expectedReaderIndex);
        }

        int tileEntityCount = readVarInt();
        TileEntity[] tileEntities = new TileEntity[tileEntityCount];
        for (int i = 0; i < tileEntities.length; i++) {
            tileEntities[i] = new TileEntity(readByte(), readShort(), readVarInt(), readNBT());
        }

        this.lightData = LightData.read(this);
        if (modernHeightmaps != null) {
            this.column = new Column(chunkX, chunkZ, true, chunks, tileEntities, modernHeightmaps);
        } else {
            this.column = new Column(chunkX, chunkZ, true, chunks, tileEntities, heightmapsNbt);
        }
    }

    @Override
    public void write() {
        writeInt(column.getX());
        writeInt(column.getZ());

        BaseChunk[] chunks = column.getChunks();
        Object originalBuffer = this.buffer;
        Object dataBuffer = ByteBufHelper.allocateNewBuffer(this.buffer);
        this.buffer = dataBuffer;

        for (BaseChunk chunk : chunks) {
            Chunk_v1_18.write(this, (Chunk_v1_18) chunk);
        }

        this.buffer = originalBuffer;

        if (this.serverVersion.isOlderThan(ServerVersion.V_1_21_6)
                && this.serverVersion.isNewerThanOrEquals(ServerVersion.V_1_21_5)) {
            int zeroBytes = ChunkReader_v1_18.getMojangZeroByteSuffixLength(chunks);
            int newWriterIndex = ByteBufHelper.writerIndex(dataBuffer) + zeroBytes;
            if (newWriterIndex > ByteBufHelper.capacity(dataBuffer)) {
                ByteBufHelper.capacity(dataBuffer, newWriterIndex);
            }
            ByteBufHelper.writerIndex(dataBuffer, newWriterIndex);
        }

        if (this.serverVersion.isNewerThanOrEquals(ServerVersion.V_1_21_5)) {
            this.writeMap(this.column.getHeightmaps(), HeightmapType::write, PacketWrapper::writeLongArray);
        } else {
            this.writeNBT(this.column.getHeightMaps());
        }

        this.writeVarInt(ByteBufHelper.readableBytes(dataBuffer));
        ByteBufHelper.writeBytes(this.buffer, dataBuffer);
        ByteBufHelper.release(dataBuffer);

        writeVarInt(column.getTileEntities().length);
        for (TileEntity tileEntity : column.getTileEntities()) {
            writeByte(tileEntity.getPackedByte());
            writeShort(tileEntity.getYShort());
            writeVarInt(tileEntity.getType());
            writeNBT(tileEntity.getNBT());
        }

        LightData.write(this, lightData);
    }

    @Override
    public void copy(WrapperPlayServerChunkData wrapper) {
        this.column = wrapper.column;
        this.lightData = wrapper.lightData != null ? wrapper.lightData.clone() : null;
        this.ignoreOldData = wrapper.ignoreOldData;
    }

    public Column getColumn() {
        return column;
    }

    public void setColumn(Column column) {
        this.column = column;
    }

    public LightData getLightData() {
        return lightData;
    }

    public void setLightData(LightData lightData) {
        this.lightData = lightData;
    }

    public boolean isIgnoreOldData() {
        return ignoreOldData;
    }

    public void setIgnoreOldData(boolean ignoreOldData) {
        this.ignoreOldData = ignoreOldData;
    }
}
