package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.compat.jei.JeiHooks;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import com.chappadodle.elementalarcana.network.AuraPayload;
import com.chappadodle.elementalarcana.network.CastSpellPayload;
import com.chappadodle.elementalarcana.network.SelectSpellPayload;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A scripted test run of the game client, for checking visuals without anyone at the keyboard
 * (see tools/autotest.sh, which starts the game hidden in a virtual display). It does nothing
 * unless the game was started with {@code -Delementalarcana.autotest=true}. Once a world is loaded
 * it plays {@code <game dir>/autotest.txt}, one step per line:
 * <pre>
 * wait 40                  wait 40 ticks
 * cmd tp @s 0 100 0        run a command as the player (with full permissions)
 * shot fireball_held       save a screenshot (screenshots/fireball_held.png)
 * look 90 10               face yaw 90, pitch 10
 * select elementalarcana:boulder   select a spell
 * press / release          the cast key going down / up
 * aura                     hide the player's aura, or show it again (the H key)
 * launch_one / launch_all  the conjuring launch keys
 * screen status|tree [spell]|stats|journal|inventory|advancements|close   open one of the mod's
 *                          screens, the inventory, or the advancements on the mod's tab
 * hud on|off               show or hide the HUD
 * slot 2                   hold what's in hotbar slot 2 (0 to 8)
 * camera first|back|front  the camera view
 * use                      right-click the creature or block under the crosshair (or, with
 *                          neither, use the held item)
 * hold_use 45              hold right click down for 45 ticks (items used over time)
 * key jump down / key jump up   hold a movement key down (jump, sneak or forward), or let it go
 * goto elementalarcana:fire_shrine 18 14 [2]   stand 18 blocks south of the nearest such structure
 *                          and 14 above its base, looking at its middle, 2 blocks above the base
 *                          (default 0); it's searched for from where the player is
 * find elementalarcana:arcane_lectern 80   look for that block within 80 blocks (and 60 below to
 *                          20 above); log where, and view the first from 8 blocks south, 5 up
 * jei_filter @elementalarcana   filter JEI's item list, as if typed in its search box (when JEI
 *                          is loaded; with nothing after it, show everything again)
 * jei_show elementalarcana:sovereign_heart fire   open JEI's recipes and info for an item (of an
 *                          element, for the items that come in one per element)
 * quit                     close the game
 * </pre>
 * A player who is dead (in this run, or in the saved world) is respawned before any step runs.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class AutoTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("elementalarcana.autotest");

    private static List<String> steps;
    private static int index;
    private static int waitTicks;
    private static int respawnCooldown;
    private static int holdUseTicks;
    private static boolean finished;

    private AutoTest() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || finished) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null && minecraft.screen instanceof BackupConfirmScreen confirm) {
            // A world with the mod's own dimension (the Hollow) asks once whether to load it without a
            // backup: the test world needs none.
            for (GuiEventListener child : confirm.children()) {
                if (child instanceof Button button && button.getMessage().getContents() instanceof TranslatableContents text
                        && text.getKey().equals("selectWorld.backupJoinSkipButton")) {
                    LOGGER.info("[autotest] loading the world without a backup");
                    button.onPress();
                    return;
                }
            }
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        if (minecraft.player.isDeadOrDying()) {
            // A player who died (in this run, or in the saved world) comes back first: steps need one alive.
            if (respawnCooldown-- <= 0) {
                LOGGER.info("[autotest] respawning");
                minecraft.player.respawn();
                respawnCooldown = 20;
            }
            return;
        }
        if (steps == null) {
            Path script = minecraft.gameDirectory.toPath().resolve("autotest.txt");
            try {
                steps = Files.readAllLines(script);
            } catch (IOException e) {
                LOGGER.error("[autotest] can't read {}: {}", script, e.getMessage());
                finished = true;
                return;
            }
            LOGGER.info("[autotest] running {} ({} lines)", script, steps.size());
            // Give the world a moment to settle before the first step.
            waitTicks = 60;
        }
        if (holdUseTicks > 0 && --holdUseTicks == 0) {
            minecraft.options.keyUse.setDown(false);
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        while (index < steps.size()) {
            String line = steps.get(index++).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            LOGGER.info("[autotest] {}", line);
            try {
                if (!run(minecraft, line)) {
                    return;
                }
            } catch (RuntimeException e) {
                LOGGER.error("[autotest] step failed: {}", line, e);
            }
        }
        LOGGER.info("[autotest] done");
        finished = true;
    }

    /** Runs one step; returns false when the script should pause (a wait, or the game closing). */
    private static boolean run(Minecraft minecraft, String line) {
        String[] parts = line.split("\\s+", 2);
        String argument = parts.length > 1 ? parts[1] : "";
        switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "wait" -> {
                waitTicks = Integer.parseInt(argument);
                return false;
            }
            case "cmd" -> command(minecraft, argument);
            case "shot" -> Screenshot.grab(minecraft.gameDirectory, argument + ".png", minecraft.getMainRenderTarget(),
                    message -> LOGGER.info("[autotest] {}", message.getString()));
            case "look" -> {
                String[] angles = argument.split("\\s+");
                float yaw = Float.parseFloat(angles[0]);
                float pitch = Float.parseFloat(angles[1]);
                minecraft.player.setYRot(yaw);
                minecraft.player.setXRot(pitch);
                minecraft.player.yRotO = yaw;
                minecraft.player.xRotO = pitch;
                minecraft.player.setYHeadRot(yaw);
            }
            case "select" -> PacketDistributor.sendToServer(new SelectSpellPayload(ResourceLocation.parse(argument)));
            case "aura" -> PacketDistributor.sendToServer(AuraPayload.TOGGLE);
            case "press" -> PacketDistributor.sendToServer(CastSpellPayload.PRESS);
            case "release" -> PacketDistributor.sendToServer(CastSpellPayload.RELEASE);
            case "launch_one" -> PacketDistributor.sendToServer(CastSpellPayload.LAUNCH_ONE);
            case "launch_all" -> PacketDistributor.sendToServer(CastSpellPayload.LAUNCH_ALL);
            case "screen" -> openScreen(minecraft, argument);
            case "jei_filter" -> {
                if (ModList.get().isLoaded("jei")) {
                    JeiHooks.filter(argument);
                }
            }
            case "jei_show" -> {
                if (ModList.get().isLoaded("jei")) {
                    JeiHooks.show(stackOf(argument));
                }
            }
            case "hud" -> minecraft.options.hideGui = argument.equals("off");
            case "slot" -> minecraft.player.getInventory().selected = Integer.parseInt(argument.trim());
            case "use" -> {
                if (minecraft.gameMode == null) {
                    LOGGER.warn("[autotest] use: no game mode");
                } else if (minecraft.hitResult instanceof EntityHitResult hit) {
                    minecraft.gameMode.interact(minecraft.player, hit.getEntity(), InteractionHand.MAIN_HAND);
                } else if (minecraft.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                        && minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit).consumesAction()) {
                    LOGGER.info("[autotest] use: used the block");
                } else {
                    // Like a right click: if the block didn't take it, the item is used.
                    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
                }
            }
            case "hold_use" -> {
                // Right click held down (for items used over time, like the Prime Key), let go after the wait.
                minecraft.options.keyUse.setDown(true);
                holdUseTicks = Integer.parseInt(argument);
                waitTicks = holdUseTicks;
                return false;
            }
            case "key" -> {
                String[] words = argument.split("\\s+");
                KeyMapping key = switch (words[0]) {
                    case "jump" -> minecraft.options.keyJump;
                    case "sneak" -> minecraft.options.keyShift;
                    case "forward" -> minecraft.options.keyUp;
                    default -> throw new IllegalArgumentException("unknown key: " + words[0]);
                };
                key.setDown(words.length > 1 && words[1].equals("down"));
            }
            case "goto" -> gotoStructure(minecraft, argument);
            case "find" -> findBlock(minecraft, argument);
            case "camera" -> minecraft.options.setCameraType(switch (argument) {
                case "back" -> CameraType.THIRD_PERSON_BACK;
                case "front" -> CameraType.THIRD_PERSON_FRONT;
                default -> CameraType.FIRST_PERSON;
            });
            case "quit" -> {
                finished = true;
                minecraft.stop();
                return false;
            }
            default -> LOGGER.warn("[autotest] unknown step: {}", line);
        }
        return true;
    }

    /** Runs {@code command} on the integrated server as the player, with full permissions. */
    private static void command(Minecraft minecraft, String command) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        java.util.UUID id = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
            }
        });
    }

    /**
     * Finds the nearest structure of a kind (like /locate, from where the player is) and puts the
     * player {@code distance} blocks south of its middle and {@code height} above its base, looking
     * at its middle, {@code aim} blocks above the base. The structure's chunks generate on the way,
     * with the current code.
     */
    private static void gotoStructure(Minecraft minecraft, String argument) {
        String[] parts = argument.split("\\s+");
        ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(parts[0]));
        double distance = parts.length > 1 ? Double.parseDouble(parts[1]) : 16;
        double height = parts.length > 2 ? Double.parseDouble(parts[2]) : 12;
        double aim = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
        MinecraftServer server = minecraft.getSingleplayerServer();
        java.util.UUID id = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            Optional<Holder.Reference<Structure>> structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key);
            if (structure.isEmpty()) {
                LOGGER.warn("[autotest] goto: no structure {}", key.location());
                return;
            }
            Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, HolderSet.direct(structure.get()), player.blockPosition(), 100, false);
            if (found == null) {
                LOGGER.warn("[autotest] goto: no {} nearby", key.location());
                return;
            }
            ChunkPos chunkPos = new ChunkPos(found.getFirst());
            ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
            StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), structure.get().value(), chunk);
            // The pieces' own box: a start's box is padded for terrain blending.
            BoundingBox box = (start != null && start.isValid()
                    ? BoundingBox.encapsulatingBoxes(start.getPieces().stream().map(StructurePiece::getBoundingBox).toList())
                    : Optional.<BoundingBox>empty())
                    .orElse(BoundingBox.fromCorners(found.getFirst(), found.getFirst().offset(15, level.getSeaLevel(), 15)));
            BlockPos middle = box.getCenter();
            double x = middle.getX() + 0.5;
            double y = box.minY() + height;
            double z = middle.getZ() + 0.5 + distance;
            float pitch = (float) Math.toDegrees(Math.atan2(height + player.getEyeHeight() - aim, distance));
            player.teleportTo(level, x, y, z, 180f, pitch);
            LOGGER.info("[autotest] goto {}: found at {} {} {}", key.location(), middle.getX(), box.minY(), middle.getZ());
        });
    }

    /** Scans the loaded world around the player for a block; logs up to five and views the first one. */
    private static void findBlock(Minecraft minecraft, String argument) {
        String[] parts = argument.split("\\s+");
        Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(parts[0]));
        int radius = parts.length > 1 ? Integer.parseInt(parts[1]) : 64;
        MinecraftServer server = minecraft.getSingleplayerServer();
        java.util.UUID id = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockPos center = player.blockPosition();
            List<BlockPos> found = new java.util.ArrayList<>();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = -radius; x <= radius && found.size() < 5; x++) {
                for (int z = -radius; z <= radius && found.size() < 5; z++) {
                    if (!level.hasChunkAt(center.offset(x, 0, z))) {
                        continue;
                    }
                    for (int y = -60; y <= 20; y++) {
                        pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                        if (level.getBlockState(pos).is(block)) {
                            found.add(pos.immutable());
                            break;
                        }
                    }
                }
            }
            LOGGER.info("[autotest] find {}: {}", parts[0], found.isEmpty() ? "none" : found);
            if (!found.isEmpty()) {
                BlockPos first = found.get(0);
                player.teleportTo(level, first.getX() + 0.5, first.getY() + 5, first.getZ() + 8.5, 180f,
                        (float) Math.toDegrees(Math.atan2(5 + player.getEyeHeight() - 0.5, 8)));
            }
        });
    }

    /** An item from "namespace:item", or "namespace:item element" for the items that come in one per element. */
    private static ItemStack stackOf(String argument) {
        String[] parts = argument.split("\\s+");
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(parts[0])));
        if (parts.length > 1) {
            stack.set(ModGear.ELEMENT.get(), Element.valueOf(parts[1].toUpperCase(Locale.ROOT)));
        }
        return stack;
    }

    private static void openScreen(Minecraft minecraft, String name) {
        switch (name) {
            case "status" -> minecraft.setScreen(new StatusScreen());
            case "stats" -> minecraft.setScreen(new StatsScreen(null));
            case "close" -> minecraft.setScreen(null);
            case "journal" -> JournalBook.open(minecraft.player);
            case "inventory" -> minecraft.setScreen(new InventoryScreen(minecraft.player));
            case "advancements" -> {
                ClientAdvancements advancements = minecraft.player.connection.getAdvancements();
                AdvancementNode root = advancements.getTree().get(ElementalArcana.id("arcana/root"));
                if (root != null) {
                    advancements.setSelectedTab(root.holder(), true);
                }
                minecraft.setScreen(new AdvancementsScreen(advancements));
            }
            default -> {
                if (name.startsWith("tree")) {
                    String[] parts = name.split("\\s+");
                    Spell focus = parts.length > 1 ? SpellRegistries.SPELLS.get(ResourceLocation.parse(parts[1])) : null;
                    minecraft.setScreen(new SkillTreeScreen(null, focus));
                }
            }
        }
    }
}
