package com.ironcladrs.events;

import com.ironcladrs.events.ui.Ui;
import org.junit.Test;
import static org.junit.Assert.*;

public class UiTest
{
    @Test public void linksCannotLeaveClanOrigin()
    {
        assertTrue(Ui.allowedLink("https://ironcladrs.com/domination/91"));
        for (String url : new String[]{"http://ironcladrs.com/", "https://ironcladrs.com.evil.test/", "https://evil.test/", "javascript:alert(1)", "https://me@ironcladrs.com/", "https://ironcladrs.com:444/"}) { assertFalse(url, Ui.allowedLink(url)); }
    }
    @Test public void sidebarTextUsesAnAsciiApostrophe() { assertTrue(Ui.text("your team\u2019s current view").getText().contains("team's")); }
    @Test public void remoteHtmlCannotInjectLabelMarkup() { assertEquals("&lt;img&gt;&amp;", Ui.html("<img>&")); }
}
