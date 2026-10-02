package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SkillTree;
import com.chappadodle.elementalarcana.api.SkillTrees;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.network.SkillTreeSyncPayload;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Loads the skill tree from data/&lt;namespace&gt;/skill_tree/*.json (all files merge into one tree,
 * so addons can add regions) on server start and /reload, and sends it to players. Entries:
 * <ul>
 * <li>start, small, spell, notable and keystone nodes: {@code id, type, x, y, links, element?,
 * stat?, amount?, spell?, stats?: {key: amount} (a notable's), keystone? (a keystone's rule, see
 * Keystones), requires?: {stat, min}}.</li>
 * <li>{@code path}: a spell's whole upgrade chain, built here from the spell's own max level and
 * branch options so it always matches the spell: {@code id, spell, x, y, dx, dy, from?, links?,
 * element?}. With {@code from}, the chain hangs off that node (a start); without it, a spell node
 * {@code id} is made at (x, y).</li>
 * </ul>
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class SkillTreeLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    // How far apart a fork's options sit, across the chain.
    private static final double FORK_SPREAD = 26;

    private SkillTreeLoader() {
        super(GSON, "skill_tree");
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SkillTreeLoader());
    }

    /** Players joining get the tree; after /reload everyone does. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        SkillTreeSyncPayload payload = SkillTreeSyncPayload.of(SkillTrees.current());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<SkillTree.Node> nodes = new ArrayList<>();
        List<List<String>> links = new ArrayList<>();
        // Sorted, so the tree is the same on every load.
        for (Map.Entry<ResourceLocation, JsonElement> file : new TreeMap<>(files).entrySet()) {
            try {
                for (JsonElement entry : GsonHelper.getAsJsonArray(file.getValue().getAsJsonObject(), "nodes")) {
                    read(entry.getAsJsonObject(), nodes, links);
                }
            } catch (RuntimeException e) {
                LOGGER.error("Skipping skill tree file {}: {}", file.getKey(), e.getMessage());
            }
        }
        SkillTree tree = new SkillTree(nodes, links);
        int dropped = links.size() - tree.linkPairs().size();
        if (dropped > 0) {
            LOGGER.warn("Skill tree: {} links point at missing nodes (or repeat) and were dropped", dropped);
        }
        SkillTrees.set(tree);
        LOGGER.info("Loaded the skill tree: {} nodes, {} links", tree.nodes().size(), tree.linkPairs().size());
    }

    private static void read(JsonObject json, List<SkillTree.Node> nodes, List<List<String>> links) {
        String id = GsonHelper.getAsString(json, "id");
        String type = GsonHelper.getAsString(json, "type");
        String element = GsonHelper.getAsString(json, "element", null);
        int x = GsonHelper.getAsInt(json, "x");
        int y = GsonHelper.getAsInt(json, "y");
        if (json.has("links")) {
            for (JsonElement link : GsonHelper.getAsJsonArray(json, "links")) {
                links.add(List.of(id, link.getAsString()));
            }
        }
        if (type.equals("path")) {
            expandPath(json, id, element, x, y, nodes, links);
            return;
        }
        String requiresStat = null;
        int requiresMin = 0;
        if (json.has("requires")) {
            JsonObject requires = GsonHelper.getAsJsonObject(json, "requires");
            requiresStat = GsonHelper.getAsString(requires, "stat");
            requiresMin = GsonHelper.getAsInt(requires, "min");
        }
        SkillTree.Type nodeType = switch (type) {
            case "start" -> SkillTree.Type.START;
            case "small" -> SkillTree.Type.SMALL;
            case "spell" -> SkillTree.Type.SPELL;
            case "notable" -> SkillTree.Type.NOTABLE;
            case "keystone" -> SkillTree.Type.KEYSTONE;
            default -> throw new IllegalArgumentException("unknown node type '" + type + "' for " + id);
        };
        String spell = GsonHelper.getAsString(json, "spell", null);
        if (spell != null && spellOf(spell) == null) {
            throw new IllegalArgumentException("unknown spell " + spell + " for " + id);
        }
        Map<String, Integer> stats = new TreeMap<>();
        if (json.has("stats")) {
            GsonHelper.getAsJsonObject(json, "stats").entrySet().forEach(stat -> stats.put(stat.getKey(), stat.getValue().getAsInt()));
        }
        String keystone = GsonHelper.getAsString(json, "keystone", null);
        if (nodeType == SkillTree.Type.KEYSTONE && keystone == null) {
            throw new IllegalArgumentException("keystone " + id + " names no keystone");
        }
        nodes.add(new SkillTree.Node(id, nodeType, x, y, element, GsonHelper.getAsString(json, "stat", null),
                GsonHelper.getAsInt(json, "amount", 0), spell, 0, null, requiresStat, requiresMin, Map.copyOf(stats), keystone));
    }

    /** A spell's chain: Lv 2..max, one upgrade node per level, or one fork per branch where it branches. */
    private static void expandPath(JsonObject json, String id, @Nullable String element, int x, int y,
                                   List<SkillTree.Node> nodes, List<List<String>> links) {
        String spellId = GsonHelper.getAsString(json, "spell");
        Spell spell = spellOf(spellId);
        if (spell == null) {
            throw new IllegalArgumentException("unknown spell " + spellId + " for path " + id);
        }
        double dx = GsonHelper.getAsDouble(json, "dx");
        double dy = GsonHelper.getAsDouble(json, "dy");
        double length = Math.max(1e-6, Math.hypot(dx, dy));
        double px = -dy / length;
        double py = dx / length;
        List<String> previous = new ArrayList<>();
        if (json.has("from")) {
            previous.add(GsonHelper.getAsString(json, "from"));
        } else {
            nodes.add(new SkillTree.Node(id, SkillTree.Type.SPELL, x, y, element, null, 0, spellId, 0, null, null, 0));
            previous.add(id);
        }
        for (int level = 2; level <= spell.maxLevel(); level++) {
            double cx = x + dx * (level - 1);
            double cy = y + dy * (level - 1);
            List<String> options = spell.branchOptions(level);
            List<String> current = new ArrayList<>();
            if (options.isEmpty()) {
                String nodeId = id + "/" + level;
                nodes.add(new SkillTree.Node(nodeId, SkillTree.Type.UPGRADE, (int) Math.round(cx), (int) Math.round(cy), element,
                        null, 0, spellId, level, null, null, 0));
                current.add(nodeId);
            } else {
                for (int i = 0; i < options.size(); i++) {
                    double offset = FORK_SPREAD * (i - (options.size() - 1) / 2.0);
                    String nodeId = id + "/" + level + "/" + options.get(i);
                    nodes.add(new SkillTree.Node(nodeId, SkillTree.Type.FORK, (int) Math.round(cx + px * offset),
                            (int) Math.round(cy + py * offset), element, null, 0, spellId, level, options.get(i), null, 0));
                    current.add(nodeId);
                }
            }
            for (String node : current) {
                for (String before : previous) {
                    links.add(List.of(node, before));
                }
            }
            previous = current;
        }
    }

    @Nullable
    private static Spell spellOf(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? null : SpellRegistries.SPELLS.get(location);
    }

    /** Node counts by type, for /arcana tree info. */
    public static String describe(SkillTree tree) {
        Map<SkillTree.Type, Integer> counts = new TreeMap<>();
        tree.nodes().forEach(node -> counts.merge(node.type(), 1, Integer::sum));
        return tree.nodes().size() + " nodes " + counts + ", " + tree.linkPairs().size() + " links";
    }
}
