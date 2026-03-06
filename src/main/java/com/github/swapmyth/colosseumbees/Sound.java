package com.github.swapmyth.colosseumbees;

public enum Sound
{
	BEE_SOUND_1("bees", "Bee_r1.wav"),
	BEE_SOUND_2("bees", "Bee_r2.wav"),
	BEE_SOUND_3("bees", "Bee_r3.wav"),
	BEE_SOUND_4("bees", "Bee_r4.wav");

	private final String resourceName;
	private final String directory;

	Sound(String directory, String resourceName)
	{
		this.directory = directory;
		this.resourceName = resourceName;
	}

	String getResourceName()
	{
		return resourceName;
	}

	String getDirectory()
	{
		return directory;
	}
}
