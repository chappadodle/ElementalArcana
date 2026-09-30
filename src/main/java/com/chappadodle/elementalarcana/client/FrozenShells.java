package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.client.particle.IceCrystalParticle;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A Frozen creature, encased in ice: a block of translucent ice around it with crystals jutting
 * from its top, for as long as it's Frozen (MagicAttachments#FROZEN_UNTIL). When the freeze ends,
 * or the creature dies inside it, the ice shatters into 3D shards with a crack of glass.
 */
public final class FrozenShells {
    private static final ResourceLocation ICE = ElementalArcana.id("block/icicle_frost3");
    /** How much bigger than the creature the ice is. */
    private static final float MARGIN = 0.15f;

    /** A shell being drawn: where it was last and how big, so it can shatter there. */
    private record Shell(Vec3 position, float width, float height) {
    }

    private static final Map<Integer, Shell> SHELLS = new HashMap<>();

    private FrozenShells() {
    }

    private static boolean frozen(LivingEntity entity) {
        return entity.hasData(MagicAttachments.FROZEN_UNTIL)
                && entity.getData(MagicAttachments.FROZEN_UNTIL) > entity.level().getGameTime();
    }

    /** After a creature is drawn: its ice, if it's Frozen (the pose is at its feet). */
    static void render(LivingEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        if (!frozen(entity)) {
            return;
        }
        float width = entity.getBbWidth() / 2 + MARGIN;
        float height = entity.getBbHeight() + MARGIN;
        SHELLS.put(entity.getId(), new Shell(entity.position(), width, height));
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ICE);
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.FULL_BRIGHT;
        // A slow shimmer, so the ice looks alive.
        float time = entity.tickCount + partialTick;
        int alpha = Math.round(255 * (0.5f + 0.06f * Mth.sin(time * 0.15f)));
        box(consumer, pose, sprite, -width, -0.02f, -width, width, height, width, alpha, light);
        // Crystals jutting from the top, the same for a creature each time it's frozen.
        RandomSource random = RandomSource.create(entity.getId());
        for (int i = 0; i < 4; i++) {
            float cx = (random.nextFloat() - 0.5f) * width * 1.4f;
            float cz = (random.nextFloat() - 0.5f) * width * 1.4f;
            float tall = 0.25f + random.nextFloat() * 0.35f;
            float thick = 0.08f + random.nextFloat() * 0.06f;
            spike(consumer, pose, sprite, cx, height, cz, thick, tall, alpha, light);
        }
    }

    /** Every client tick: shells whose creature is no longer Frozen (or is gone) shatter. */
    static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            SHELLS.clear();
            return;
        }
        for (Iterator<Map.Entry<Integer, Shell>> it = SHELLS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Shell> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity instanceof LivingEntity living && living.isAlive() && frozen(living)) {
                continue;
            }
            it.remove();
            shatter(level, entry.getValue());
        }
    }

    private static void shatter(ClientLevel level, Shell shell) {
        RandomSource random = level.getRandom();
        Vec3 center = shell.position().add(0, shell.height() / 2, 0);
        int shards = 6 + Math.round(shell.height() * 4);
        for (int i = 0; i < shards; i++) {
            Vec3 at = center.add((random.nextDouble() - 0.5) * shell.width() * 2, (random.nextDouble() - 0.5) * shell.height(),
                    (random.nextDouble() - 0.5) * shell.width() * 2);
            Vec3 velocity = at.subtract(center).normalize().scale(0.15 + random.nextDouble() * 0.1).add(0, 0.15, 0);
            Minecraft.getInstance().particleEngine.add(new IceCrystalParticle(level, at, velocity, ICE, 0.12f + random.nextFloat() * 0.12f));
        }
        for (int i = 0; i < 12; i++) {
            level.addParticle(ModContent.FROST_SPARKLE.get(), center.x + (random.nextDouble() - 0.5) * shell.width() * 2,
                    center.y + (random.nextDouble() - 0.5) * shell.height(), center.z + (random.nextDouble() - 0.5) * shell.width() * 2,
                    0, 0.02, 0);
        }
        level.playLocalSound(center.x, center.y, center.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS,
                0.9f, 1.1f + random.nextFloat() * 0.2f, false);
    }

    /** A box of ice, the texture stretched over each face. */
    private static void box(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
                            float x0, float y0, float z0, float x1, float y1, float z1, int alpha, int light) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        // Each face wound to face outward: north, south, west, east, up, down.
        quad(consumer, pose, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1, u0, u1, v0, v1, alpha, light);
        quad(consumer, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, u0, u1, v0, v1, alpha, light);
        quad(consumer, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, u0, u1, v0, v1, alpha, light);
        quad(consumer, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0, u0, u1, v0, v1, alpha, light);
        quad(consumer, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0, u0, u1, v0, v1, alpha, light);
        quad(consumer, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, u0, u1, v0, v1, alpha, light);
    }

    /** A four-sided crystal standing on the shell's top at (x, y, z). */
    private static void spike(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite sprite,
                              float x, float y, float z, float thick, float tall, int alpha, int light) {
        float u0 = sprite.getU(0.3f);
        float u1 = sprite.getU(0.7f);
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float[][] corners = {{-thick, -thick}, {thick, -thick}, {thick, thick}, {-thick, thick}};
        for (int i = 0; i < 4; i++) {
            float[] a = corners[i];
            float[] b = corners[(i + 1) % 4];
            float nx = (a[0] + b[0]) / 2;
            float nz = (a[1] + b[1]) / 2;
            // A triangle as a quad with its tip repeated; outward-facing (seen from outside).
            quad(consumer, pose, x + b[0], y, z + b[1], x + a[0], y, z + a[1], x, y + tall, z, x, y + tall, z,
                    nx, 0.5f, nz, u0, u1, v0, v1, alpha, light);
        }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float nx, float ny, float nz, float u0, float u1, float v0, float v1, int alpha, int light) {
        vertex(consumer, pose, x0, y0, z0, u0, v1, nx, ny, nz, alpha, light);
        vertex(consumer, pose, x1, y1, z1, u1, v1, nx, ny, nz, alpha, light);
        vertex(consumer, pose, x2, y2, z2, u1, v0, nx, ny, nz, alpha, light);
        vertex(consumer, pose, x3, y3, z3, u0, v0, nx, ny, nz, alpha, light);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int alpha, int light) {
        consumer.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
