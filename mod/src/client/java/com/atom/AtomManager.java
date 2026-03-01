package com.atom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;

/**
 * Серверный менеджер визуализации: облако из частиц (particles) вместо BlockDisplay.
 */
public class AtomManager {
    private static final double JITTER = 0.05;
    private static final int PARTICLES_PER_PACKET = 7;

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

        // Спавним 5–10 частиц с небольшим случайным смещением для объёма
        for (int i = 0; i < PARTICLES_PER_PACKET; i++) {
            double jx = (world.random.nextDouble() - 0.5) * 2 * JITTER;
            double jy = (world.random.nextDouble() - 0.5) * 2 * JITTER;
            double jz = (world.random.nextDouble() - 0.5) * 2 * JITTER;
            world.sendParticles(options, baseX + jx, baseY + jy, baseZ + jz, 1, 0, 0, 0, 0);
        }
    }

    private static ParticleOptions particleForDist(double dist) {
        if (dist < 2.0) {
            return ParticleTypes.END_ROD;
        } else if (dist < 5.0) {
            return new DustParticleOptions(new Vector3f(1f, 1f, 0f), 1.0f); // жёлтый
        } else {
            return new DustParticleOptions(new Vector3f(0.8f, 0f, 0.8f), 1.0f); // фиолетовый
        }
    }
}
