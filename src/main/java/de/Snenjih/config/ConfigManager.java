package de.Snenjih.config;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final Path configFile;
    private final Logger logger;
    private Map<String, Object> data = new LinkedHashMap<>();

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.logger = logger;
        this.configFile = dataDirectory.resolve("config.yml");
        init();
    }

    private void init() {
        try {
            if (!Files.exists(configFile.getParent())) {
                Files.createDirectories(configFile.getParent());
            }
            if (!Files.exists(configFile)) {
                try (InputStream defaults = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                    if (defaults != null) {
                        Files.copy(defaults, configFile);
                    }
                }
            }
            reload();
        } catch (IOException e) {
            logger.error("Failed to initialize config", e);
        }
    }

    public void reload() {
        try (InputStream in = Files.newInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> loaded = yaml.load(in);
            if (loaded != null) {
                data = loaded;
            }
        } catch (IOException e) {
            logger.error("Failed to reload config", e);
        }
    }

    public void save() {
        try (OutputStream out = Files.newOutputStream(configFile)) {
            Yaml yaml = new Yaml();
            out.write(yaml.dump(data).getBytes());
        } catch (IOException e) {
            logger.error("Failed to save config", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object resolve(String key) {
        String[] parts = key.split("\\.", -1);
        Object current = data;
        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = ((Map<String, Object>) map).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    public String getString(String key, String defaultValue) {
        Object value = resolve(key);
        return value instanceof String s ? s : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object value = resolve(key);
        return value instanceof Number n ? n.intValue() : defaultValue;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = resolve(key);
        return value instanceof Boolean b ? b : defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        Object value = resolve(key);
        return value instanceof Number n ? n.doubleValue() : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key, List<String> defaultValue) {
        Object value = resolve(key);
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public void setNested(String key, Object value) {
        String[] parts = key.split("\\.", 2);
        if (parts.length == 1) {
            data.put(key, value);
        } else {
            Object existing = data.get(parts[0]);
            Map<String, Object> section;
            if (existing instanceof Map<?, ?> m) {
                section = (Map<String, Object>) m;
            } else {
                section = new LinkedHashMap<>();
                data.put(parts[0], section);
            }
            setNestedInMap(section, parts[1], value);
        }
        save();
    }

    @SuppressWarnings("unchecked")
    private void setNestedInMap(Map<String, Object> map, String key, Object value) {
        String[] parts = key.split("\\.", 2);
        if (parts.length == 1) {
            map.put(key, value);
        } else {
            Object existing = map.get(parts[0]);
            Map<String, Object> section;
            if (existing instanceof Map<?, ?> m) {
                section = (Map<String, Object>) m;
            } else {
                section = new LinkedHashMap<>();
                map.put(parts[0], section);
            }
            setNestedInMap(section, parts[1], value);
        }
    }

    public void set(String key, Object value) {
        data.put(key, value);
        save();
    }
}
