/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.hooks.chat;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import me.andarguy.cc.bukkit.CCBukkit;
import me.andarguy.cc.common.models.PlayerAccount;
import me.andarguy.cc.common.models.UserAccount;
import me.andarguy.chattweaks.events.PostChatEvent;
import me.andarguy.chattweaks.messages.MessageComponent;
import me.andarguy.chattweaks.models.ChatProperty;
import me.andarguy.chattweaks.models.PlayerMessage;
import me.andarguy.chattweaks.models.TweakedMessage;
import net.dv8tion.jda.api.entities.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import java.text.MessageFormat;
import java.util.stream.Collectors;

public class ChatTweaksChatHook implements ChatHook {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPostChatEvent(PostChatEvent event) {
        PlayerMessage message = event.getMessage();

        if (message.getAccessibility().equals(ChatProperty.Accessibility.LOCAL)) return;

        if (message.getType().equals(ChatProperty.Type.CHAT)) {
            DiscordSRV.getPlugin().processChatMessage((Player) event.getMessage().getMessage().getSender(), message.getMessage().build(), "general", false);
        }
    }

    @Override
    public void broadcastMessageToChannel(String channel, Component message, User author) {

        AccountLinkManager manager = DiscordSRV.getPlugin().getAccountLinkManager();

        Integer userId = manager.getUserId(author.getId());

        if (userId == null) return;

        UserAccount userAccount = CCBukkit.getApi().getUserAccount(userId);

        if (userAccount == null) return;

        String content = MessageUtil.toLegacy(message);

        PlayerMessage playerMessage = new PlayerMessage(content, Bukkit.getConsoleSender(), ChatProperty.Accessibility.GLOBAL, ChatProperty.Type.DISCORD);

        playerMessage.getFormat().withComponent(UserMessageComponent.class, userAccount);

        playerMessage.send(true);
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("ChatTweaks");
    }

    @Override
    public boolean isEnabled() {
        return getPlugin() != null && getPlugin().isEnabled() && PluginUtil.pluginHookIsEnabled(getPlugin().getName());
    }

    public static class UserMessageComponent extends MessageComponent {
        public static final String NAME = "user";

        private final UserAccount userAccount;

        public UserMessageComponent(TweakedMessage message, UserAccount userAccount) {
            super(message, NAME);
            this.userAccount = userAccount;
            TweakedMessage.LIBRARY.put(NAME, UserMessageComponent.class);
        }

        @Override
        public void prepare() {
            super.prepare();
            this.getMessage().withResolver(Placeholder.parsed(this.getTag(), MessageFormat.format("<hover:show_text:\"<italic><gray>{1}</gray></italic>\">{0}</hover>", userAccount.getUsername(), userAccount.getLinkedPlayers().stream().map(PlayerAccount::getName).collect(Collectors.joining(", ")))));
        }
    }
}
