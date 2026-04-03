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

import com.github.retrooper.packetevents.protocol.component.ComponentPatchCodec;
import com.github.retrooper.packetevents.protocol.component.PatchableComponentMap;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

import java.util.List;
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

    private static ItemStack readItemStackTemplate(PacketWrapper<?> wrapper) {
        ItemType itemType = wrapper.readMappedEntity(ItemTypes.getRegistry());
        int count = wrapper.readVarInt();
        if (count <= 0) {
            throw new IllegalStateException("present item stack has non-positive count: " + count);
        }
        PatchableComponentMap components = ComponentPatchCodec.readPatchMap(wrapper, itemType, false);
        if (components == null) {
            return ItemStack.builder().type(itemType).amount(count).wrapper(wrapper).build();
        }

        return ItemStack.builder().type(itemType).amount(count).components(components).wrapper(wrapper).build();
    }

    private static void writeItemStackTemplate(PacketWrapper<?> wrapper, ItemStack stack) {
        wrapper.writeMappedEntity(stack.getType());
        wrapper.writeVarInt(stack.getAmount());
        ComponentPatchCodec.writePatchMap(wrapper, stack, false);
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
