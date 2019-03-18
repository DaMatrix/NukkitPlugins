package net.daporkchop.autoreply;


import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.utils.Utils;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Getter
public final class AutoReply extends PluginBase {
    private boolean colorize;
    private boolean caseSensitive;
    private boolean cancelChat;
    private BiFunction<String, String, Boolean> compareStrings;
    private final Map<StringWrapper, Function<Player, String>> mappings = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        {
            File file = new File(this.getDataFolder(), "config.yml");
            if (file.exists()) {
                try {
                    String content = Utils.readFile(file);
                    if (content.trim().isEmpty() && !file.delete()) {
                        throw new IllegalStateException(String.format("Unable to delete empty config file %s", file.getAbsolutePath()));
                    }
                } catch (IOException e) {
                    Server.getInstance().getLogger().logException(e);
                }
            }
        }

        this.saveResource("messages.json");
        this.saveResource("config.yml");

        this.reloadConfig();

        this.getServer().getPluginManager().registerEvents(
                new Listener() {
                    @EventHandler
                    public void onChat(PlayerChatEvent event) {
                        Function<Player, String> function = AutoReply.this.mappings.get(new StringWrapper(event.getMessage()));
                        if (function != null) {
                            String msg = function.apply(event.getPlayer());
                            if (AutoReply.this.colorize) {
                                msg = TextFormat.colorize(msg);
                            }
                            event.getPlayer().sendMessage(msg);
                            if (AutoReply.this.cancelChat) {
                                event.setCancelled(true);
                            }
                        }
                    }
                }, this
        );
    }

    @Override
    public void onDisable() {
        this.mappings.clear();
    }

    @Override
    public void reloadConfig() {
        File dataFolder = this.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            throw new IllegalStateException(String.format("Unable to create directory %s!", dataFolder.getAbsolutePath()));
        }

        Config config = new Config(
                new File(dataFolder, "config.yml")
        );
        if (!config.isCorrect()) {
            throw new IllegalStateException();
        }
        this.colorize = config.getBoolean("colorize", true);
        this.compareStrings = (this.caseSensitive = config.getBoolean("caseSensitive", false)) ? String::equals : String::equalsIgnoreCase;
        this.cancelChat = config.getBoolean("cancelChat", true);
        config.save();

        this.mappings.clear();
        config = new Config(
                new File(dataFolder, "messages.json")
        );
        if (!config.isCorrect()) {
            throw new IllegalStateException();
        }
        config.getAll().entrySet().stream().map(entry -> new Tuple<>(entry.getKey(), (String) entry.getValue()))
                .forEach(tuple -> this.mappings.put(new StringWrapper(tuple.key), (player) -> {
                    String response = tuple.value;
                    response = response.replaceAll("\\$name", player.getName());
                    response = response.replaceAll("\\$displayName", player.getDisplayName());
                    response = response.replaceAll("\\$x", String.valueOf(player.getFloorX()));
                    response = response.replaceAll("\\$y", String.valueOf(player.getFloorY()));
                    response = response.replaceAll("\\$z", String.valueOf(player.getFloorZ()));
                    return response;
                }));
        this.getLogger().info(String.format("Loaded %d message/response pairs!", this.mappings.size()));
    }

    @RequiredArgsConstructor
    protected static final class Tuple<K, V> {
        @NonNull
        protected final K key;

        @NonNull
        protected final V value;
    }

    @RequiredArgsConstructor
    protected final class StringWrapper {
        @NonNull
        protected final String delegate;

        @Override
        public int hashCode() {
            return AutoReply.this.caseSensitive ? this.delegate.hashCode() : this.delegate.toLowerCase().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof StringWrapper) {
                o = ((StringWrapper) o).delegate;
            }
            if (o instanceof String) {
                return AutoReply.this.compareStrings.apply(this.delegate, (String) o);
            } else {
                return false;
            }
        }
    }
}
