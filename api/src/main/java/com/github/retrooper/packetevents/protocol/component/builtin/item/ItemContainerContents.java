/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2024 retrooper and contributors
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

package com.github.retrooper.packetevents.protocol.component.builtin.item;

import com.github.retrooper.packetevents.protocol.component.ComponentType;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.PatchableComponentMap;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ItemContainerContents {

    private List<ItemStack> items;

    public ItemContainerContents(List<ItemStack> items) {
        this.items = items;
    }

    public static ItemContainerContents read(PacketWrapper<?> wrapper) {
        List<ItemStack> items;
        if (wrapper.getServerVersion().isNewerThanOrEquals(com.github.retrooper.packetevents.manager.server.ServerVersion.V_26_1)) {
            // 26.1 uses ItemStackTemplate optional entries for container slots.
            items = wrapper.readList(w -> {
                ItemStack stack = w.readOptional(ItemContainerContents::readItemStackTemplate);
                return stack == null ? ItemStack.EMPTY : stack;
            });
        } else {
            items = wrapper.readList(PacketWrapper::readItemStack);
        }
        return new ItemContainerContents(items);
    }

    public static void write(PacketWrapper<?> wrapper, ItemContainerContents contents) {
        if (wrapper.getServerVersion().isNewerThanOrEquals(com.github.retrooper.packetevents.manager.server.ServerVersion.V_26_1)) {
            wrapper.writeList(contents.items, (w, item) ->
                    w.writeOptional(item == null || item.isEmpty() ? null : item, ItemContainerContents::writeItemStackTemplate));
        } else {
            wrapper.writeList(contents.items, PacketWrapper::writeItemStack);
        }
    }

    @SuppressWarnings("unchecked")
    private static ItemStack readItemStackTemplate(PacketWrapper<?> wrapper) {
        ItemType itemType = wrapper.readMappedEntity(ItemTypes.getRegistry());
        int count = wrapper.readVarInt();
        int presentCount = wrapper.readVarInt();
        int absentCount = wrapper.readVarInt();

        if (presentCount == 0 && absentCount == 0) {
            return ItemStack.builder().type(itemType).amount(count).wrapper(wrapper).build();
        }

        PatchableComponentMap components = new PatchableComponentMap(
                itemType.getComponents(wrapper.getServerVersion().toClientVersion()),
                new HashMap<>(presentCount + absentCount),
                wrapper.getRegistryHolder());

        for (int i = 0; i < presentCount; i++) {
            ComponentType<?> type = wrapper.readMappedEntity(ComponentTypes.getRegistry());
            Object value = type.read(wrapper);
            components.set((ComponentType<Object>) type, value);
        }
        for (int i = 0; i < absentCount; i++) {
            components.unset(wrapper.readMappedEntity(ComponentTypes.getRegistry()));
        }

        return ItemStack.builder().type(itemType).amount(count).components(components).wrapper(wrapper).build();
    }

    @SuppressWarnings("unchecked")
    private static void writeItemStackTemplate(PacketWrapper<?> wrapper, ItemStack stack) {
        wrapper.writeMappedEntity(stack.getType());
        wrapper.writeVarInt(stack.getAmount());

        if (!stack.hasComponentPatches()) {
            wrapper.writeShort(0);
            return;
        }

        Map<ComponentType<?>, java.util.Optional<?>> allPatches = stack.getComponents().getPatches();
        int presentCount = 0;
        int absentCount = 0;
        for (Map.Entry<ComponentType<?>, java.util.Optional<?>> patch : allPatches.entrySet()) {
            if (patch.getValue().isPresent()) {
                presentCount++;
            } else {
                absentCount++;
            }
        }
        wrapper.writeVarInt(presentCount);
        wrapper.writeVarInt(absentCount);

        for (Map.Entry<ComponentType<?>, java.util.Optional<?>> patch : allPatches.entrySet()) {
            if (patch.getValue().isPresent()) {
                wrapper.writeVarInt(patch.getKey().getId(wrapper.getServerVersion().toClientVersion()));
                ((ComponentType<Object>) patch.getKey()).write(wrapper, patch.getValue().get());
            }
        }
        for (Map.Entry<ComponentType<?>, java.util.Optional<?>> patch : allPatches.entrySet()) {
            if (!patch.getValue().isPresent()) {
                wrapper.writeVarInt(patch.getKey().getId(wrapper.getServerVersion().toClientVersion()));
            }
        }
    }

    public void addItem(ItemStack itemStack) {
        this.items.add(itemStack);
    }

    public List<ItemStack> getItems() {
        return this.items;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ItemContainerContents)) return false;
        ItemContainerContents that = (ItemContainerContents) obj;
        return this.items.equals(that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.items);
    }
}
