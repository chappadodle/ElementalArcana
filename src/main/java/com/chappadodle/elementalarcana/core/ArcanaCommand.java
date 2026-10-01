package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.BubblePrisons;
import com.chappadodle.elementalarcana.content.CreatureLevels;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Consumer;

/** /arcana: testing and admin shortcuts. Op level 2. */
public final class ArcanaCommand {
    private static final DynamicCommandExceptionType UNKNOWN_SCHOOL =
            new DynamicCommandExceptionType(id -> Component.translatable("commands.elementalarcana.unknown_school", String.valueOf(id)));

    private ArcanaCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arcana")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("level").then(Commands.literal("set")
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, MagicData.MAX_LEVEL))
                                .executes(ctx -> {
                                    int level = IntegerArgumentType.getInteger(ctx, "level");
                                    return modify(ctx.getSource(), data -> data.setLevel(level), "commands.elementalarcana.level_set");
                                }))))
                .then(Commands.literal("xp").then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    return modify(ctx.getSource(), data -> data.addXp(amount), "commands.elementalarcana.xp_added");
                                }))))
                .then(Commands.literal("stats")
                        .then(Commands.literal("reset")
                                .executes(ctx -> modify(ctx.getSource(), MagicData::resetStats, "commands.elementalarcana.stats_reset")))
                        .then(Commands.literal("spend").then(Commands.argument("count", IntegerArgumentType.integer(1))
                                .then(Commands.argument("stat", StringArgumentType.greedyString())
                                        .executes(ctx -> spendStats(ctx.getSource(), StringArgumentType.getString(ctx, "stat"),
                                                IntegerArgumentType.getInteger(ctx, "count")))))))
                .then(Commands.literal("affinity")
                        .then(Commands.literal("add").then(Commands.argument("school", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(SpellRegistries.SCHOOLS.keySet(), builder))
                                .executes(ctx -> {
                                    ResourceLocation id = ResourceLocationArgument.getId(ctx, "school");
                                    SpellSchool school = SpellRegistries.SCHOOLS.get(id);
                                    if (school == null) {
                                        throw UNKNOWN_SCHOOL.create(id);
                                    }
                                    return modify(ctx.getSource(), data -> data.forceAffinity(school), "commands.elementalarcana.affinity_added");
                                })))
                        .then(Commands.literal("reset")
                                .executes(ctx -> modify(ctx.getSource(), MagicData::clearAffinities, "commands.elementalarcana.affinity_reset"))))
                .then(Commands.literal("mana")
                        .then(Commands.literal("fill")
                                .executes(ctx -> modify(ctx.getSource(), MagicData::fillMana, "commands.elementalarcana.mana_set")))
                        .then(Commands.literal("set").then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                .executes(ctx -> {
                                    float amount = FloatArgumentType.getFloat(ctx, "amount");
                                    return modify(ctx.getSource(), data -> data.setMana(amount), "commands.elementalarcana.mana_set");
                                }))))
                .then(Commands.literal("cooldowns").then(Commands.literal("reset")
                        .executes(ctx -> modify(ctx.getSource(), MagicData::clearCooldowns, "commands.elementalarcana.cooldowns_reset"))))
                .then(Commands.literal("attune").then(attuneTargets()))
                .then(Commands.literal("creaturelevel").then(Commands.argument("targets", EntityArgument.entities())
                        .executes(ctx -> showLevels(ctx.getSource(), EntityArgument.getEntities(ctx, "targets")))
                        .then(Commands.literal("set").then(Commands.argument("level", IntegerArgumentType.integer(1, MagicData.MAX_LEVEL))
                                .executes(ctx -> setLevels(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"),
                                        IntegerArgumentType.getInteger(ctx, "level")))))))
                .then(Commands.literal("bubble").then(Commands.argument("targets", EntityArgument.entities())
                        .executes(ctx -> bubble(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"))))));
    }

    /** /arcana attune <targets> <element> <rank>, or /arcana attune <targets> none. Mobs only. */
    private static RequiredArgumentBuilder<CommandSourceStack, EntitySelector> attuneTargets() {
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targets = Commands.argument("targets", EntityArgument.entities());
        targets.then(Commands.literal("none")
                .executes(ctx -> clearAttunement(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"))));
        for (Element element : Element.values()) {
            LiteralArgumentBuilder<CommandSourceStack> elementNode = Commands.literal(element.name().toLowerCase(Locale.ROOT));
            for (AttunementRank rank : AttunementRank.values()) {
                elementNode.then(Commands.literal(rank.name().toLowerCase(Locale.ROOT))
                        .executes(ctx -> attune(ctx.getSource(), EntityArgument.getEntities(ctx, "targets"), element, rank)));
            }
            targets.then(elementNode);
        }
        return targets;
    }

    private static int attune(CommandSourceStack source, Collection<? extends Entity> targets, Element element, AttunementRank rank) {
        int attuned = 0;
        int refused = 0;
        for (Entity entity : targets) {
            if (entity instanceof Mob mob) {
                if (Attunement.attune(mob, element, rank)) {
                    attuned++;
                } else {
                    refused++;
                }
            }
        }
        int done = attuned;
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.attuned", done), true);
        if (refused > 0) {
            source.sendFailure(Component.translatable("commands.elementalarcana.attune_innate", refused));
        }
        return done;
    }

    /** /arcana creaturelevel <targets>: each one's level (with its rank's bonus) and max health. */
    private static int showLevels(CommandSourceStack source, Collection<? extends Entity> targets) {
        int shown = 0;
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
                int level = CreatureLevels.levelOf(living);
                String health = String.format(Locale.ROOT, "%.1f", living.getMaxHealth());
                source.sendSuccess(() -> Component.translatable("commands.elementalarcana.creature_level", living.getDisplayName(), level, health), false);
                shown++;
            }
        }
        return shown;
    }

    /** /arcana creaturelevel <targets> set <level>: sets creatures' zone level (players are skipped). */
    private static int setLevels(CommandSourceStack source, Collection<? extends Entity> targets, int level) {
        int set = 0;
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                CreatureLevels.setBaseLevel(living, level);
                set++;
            }
        }
        int done = set;
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.creature_level_set", done, level), true);
        return done;
    }

    /** /arcana bubble <targets>: traps them in a Bubble Prison, as if hit by the spell (for testing). */
    private static int bubble(CommandSourceStack source, Collection<? extends Entity> targets) {
        int trapped = 0;
        for (Entity entity : targets) {
            if (entity instanceof LivingEntity living && BubblePrisons.canTrap(living)) {
                BubblePrisons.trap(living);
                trapped++;
            }
        }
        int done = trapped;
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.bubbled", done), true);
        return done;
    }

    private static int clearAttunement(CommandSourceStack source, Collection<? extends Entity> targets) {
        int cleared = 0;
        for (Entity entity : targets) {
            if (entity instanceof Mob mob && Attunement.clear(mob)) {
                cleared++;
            }
        }
        int done = cleared;
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.attune_cleared", done), true);
        return done;
    }

    /** Spends up to {@code count} stat points on {@code key} (e.g. "potency", "affinity/fire"). */
    private static int spendStats(CommandSourceStack source, String key, int count) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MagicData data = MagicAttachments.get(player);
        int spent = 0;
        while (spent < count && data.spend(key)) {
            spent++;
        }
        PlayerStats.apply(player);
        MagicAttachments.sync(player);
        int total = spent;
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.stats_spent", total, key, data.stats().get(key)), false);
        return spent;
    }

    private static int modify(CommandSourceStack source, Consumer<MagicData> change, String messageKey) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        change.accept(MagicAttachments.get(player));
        PlayerStats.apply(player);
        MagicAttachments.sync(player);
        source.sendSuccess(() -> Component.translatable(messageKey), false);
        return 1;
    }
}
