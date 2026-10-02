package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.network.CastSpellPayload;
import com.chappadodle.elementalarcana.network.SelectSpellPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

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
 * launch_one / launch_all  the conjuring launch keys
 * screen status|tree|stats|close   open one of the mod's screens
 * hud on|off               show or hide the HUD
 * camera first|back|front  the camera view
 * quit                     close the game
 * </pre>
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class AutoTest {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = Boolean.getBoolean("elementalarcana.autotest");

    private static List<String> steps;
    private static int index;
    private static int waitTicks;
    private static boolean finished;

    private AutoTest() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED || finished) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
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
            case "press" -> PacketDistributor.sendToServer(CastSpellPayload.PRESS);
            case "release" -> PacketDistributor.sendToServer(CastSpellPayload.RELEASE);
            case "launch_one" -> PacketDistributor.sendToServer(CastSpellPayload.LAUNCH_ONE);
            case "launch_all" -> PacketDistributor.sendToServer(CastSpellPayload.LAUNCH_ALL);
            case "screen" -> openScreen(minecraft, argument);
            case "hud" -> minecraft.options.hideGui = argument.equals("off");
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

    private static void openScreen(Minecraft minecraft, String name) {
        switch (name) {
            case "status" -> minecraft.setScreen(new StatusScreen());
            case "stats" -> minecraft.setScreen(new StatsScreen(null));
            case "close" -> minecraft.setScreen(null);
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
