package io.github.retrooper.packetevents.factory.fabric;

import com.github.retrooper.packetevents.manager.registry.RegistryManager;
import io.github.retrooper.packetevents.manager.AbstractFabricPlayerManager;
import io.github.retrooper.packetevents.util.LazyHolder;
import io.github.retrooper.packetevents.impl.netty.manager.player.PlayerManagerAbstract;
import io.github.retrooper.packetevents.loader.ChainLoadData;

public class FabricPacketEventsAPIManagerFactory {
    // TODO, refactor if booky and retrooper approve, bad design having settable static field
    // exists to maintain 100% backward compatability
    private static LazyHolder<AbstractFabricPlayerManager> lazyPlayerManagerHolder;
    private static LazyHolder<AbstractFabricPlayerManager> lazyClientPlayerManagerHolder;
    private static LazyHolder<RegistryManager> registryManagerLazyHolder;

    public static LazyHolder<AbstractFabricPlayerManager> getLazyPlayerManagerHolder() {
        if (lazyPlayerManagerHolder == null) {
            throw new IllegalStateException("PacketEvents manager factory not initialized: player manager holder is null");
        }
        return lazyPlayerManagerHolder;
    }

    public static LazyHolder<AbstractFabricPlayerManager> getClientLazyPlayerManagerHolder() {
        if (lazyClientPlayerManagerHolder == null) {
            throw new IllegalStateException("PacketEvents manager factory not initialized: client player manager holder is null");
        }
        return lazyClientPlayerManagerHolder;
    }

    public static LazyHolder<RegistryManager> getLazyRegistryManagerHolder() {
        if (registryManagerLazyHolder == null) {
            throw new IllegalStateException("PacketEvents manager factory not initialized: registry manager holder is null");
        }
        return registryManagerLazyHolder;
    }

    public static void init(ChainLoadData chainLoadData) {
        LazyHolder<AbstractFabricPlayerManager> playerHolder = chainLoadData.getPlayerManagerAbstractLazyHolder();
        LazyHolder<AbstractFabricPlayerManager> clientPlayerHolder = chainLoadData.getClientPlayerManagerAbstractLazyHolder();
        LazyHolder<RegistryManager> registryHolder = chainLoadData.getRegistryManagerLazyHolder();

        if (playerHolder == null) {
            throw new IllegalStateException("PacketEvents chain load missing player manager holder");
        }
        if (registryHolder == null) {
            throw new IllegalStateException("PacketEvents chain load missing registry manager holder");
        }

        FabricPacketEventsAPIManagerFactory.lazyPlayerManagerHolder = playerHolder;
        FabricPacketEventsAPIManagerFactory.lazyClientPlayerManagerHolder = clientPlayerHolder;
        FabricPacketEventsAPIManagerFactory.registryManagerLazyHolder = registryHolder;
    }
}
