package com.atom;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

/**
 * WebSocket-клиент. Парсит JSON в AtomPacket и передаёт в AtomManager.
 */
public class AtomClient extends WebSocketClient {

    private static final Logger LOGGER = LogManager.getLogger("atom");
    private static final Gson GSON = new Gson();
    private static final Type PACKET_LIST_TYPE = new TypeToken<List<AtomPacket>>() {}.getType();

    public AtomClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOGGER.info("[Atom] WebSocket connected to {}", getURI());
        sendChat("§a[Atom] Подключено к " + getURI());
    }

    @Override
    public void onMessage(String message) {
        try {
            List<AtomPacket> packets = GSON.fromJson(message, PACKET_LIST_TYPE);
            if (packets != null && !packets.isEmpty()) {
                Minecraft.getInstance().execute(() -> {
                    for (AtomPacket packet : packets) {
                        AtomManager.update(packet);
                    }
                });
            }
        } catch (Exception e) {
            LOGGER.warn("[Atom] Failed to parse message: {}", e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("[Atom] WebSocket closed: {} {}", code, reason);
        sendChat("§c[Atom] Отключено: " + code + " " + reason);
        Minecraft.getInstance().execute(AtomManager::clear);
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.warn("[Atom] WebSocket error: {}", ex.getMessage());
        sendChat("§c[Atom] Ошибка: " + ex.getMessage());
    }

    private void sendChat(String text) {
        Minecraft.getInstance().execute(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(text));
            }
        });
    }
}
