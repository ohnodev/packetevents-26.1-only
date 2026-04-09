package io.github.retrooper.packetevents;

import com.github.retrooper.packetevents.manager.registry.ItemRegistry;
import com.github.retrooper.packetevents.manager.registry.RegistryManager;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.util.mappings.IRegistry;
import com.github.retrooper.packetevents.util.mappings.SynchronizedRegistriesHandler;
import io.github.retrooper.packetevents.factory.fabric.FabricPacketEventsAPI;
import io.github.retrooper.packetevents.factory.fabric.FabricPlayerManager;
import io.github.retrooper.packetevents.loader.ChainLoadData;
import io.github.retrooper.packetevents.loader.ChainLoadEntryPoint;
import io.github.retrooper.packetevents.manager.AbstractFabricPlayerManager;
import io.github.retrooper.packetevents.manager.registry.FabricRegistryManager;
import io.github.retrooper.packetevents.util.LazyHolder;
import org.jetbrains.annotations.Nullable;

public class FabricOfficialChainLoadEntrypoint implements ChainLoadEntryPoint {

    protected LazyHolder<AbstractFabricPlayerManager> playerManagerHolder =
            LazyHolder.simple(() -> new FabricPlayerManager());
    protected LazyHolder<RegistryManager> registryManagerHolder =
            LazyHolder.simple(() -> new FabricRegistryManager(new ItemRegistry() {
                @Override
                public @Nullable ItemType getByName(String name) {
                    IRegistry<ItemType> registry = resolveSyncedItemRegistry();
                    return registry != null
                            ? registry.getByName(ClientVersion.V_26_2, name)
                            : ItemTypes.getRegistry().getByName(ClientVersion.V_26_2, name);
                }

                @Override
                public @Nullable ItemType getById(int id) {
                    IRegistry<ItemType> registry = resolveSyncedItemRegistry();
                    return registry != null
                            ? registry.getById(ClientVersion.V_26_2, id)
                            : ItemTypes.getRegistry().getById(ClientVersion.V_26_2, id);
                }
            }));

    @Override
    public void initialize(ChainLoadData chainLoadData) {
        chainLoadData.setPlayerManagerIfNull(playerManagerHolder);
        chainLoadData.setClientPlayerManagerIfNull(playerManagerHolder);
        chainLoadData.setRegistryManagerIfNull(registryManagerHolder);
    }

    @Override
    public ServerVersion getNativeVersion() {
        return ServerVersion.V_26_2;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable IRegistry<ItemType> resolveSyncedItemRegistry() {
        SynchronizedRegistriesHandler.RegistryEntry<?> entry =
                SynchronizedRegistriesHandler.getRegistryEntry(ItemTypes.getRegistry().getRegistryKey());
        if (entry == null) {
            return null;
        }
        return (IRegistry<ItemType>) entry.getSyncedRegistry(ClientVersion.V_26_2);
    }
}
