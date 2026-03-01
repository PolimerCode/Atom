package com.atom;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Хранит BlockDisplay по id частицы. Спавнит и обновляет позиции относительно origin.
 */
public class AtomManager {
    private static final Map<Integer, Display.BlockDisplay> DISPLAYS = new HashMap<>();

    private static BlockPos centerPos;
    private static Level world;

    public static void setOrigin(Level level, BlockPos origin) {
        world = level;
        centerPos = origin;
    }

    public static void clear() {
        for (Display.BlockDisplay display : DISPLAYS.values()) {
            if (display.isAlive()) {
                display.discard();
            }
        }
        DISPLAYS.clear();
        world = null;
    }

    public static void update(AtomPacket packet) {
        if (world == null || centerPos == null) return;

        Display.BlockDisplay entity = DISPLAYS.get(packet.id);
        if (entity == null) {
            entity = createDisplay(packet);
            if (entity != null) {
                world.addFreshEntity(entity);
                DISPLAYS.put(packet.id, entity);
            }
        }
        if (entity != null && entity.isAlive()) {
            double x = centerPos.getX() + 0.5 + packet.x;
            double y = centerPos.getY() + packet.y;
            double z = centerPos.getZ() + 0.5 + packet.z;
            entity.setPos(x, y, z);
            // Частицы-подсветка на клиенте (видны даже если BlockDisplay не рендерится)
            if (world.isClientSide() && world.getGameTime() % 3 == packet.id % 3) {
                world.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0, 0);
            }
        }
    }

    private static Display.BlockDisplay createDisplay(AtomPacket packet) {
        if (world == null) return null;
        Display.BlockDisplay entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, world);
        // Масштаб (1,1,1) — иначе Display по умолчанию с масштабом 0 не рисуется
        setTransformationViaReflection(entity, new Transformation(
            new Vector3f(0, 0, 0),
            new Quaternionf(0, 0, 0, 1),
            new Vector3f(1, 1, 1),
            new Quaternionf(0, 0, 0, 1)
        ));
        BlockState state = packet.isNucleus()
            ? Blocks.SEA_LANTERN.defaultBlockState()
            : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        setBlockStateViaDataAccessor(entity, state);
        entity.setPos(
            centerPos.getX() + 0.5 + packet.x,
            centerPos.getY() + packet.y,
            centerPos.getZ() + 0.5 + packet.z
        );
        return entity;
    }

    private static void setTransformationViaReflection(Display.BlockDisplay entity, Transformation transformation) {
        try {
            var method = Display.class.getDeclaredMethod("setTransformation", Transformation.class);
            method.setAccessible(true);
            method.invoke(entity, transformation);
        } catch (Exception ignored) {
            // Без масштаба Display может быть невидим
        }
    }

    @SuppressWarnings("unchecked")
    private static void setBlockStateViaDataAccessor(Display.BlockDisplay entity, BlockState state) {
        try {
            Field field = Display.BlockDisplay.class.getDeclaredField("DATA_BLOCK_STATE_ID");
            field.setAccessible(true);
            EntityDataAccessor<BlockState> accessor =
                (EntityDataAccessor<BlockState>) field.get(null);
            entity.getEntityData().set(accessor, state);
        } catch (Exception ignored) {
            // Если что-то пошло не так, просто оставим дефолтный блок.
        }
    }
}
