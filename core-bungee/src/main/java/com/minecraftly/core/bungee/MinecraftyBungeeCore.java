package com.minecraftly.core.bungee;

import com.ikeirnez.pluginmessageframework.gateway.ProxyGateway;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

/**
 * Created by Keir on 05/04/2015.
 */
public interface MinecraftyBungeeCore {

    ProxyServer getProxy();

    ProxyGateway<ProxiedPlayer, ServerInfo> getGateway();

    Configuration getConfiguration();
}
