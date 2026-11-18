package com.equipo5.pillsense;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Guarda el token de Firebase Cloud Messaging de este móvil en
 * usuarios/{uid}/fcmTokens/{token} para que el servidor sepa a qué móviles
 * mandar el aviso de toma olvidada (T071).
 */
public final class TokenFcm {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private TokenFcm() { }

    /** Pide el token actual y lo guarda en el usuario que ha iniciado sesión. */
    public static void guardar() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(TokenFcm::guardar);
    }

    /** Guarda un token concreto (también se usa cuando Firebase lo renueva). */
    public static void guardar(String token) {
        FirebaseUser yo = FirebaseAuth.getInstance().getCurrentUser();
        if (yo == null || token == null) return;
        FirebaseDatabase.getInstance(DB_URL).getReference("usuarios")
                .child(yo.getUid()).child("fcmTokens").child(token).setValue(true);
    }

    /** Al cerrar sesión: este móvil deja de recibir avisos de ese usuario. */
    public static void borrar(Runnable despues) {
        FirebaseUser yo = FirebaseAuth.getInstance().getCurrentUser();
        if (yo == null) { despues.run(); return; }
        String uid = yo.getUid();
        // Se borra antes de cerrar la sesión: las reglas piden estar identificado
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(tarea -> {
            if (!tarea.isSuccessful() || tarea.getResult() == null) { despues.run(); return; }
            FirebaseDatabase.getInstance(DB_URL).getReference("usuarios")
                    .child(uid).child("fcmTokens").child(tarea.getResult()).removeValue()
                    .addOnCompleteListener(x -> despues.run());
        });
    }
}
