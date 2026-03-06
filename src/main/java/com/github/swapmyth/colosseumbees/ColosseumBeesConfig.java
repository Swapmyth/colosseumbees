package com.github.swapmyth.colosseumbees;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(ColosseumBeesConfig.CONFIG_GROUP)
public interface ColosseumBeesConfig extends Config
{
	String CONFIG_GROUP = "colosseumbees";

	@ConfigItem(
		keyName = "colosseumBees",
		name = "Colosseum Bees",
		description = "Play bee buzzing sounds when the bee swarm spawns in the Colosseum. Gets louder as bees get closer!",
		position = 0
	)
	default boolean colosseumBees()
	{
		return true;
	}

	@Range(
		min = 0,
		max = 200
	)
	@ConfigItem(
		keyName = "announcementVolume",
		name = "Volume",
		description = "Adjust how loud the bee sounds are played!",
		position = 1
	)
	default int announcementVolume()
	{
		return 100;
	}
}
