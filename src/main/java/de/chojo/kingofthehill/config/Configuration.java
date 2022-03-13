/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) 2022 RainbowDashLabs and Contributor
 */

package de.chojo.kingofthehill.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import de.chojo.kingofthehill.util.FileUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import static de.chojo.kingofthehill.util.FileUtil.home;

@Data
@Slf4j
public class Configuration {
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    private String token = "";
    private long voiceChannel = 0;
    private long gameChannel = 0;
    private long role = 0;
    private TreeMap<Long, UserStat> stats = new TreeMap<>();

    public static Configuration load() throws IOException {
        File config = FileUtil.createDirectory("config");
        try (var in = ClassLoader.getSystemClassLoader().getResourceAsStream("config.json")) {
            var file = FileUtil.createFile(in, "/config/config.json");
            try (JsonReader reader = new JsonReader(new FileReader(file))) {
                Configuration configuration = GSON.fromJson(reader, Configuration.class);
                configuration.forceConsistency();
                return configuration;
            }
        }
    }

    public void forceConsistency() {
        double total = getStats().values().stream().mapToDouble(UserStat::getPoints).sum();
        for (UserStat value : getStats().values()) {
            value.forcePercent(total);
        }
        save();
    }

    public void save() {
        try (var a = new FileWriter(Paths.get(home(), "/config/config.json").toFile())) {
            GSON.toJson(this, a);
        } catch (IOException e) {
            log.warn("Could not save config", e);
        }
    }

    public void upcount(long id) {
        double value = 0;
        for (UserStat stat : stats.values()) {
            if (stat.getId() == id) continue;
            value += stat.draw();
        }
        stats.computeIfAbsent(id, UserStat::new).upcount(value);
        save();
    }

    public long getKing() {
        @Nullable UserStat id = getKingData();
        return id == null ? 0 : id.getId();
    }

    public @Nullable UserStat getKingData() {
        if (stats.isEmpty()) return null;
        List<UserStat> values = new ArrayList<>(stats.values());
        Collections.sort(values);
        return values.get(values.size() - 1);
    }
    @Deprecated
    public void reduce() {
        stats.values().forEach(UserStat::reduce);
    }
}
