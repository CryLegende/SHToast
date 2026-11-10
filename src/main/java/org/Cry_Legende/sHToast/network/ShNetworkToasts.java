package org.Cry_Legende.sHToast.network;

import com.google.gson.JsonObject;
import org.Cry_Legende.sHNetwork.api.NetPlayer;
import org.Cry_Legende.sHNetwork.api.SHNetworkAPI;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** Pont vers SHNetwork. Cette classe n'est chargée que si SHNetwork est installé. */
public final class ShNetworkToasts implements ToastNetwork {

    private static final String CHANNEL = "shtoast.general";

    private final SHNetworkAPI api;

    private ShNetworkToasts() {
        this.api = SHNetworkAPI.get();
    }

    /** Point d'entrée : le plugin ne résout cette classe qu'à cet appel, donc jamais sans SHNetwork. */
    public static ToastNetwork open() {
        return new ShNetworkToasts();
    }

    @Override
    public boolean relay(String player, UnaryOperator<String> command) {
        Optional<NetPlayer> found = api.find(player).filter(p -> !p.server().equals(api.serverName()));
        if (found.isEmpty()) {
            return false;
        }
        api.runFor(found.get().uuid().toString(), command.apply(found.get().name()));
        return true;
    }

    @Override
    public int playersElsewhere() {
        String here = api.serverName();
        return (int) api.online().stream().filter(p -> !p.server().equals(here)).count();
    }

    @Override
    public void broadcast(JsonObject toast) {
        api.publish(CHANNEL, toast.toString());
    }

    @Override
    public void onBroadcast(Consumer<JsonObject> handler) {
        api.subscribe(CHANNEL, message -> {
            JsonObject json;
            try {
                json = message.object();
            } catch (RuntimeException e) {
                return; // message illisible : le bus ne s'arrête jamais pour lui
            }
            handler.accept(json);
        }, true);
    }
}
