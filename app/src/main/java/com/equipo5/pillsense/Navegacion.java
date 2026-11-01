package com.equipo5.pillsense;

import android.app.Activity;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

/** Decide a qué pantalla va el usuario al entrar, según su rol */
public final class Navegacion {

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private Navegacion() { }


    public static void abrirPantallaInicio(Activity activity) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) return;

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("usuarios").child(usuario.getUid()).child("rol")
                .get()
                .addOnCompleteListener(task -> {
                    String rol = task.isSuccessful() ? task.getResult().getValue(String.class) : null;
                    Class<?> destino = "paciente".equals(rol)
                            ? PacienteActivity.class
                            : PrincipalActivity.class;

                    Intent intent = new Intent(activity, destino);

                    // Borra las pantallas anteriores: al pulsar "atrás" no se vuelve al login
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(intent);
                    activity.finish();
                });
    }
}