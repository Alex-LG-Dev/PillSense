package com.equipo5.pillsense;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class PacienteActivo {

    private static final String PREFS = "PillSensePacienteActivo";

    private PacienteActivo() { }

    /** El cuidador elige con qué paciente trabajar. */
    public static void elegir(Context ctx, String uidPaciente, String nombre) {
        prefs(ctx).edit()
                .putString("uid", uidPaciente)
                .putString("nombre", nombre)
                .apply();
    }


    public static String nombre(Context ctx) {
        return prefs(ctx).getString("nombre", null);
    }


    public static boolean hayElegido(Context ctx) {
        return prefs(ctx).getString("uid", null) != null;
    }

    /** Al cerrar sesión: que el siguiente que entre en este móvil no vea al paciente de otro. */
    public static void olvidar(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }


    public static String uidDatos(Context ctx) {
        String uid = prefs(ctx).getString("uid", null);
        if (uid != null) return uid;
        FirebaseUser yo = FirebaseAuth.getInstance().getCurrentUser();
        return (yo != null) ? yo.getUid() : null;
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}