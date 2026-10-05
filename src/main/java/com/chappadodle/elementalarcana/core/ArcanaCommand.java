package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.AwakeningRules;
import com.chappadodle.elementalarcana.api.BountyRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SkillTree;
import com.chappadodle.elementalarcana.api.SkillTrees;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.Awakenings;
import com.chappadodle.elementalarcana.content.BubblePrisons;
import com.chappadodle.elementalarcana.content.CreatureLevels;
import com.chappadodle.elementalarcana.content.ManaTides;
import com.chappadodle.elementalarcana.content.SkillTreeLoader;
import com.chappadodle.elementalarcana.content.creature.ModCreatures;
import com.chappadodle.elementalarcana.content.creature.WispEntity;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import com.chappadodle.elementalarcana.content.people.Bounties;
import com.chappadodle.elementalarcana.content.rift.Rifts;
import com.chappadodle.elementalarcana.content.sanctum.SanctumSealBlockEntity;
import com.chappadodle.elementalarcana.content.wild.WildSpawner;
import com.chappadodle.elementalarcana.content.cantrip.ModCantrips;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
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
                .then(Commands.literal("awaken")
                        .then(Commands.literal("clock").executes(ctx -> awakeningClock(ctx.getSource())))
                        .then(Commands.literal("day").then(Commands.argument("day", IntegerArgumentType.integer(0, 1000))
                                .executes(ctx -> setAwakeningDay(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "day")))))
                        .then(awakenElement(Element.FIRE)).then(awakenElement(Element.WATER))
                        .then(awakenElement(Element.ICE)).then(awakenElement(Element.WIND))
                        .then(awakenElement(Element.EARTH)).then(awakenElement(Element.CRYSTAL))
                        .then(awakenElement(Element.LIGHTNING)).then(awakenElement(Element.RADIANCE)))
                .then(Commands.literal("spell").then(Commands.argument("spell", ResourceLocationArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(SpellRegistries.SPELLS.keySet(), builder))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 10))
                                .executes(ctx -> setSpellLevel(ctx.getSource(), ResourceLocationArgument.getId(ctx, "spell"),
                                        IntegerArgumentType.getInteger(ctx, "level"), ""))
                                .then(Commands.argument("branches", StringArgumentType.greedyString())
                                        .executes(ctx -> setSpellLevel(ctx.getSource(), ResourceLocationArgument.getId(ctx, "spell"),
                                                IntegerArgumentType.getInteger(ctx, "level"), StringArgumentType.getString(ctx, "branches")))))))
                .then(Commands.literal("tree")
                        .then(Commands.literal("info").executes(ctx -> {
                            String info = SkillTreeLoader.describe(SkillTrees.current());
                            ctx.getSource().sendSuccess(() -> Component.literal("Skill tree: " + info), false);
                            return 1;
                        }))
                        .then(Commands.literal("take").then(Commands.argument("node", StringArgumentType.greedyString())
                                .executes(ctx -> treeChange(ctx.getSource(), StringArgumentType.getString(ctx, "node"), true))))
                        .then(Commands.literal("refund").then(Commands.argument("node", StringArgumentType.greedyString())
                                .executes(ctx -> treeChange(ctx.getSource(), StringArgumentType.getString(ctx, "node"), false))))
                        .then(Commands.literal("reset")
                                .executes(ctx -> modify(ctx.getSource(), MagicData::resetTree, "commands.elementalarcana.tree_reset"))))
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
                        .executes(ctx -> bubble(ctx.getSource(), EntityArgument.getEntities(ctx, "targets")))))
                .then(Commands.literal("wisp").then(wispSpawn()))
                .then(riftOpen())
                .then(bountyGive())
                .then(golemSpawn())
                .then(wildSpawn())
                .then(cantripLearn())
                .then(Commands.literal("sanctum").then(Commands.literal("reset").executes(ctx -> resetSanctums(ctx.getSource()))))
                .then(Commands.literal("tide")
                        .then(Commands.literal("start").executes(ctx -> {
                            ManaTides.force(ctx.getSource().getServer());
                            return 1;
                        }))
                        .then(Commands.literal("stop").executes(ctx -> {
                            ManaTides.calm(ctx.getSource().getServer());
                            return 1;
                        }))));
    }

    /** /arcana sanctum reset: seals again every sanctum seal within 3 chunks (its Sovereign, if out, is gone). */
    private static int resetSanctums(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos at = BlockPos.containing(source.getPosition());
        int reset = 0;
        for (int cx = (at.getX() >> 4) - 3; cx <= (at.getX() >> 4) + 3; cx++) {
            for (int cz = (at.getZ() >> 4) - 3; cz <= (at.getZ() >> 4) + 3; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity block : List.copyOf(level.getChunk(cx, cz).getBlockEntities().values())) {
                    if (block instanceof SanctumSealBlockEntity seal) {
                        seal.reset();
                        reset++;
                    }
                }
            }
        }
        int count = reset;
        source.sendSuccess(() -> Component.literal("Reset " + count + " sanctum seal(s)"), true);
        return count;
    }

    /** /arcana wisp spawn [element]: calls a wisp near you now, the way the wild spawner would (for testing). */
    /**
     * /arcana rift [element]: opens a rift 6 blocks ahead of whoever ran it (from the console, where
     * it ran), on the ground. /arcana rift near: opens one where a rift would open by itself near
     * there (24-40 blocks off, on open ground; see Rifts), in the land's element.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> riftOpen() {
        LiteralArgumentBuilder<CommandSourceStack> rift = Commands.literal("rift").executes(ctx -> openRift(ctx.getSource(), null));
        for (Element element : Element.values()) {
            rift.then(Commands.literal(element.name().toLowerCase(Locale.ROOT)).executes(ctx -> openRift(ctx.getSource(), element)));
        }
        rift.then(Commands.literal("near").executes(ctx -> {
            CommandSourceStack source = ctx.getSource();
            ServerLevel level = source.getLevel();
            BlockPos pos = Rifts.findSpot(level, source.getPosition(), level.random);
            if (pos == null) {
                source.sendFailure(Component.translatable("commands.elementalarcana.rift_no_spot"));
                return 0;
            }
            Element element = Attunement.landElement(level, pos, level.random);
            Rifts.open(level, pos, element, AttunementRank.ARCHMAGE);
            Component name = Component.translatable("school.elementalarcana." + element.name().toLowerCase(Locale.ROOT));
            source.sendSuccess(() -> Component.translatable("commands.elementalarcana.rift_opened", name, pos.getX(), pos.getY(), pos.getZ()), true);
            return 1;
        }));
        return rift;
    }

    private static int openRift(CommandSourceStack source, @Nullable Element element) {
        ServerLevel level = source.getLevel();
        Vec3 at = source.getPosition();
        Entity runner = source.getEntity();
        if (runner != null) {
            at = at.add(Vec3.directionFromRotation(0, runner.getYRot()).scale(6));
        }
        int x = Mth.floor(at.x);
        int z = Mth.floor(at.z);
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        Element chosen = element != null ? element : Attunement.landElement(level, pos, level.random);
        if (Rifts.open(level, pos, chosen, AttunementRank.ARCHMAGE) == null) {
            return 0;
        }
        Component name = Component.translatable("school.elementalarcana." + chosen.name().toLowerCase(Locale.ROOT));
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.rift_opened", name, pos.getX(), pos.getY(), pos.getZ()), true);
        return 1;
    }

    /** /arcana bounty <task> [element]: gives the player a Bounty Contract for that task, rolled for the land they stand in (or that element). */
    private static LiteralArgumentBuilder<CommandSourceStack> bountyGive() {
        LiteralArgumentBuilder<CommandSourceStack> bounty = Commands.literal("bounty");
        for (BountyRules.Task task : BountyRules.Task.values()) {
            LiteralArgumentBuilder<CommandSourceStack> kind = Commands.literal(task.id()).executes(ctx -> giveBounty(ctx.getSource(), task, null));
            // For testing: the element it's rolled for, instead of the land's.
            for (Element element : Element.values()) {
                kind.then(Commands.literal(element.name().toLowerCase(Locale.ROOT)).executes(ctx -> giveBounty(ctx.getSource(), task, element)));
            }
            bounty.then(kind);
        }
        return bounty;
    }

    private static int giveBounty(CommandSourceStack source, BountyRules.Task task, @Nullable Element element) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Element land = element != null ? element : Attunement.landElement(player.level(), player.blockPosition(), player.getRandom());
        ItemStack contract = Bounties.contract(task, land, player.getRandom());
        Component name = contract.getHoverName();
        if (!player.getInventory().add(contract)) {
            player.drop(contract, false);
        }
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.bounty_given", name), true);
        return 1;
    }

    /** /arcana golem <element>: summons an Elemental Golem 5 blocks ahead of whoever ran it (or where it ran). */
    private static LiteralArgumentBuilder<CommandSourceStack> golemSpawn() {
        LiteralArgumentBuilder<CommandSourceStack> golem = Commands.literal("golem");
        for (Element element : ModCreatures.GOLEM_ELEMENTS) {
            golem.then(Commands.literal(element.name().toLowerCase(Locale.ROOT)).executes(ctx -> {
                CommandSourceStack source = ctx.getSource();
                Vec3 at = source.getPosition();
                if (source.getEntity() != null) {
                    at = at.add(Vec3.directionFromRotation(0, source.getEntity().getYRot()).scale(5));
                }
                return ModCreatures.golem(element).spawn(source.getLevel(), BlockPos.containing(at), MobSpawnType.COMMAND) == null ? 0 : 1;
            }));
        }
        return golem;
    }

    /**
     * /arcana wild <treant|wraith|salamander> [natural]: brings one of the Creatures of the Wild in
     * 20 to 40 blocks from where it ran, the way the spawner does; with {@code natural}, only in its
     * own land and away from others of its kind, as the spawner would.
     */
    private static LiteralArgumentBuilder<CommandSourceStack> wildSpawn() {
        LiteralArgumentBuilder<CommandSourceStack> wild = Commands.literal("wild");
        for (WildSpawner.Kind kind : WildSpawner.Kind.values()) {
            String name = kind.name().toLowerCase(Locale.ROOT);
            wild.then(Commands.literal(name).executes(ctx -> spawnWild(ctx.getSource(), kind, true))
                    .then(Commands.literal("natural").executes(ctx -> spawnWild(ctx.getSource(), kind, false))));
        }
        return wild;
    }

    private static int spawnWild(CommandSourceStack source, WildSpawner.Kind kind, boolean forced) {
        Mob mob = WildSpawner.trySpawn(source.getLevel(), kind, source.getPosition(), source.getLevel().getRandom(), forced);
        if (mob == null) {
            source.sendFailure(Component.translatable("commands.elementalarcana.wild_none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.wild_spawned", mob.getDisplayName(),
                mob.getBlockX(), mob.getBlockY(), mob.getBlockZ()), false);
        return 1;
    }

    /** /arcana cantrip <cantrip>|all|forget: learns cantrips without their scrolls, or forgets them all (for tests). */
    private static LiteralArgumentBuilder<CommandSourceStack> cantripLearn() {
        LiteralArgumentBuilder<CommandSourceStack> cantrip = Commands.literal("cantrip")
                .then(Commands.literal("all").executes(ctx -> learnCantrips(ctx.getSource(), ModCantrips.cantrips())))
                .then(Commands.literal("forget").executes(ctx -> modify(ctx.getSource(), MagicData::forgetCantrips,
                        "commands.elementalarcana.cantrips_forgotten")));
        for (Spell spell : ModCantrips.cantrips()) {
            cantrip.then(Commands.literal(spell.id().getPath()).executes(ctx -> learnCantrips(ctx.getSource(), List.of(spell))));
        }
        return cantrip;
    }

    private static int learnCantrips(CommandSourceStack source, List<Spell> cantrips) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MagicData data = MagicAttachments.get(player);
        int learned = 0;
        for (Spell spell : cantrips) {
            if (data.learnCantrip(spell.id())) {
                learned++;
            }
        }
        MagicAttachments.sync(player);
        int count = learned;
        source.sendSuccess(() -> Component.literal("Learned " + count + " cantrip(s)"), false);
        return count;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> wispSpawn() {
        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("spawn").executes(ctx -> spawnWisp(ctx.getSource(), null));
        for (Element element : Element.values()) {
            spawn.then(Commands.literal(element.name().toLowerCase(Locale.ROOT)).executes(ctx -> spawnWisp(ctx.getSource(), element)));
        }
        return spawn;
    }

    private static int spawnWisp(CommandSourceStack source, @Nullable Element element) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        WispEntity wisp = WispSpawner.trySpawnWild(player.serverLevel(), player, element);
        if (wisp == null) {
            source.sendFailure(Component.translatable("commands.elementalarcana.wisp_none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.wisp_spawned", wisp.getDisplayName(),
                wisp.getBlockX(), wisp.getBlockY(), wisp.getBlockZ()), false);
        return 1;
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

    /** /arcana awaken <element>: wakes that element in you at once (opposites don't hold it back). */
    private static LiteralArgumentBuilder<CommandSourceStack> awakenElement(Element element) {
        return Commands.literal(element.name().toLowerCase(Locale.ROOT)).executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            boolean woke = Awakenings.force(player, element, Component.translatable("message.elementalarcana.awakening.command"));
            ctx.getSource().sendSuccess(() -> Component.literal(woke ? "Awakened " + element.name() : "Already awakened to " + element.name()), false);
            return woke ? 1 : 0;
        });
    }

    /** /arcana awaken day <n>: makes today day n of your awakening count (to test the day-10 ramp). */
    private static int setAwakeningDay(CommandSourceStack source, int day) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MagicAttachments.get(player).setAwakeningDay(player.serverLevel().getDayTime(), day);
        MagicAttachments.sync(player);
        source.sendSuccess(() -> Component.literal("Awakening day set to " + day), false);
        return 1;
    }

    /** /arcana awaken clock: your day count, the odds of waking now, and what a Catalyst would give. */
    private static int awakeningClock(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MagicData data = MagicAttachments.get(player);
        int day = data.awakeningDay(player.serverLevel().getDayTime());
        int held = data.familiesHeld();
        String line = String.format(Locale.ROOT, "Day %d. First awakening: %.0f%% each day, %.0f%% per brush. %d element families held",
                day, AwakeningRules.dailyChance(day) * 100, AwakeningRules.brushChance(day) * 100, held);
        if (held > 0) {
            line += String.format(Locale.ROOT, "; a Catalyst now: %.2f%% (level %d, %d failed tries)",
                    AwakeningRules.catalystChance(held, data.level(), data.awakening().failures(held)) * 100, data.level(),
                    data.awakening().failures(held));
        }
        String text = line;
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    /** /arcana tree take|refund <node>: like clicking it in the tree (refunds here cost no Essence). */
    /** Dev/admin: a spell at a level, through its tree path for free, with the named branches at its forks. */
    private static int setSpellLevel(CommandSourceStack source, ResourceLocation id, int level, String branches) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Spell spell = SpellRegistries.SPELLS.get(id);
        if (spell == null) {
            source.sendFailure(Component.literal("No spell " + id));
            return 0;
        }
        MagicData data = MagicAttachments.get(player);
        data.setSpellPath(spell, level, branches.isBlank() ? List.of() : List.of(branches.trim().split("\\s+")));
        PlayerStats.apply(player);
        MagicAttachments.sync(player);
        source.sendSuccess(() -> Component.empty().append(spell.displayName())
                .append(": level " + data.spellLevel(spell) + " " + data.branches(spell).values()), false);
        return 1;
    }

    private static int treeChange(CommandSourceStack source, String node, boolean take) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        MagicData data = MagicAttachments.get(player);
        SkillTree.Check check = take ? data.take(node) : data.refund(node);
        PlayerStats.apply(player);
        MagicAttachments.sync(player);
        source.sendSuccess(() -> Component.translatable("commands.elementalarcana.tree_change", node, check.name(), data.treePoints()), false);
        return check == SkillTree.Check.OK ? 1 : 0;
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
