package com.map;

import com.map.util.GeometryUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.jpenilla.squaremap.api.*;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.api.marker.Polygon;
import org.bstats.bukkit.Metrics;

import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class Map extends JavaPlugin {
    private final Key layerKey = Key.of("folia_regions");
    private int shiftValue = 4;
    private ScheduledTask task;
    private final java.util.Map<UUID, SimpleLayerProvider> providers = new HashMap<>();

    @Override
    public void onEnable() {
        int pluginId = 33067;
        Metrics metrics = new Metrics(this, pluginId);
        
        try {
            Class<?> trClass = Class.forName("io.papermc.paper.threadedregions.TickRegions");
            Method getShift = trClass.getMethod("getRegionChunkShift");
            shiftValue = (int) getShift.invoke(null);
        } catch (Throwable ignored) {}

        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, t -> {
            try {
                for (MapWorld mapWorld : SquaremapProvider.get().mapWorlds()) {
                    World bukkitWorld = findBukkitWorld(mapWorld.identifier());
                    if (bukkitWorld != null) {
                        updateMarkers(bukkitWorld, mapWorld);
                    }
                }
            } catch (Throwable ignored) {}
        }, 200L, 200L);
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        for (MapWorld sw : SquaremapProvider.get().mapWorlds()) {
            if (sw.layerRegistry().hasEntry(layerKey)) {
                sw.layerRegistry().unregister(layerKey);
            }
        }
        providers.clear();
    }

    private World findBukkitWorld(WorldIdentifier identifier) {
        String idStr = identifier.asString();
        for (World world : Bukkit.getWorlds()) {
            if (world.getKey().toString().equalsIgnoreCase(idStr)) return world;
            if (world.getName().equalsIgnoreCase(identifier.value())) return world;
        }
        return null;
    }

    private void updateMarkers(World bukkitWorld, MapWorld sw) {
        try {
            SimpleLayerProvider provider = providers.get(bukkitWorld.getUID());
            if (provider == null) {
                if (sw.layerRegistry().hasEntry(layerKey)) {
                    sw.layerRegistry().unregister(layerKey);
                }
                provider = SimpleLayerProvider.builder("Regions")
                        .showControls(true)
                        .defaultHidden(true)
                        .build();
                sw.layerRegistry().register(layerKey, provider);
                providers.put(bukkitWorld.getUID(), provider);
            }

            Method getHandle = bukkitWorld.getClass().getMethod("getHandle");
            Object worldServer = getHandle.invoke(bukkitWorld);
            Field regioniserField = findField(worldServer.getClass(), "regioniser");
            if (regioniserField == null) return;
            regioniserField.setAccessible(true);
            Object engine = regioniserField.get(worldServer);

            java.util.Map<Object, List<Long>> activeRegions = new HashMap<>();
            Method compute = engine.getClass().getMethod("computeForAllRegions", java.util.function.Consumer.class);
            compute.invoke(engine, (java.util.function.Consumer<Object>) r -> {
                try {
                    Method getOwned = r.getClass().getMethod("getOwnedSections");
                    activeRegions.put(r, (List<Long>) getOwned.invoke(r));
                } catch (Throwable ignored) {}
            });

            provider.clearMarkers();
            int multiplier = (1 << shiftValue) * 16;

            for (java.util.Map.Entry<Object, List<Long>> entry : activeRegions.entrySet()) {
                try {
                    Object region = entry.getKey();
                    List<Long> sectionKeys = entry.getValue();
                    if (sectionKeys.isEmpty()) continue;

                    Method getCenter = region.getClass().getMethod("getCenterChunk");
                    Object mid = getCenter.invoke(region);
                    if (mid == null) continue;

                    int cx = mid.getClass().getField("x").getInt(mid);
                    int cz = mid.getClass().getField("z").getInt(mid);

                    List<Point> pts = new ArrayList<>();
                    for (long k : sectionKeys) {
                        int sx = (int) k;
                        int sz = (int) (k >>> 32);
                        pts.add(Point.of(sx, sz));
                        pts.add(Point.of(sx + 1, sz));
                        pts.add(Point.of(sx, sz + 1));
                        pts.add(Point.of(sx + 1, sz + 1));
                    }

                    List<Point> hull = GeometryUtil.computeConvexHull(pts);
                    List<Point> scaled = new ArrayList<>();
                    for (Point p : hull) {
                        scaled.add(Point.of(p.x() * multiplier, p.z() * multiplier));
                    }

                    Polygon poly = Marker.polygon(scaled);
                    poly.markerOptions(MarkerOptions.builder()
                            .strokeColor(new Color(255, 0, 0))
                            .strokeWeight(2)
                            .fillColor(new Color(255, 0, 0, 70))
                            .clickTooltip(getHtml(region, sectionKeys.size()))
                            .build());

                    String markerId = "reg_" + cx + "_" + cz;
                    provider.addMarker(Key.of(markerId), poly);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private String getHtml(Object region, int sections) {
        try {
            Object d = region.getClass().getMethod("getData").invoke(region);
            Object s = d.getClass().getMethod("getRegionStats").invoke(d);
            StringBuilder h = new StringBuilder("<b>Informations</b><br>");
            h.append("Sections: ").append(sections).append("<br>");
            h.append("Chunks: ").append(s.getClass().getMethod("getChunkCount").invoke(s)).append("<br>");
            h.append("Entities: ").append(s.getClass().getMethod("getEntityCount").invoke(s)).append("<br>");
            h.append("Players: ").append(s.getClass().getMethod("getPlayerCount").invoke(s)).append("<br>");
            
            Object hnd = d.getClass().getMethod("getRegionSchedulingHandle").invoke(d);
            Object r = hnd.getClass().getMethod("getTickReport5s", long.class).invoke(hnd, System.nanoTime());
            if (r != null) {
                Object tpsD = r.getClass().getMethod("tpsData").invoke(r);
                Object tpsS = tpsD.getClass().getMethod("segmentAll").invoke(tpsD);
                double tps = (double) tpsS.getClass().getMethod("average").invoke(tpsS);
                h.append("TPS: ").append(String.format(Locale.US, "%.2f", tps)).append("<br>");

                Object timeD = r.getClass().getMethod("timePerTickData").invoke(r);
                Object timeS = timeD.getClass().getMethod("segmentAll").invoke(timeD);
                double mspt = (double) timeS.getClass().getMethod("average").invoke(timeS) / 1_000_000.0;
                h.append("MSPT: ").append(String.format(Locale.US, "%.2f", mspt));
            }
            return h.toString();
        } catch (Throwable e) { return "Region Info"; }
    }
}