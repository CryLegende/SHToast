package org.Cry_Legende.sHToast.network;

import com.google.gson.JsonObject;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Ce dont les toasts ont besoin du réseau. Seule {@link ShNetworkToasts} connaît SHNetwork : sans lui, aucune
 * classe de ce paquet n'est chargée.
 */
public interface ToastNetwork {

    /**
     * Si le joueur est connecté sur un autre serveur, y fait exécuter la commande par la console, une seule fois,
     * et rend vrai. Rend faux s'il n'est connecté nulle part ailleurs.
     *
     * @param player  pseudo tapé par l'émetteur (casse ignorée)
     * @param command commande sans {@code /} à partir du pseudo exact du joueur
     */
    boolean relay(String player, UnaryOperator<String> command);

    /** Nombre de joueurs connectés sur les autres serveurs du réseau. */
    int playersElsewhere();

    /** Envoie un toast à tous les joueurs des autres serveurs. */
    void broadcast(JsonObject toast);

    /** Reçoit les toasts généraux des autres serveurs, sur le fil principal. */
    void onBroadcast(Consumer<JsonObject> handler);
}
