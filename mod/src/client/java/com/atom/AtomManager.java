package com.atom;

import com.mojang.math.Transformation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Серверный менеджер визуализации: BlockDisplay с масштабом и цветом по радиусу.
 */
public class AtomManager {
    private static final Map<Integer, Display.BlockDisplay> ENTITIES = new HashMap<>();
    private static final Map<Integer, Long> LAST_SEEN_TICK = new HashMap<>();

    private static BlockPos centerPos;
    private static ServerLevel world;
    private static float scale = 1.0f; // общий множитель радиусов

    public static void setOrigin(ServerLevel level, BlockPos origin) {
        world = level;
        centerPos = origin;
    }

    public static void setScale(float newScale) {
        scale = newScale;
    }

    public static void clear() {
        for (Display.BlockDisplay entity : ENTITIES.values()) {
            if (entity.isAlive()) {
                entity.discard();
            }
        }
        ENTITIES.clear();
        LAST_SEEN_TICK.clear();
        world = null;
    }

    public static void update(AtomPacket packet) {
        if (world == null || centerPos == null) return;

        long now = world.getGameTime();

        // Авто-очистка: удаляем точки, которые не обновлялись ~2 секунды (40 тиков)
        long lifeTicks = 40;
        if (!ENTITIES.isEmpty()) {
            var it = ENTITIES.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                int id = entry.getKey();
                long last = LAST_SEEN_TICK.getOrDefault(id, now);
                if (now - last > lifeTicks) {
                    Display.BlockDisplay old = entry.getValue();
                    if (old.isAlive()) {
                        old.discard();
                    }
                    it.remove();
                    LAST_SEEN_TICK.remove(id);
                }
            }
        }

        Display.BlockDisplay entity = ENTITIES.get(packet.id);
        if (entity == null) {
            entity = createDisplay(packet);
            if (entity != null) {
                world.addFreshEntity(entity);
                ENTITIES.put(packet.id, entity);
            }
        }
        if (entity != null && entity.isAlive()) {
            double x = centerPos.getX() + 0.5 + packet.x * scale;
            double y = centerPos.getY() + packet.y * scale;
            double z = centerPos.getZ() + 0.5 + packet.z * scale;
            entity.setPos(x, y, z);

            // Обновляем цвет по текущему расстоянию
            BlockState state = colorFor(packet);
            setBlockStateViaDataAccessor(entity, state);

            LAST_SEEN_TICK.put(packet.id, now);

            // (Опциональная подсветка частицами была убрана, чтобы не забивать экран белыми трейсами)
        }
    }

    private static Display.BlockDisplay createDisplay(AtomPacket packet) {
        if (world == null) return null;

        Display.BlockDisplay entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, world);

        // Масштаб и центрирование отдельного блока
        float size = packet.isNucleus() ? 0.5f : 0.1f; // электроны ещё мельче
        Vector3f translation = new Vector3f(-size / 2.0f, -size / 2.0f, -size / 2.0f);
        Vector3f scaleVec = new Vector3f(size, size, size);
        Transformation transform = new Transformation(
            translation,
            new Quaternionf(0, 0, 0, 1),
            scaleVec,
            new Quaternionf(0, 0, 0, 1)
        );
        setTransformationViaReflection(entity, transform);
        setBillboardCenter(entity);
        setFullBright(entity);

        // Цвет по радиусу (heatmap)
        BlockState state = colorFor(packet);
        setBlockStateViaDataAccessor(entity, state);

        double x = centerPos.getX() + 0.5 + packet.x * scale;
        double y = centerPos.getY() + packet.y * scale;
        double z = centerPos.getZ() + 0.5 + packet.z * scale;
        entity.setPos(x, y, z);
        return entity;
    }

    private static BlockState colorFor(AtomPacket packet) {
        // Цвет только по расстоянию от центра, независимо от типа частицы
        double sx = packet.x * scale;
        double sy = packet.y * scale;
        double sz = packet.z * scale;
        double d = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (d < 2.0) {
            return Blocks.SEA_LANTERN.defaultBlockState();
        } else if (d < 5.0) {
            return Blocks.YELLOW_STAINED_GLASS.defaultBlockState();
        } else if (d < 8.0) {
            return Blocks.MAGENTA_STAINED_GLASS.defaultBlockState();
        } else {
            return Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
        }
    }

    private static void setTransformationViaReflection(Display.BlockDisplay entity, Transformation transformation) {
        try {
            var method = Display.class.getDeclaredMethod("setTransformation", Transformation.class);
            method.setAccessible(true);
            method.invoke(entity, transformation);
        } catch (Exception ignored) {
            // Останется дефолтный трансформ, если что-то пошло не так
        }
    }

    private static void setBillboardCenter(Display.BlockDisplay entity) {
        try {
            var method = Display.class.getDeclaredMethod("setBillboardConstraints", Display.BillboardConstraints.class);
            method.setAccessible(true);
            method.invoke(entity, Display.BillboardConstraints.CENTER);
        } catch (Exception ignored) {
            // Если API изменилось, просто не используем billboard
        }
    }

    private static void setFullBright(Display.BlockDisplay entity) {
        try {
            Class<?> brightnessClass = Class.forName("net.minecraft.util.Brightness");
            var ctor = brightnessClass.getDeclaredConstructor(int.class, int.class);
            ctor.setAccessible(true);
            Object brightness = ctor.newInstance(15, 15);
            try {
                var method = Display.class.getDeclaredMethod("setBrightness", brightnessClass);
                method.setAccessible(true);
                method.invoke(entity, brightness);
            } catch (NoSuchMethodException e) {
                var method = Display.class.getDeclaredMethod("setBrightnessOverride", brightnessClass);
                method.setAccessible(true);
                method.invoke(entity, brightness);
            }
        } catch (Exception ignored) {
            // В худшем случае останется обычное освещение мира
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
            // Если что-то пошло не так, оставляем дефолтный блок
        }
    }
}
