package com.raven.client;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;

public class ClientEventHandler {

    @SubscribeEvent
    public void onJoinServer(ClientConnectedToServerEvent event) {
        // Event handler for server join - can be used for future features
    }
}
