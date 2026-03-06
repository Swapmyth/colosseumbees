package com.github.swapmyth.colosseumbees.sounds;

import com.github.swapmyth.colosseumbees.ColosseumBeesConfig;
import com.github.swapmyth.colosseumbees.Sound;
import com.github.swapmyth.colosseumbees.SoundEngine;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.Clip;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;

@Singleton
@Slf4j
public class BeeSwarmSound
{
	// The Colosseum bee swarm NPC name - adjust if the exact name differs
	private static final String BEE_SWARM_NPC_NAME = "Bee Swarm";

	// Sound entries mapped by layer index
	private static final Sound[] BEE_SOUNDS = {
		Sound.BEE_SOUND_1,
		Sound.BEE_SOUND_2,
		Sound.BEE_SOUND_3,
		Sound.BEE_SOUND_4
	};

	// Distance thresholds for number of layers
	// 10+ tiles = 1 layer, 5-9 = 2, 1-4 = 3, 0 = 4
	private static final int DISTANCE_FAR = 10;
	private static final int DISTANCE_MEDIUM = 5;
	private static final int DISTANCE_CLOSE = 1;

	@Inject
	private Client client;

	@Inject
	private ColosseumBeesConfig config;

	@Inject
	private SoundEngine soundEngine;

	private NPC trackedBeeSwarm;
	private final List<Clip> activeClips = new ArrayList<>();
	private int currentLayerCount = 0;

	public void onNpcSpawned(NpcSpawned event)
	{
		if (!config.colosseumBees())
		{
			return;
		}

		NPC npc = event.getNpc();
		if (npc.getName() != null && npc.getName().equalsIgnoreCase(BEE_SWARM_NPC_NAME))
		{
			trackedBeeSwarm = npc;
			log.debug("Bee swarm spawned, starting bee sounds");
			updateLayers(1);
		}
	}

	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (npc == trackedBeeSwarm)
		{
			log.debug("Bee swarm despawned, stopping bee sounds");
			cleanup();
		}
	}

	public void onGameTick(GameTick event)
	{
		if (trackedBeeSwarm == null)
		{
			return;
		}

		if (!config.colosseumBees())
		{
			cleanup();
			return;
		}

		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		WorldPoint playerLocation = localPlayer.getWorldLocation();
		WorldPoint beeLocation = trackedBeeSwarm.getWorldLocation();
		int distance = playerLocation.distanceTo(beeLocation);

		int desiredLayers = getDesiredLayerCount(distance);
		if (desiredLayers != currentLayerCount)
		{
			updateLayers(desiredLayers);
		}
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
			case CONNECTION_LOST:
				cleanup();
				break;
		}
	}

	private int getDesiredLayerCount(int distance)
	{
		if (distance >= DISTANCE_FAR)
		{
			return 1;
		}
		else if (distance >= DISTANCE_MEDIUM)
		{
			return 2;
		}
		else if (distance >= DISTANCE_CLOSE)
		{
			return 3;
		}
		else
		{
			return 4;
		}
	}

	private void updateLayers(int desiredLayers)
	{
		if (desiredLayers > currentLayerCount)
		{
			// Add more layers
			for (int i = currentLayerCount; i < desiredLayers; i++)
			{
				Sound sound = BEE_SOUNDS[i % BEE_SOUNDS.length];
				Clip clip = soundEngine.playLoopingClip(sound);
				if (clip != null)
				{
					activeClips.add(clip);
				}
			}
		}
		else if (desiredLayers < currentLayerCount)
		{
			// Remove excess layers (remove from the end)
			while (activeClips.size() > desiredLayers)
			{
				Clip clip = activeClips.remove(activeClips.size() - 1);
				soundEngine.stopClip(clip);
			}
		}
		currentLayerCount = desiredLayers;
	}

	public void cleanup()
	{
		for (Clip clip : activeClips)
		{
			soundEngine.stopClip(clip);
		}
		activeClips.clear();
		currentLayerCount = 0;
		trackedBeeSwarm = null;
	}
}
