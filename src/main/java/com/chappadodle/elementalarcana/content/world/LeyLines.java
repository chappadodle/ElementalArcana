package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.LeyRules;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.CastingService;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Ley Lines (see the Ley Lines spec): the shrines a mage has touched are remembered (a player
 * attachment, kept through death), and from any Shrine Core they can travel to any other they
 * remember: sneak and right-click the core for the ley menu (clickable lines in chat), each line
 * running {@code /ley <x> <y> <z>}. A journey costs mana by distance (LeyRules) and the lines take a
 * minute to settle after it. A mage's own Ley Anchors (see the Ley Anchors spec) are places on the
 * network too: remembered when touched, travelled to and from as a shrine is.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class LeyLines {
    /** The colour of an anchor's line in the menu and its pillar of light: the amethyst's violet. */
    public static final int ANCHOR_COLOR = 0xB57EDC;

    /** A remembered place: a shrine (where its core is, and its element) or a Ley Anchor (where it is, and its name, if any). */
    public record LeyShrine(BlockPos pos, Optional<ShrineKind> kind, Optional<String> anchor) {
        public static final Codec<LeyShrine> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(LeyShrine::pos),
                ShrineKind.CODEC.optionalFieldOf("kind").forGetter(LeyShrine::kind),
                Codec.STRING.optionalFieldOf("anchor").forGetter(LeyShrine::anchor)
        ).apply(instance, LeyShrine::new));

        public static LeyShrine shrine(BlockPos pos, ShrineKind kind) {
            return new LeyShrine(pos.immutable(), Optional.of(kind), Optional.empty());
        }

        public static LeyShrine anchor(BlockPos pos, String name) {
            return new LeyShrine(pos.immutable(), Optional.empty(), Optional.of(name));
        }

        public int color() {
            return kind.map(shrine -> shrine.element().color()).orElse(ANCHOR_COLOR);
        }

        /** What the menu calls it: "Fire Shrine", or the anchor's name ("Ley Anchor" if it has none). */
        public Component title() {
            return kind.map(LeyLines::name).orElseGet(() -> anchorTitle(anchor.orElse("")));
        }

        /** The key the "ley" advancement trigger gets: the shrine's element, or "anchor". */
        public String trigger() {
            return kind.map(ShrineKind::getSerializedName).orElse("anchor");
        }
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ElementalArcana.MODID);
    /** The shrines a player remembers. */
    public static final Supplier<AttachmentType<List<LeyShrine>>> MEMORY = ATTACHMENTS.register("ley_shrines",
            () -> AttachmentType.<List<LeyShrine>>builder(() -> List.of())
                    .serialize(LeyShrine.CODEC.listOf())
                    .copyOnDeath()
                    .build());
    private static final Map<UUID, Long> SETTLED_AT = new HashMap<>();

    private LeyLines() {
    }

    /** Remembers the shrine whose core is at {@code pos}, if {@code player} doesn't already. */
    public static void remember(ServerPlayer player, BlockPos pos, ShrineKind kind) {
        List<LeyShrine> memory = player.getData(MEMORY);
        if (memory.stream().noneMatch(shrine -> shrine.pos().equals(pos))) {
            List<LeyShrine> more = new ArrayList<>(memory);
            more.add(LeyShrine.shrine(pos, kind));
            player.setData(MEMORY, List.copyOf(more));
        }
    }

    /** Remembers the Ley Anchor at {@code pos} by its {@code name} (or remembers it anew, renamed); whether it was already known. */
    public static boolean rememberAnchor(ServerPlayer player, BlockPos pos, String name) {
        List<LeyShrine> memory = player.getData(MEMORY);
        boolean known = memory.stream().anyMatch(place -> place.pos().equals(pos));
        List<LeyShrine> more = new ArrayList<>(memory.stream().filter(place -> !place.pos().equals(pos)).toList());
        more.add(LeyShrine.anchor(pos, name));
        player.setData(MEMORY, List.copyOf(more));
        return known;
    }

    /** An anchor's name in the menu: its own, or "Ley Anchor". */
    public static Component anchorTitle(String name) {
        return name.isEmpty() ? Component.translatable("block.elementalarcana.ley_anchor") : Component.literal(name);
    }

    private static void forget(ServerPlayer player, BlockPos pos) {
        player.setData(MEMORY, player.getData(MEMORY).stream().filter(shrine -> !shrine.pos().equals(pos)).toList());
    }

    /** The ley menu at the shrine whose core is at {@code here}: the other shrines {@code player} remembers, nearest first. */
    public static void showMenu(ServerPlayer player, BlockPos here) {
        if (!MagicAttachments.get(player).isAwakened()) {
            player.displayClientMessage(Component.translatable("message.elementalarcana.ley.asleep"), true);
            return;
        }
        List<LeyShrine> others = player.getData(MEMORY).stream()
                .filter(shrine -> !shrine.pos().equals(here))
                .sorted(Comparator.comparingDouble(shrine -> shrine.pos().distSqr(here)))
                .limit(LeyRules.MENU_SIZE)
                .toList();
        if (others.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.elementalarcana.ley.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.elementalarcana.ley.header").withStyle(ChatFormatting.LIGHT_PURPLE));
        for (LeyShrine shrine : others) {
            double dx = shrine.pos().getX() - here.getX();
            double dz = shrine.pos().getZ() - here.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            int cost = LeyRules.manaCost(distance);
            String command = "/ley " + shrine.pos().getX() + " " + shrine.pos().getY() + " " + shrine.pos().getZ();
            Component line = Component.literal("  ").append(Component.translatable("message.elementalarcana.ley.entry", shrine.title(),
                            String.format(Locale.ROOT, "%,d", Math.round(distance)),
                            Component.translatable("direction.elementalarcana." + LeyRules.direction(dx, dz)), cost))
                    .withStyle(style -> style.withColor(shrine.color())
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("message.elementalarcana.ley.hover", cost))));
            player.sendSystemMessage(line);
        }
    }

    private static Component name(ShrineKind kind) {
        return Component.translatable("message.elementalarcana.ley.shrine",
                Component.translatable("school.elementalarcana." + kind.getSerializedName()));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ley")
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(LeyLines::travel)));
    }

    /** /ley x y z: from the shrine the player stands at to the remembered one at (x, y, z). */
    private static int travel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        BlockPos target = BlockPosArgument.getBlockPos(context, "pos");
        ServerLevel level = player.serverLevel();
        String refusal = refusal(player, level, target);
        if (refusal != null) {
            player.displayClientMessage(Component.translatable(refusal), true);
            return 0;
        }
        LeyShrine destination = player.getData(MEMORY).stream().filter(shrine -> shrine.pos().equals(target)).findFirst().orElseThrow();
        BlockPos here = nearestCore(level, player.blockPosition());
        double distance = Math.sqrt(Math.pow(target.getX() - here.getX(), 2) + Math.pow(target.getZ() - here.getZ(), 2));
        // The far shrine or anchor must still be there (this loads its ground, if it isn't loaded).
        if (!isPlace(level.getBlockState(target))) {
            forget(player, target);
            player.displayClientMessage(Component.translatable("message.elementalarcana.ley.gone"), true);
            return 0;
        }
        if (!CastingService.payUpkeep(player, MagicAttachments.get(player), LeyRules.manaCost(distance))) {
            player.displayClientMessage(Component.translatable("message.elementalarcana.ley.no_mana", LeyRules.manaCost(distance)), true);
            return 0;
        }
        Vec3 arrival = arrival(level, target);
        int color = destination.color();
        pillar(level, player.position(), color);
        Vec3 core = Vec3.atCenterOf(target);
        float yaw = (float) Math.toDegrees(Math.atan2(-(core.x - arrival.x), core.z - arrival.z));
        player.teleportTo(level, arrival.x, arrival.y, arrival.z, yaw, 10f);
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
        pillar(level, arrival, color);
        SETTLED_AT.put(player.getUUID(), level.getGameTime() + LeyRules.COOLDOWN_TICKS);
        MagicTriggers.fire(player, "ley", destination.trigger(), 1);
        return 1;
    }

    /** Why {@code player} can't travel to {@code target} now (a message key), or null if they can. */
    @Nullable
    private static String refusal(ServerPlayer player, ServerLevel level, BlockPos target) {
        if (level.dimension() != Level.OVERWORLD) {
            return "message.elementalarcana.ley.not_here";
        }
        if (!MagicAttachments.get(player).isAwakened()) {
            return "message.elementalarcana.ley.asleep";
        }
        BlockPos here = nearestCore(level, player.blockPosition());
        if (here == null) {
            return "message.elementalarcana.ley.not_at_shrine";
        }
        if (here.equals(target) || player.getData(MEMORY).stream().noneMatch(shrine -> shrine.pos().equals(target))) {
            return "message.elementalarcana.ley.unknown";
        }
        Long settled = SETTLED_AT.get(player.getUUID());
        if (settled != null && level.getGameTime() < settled) {
            return "message.elementalarcana.ley.settling";
        }
        return null;
    }

    /** Whether a block is a place on the ley lines: a Shrine Core or a Ley Anchor. */
    private static boolean isPlace(BlockState state) {
        return state.is(ModWorld.SHRINE_CORE.get()) || state.is(ModWorld.LEY_ANCHOR.get());
    }

    /** The Shrine Core or Ley Anchor within reach of {@code at}, or null. */
    @Nullable
    private static BlockPos nearestCore(ServerLevel level, BlockPos at) {
        int reach = (int) Math.ceil(LeyRules.REACH);
        BlockPos best = null;
        double bestDistance = LeyRules.REACH * LeyRules.REACH;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-reach, -reach, -reach), at.offset(reach, reach, reach))) {
            if (isPlace(level.getBlockState(pos)) && pos.distSqr(at) <= bestDistance) {
                best = pos.immutable();
                bestDistance = pos.distSqr(at);
            }
        }
        return best;
    }

    /**
     * Where a traveller arrives: two blocks out from the core on the shrine's platform (or from an
     * anchor, beside it on its ground), wherever there's room.
     */
    private static Vec3 arrival(ServerLevel level, BlockPos core) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            for (int down = 0; down <= 3; down++) {
                BlockPos feet = core.relative(side, 2).below(down);
                if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                        && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()) {
                    return Vec3.atBottomCenterOf(feet);
                }
            }
        }
        return Vec3.atBottomCenterOf(core.above());
    }

    /** A pillar of the element's light where a traveller leaves or arrives, with its sound. */
    private static void pillar(ServerLevel level, Vec3 at, int color) {
        GlowParticleOptions glow = GlowParticleOptions.of(ModContent.FLARE.get(), color, 0xFFFFFF, 0.5f, 16);
        for (int i = 0; i < 12; i++) {
            level.sendParticles(glow, at.x, at.y + i * 0.4, at.z, 2, 0.15, 0.1, 0.15, 0.01);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1, at.z, 40, 0.4, 0.8, 0.4, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 1.5f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.7f);
    }
}
