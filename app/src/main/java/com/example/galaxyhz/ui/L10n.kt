package com.example.galaxyhz.ui

/**
 * Tiny localization helper. English is the base language; a few high-traffic
 * strings ship with translations, everything else falls back to English.
 */
object L10n {
    val languages = listOf("en" to "English", "fr" to "Français")

    private val fr = mapOf(
        "home" to "Accueil",
        "resolution" to "Résolution",
        "custom_rate" to "Fréquence personnalisée",
        "adaptive" to "Comportement adaptatif",
        "overclock" to "Modes expérimentaux",
        "tools" to "Outils anti-grésillement",
        "settings" to "Paramètres",
        "setup_title" to "Bienvenue dans GalaxyHz",
        "setup_body" to "Cette application force le taux de rafraîchissement de votre " +
            "Samsung Galaxy. Une demande d'accès superutilisateur (Magisk) va apparaître : " +
            "acceptez-la pour continuer.",
        "setup_grant" to "Accorder l'accès root",
        "setup_skip" to "Continuer sans root",
        "setup_done" to "Terminé",
        "active_rate" to "FRÉQUENCE ACTIVE",
        "panel" to "Panneau",
        "not_root" to "ROOT REQUIS - ouvrez Magisk et accordez l'accès à GalaxyHz",
        "lock_rate" to "Verrouiller la fréquence",
        "locked" to "Fréquence verrouillée et vérifiée !",
        "aod" to "Écran Always-On (AOD)",
        "reset_ddi" to "Réinitialiser l'écran (Reset DDI)",
        "repair" to "Réparer le mode sur toutes les couches",
        "apply" to "Appliquer",
        "language" to "Langue",
        "about" to "À propos",
        "repo" to "Voir le projet sur GitHub"
    )

    fun t(key: String, lang: String): String = if (lang == "fr") fr[key] ?: key else key
}
