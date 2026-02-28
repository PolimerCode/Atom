package com.atom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
        }
    }

    private static Display.BlockDisplay createDisplay(AtomPacket packet) {
        if (world == null) return null;
        Display.BlockDisplay entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, world);
        BlockState state = packet.isNucleus()
            ? Blocks.SEA_LANTERN.defaultBlockState()
            : Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
        entity.getEntityData().set(Display.BlockDisplay.DATA_BLOCK_STATE, state);
        entity.setInterpolationDuration(1);
        entity.setInterpolationDelay(0);
        entity.setPos(
            centerPos.getX() + 0.5 + packet.x,
            centerPos.getY() + packet.y,
            centerPos.getZ() + 0.5 + packet.z
        );
        return entity;
    }
}
