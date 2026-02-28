package com.atom;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.net.URI;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Главный класс мода. Регистрирует команду /atom connect <ip>.
 */
public class AtomVisualizer implements ClientModInitializer {

    private static AtomClient currentClient;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("atom")
                    .then(literal("connect")
                        .then(argument("ip", StringArgumentType.string())
                            .executes(context -> {
                                String ip = StringArgumentType.getString(context, "ip");
                                runConnect(ip.trim());
                                return 0;
                            }))
                        .executes(context -> {
                            runConnect("localhost");
                            return 0;
                        }))
            );
        });
    }

    private static void runConnect(String ipOrHost) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§c[Atom] Зайдите в мир."));
            }
            return;
        }
        if (currentClient != null && currentClient.isOpen()) {
            currentClient.close();
            currentClient = null;
        }
        try {
            String host = ipOrHost.contains(":") ? ipOrHost : ipOrHost + ":8080";
            if (!host.startsWith("ws://") && !host.startsWith("wss://")) {
                host = "ws://" + host;
            }
            URI uri = URI.create(host);
            Vec3 pos = mc.player.position();
            BlockPos centerPos = BlockPos.containing(pos.x, pos.y, pos.z);
            AtomManager.setOrigin(mc.level, centerPos);
            currentClient = new AtomClient(uri);
            currentClient.connect();
            mc.player.sendSystemMessage(Component.literal("§a[Atom] Подключение к " + host + " ..."));
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§c[Atom] Ошибка: " + e.getMessage()));
            }
            AtomManager.clear();
        }
    }
}

