package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ManaSenseRules;
import com.chappadodle.elementalarcana.api.Stat;
import com.chappadodle.elementalarcana.content.CreatureLevels;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Mana sense (see the mana sense and weather spec): under the crosshair, what the player reads of the
 * aura of the creature they look at within 24 blocks: with little Insight only a mark in the colour of
 * how dangerous it feels, then its level, then its element too.
 */
public class ManaSenseLayer implements LayeredDraw.Layer {
    private static final double RANGE = 24;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        if (!data.isAwakened()) {
            return;
        }
        LivingEntity creature = lookedAt(player, deltaTracker.getGameTimeDeltaPartialTick(true));
        if (creature == null) {
            return;
        }
        int level = CreatureLevels.displayLevel(creature);
        if (level <= 0) {
            return;
        }
        ManaSenseRules.Danger danger = ManaSenseRules.danger(level - data.level());
        int color = FastColor.ARGB32.opaque(danger.color());
        MutableComponent line = switch (ManaSenseRules.detail(data.stat(Stat.INSIGHT))) {
            case DANGER -> Component.literal("\u25C6");
            case LEVEL -> Component.translatable("hud.elementalarcana.sense.level", level);
            case ELEMENT -> {
                MutableComponent text = Component.translatable("hud.elementalarcana.sense.level", level);
                Element element = CreatureElements.elementOf(creature);
                if (element != null) {
                    text.append(Component.literal(" \u00B7 ").withColor(0xAAAAAA))
                            .append(Component.translatable("school.elementalarcana." + element.name().toLowerCase(Locale.ROOT)).withColor(element.color()));
                }
                yield text;
            }
        };
        Font font = minecraft.font;
        int x = graphics.guiWidth() / 2 - font.width(line) / 2;
        int y = graphics.guiHeight() / 2 + 10;
        graphics.drawString(font, line, x, y, color, true);
    }

    /** The creature (not a player, not a stand) the crosshair is on within 24 blocks, walls stopping the look. */
    @Nullable
    private static LivingEntity lookedAt(Player player, float partialTick) {
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 end = eye.add(look.scale(RANGE));
        BlockHitResult wall = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        AABB sweep = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, sweep,
                entity -> entity instanceof LivingEntity living && !(entity instanceof Player) && !(entity instanceof ArmorStand) && living.isAlive(),
                eye.distanceToSqr(end));
        Entity entity = hit == null ? null : hit.getEntity();
        return entity instanceof LivingEntity living ? living : null;
    }
}
