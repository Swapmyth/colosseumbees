package com.github.swapmyth.colosseumbees;

import com.github.swapmyth.colosseumbees.sounds.BeeSwarmSound;
import com.google.inject.Provides;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.OkHttpClient;

@Slf4j
@PluginDescriptor(
	name = "Colosseum Bee Sounds",
	description = "Plays bee buzzing sounds in the Colosseum when the Bees modifier is active. Gets louder as the swarm approaches!",
	tags = {"colosseum", "bees", "sound", "buzzing"}
)
public class ColosseumBeesPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private SoundEngine soundEngine;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private BeeSwarmSound beeSwarmSound;

	@Override
	protected void startUp() throws Exception
	{
		executor.submit(() -> {
			SoundFileManager.ensureDownloadDirectoryExists();
			SoundFileManager.downloadAllMissingSounds(okHttpClient);
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		beeSwarmSound.cleanup();
		soundEngine.close();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		beeSwarmSound.onGameStateChanged(event);
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		beeSwarmSound.onNpcSpawned(event);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		beeSwarmSound.onNpcDespawned(event);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		beeSwarmSound.onGameTick(event);
	}

	@Provides
	ColosseumBeesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ColosseumBeesConfig.class);
	}
}
