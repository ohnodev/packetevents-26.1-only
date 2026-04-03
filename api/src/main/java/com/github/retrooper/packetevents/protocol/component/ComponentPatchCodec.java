/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2025 retrooper and contributors
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

package com.github.retrooper.packetevents.protocol.component;

import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ComponentPatchCodec {
    // Mirrors Mojang's conservative expected-size bound in DataComponentPatch.
    private static final int MAX_COMPONENT_PATCH_ENTRIES = 65536;

    private ComponentPatchCodec() {
    }

    public static @Nullable PatchableComponentMap readPatchMap(PacketWrapper<?> wrapper, ItemType itemType, boolean lengthPrefixed) {
        int presentCount = wrapper.readVarInt();
        int absentCount = wrapper.readVarInt();
        int total = validatePatchCounts(presentCount, absentCount);
        if (total == 0) {
            return null;
        }

        PatchableComponentMap components = new PatchableComponentMap(
                itemType.getComponents(wrapper.getServerVersion().toClientVersion()),
                new HashMap<>(total),
                wrapper.getRegistryHolder());

        for (int i = 0; i < presentCount; i++) {
            ComponentType<?> type = wrapper.readMappedEntity(ComponentTypes.getRegistry());
            int expectedReaderIndex;
            if (lengthPrefixed) {
                int size = wrapper.readVarInt();
                if (size < 0) {
                    throw new RuntimeException("Negative component size " + size + " for " + type.getName());
                }
                if (size > ByteBufHelper.readableBytes(wrapper.buffer)) {
                    throw new RuntimeException("Component size " + size + " for " + type.getName() + " out of bounds");
                }
                expectedReaderIndex = ByteBufHelper.readerIndex(wrapper.buffer) + size;
            } else {
                expectedReaderIndex = -1;
            }

            Object value = type.read(wrapper);
            if (expectedReaderIndex != -1) {
                int readerIndex = ByteBufHelper.readerIndex(wrapper.buffer);
                if (readerIndex != expectedReaderIndex) {
                    throw new RuntimeException("Invalid component read for " + type.getName() + "; expected reader index "
                            + expectedReaderIndex + ", got reader index " + readerIndex);
                }
            }

            components.set(castComponentType(type), value);
        }

        for (int i = 0; i < absentCount; i++) {
            components.unset(wrapper.readMappedEntity(ComponentTypes.getRegistry()));
        }

        return components;
    }

    public static boolean writePatchMap(PacketWrapper<?> wrapper, ItemStack stack, boolean lengthPrefixed) {
        if (!stack.hasComponentPatches()) {
            wrapper.writeVarInt(0);
            wrapper.writeVarInt(0);
            return false;
        }

        Map<ComponentType<?>, Optional<?>> allPatches = stack.getComponents().getPatches();
        int presentCount = 0;
        int absentCount = 0;
        for (Map.Entry<ComponentType<?>, Optional<?>> patch : allPatches.entrySet()) {
            if (patch.getValue().isPresent()) {
                presentCount++;
            } else {
                absentCount++;
            }
        }
        validatePatchCounts(presentCount, absentCount);

        wrapper.writeVarInt(presentCount);
        wrapper.writeVarInt(absentCount);

        for (Map.Entry<ComponentType<?>, Optional<?>> patch : allPatches.entrySet()) {
            if (patch.getValue().isPresent()) {
                wrapper.writeVarInt(patch.getKey().getId(wrapper.getServerVersion().toClientVersion()));
                Runnable writer = () -> castComponentType(patch.getKey()).write(wrapper, patch.getValue().get());
                if (lengthPrefixed) {
                    Object originalBuffer = wrapper.buffer;
                    Object componentBuffer = ByteBufHelper.allocateNewBuffer(originalBuffer);
                    try {
                        wrapper.buffer = componentBuffer;
                        writer.run();
                        wrapper.buffer = originalBuffer;
                        wrapper.writeVarInt(ByteBufHelper.readableBytes(componentBuffer));
                        ByteBufHelper.writeBytes(wrapper.buffer, componentBuffer);
                    } finally {
                        wrapper.buffer = originalBuffer;
                        ByteBufHelper.release(componentBuffer);
                    }
                } else {
                    writer.run();
                }
            }
        }

        for (Map.Entry<ComponentType<?>, Optional<?>> patch : allPatches.entrySet()) {
            if (!patch.getValue().isPresent()) {
                wrapper.writeVarInt(patch.getKey().getId(wrapper.getServerVersion().toClientVersion()));
            }
        }
        return true;
    }

    private static int validatePatchCounts(int presentCount, int absentCount) {
        if (presentCount < 0 || absentCount < 0) {
            throw new RuntimeException("Invalid component patch counts: present=" + presentCount + ", absent=" + absentCount);
        }
        final int total;
        try {
            total = Math.addExact(presentCount, absentCount);
        } catch (ArithmeticException ex) {
            throw new RuntimeException("Component patch count overflow: present=" + presentCount + ", absent=" + absentCount, ex);
        }
        if (total > MAX_COMPONENT_PATCH_ENTRIES) {
            throw new RuntimeException("Component patch count " + total + " exceeds max " + MAX_COMPONENT_PATCH_ENTRIES);
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private static <T> ComponentType<T> castComponentType(ComponentType<?> type) {
        return (ComponentType<T>) type;
    }
}
