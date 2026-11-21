package com.ironcladrs.events;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class IroncladEventsPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(IroncladEventsPlugin.class);
		RuneLite.main(args);
	}
}