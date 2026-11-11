package de.angelbot.core.listener;

import de.angelbot.core.AngelbotAddon;
import net.labymod.api.client.chat.ChatMessage;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.serializer.plain.PlainTextComponentSerializer;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.chat.ActionBarReceiveEvent;
import net.labymod.api.event.client.chat.ChatReceiveEvent;

/**
 * Reicht Meldungen des Servers an den Bot weiter, damit er erkennt, wenn der Server meldet, dass an
 * der Stelle nichts mehr zu fangen ist. Chat von Spielern wird ignoriert: Wenn jemand "keine Fische"
 * schreibt, soll der Bot nicht losgehen.
 */
public class AngelbotChatListener {

  private final AngelbotAddon addon;

  public AngelbotChatListener(AngelbotAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onChat(ChatReceiveEvent event) {
    ChatMessage message = event.chatMessage();
    if (message == null || message.getSenderUniqueId() != null) {
      return;
    }

    this.addon.onServerMessage(message.getPlainText());
  }

  @Subscribe
  public void onActionBar(ActionBarReceiveEvent event) {
    if (event.phase() != Phase.PRE) {
      return;
    }

    // getMessage() statt message(): Letzteres ist veraltet und zum Entfernen vorgemerkt.
    Component component = event.getMessage();
    if (component == null) {
      return;
    }

    this.addon.onServerMessage(PlainTextComponentSerializer.plainText().serialize(component));
  }
}
