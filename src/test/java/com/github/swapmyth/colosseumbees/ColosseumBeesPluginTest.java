package com.github.swapmyth.colosseumbees;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ColosseumBeesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ColosseumBeesPlugin.class);
		RuneLite.main(args);
	}
}
