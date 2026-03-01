package com.atom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import org.joml.Vector3f;

/**
 * Пуантилизм: точечный стиль из DUST-частиц (вероятностное поле, как у Кавана).
 */
public class AtomManager {
    private static final float visualScale = 0.8f;
    private static final int PARTICLES_PER_POINT = 7;

    private static final DustParticleOptions DUST_WHITE =
        new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 0.5f);
    private static final DustParticleOptions DUST_YELLOW_ORANGE =
        new DustParticleOptions(new Vector3f(1.0f, 0.8f, 0.0f), 0.4f);
    private static final DustParticleOptions DUST_PURPLE =
        new DustParticleOptions(new Vector3f(0.6f, 0.0f, 0.8f), 0.3f);

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

        double mul = scale * visualScale;
        double baseX = centerPos.getX() + 0.5 + packet.x * mul;
        double baseY = centerPos.getY() + packet.y * mul;
        double baseZ = centerPos.getZ() + 0.5 + packet.z * mul;

        double d = Math.sqrt(packet.x * packet.x + packet.y * packet.y + packet.z * packet.z) * mul;
        ParticleOptions dust = dustForDist(d);

        for (int i = 0; i < PARTICLES_PER_POINT; i++) {
            world.sendParticles(dust, baseX, baseY, baseZ, 1, 0, 0, 0, 0);
        }
    }

    private static ParticleOptions dustForDist(double dist) {
        if (dist < 2.0) return DUST_WHITE;
        if (dist < 5.0) return DUST_YELLOW_ORANGE;
        return DUST_PURPLE;
    }
}
