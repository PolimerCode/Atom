package com.atom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

/**
 * Серверный менеджер визуализации: мягкое облако из светящихся частиц (стиль Kavan).
 */
public class AtomManager {
    private static final double JITTER = 0.1;       // ±0.1 — «пушистое» облако вместо чётких линий
    private static final int PARTICLES_PER_PACKET = 25;  // 20–30 для эффекта наложения и яркости
    private static final double VELOCITY_OUT = 0.03;     // лёгкое разлётывание от центра

    private static BlockPos centerPos;
    private static ServerLevel world;
    private static float scale = 1.0f;

    public static void setOrigin(ServerLevel level, BlockPos origin) {
        world = level;
        centerPos = origin;
    }

    public static void setScale(float newScale) {
        scale = newScale;
    }

    public static void clear() {
        world = null;
    }

    public static void update(AtomPacket packet) {
        if (world == null || centerPos == null) return;

        double baseX = centerPos.getX() + 0.5 + packet.x * scale;
        double baseY = centerPos.getY() + packet.y * scale;
        double baseZ = centerPos.getZ() + 0.5 + packet.z * scale;

        double d = Math.sqrt(packet.x * packet.x + packet.y * packet.y + packet.z * packet.z) * scale;
        ParticleOptions options = particleForDist(d);

        // Направление от центра (для мягкого разлётывания)
        double dx = packet.x * scale;
        double dy = packet.y * scale;
        double dz = packet.z * scale;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) {
            dx = world.random.nextDouble() - 0.5;
            dy = world.random.nextDouble() - 0.5;
            dz = world.random.nextDouble() - 0.5;
            len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
        if (len > 1e-6) {
            dx /= len;
            dy /= len;
            dz /= len;
        }
        double vx = dx * VELOCITY_OUT;
        double vy = dy * VELOCITY_OUT;
        double vz = dz * VELOCITY_OUT;

        // 20–30 частиц с джиттером и скоростью «от центра» — эффект Bloom
        for (int i = 0; i < PARTICLES_PER_PACKET; i++) {
            double jx = (world.random.nextDouble() - 0.5) * 2 * JITTER;
            double jy = (world.random.nextDouble() - 0.5) * 2 * JITTER;
            double jz = (world.random.nextDouble() - 0.5) * 2 * JITTER;
            world.sendParticles(options, baseX + jx, baseY + jy, baseZ + jz, 1, vx, vy, vz, 1.0);
        }
    }

    /** Светящиеся частицы: центр — END_ROD, жёлтая зона — WAX_OFF, фиолетовая — DRAGON_BREATH. */
    private static ParticleOptions particleForDist(double dist) {
        if (dist < 2.0) {
            return ParticleTypes.END_ROD;
        } else if (dist < 5.0) {
            return ParticleTypes.WAX_OFF;  // белые/жёлтые искры
        } else {
            return ParticleTypes.DRAGON_BREATH;  // плавное расширение и затухание
        }
    }
}
