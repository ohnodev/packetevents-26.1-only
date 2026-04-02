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

package io.github.retrooper.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPIManagerFactory;
import io.github.retrooper.packetevents.loader.ChainLoadData;
import io.github.retrooper.packetevents.loader.ChainLoadEntryPoint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

import java.util.List;

public class PacketEventsMod implements PreLaunchEntrypoint, ModInitializer {

    public static PacketEventsMod INSTANCE;
    public static final String MOD_ID = "packetevents";

    @Override
    public void onPreLaunch() {
        INSTANCE = this;
        FabricLoader loader = FabricLoader.getInstance();
        // 26.1-only fork: only support official mapping entrypoint.
        List<ChainLoadEntryPoint> allEntryPoints = loader.getEntrypoints("peMainChainLoad", ChainLoadEntryPoint.class);
        if (allEntryPoints.isEmpty()) {
            throw new IllegalStateException("PacketEvents official entrypoint missing. This fork only supports official mappings.");
        }

        ChainLoadData chainLoadData = new ChainLoadData();
        allEntryPoints.get(0).initialize(chainLoadData);

        // Ordinarily I wouldn't be using a static here but since we need to maintain compile-time backwards compatibility
        // We need to preserve the ABI of FactoryPacketEventsAPI and do this static awfulness
        FabricPacketEventsAPIManagerFactory.init(chainLoadData);

        FabricPacketEventsAPI fabricPacketEventsAPI = new FabricPacketEventsAPI(PacketEventsMod.MOD_ID, loader.getEnvironmentType());

        PacketEvents.setAPI(fabricPacketEventsAPI);
        PacketEvents.getAPI().load();

        switch (loader.getEnvironmentType()) {
            case CLIENT -> {
                FabricPacketEventsAPI.setClientAPI(fabricPacketEventsAPI);
                FabricPacketEventsAPI fabricServerPacketEventsAPI = new FabricPacketEventsAPI(PacketEventsMod.MOD_ID, EnvType.SERVER);
                FabricPacketEventsAPI.setServerAPI(fabricPacketEventsAPI);
                fabricServerPacketEventsAPI.load();
            }
            case SERVER -> FabricPacketEventsAPI.setServerAPI(fabricPacketEventsAPI);
        }
    }

    @Override
    public void onInitialize() {
        PacketEventsAPI<?> api = PacketEvents.getAPI();
        if (api != null) {
            api.init();
        }
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) FabricPacketEventsAPI.getClientAPI().init();
    }
}
