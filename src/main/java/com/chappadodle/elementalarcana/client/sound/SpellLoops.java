package com.chappadodle.elementalarcana.client.sound;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The loops following each spell projectile (ProjectileLoopSound): which were started, and for
 * which phase (held or flying) and look, so they're restarted only when one of those changes.
 */
public final class SpellLoops {
    /** Loops playing for a projectile, and what they were started for. */
    private record Playing(boolean held, int look, List<ProjectileLoopSound> loops) {
    }

    private static final Map<SpellProjectile, Playing> PLAYING = new WeakHashMap<>();

    private SpellLoops() {
    }

    /** The look its loops were last started for, or -1 if it has had none yet. */
    public static int lastLook(SpellProjectile projectile) {
        Playing playing = PLAYING.get(projectile);
        return playing == null ? -1 : playing.look();
    }

    /** Starts {@code layers} as its loops if its phase or look changed; true if it did. */
    public static boolean update(SpellProjectile projectile, List<SoundLayer> layers) {
        Playing playing = PLAYING.get(projectile);
        if (playing != null && playing.held() == projectile.isHeld() && playing.look() == projectile.variant()) {
            return false;
        }
        if (playing != null) {
            playing.loops().forEach(ProjectileLoopSound::end);
        }
        List<ProjectileLoopSound> loops = new ArrayList<>();
        for (SoundLayer layer : layers) {
            ProjectileLoopSound loop = new ProjectileLoopSound(projectile, layer.sound(), layer.volume(), layer.pitch());
            Minecraft.getInstance().getSoundManager().play(loop);
            loops.add(loop);
        }
        PLAYING.put(projectile, new Playing(projectile.isHeld(), projectile.variant(), loops));
        return true;
    }
}
