package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/** /arcana: testing and admin shortcuts, applied to the command's player. Op level 2. */
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
                        .executes(ctx -> modify(ctx.getSource(), MagicData::clearCooldowns, "commands.elementalarcana.cooldowns_reset")))));
    }

    private static int modify(CommandSourceStack source, Consumer<MagicData> change, String messageKey) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        change.accept(MagicAttachments.get(player));
        MagicAttachments.sync(player);
        source.sendSuccess(() -> Component.translatable(messageKey), false);
        return 1;
    }
}
