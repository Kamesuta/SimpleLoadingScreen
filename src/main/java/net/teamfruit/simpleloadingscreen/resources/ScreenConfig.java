package net.teamfruit.simpleloadingscreen.resources;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.logging.log4j.Level;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import cpw.mods.fml.common.FMLLog;
import net.teamfruit.simpleloadingscreen.api.IConfig;
import net.teamfruit.simpleloadingscreen.api.IConfigProperty;

public class ScreenConfig implements IConfig {
	private final File configFile;
	private final Properties properties;
	private final Table<String, Class<?>, ConfigProperty<?>> configPropertiesTable = HashBasedTable.create();
	private final Set<ConfigProperty<?>> configProperties = Sets.newHashSet();
	private boolean changeToSave;

	public ScreenConfig(final File configFile, final Properties properties) {
		this.configFile = configFile;
		this.properties = properties;
	}

	public ScreenConfig(final File configFile) {
		this(configFile, new Properties());
	}

	@Override
	public void load() {
		try (FileReader r = new FileReader(this.configFile)) {
			this.properties.load(r);
		} catch (final IOException e) {
			FMLLog.info("Could not load "+this.configFile.getName()+", will create a default one");
		}
	}

	@Override
	public void save() {
		try (FileWriter w = new FileWriter(this.configFile);) {
			this.properties.store(w, "Splash screen properties");
		} catch (final IOException e) {
			FMLLog.log(Level.ERROR, e, "Could not save the "+this.configFile.getName()+" file");
		}
	}

	@Override
	public void fillDefaults() {
		for (final ConfigProperty<?> property : this.configProperties)
			property.fillDefault();
	}

	private <T> ConfigProperty<T> register(final ConfigProperty<T> property) {
		this.configPropertiesTable.put(property.getName(), property.getType(), property);
		this.configProperties.add(property);
		return property;
	}

	private void onChanged() {
		if (this.changeToSave)
			save();
	}

	@Deprecated
	public void setEnableChangeToSave(final boolean enable) {
		this.changeToSave = enable;
	}

	@Override
	public File getLocation() {
		return this.configFile;
	}

	@Override
	public Properties getProperties() {
		return this.properties;
	}

	@Override
	public Set<IConfigProperty<?>> getConfigs() {
		return Collections.unmodifiableSet(this.configProperties);
	}

	@Override
	public IConfigProperty<String> stringProperty(final String name, final String def) {
		@SuppressWarnings("unchecked")
		ConfigProperty<String> property = (ConfigProperty<String>) this.configPropertiesTable.get(name, String.class);
		if (property==null)
			property = register(new ConfigProperty<String>(this, name, def, String.class) {
				@Override
				public String get() {
					return getProperty(this.def);
				}

				@Override
				public void set(final String value) {
					setProperty(value);
				}
			});
		return property;
	}

	@Override
	public IConfigProperty<Boolean> booleanProperty(final String name, final boolean def) {
		@SuppressWarnings("unchecked")
		ConfigProperty<Boolean> property = (ConfigProperty<Boolean>) this.configPropertiesTable.get(name, Boolean.class);
		if (property==null)
			property = register(new ConfigProperty<Boolean>(this, name, def, Boolean.class) {
				@Override
				public Boolean get() {
					return BooleanUtils.toBooleanDefaultIfNull(BooleanUtils.toBooleanObject(getProperty(Boolean.toString(this.def))), this.def);
				}

				@Override
				public void set(final Boolean value) {
					setProperty(Boolean.toString(value));
				}
			});
		return property;
	}

	@Override
	public IConfigProperty<Integer> intProperty(final String name, final int def) {
		@SuppressWarnings("unchecked")
		ConfigProperty<Integer> property = (ConfigProperty<Integer>) this.configPropertiesTable.get(name, Integer.class);
		if (property==null)
			property = register(new ConfigProperty<Integer>(this, name, def, Integer.class) {
				@Override
				public Integer get() {
					try {
						return Integer.decode(getProperty(Integer.toString(this.def)));
					} catch (final NumberFormatException nfe) {
						return this.def;
					}
				}

				@Override
				public void set(final Integer value) {
					setProperty(Integer.toString(value));
				}
			});
		return property;
	}

	@Override
	public IConfigProperty<Integer> hexProperty(final String name, final int def) {
		@SuppressWarnings("unchecked")
		ConfigProperty<Integer> property = (ConfigProperty<Integer>) this.configPropertiesTable.get(name, Integer.class);
		if (property==null)
			property = register(new ConfigProperty<Integer>(this, name, def, Integer.class) {
				@Override
				public Integer get() {
					try {
						return Integer.decode(getProperty("0x"+Integer.toString(this.def, 16).toUpperCase()));
					} catch (final NumberFormatException nfe) {
						return this.def;
					}
				}

				@Override
				public void set(final Integer value) {
					setProperty("0x"+Integer.toString(value, 16).toUpperCase());
				}
			});
		return property;
	}

	public static abstract class ConfigProperty<T> implements IConfigProperty<T> {
		protected final ScreenConfig config;
		protected final String name;
		protected final T def;
		protected final Class<T> type;

		public ConfigProperty(final ScreenConfig config, final String name, final T def, final Class<T> type) {
			this.config = config;
			this.name = name;
			this.def = def;
			this.type = type;
		}

		protected String getProperty(final String def) {
			return this.config.properties.getProperty(this.name, def);
		}

		protected void setProperty(final String value) {
			this.config.properties.setProperty(this.name, value);
			this.config.onChanged();
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public T getDefault() {
			return this.def;
		}

		@Override
		public Class<T> getType() {
			return this.type;
		}

		@Override
		public void fillDefault() {
			set(get());
		}

		@Override
		public abstract T get();

		@Override
		public abstract void set(T value);
	}
}
