package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.api.SchoolElements;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.WispRingRules;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.WispRingOptions;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import com.chappadodle.elementalarcana.content.star.ModStars;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Wisp Rings at night (see their spec): over each ring near a player the wisps' lights circle (one
 * particle sent now and then, each client drawing them: WispRingOptions) and it chimes softly; and
 * whoever steps into one gets a boon or a trick, once a night for each ring. Small Folk's shrinking
 * wears off by itself.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class WispRings {
    private static final ResourceKey<Structure> RING = ResourceKey.create(Registries.STRUCTURE, ElementalArcana.id("wisp_ring"));
    private static final ResourceLocation SMALL_FOLK = ElementalArcana.id("small_folk");
    private static final String VISITS = "elementalarcana_wisp_rings";
    private static final String SMALL_UNTIL = "elementalarcana_small_until";
    /** How often the lights are sent (each lasts a little longer, so they never go out between). */
    private static final int LIGHTS_EVERY = 40;
    private static final int SMALL_TICKS = 2400;

    private WispRings() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        CompoundTag data = player.getPersistentData();
        if (data.contains(SMALL_UNTIL) && now >= data.getLong(SMALL_UNTIL)) {
            data.remove(SMALL_UNTIL);
            AttributeInstance scale = player.getAttribute(Attributes.SCALE);
            if (scale != null) {
                scale.removeModifier(SMALL_FOLK);
            }
        }
        if (level.dimension() != Level.OVERWORLD || !WispRingRules.isNight(level.getDayTime()) || (now + player.getId()) % 10 != 0) {
            return;
        }
        Structure ring = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(RING);
        if (ring == null) {
            return;
        }
        if ((now + player.getId()) % LIGHTS_EVERY == 0) {
            for (BlockPos middle : ringsNear(level, player, ring)) {
                level.sendParticles(player, new WispRingOptions(LIGHTS_EVERY + 6), true, middle.getX() + 0.5, middle.getY() + 1.5,
                        middle.getZ() + 0.5, 1, 0, 0, 0, 0);
                if (level.getRandom().nextInt(3) == 0) {
                    player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.35f, 1.2f + level.getRandom().nextFloat() * 0.6f);
                }
            }
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), ring);
        if (!start.isValid()) {
            return;
        }
        BlockPos middle = start.getBoundingBox().getCenter();
        if (!WispRingRules.inside(player.getX() - (middle.getX() + 0.5), player.getZ() - (middle.getZ() + 0.5))) {
            return;
        }
        CompoundTag visits = data.getCompound(VISITS);
        String key = Long.toString(middle.asLong());
        long night = WispRingRules.night(level.getDayTime());
        if (visits.contains(key) && visits.getLong(key) == night) {
            return;
        }
        visits.putLong(key, night);
        data.put(VISITS, visits);
        RandomSource random = level.getRandom();
        WispRingRules.Outcome outcome = WispRingRules.pick(random.nextDouble(), random.nextDouble());
        befall(level, player, outcome, middle);
        player.sendSystemMessage(Component.translatable("message.elementalarcana.wisp_ring." + outcome.id())
                .withStyle(outcome.boon() ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GOLD));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f,
                outcome.boon() ? 1.5f : 0.7f);
        MagicTriggers.fire(player, "wisp_ring", outcome.id(), 1);
    }

    /** The middles of the rings in the player's chunk and the eight round it. */
    private static List<BlockPos> ringsNear(ServerLevel level, ServerPlayer player, Structure ring) {
        List<BlockPos> middles = new ArrayList<>();
        ChunkPos chunk = player.chunkPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(chunk.x + dx, chunk.z + dz), s -> s == ring)) {
                    BlockPos middle = start.getBoundingBox().getCenter();
                    if (!middles.contains(middle)) {
                        middles.add(middle);
                    }
                }
            }
        }
        return middles;
    }

    private static void befall(ServerLevel level, ServerPlayer player, WispRingRules.Outcome outcome, BlockPos middle) {
        RandomSource random = level.getRandom();
        switch (outcome) {
            case FEY_LUCK -> player.addEffect(new MobEffectInstance(MobEffects.LUCK, 12000, 1));
            case WISP_SIGHT -> player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 6000, 0));
            case MOONLIT_STEP -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3600, 1));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 3600, 1));
            }
            case GIFT -> {
                ItemStack gift = switch (random.nextInt(3)) {
                    case 0 -> new ItemStack(ModItems.essence(ownElement(player, random)), 2);
                    case 1 -> new ItemStack(ModStars.STAR_FRAGMENT.get());
                    default -> new ItemStack(Items.GOLDEN_CARROT, 2);
                };
                level.addFreshEntity(new ItemEntity(level, middle.getX() + 0.5, middle.getY() + 1.5, middle.getZ() + 0.5, gift));
            }
            case FULL_MOON -> {
                MagicData data = MagicAttachments.get(player);
                data.fillMana();
                MagicAttachments.sync(player);
            }
            case TURNED_AROUND -> {
                for (int attempt = 0; attempt < 8; attempt++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double distance = 30 + random.nextDouble() * 30;
                    int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
                    int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
                    if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                        continue;
                    }
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    if (!level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) {
                        continue;
                    }
                    player.teleportTo(x + 0.5, y, z + 0.5);
                    player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                    break;
                }
            }
            case SMALL_FOLK -> {
                AttributeInstance scale = player.getAttribute(Attributes.SCALE);
                if (scale != null) {
                    scale.removeModifier(SMALL_FOLK);
                    scale.addPermanentModifier(new AttributeModifier(SMALL_FOLK, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    player.getPersistentData().putLong(SMALL_UNTIL, level.getGameTime() + SMALL_TICKS);
                }
            }
            case WILL_O_THE_WISP -> {
                Element element = ownElement(player, random);
                for (int i = 0; i < 3; i++) {
                    double angle = Math.PI * 2 * i / 3;
                    WispSpawner.spawnAt(level, element, BlockPos.containing(middle.getX() + Math.cos(angle) * 2, middle.getY() + 2,
                            middle.getZ() + Math.sin(angle) * 2), MobSpawnType.EVENT);
                }
            }
        }
    }

    /** One of the player's elements (any of the eight for one whose magic sleeps). */
    private static Element ownElement(ServerPlayer player, RandomSource random) {
        List<Element> mine = new ArrayList<>();
        for (ResourceLocation school : MagicAttachments.get(player).affinities()) {
            Element element = SchoolElements.of(school);
            if (element != null) {
                mine.add(element);
            }
        }
        List<Element> pool = mine.isEmpty() ? List.of(Element.values()) : mine;
        return pool.get(random.nextInt(pool.size()));
    }

    /** For the tests: whether a ring stands at {@code pos} (its middle), if any. */
    @Nullable
    public static BlockPos ringAt(ServerLevel level, BlockPos pos) {
        Structure ring = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(RING);
        if (ring == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, ring);
        return start.isValid() ? start.getBoundingBox().getCenter() : null;
    }
}
