package com.lielamar.auth.bungee.listeners;

import com.lielamar.auth.bungee.TwoFactorAuthentication;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class OnBungeePlayerConnections implements Listener {

    private final TwoFactorAuthentication main;
    public OnBungeePlayerConnections(TwoFactorAuthentication main) {
        this.main = main;
    }

    @EventHandler
    public void onJoin(LoginEvent event) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(event.getConnection().getUniqueId());

        if(player != null)
            main.getAuthHandler().playerJoin(player.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        main.getAuthHandler().playerQuit(player.getUniqueId());
    }
}
