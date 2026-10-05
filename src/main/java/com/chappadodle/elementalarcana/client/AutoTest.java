package com.chappadodle.elementalarcana.client;

import net.minecraft.core.Direction;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.crypt.CryptRoomPiece;
import com.chappadodle.elementalarcana.content.crypt.CryptEntrancePiece;
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
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
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
import net.neoforged.fml.util.ObfuscationReflectionHelper;
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
 * (or, with {@code -Delementalarcana.autotest.multiplayer=true}, once it has joined a server: see
 * tools/mp_test.sh) it plays {@code <game dir>/autotest.txt}, one step per line:
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
 * click 37 0 QUICK_MOVE    click slot 37 of the open menu, as the player would: button 0 or 1 and
 *                          a click type (PICKUP, QUICK_MOVE, SWAP, THROW...); default 0 PICKUP
 * hover 36                 put the mouse over slot 36 of the open screen (for its tooltip)
 * button screen.elementalarcana.mentor.claim   press the open screen's button with that label
 *                          (its translation key)
 * camera first|back|front  the camera view
 * use                      right-click the creature or block under the crosshair (or, with
 *                          neither, use the held item)
 * hold_use 45              hold right click down for 45 ticks (items used over time)
 * key jump down / key jump up   hold a movement key down (jump, sneak or forward), or let it go
 * goto elementalarcana:fire_shrine 18 14 [2]   stand 18 blocks south of the nearest such structure
 *                          and 14 above its base, looking at its middle, 2 blocks above the base
 *                          (default 0); it's searched for from where the player is, and the
 *                          script waits for the search
 * find elementalarcana:arcane_lectern 80   look for that block within 80 blocks (and 60 below to
 *                          20 above); log where, and view the first from 8 blocks south, 5 up
 * crypt gate:1 3 0 0 10    stand in a room of the nearest crypt (searched for like goto the first
 *                          time, then the same crypt for the rest of the run): the second
 *                          (index 1, default 0) rune gate; 3 blocks back from its middle, 0 up,
 *                          turned 0 degrees from its way out, pitch 10. Rooms: entry, tombs,
 *                          glyphs, gate, library, store, chamber; "mausoleum" stands outside the
 *                          mausoleum's door, facing it
 * crypt_kit 4              wake the nearest crypt's element in the player (alone, level 20), teach
 *                          and select its first spell, and put 4 of its Essence in the held slot
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
    /** Joined to a dedicated server (tools/mp_test.sh) rather than playing the test world. */
    private static final boolean MULTIPLAYER = Boolean.getBoolean("elementalarcana.autotest.multiplayer");
    /** Steps that work on the test's own (integrated) server, so can't run when joined to another. */
    private static final java.util.Set<String> LOCAL_ONLY = java.util.Set.of("goto", "find", "crypt", "crypt_kit");

    private static List<String> steps;
    private static int index;
    private static int waitTicks;
    private static int respawnCooldown;
    private static int holdUseTicks;
    private static boolean finished;
    /** A step's work running on the server (a structure search can take seconds), which the script waits for. */
    private static volatile boolean serverBusy;
    /** The chunk of the crypt the crypt steps use, once one is found: the search's nearest changes as the player moves. */
    private static ChunkPos cryptChunk;

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
        if (minecraft.player == null || minecraft.level == null || (!MULTIPLAYER && minecraft.getSingleplayerServer() == null)) {
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
        if (serverBusy) {
            return;
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
        if (MULTIPLAYER && LOCAL_ONLY.contains(parts[0].toLowerCase(Locale.ROOT))) {
            LOGGER.warn("[autotest] {} needs the test's own server: skipped when joined to another", parts[0]);
            return true;
        }
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
            case "click" -> click(minecraft, argument);
            case "hover" -> hover(minecraft, Integer.parseInt(argument.trim()));
            case "button" -> pressButton(minecraft, argument.trim());
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
                    case "sprint" -> minecraft.options.keySprint;
                    default -> throw new IllegalArgumentException("unknown key: " + words[0]);
                };
                key.setDown(words.length > 1 && words[1].equals("down"));
            }
            case "goto" -> gotoStructure(minecraft, argument);
            case "find" -> findBlock(minecraft, argument);
            case "crypt" -> cryptRoom(minecraft, argument);
            case "crypt_kit" -> cryptKit(minecraft, argument);
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

    /** Clicks a slot of the open menu: "37 0 QUICK_MOVE" (the slot, a button, a click type). */
    private static void click(Minecraft minecraft, String argument) {
        String[] parts = argument.trim().split("\\s+");
        int slot = Integer.parseInt(parts[0]);
        int button = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        ClickType type = parts.length > 2 ? ClickType.valueOf(parts[2].toUpperCase(Locale.ROOT)) : ClickType.PICKUP;
        AbstractContainerMenu menu = minecraft.player.containerMenu;
        minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, type, minecraft.player);
        LOGGER.info("[autotest] click {} {} {}: now carrying {}", slot, button, type, menu.getCarried());
    }

    /** Puts the mouse over slot {@code index} of the open container screen (so its tooltip shows). */
    private static void hover(Minecraft minecraft, int index) {
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> screen)) {
            LOGGER.warn("[autotest] hover: no container screen open");
            return;
        }
        Slot slot = screen.getMenu().getSlot(index);
        double scale = minecraft.getWindow().getGuiScale();
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, minecraft.mouseHandler, (screen.getGuiLeft() + slot.x + 8) * scale, "xpos");
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, minecraft.mouseHandler, (screen.getGuiTop() + slot.y + 8) * scale, "ypos");
    }

    /** Presses the open screen's button labelled by translation key {@code key}. */
    private static void pressButton(Minecraft minecraft, String key) {
        if (minecraft.screen != null) {
            for (GuiEventListener child : minecraft.screen.children()) {
                if (child instanceof Button button && button.active && button.getMessage().getContents() instanceof TranslatableContents text
                        && text.getKey().equals(key)) {
                    button.onPress();
                    LOGGER.info("[autotest] pressed {}", key);
                    return;
                }
            }
        }
        LOGGER.warn("[autotest] button: no {} to press", key);
    }

    /** Runs {@code command} on the integrated server as the player, with full permissions. */
    private static void command(Minecraft minecraft, String command) {
        if (MULTIPLAYER) {
            // Joined to a dedicated server: sent as the player would type it (tools/mp_test.sh makes them an operator).
            minecraft.player.connection.sendCommand(command);
            return;
        }
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
        serverBusy = true;
        server.execute(() -> {
            try {
                gotoStructure(server.getPlayerList().getPlayer(id), key, distance, height, aim);
            } finally {
                serverBusy = false;
            }
        });
    }

    private static void gotoStructure(ServerPlayer player, ResourceKey<Structure> key, double distance, double height, double aim) {
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
    }

    /** Runs {@code task} on the server as the player; the script waits for it. */
    private static void onServer(Minecraft minecraft, java.util.function.Consumer<ServerPlayer> task) {
        MinecraftServer server = minecraft.getSingleplayerServer();
        java.util.UUID id = minecraft.player.getUUID();
        serverBusy = true;
        server.execute(() -> {
            try {
                ServerPlayer player = server.getPlayerList().getPlayer(id);
                if (player != null) {
                    task.accept(player);
                }
            } finally {
                serverBusy = false;
            }
        });
    }

    /**
     * The start of the crypt the crypt steps use: the first one found near the player (generating it
     * if need be), kept for the rest of the run. Null if there's none.
     */
    private static StructureStart nearestCrypt(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Optional<Holder.Reference<Structure>> crypt = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                .getHolder(ResourceKey.create(Registries.STRUCTURE, ElementalArcana.id("crypt")));
        if (crypt.isEmpty()) {
            return null;
        }
        if (cryptChunk == null) {
            Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, HolderSet.direct(crypt.get()), player.blockPosition(), 100, false);
            if (found == null) {
                LOGGER.warn("[autotest] crypt: none nearby");
                return null;
            }
            cryptChunk = new ChunkPos(found.getFirst());
        }
        ChunkPos chunkPos = cryptChunk;
        ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
        StructureStart start = level.structureManager().getStartForStructure(SectionPos.bottomOf(chunk), crypt.get().value(), chunk);
        return start != null && start.isValid() ? start : null;
    }

    private static void cryptRoom(Minecraft minecraft, String argument) {
        String[] parts = argument.split("\\s+");
        String[] room = parts[0].split(":");
        String kind = room[0].toUpperCase(Locale.ROOT);
        int index = room.length > 1 ? Integer.parseInt(room[1]) : 0;
        double back = parts.length > 1 ? Double.parseDouble(parts[1]) : 0;
        double up = parts.length > 2 ? Double.parseDouble(parts[2]) : 0;
        float turn = parts.length > 3 ? Float.parseFloat(parts[3]) : 0;
        float pitch = parts.length > 4 ? Float.parseFloat(parts[4]) : 10;
        onServer(minecraft, player -> {
            StructureStart start = nearestCrypt(player);
            if (start == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            int seen = 0;
            for (StructurePiece piece : start.getPieces()) {
                if (kind.equals("MAUSOLEUM") && piece instanceof CryptEntrancePiece entrance) {
                    Direction heading = entrance.heading();
                    BlockPos middle = entrance.mausoleum();
                    player.teleportTo(level, middle.getX() + 0.5 - heading.getStepX() * back, middle.getY() + 1 + up,
                            middle.getZ() + 0.5 - heading.getStepZ() * back, heading.toYRot() + turn, pitch);
                    LOGGER.info("[autotest] crypt mausoleum: {} heading {}, a crypt of {}", middle, heading, entrance.element());
                    return;
                }
                if (piece instanceof CryptRoomPiece crypt && crypt.kind().name().equals(kind) && seen++ == index) {
                    Direction out = crypt.wayOut();
                    BlockPos middle = crypt.middle();
                    player.teleportTo(level, middle.getX() + 0.5 - out.getStepX() * back, middle.getY() + up,
                            middle.getZ() + 0.5 - out.getStepZ() * back, out.toYRot() + turn, pitch);
                    LOGGER.info("[autotest] crypt {}: {} facing {}", parts[0], middle, out);
                    return;
                }
            }
            LOGGER.warn("[autotest] crypt: no {} room", parts[0]);
        });
    }

    private static void cryptKit(Minecraft minecraft, String argument) {
        int count = argument.isBlank() ? 4 : Integer.parseInt(argument.trim());
        onServer(minecraft, player -> {
            StructureStart start = nearestCrypt(player);
            if (start == null) {
                return;
            }
            Element element = start.getPieces().stream().filter(piece -> piece instanceof CryptEntrancePiece)
                    .map(piece -> ((CryptEntrancePiece) piece).element()).findFirst().orElse(Element.FIRE);
            String spell = switch (element) {
                case FIRE -> "fireball";
                case WATER -> "hydro_jet";
                case ICE -> "icicle";
                case WIND -> "wind_blade";
                case EARTH -> "boulder";
                case CRYSTAL -> "prism_bolt";
                case LIGHTNING -> "chain_lightning";
                case RADIANCE -> "smite";
            };
            String name = element.name().toLowerCase(Locale.ROOT);
            MinecraftServer server = player.getServer();
            for (String command : List.of("arcana affinity reset", "arcana awaken " + name, "arcana level set 20",
                    "arcana spell elementalarcana:" + spell + " 1", "arcana mana fill", "arcana cooldowns reset")) {
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(4), command);
            }
            if (MagicAttachments.get(player).select(ElementalArcana.id(spell))) {
                MagicAttachments.sync(player);
            }
            player.getInventory().setItem(player.getInventory().selected, new ItemStack(ModItems.essence(element), count));
            LOGGER.info("[autotest] crypt_kit: a crypt of {}; {} selected, {} Essence in hand", element, spell, count);
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
            case "close" -> {
                // A menu the server opened (a pouch, a chest) is closed the way the server expects: by telling it.
                if (minecraft.screen instanceof AbstractContainerScreen<?>) {
                    minecraft.player.closeContainer();
                } else {
                    minecraft.setScreen(null);
                }
            }
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
