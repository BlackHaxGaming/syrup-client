package us.syrup.module;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import us.syrup.Syrup;
import us.syrup.utils.Config;
import us.syrup.utils.Files;
import us.syrup.utils.Strings;

public class ModuleSettings {

	public enum SettingType {
		BOOLEAN, INT, DOUBLE, FLOAT, STRING, STRING_LIST, CHAR
	}

	public static class SettingMeta {
		public final String key;
		public final SettingType type;
		public final Object defaultValue;

		// optional numeric range (null = not set)
		public final Double min;
		public final Double max;
		public final Double step;

		public SettingMeta(String key, SettingType type, Object defaultValue) {
			this(key, type, defaultValue, null, null, null);
		}

		public SettingMeta(String key, SettingType type, Object defaultValue, Double min, Double max, Double step) {
			this.key = key;
			this.type = type;
			this.defaultValue = defaultValue;
			this.min = min;
			this.max = max;
			this.step = step;
		}

		public boolean hasRange() {
			return min != null && max != null;
		}
	}


	private Module module;
	private Config config;

	// preserves insertion order (same order modules register defaults)
	private final Map<String, SettingMeta> tracked = new LinkedHashMap<String, SettingMeta>();

	private boolean wasGenerated;

	public ModuleSettings(Module module) {
		this.module = module;

		String path = new File(".").getAbsolutePath();
		path = (path.contains("jars") ? new File(".").getAbsolutePath().substring(0, path.length() - 2) : new File(".").getAbsolutePath());

		String clientName = Syrup.instance.getClientName();

		Files.createRecursiveFolder(path, clientName + Strings.getSplitter() + "modules" + Strings.getSplitter() + module.getCategory().name().toLowerCase());

		this.config = new Config(path, clientName + Strings.getSplitter() + "modules" + Strings.getSplitter()
				+ module.getCategory().toString().toLowerCase() + Strings.getSplitter() + module.getName() + ".cfg");

		if (config.exists() && config.getObject("toggled") != null)
			this.wasGenerated = true;

		config.addDefault("toggled", false);
		config.addDefault("key", module.getKey());
		config.addDefault("anticheat", module.getAntiCheat().name().toLowerCase());

		// track internals too (so they can be filtered consistently)
		tracked.put("toggled", new SettingMeta("toggled", SettingType.BOOLEAN, false));
		tracked.put("key", new SettingMeta("key", SettingType.INT, module.getKey()));
		tracked.put("anticheat", new SettingMeta("anticheat", SettingType.STRING, module.getAntiCheat().name().toLowerCase()));
	}

	public Module getModule() {
		return module;
	}

	public Config getConfig() {
		return config;
	}

	public boolean wasGenerated() {
		return wasGenerated;
	}

	public void generateConfigs() {
		config.generateConfigs();
	}

	public void addDefault(String key, String value) {
		String k = key.toLowerCase();
		tracked.put(k, new SettingMeta(k, SettingType.STRING, value));
		config.addDefault(k, value);
	}

	public void addDefault(String key, boolean value) {
		String k = key.toLowerCase();
		tracked.put(k, new SettingMeta(k, SettingType.BOOLEAN, Boolean.valueOf(value)));
		config.addDefault(k, value);
	}

	public void addDefault(String key, int value, int min, int max, int step) {
		String k = key.toLowerCase();
		tracked.put(k, new SettingMeta(k, SettingType.INT, Integer.valueOf(value), (double)min, (double)max, (double)step));
		config.addDefault(k, value);
	}

	public void addDefault(String key, char value) {
		String k = key.toLowerCase();
		tracked.put(k, new SettingMeta(k, SettingType.CHAR, Character.valueOf(value)));
		config.addDefault(k, value);
	}

	public void addDefault(String key, double value, double min, double max, double step) {
		String k = key.toLowerCase();
		tracked.put(k, new SettingMeta(k, SettingType.DOUBLE, Double.valueOf(value), min, max, step));
		config.addDefault(k, value);
	}

	public void addDefault(String key, float value, float min, float max, float step) {
		String k = key.toLowerCase();
		tracked.put(k, new SettingMeta(k, SettingType.FLOAT, Float.valueOf(value), (double)min, (double)max, (double)step));
		config.addDefault(k, value);
	}

	public void addDefault(String key, List<String> value) {
		String k = key.toLowerCase();
		tracked.put(k, new SettingMeta(k, SettingType.STRING_LIST, value));
		config.addDefault(k, value);
	}

	/** Returns settings declared via addDefault(...), excluding toggled/key/anticheat. */
	public List<String> getUserSettingKeys() {
		List<String> keys = new ArrayList<String>();
		for (String k : tracked.keySet()) {
			if (k == null) continue;
			if (k.equals("toggled") || k.equals("key") || k.equals("anticheat")) continue;
			keys.add(k);
		}
		return keys;
	}

	public SettingMeta getMeta(String key) {
		if (key == null) return null;
		return tracked.get(key.toLowerCase());
	}

	public void set(String key, Object value) { config.set(key.toLowerCase(), value); }
	public void set(String key, String value) { config.set(key.toLowerCase(), value); }
	public void set(String key, boolean value) { config.set(key.toLowerCase(), value); }
	public void set(String key, int value) { config.set(key.toLowerCase(), value); }
	public void set(String key, char value) { config.set(key.toLowerCase(), value); }
	public void set(String key, double value) { config.set(key.toLowerCase(), value); }
	public void set(String key, float value) { config.set(key.toLowerCase(), value); }
	public void set(String key, List<String> value) { config.set(key, value); }

	public String getString(String key) { return config.getString(key.toLowerCase()); }
	public boolean getBoolean(String key) { return config.getBoolean(key.toLowerCase()); }
	public double getDouble(String key) { return config.getDouble(key.toLowerCase()); }
	public float getFloat(String key) { return config.getFloat(key.toLowerCase()); }
	public Object getObject(String key) { return config.getObject(key.toLowerCase()); }
	public int getInt(String key) { return config.getInt(key.toLowerCase()); }

	public List<String> getStringList(String key) {
		return getStringList(key, null);
	}

	public List<String> getStringList(String key, Function<String, String> actions) {
		return config.getStringList(key, actions);
	}
}
