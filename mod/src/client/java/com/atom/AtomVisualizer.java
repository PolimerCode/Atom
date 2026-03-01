package com.atom;

import com.mojang.brigadier.arguments.FloatArgumentType;
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
                    .then(literal("stop")
                        .executes(context -> {
                            runStop();
                            return 0;
                        }))
                    .then(literal("scale")
                        .then(argument("factor", FloatArgumentType.floatArg(0.05f, 100f))
                            .executes(context -> {
                                float factor = FloatArgumentType.getFloat(context, "factor");
                                runScale(factor);
                                return 0;
                            })))
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
            var server = mc.getSingleplayerServer();
            if (server == null) {
                mc.player.sendSystemMessage(Component.literal("§c[Atom] Нужен одиночный мир (singleplayer)."));
                return;
            }
            var dimension = mc.level.dimension();
            server.execute(() -> {
                var serverLevel = server.getLevel(dimension);
                if (serverLevel != null) {
                    AtomManager.setOrigin(serverLevel, centerPos);
                }
            });
            currentClient = new AtomClient(uri);
            currentClient.connect();
            mc.player.sendSystemMessage(Component.literal("§a[Atom] Подключение к " + host + " ..."));
            mc.player.sendSystemMessage(Component.literal("§7[Atom] Центр атома: блок §f" + centerPos.getX() + " " + centerPos.getY() + " " + centerPos.getZ() + " §7(под вами). Ядро — здесь, электроны — в радиусе ~10 блоков."));
        } catch (Exception e) {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal("§c[Atom] Ошибка: " + e.getMessage()));
            }
            AtomManager.clear();
        }
    }

    private static void runStop() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (currentClient != null) {
            currentClient.close();
            currentClient = null;
        }
        var server = mc.getSingleplayerServer();
        if (server != null) {
            server.execute(AtomManager::clear);
        }
        mc.player.sendSystemMessage(Component.literal("§e[Atom] Визуализация остановлена и очищена."));
    }

    private static void runScale(float factor) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        var server = mc.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> AtomManager.setScale(factor));
        }
        mc.player.sendSystemMessage(Component.literal("§e[Atom] Масштаб визуализации: " + factor));
    }
}
