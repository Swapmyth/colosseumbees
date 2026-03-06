package com.github.swapmyth.colosseumbees.sounds;

import com.github.swapmyth.colosseumbees.ColosseumBeesConfig;
import com.github.swapmyth.colosseumbees.Sound;
import com.github.swapmyth.colosseumbees.SoundEngine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sound.sampled.Clip;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
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
	// The Colosseum bee swarm NPC name
	private static final String BEE_SWARM_NPC_NAME = "Bee Swarm";

	// Model override ID present when the bee swarm is inactive/dormant
	private static final int INACTIVE_MODEL_ID = 32709;

	// Sound entries mapped by layer index
	private static final Sound[] BEE_SOUNDS = {
		Sound.BEE_SOUND_1,
		Sound.BEE_SOUND_2,
		Sound.BEE_SOUND_3,
		Sound.BEE_SOUND_4
	};

	// Volume multiplier for each layer (progressive loudness)
	private static final float[] LAYER_VOLUME_MULTIPLIERS = {0.25f, 0.33f, 0.50f, 0.75f};

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

	// Per-swarm state tracking to support multiple simultaneous bee swarms (Bees II/III)
	private final List<BeeSwarmState> swarmStates = new ArrayList<>();

	/**
	 * Tracks the state of a single bee swarm NPC, including its sound layers.
	 */
	private static class BeeSwarmState
	{
		final NPC npc;
		final List<Clip> activeClips = new ArrayList<>();
		int currentLayerCount = 0;
		boolean isActive = false;

		BeeSwarmState(NPC npc)
		{
			this.npc = npc;
		}
	}

	public void onNpcSpawned(NpcSpawned event)
	{
		if (!config.colosseumBees())
		{
			return;
		}

		NPC npc = event.getNpc();
		if (npc.getName() != null && npc.getName().equalsIgnoreCase(BEE_SWARM_NPC_NAME))
		{
			// Check we're not already tracking this NPC
			for (BeeSwarmState state : swarmStates)
			{
				if (state.npc == npc)
				{
					return;
				}
			}

			swarmStates.add(new BeeSwarmState(npc));
			log.debug("Bee swarm spawned (now tracking {} swarms), waiting for activation", swarmStates.size());
		}
	}

	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		Iterator<BeeSwarmState> it = swarmStates.iterator();
		while (it.hasNext())
		{
			BeeSwarmState state = it.next();
			if (state.npc == npc)
			{
				log.debug("Bee swarm despawned, stopping its sounds");
				cleanupSwarmState(state);
				it.remove();
				return;
			}
		}
	}

	public void onGameTick(GameTick event)
	{
		if (swarmStates.isEmpty())
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

		for (BeeSwarmState state : swarmStates)
		{
			boolean currentlyActive = isBeeSwarmActive(state.npc);

			if (!currentlyActive)
			{
				// Bees are inactive — stop any playing sounds
				if (state.isActive)
				{
					log.debug("Bee swarm became inactive, stopping its sounds");
					stopSwarmSounds(state);
					state.isActive = false;
				}
				continue;
			}

			// Bees became active
			if (!state.isActive)
			{
				log.debug("Bee swarm became active, starting sounds");
				state.isActive = true;
			}

			WorldPoint beeLocation = state.npc.getWorldLocation();
			int distance = playerLocation.distanceTo(beeLocation);

			int desiredLayers = getDesiredLayerCount(distance);
			if (desiredLayers != state.currentLayerCount)
			{
				updateLayers(state, desiredLayers);
			}
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
			default:
				break;
		}
	}

	/**
	 * Check if a bee swarm NPC is active by examining its model overrides.
	 * When inactive, the NPC has model override ID 32709.
	 */
	private boolean isBeeSwarmActive(NPC npc)
	{
		NPCComposition composition = npc.getTransformedComposition();
		if (composition == null)
		{
			return false;
		}

		int[] models = composition.getModels();
		if (models != null)
		{
			log.debug("Bee swarm NPC models: {}", Arrays.toString(models));
			for (int modelId : models)
			{
				if (modelId == INACTIVE_MODEL_ID)
				{
					return false;
				}
			}
		}

		return true;
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

	private void updateLayers(BeeSwarmState state, int desiredLayers)
	{
		if (desiredLayers > state.currentLayerCount)
		{
			// Add more layers
			for (int i = state.currentLayerCount; i < desiredLayers; i++)
			{
				Sound sound = BEE_SOUNDS[i % BEE_SOUNDS.length];
				float volumeMultiplier = LAYER_VOLUME_MULTIPLIERS[i % LAYER_VOLUME_MULTIPLIERS.length];
				Clip clip = soundEngine.playLoopingClip(sound, volumeMultiplier);
				if (clip != null)
				{
					state.activeClips.add(clip);
				}
			}
		}
		else if (desiredLayers < state.currentLayerCount)
		{
			// Remove excess layers (remove from the end)
			while (state.activeClips.size() > desiredLayers)
			{
				Clip clip = state.activeClips.remove(state.activeClips.size() - 1);
				soundEngine.stopClip(clip);
			}
		}
		state.currentLayerCount = desiredLayers;
	}

	/**
	 * Stop all sounds for a specific swarm without removing it from the list.
	 */
	private void stopSwarmSounds(BeeSwarmState state)
	{
		for (Clip clip : state.activeClips)
		{
			soundEngine.stopClip(clip);
		}
		state.activeClips.clear();
		state.currentLayerCount = 0;
	}

	/**
	 * Stop all sounds for a specific swarm and reset its state fully.
	 */
	private void cleanupSwarmState(BeeSwarmState state)
	{
		stopSwarmSounds(state);
		state.isActive = false;
	}

	/**
	 * Stop all sounds and clear all tracked swarms.
	 */
	public void cleanup()
	{
		for (BeeSwarmState state : swarmStates)
		{
			cleanupSwarmState(state);
		}
		swarmStates.clear();
	}
}
