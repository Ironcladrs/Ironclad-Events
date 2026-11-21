package com.ironcladrs.events;

import java.util.EnumSet;
import net.runelite.api.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PlayerGuardTest
{
    @Test public void identityRequiresConsentLoginKeyAndStandardWorld() throws Exception
    {
        Client client = mock(Client.class); Player player = mock(Player.class); PlayerGuard guard = new PlayerGuard();
        guard.activate();
        TestSupport.inject(guard,"client",client);
        TestSupport.inject(guard,"config",new IroncladEventsConfig() {
            @Override public boolean connect() { return true; }
            @Override public String apiKey() { return TestSupport.identity().key(); }
        });
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN); when(client.getLocalPlayer()).thenReturn(player);
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS)); when(player.getName()).thenReturn("Test Player"); when(client.getAccountHash()).thenReturn(123L);
        assertTrue(guard.refresh()); assertNotNull(guard.identity()); long generation = guard.generation(); assertFalse(guard.refresh());
        guard.deactivate(); guard.refresh(); assertNull(guard.identity()); guard.activate(); guard.refresh();
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN); assertTrue(guard.refresh()); assertNull(guard.identity()); assertTrue(guard.generation()>generation);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN); when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.DEADMAN)); guard.refresh(); assertNull(guard.identity());
        when(client.getWorldType()).thenReturn(EnumSet.of(WorldType.MEMBERS)); TestSupport.inject(guard,"config",new IroncladEventsConfig() {}); guard.refresh(); assertNull(guard.identity());
    }
}
