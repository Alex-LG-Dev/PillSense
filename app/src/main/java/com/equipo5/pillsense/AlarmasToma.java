package com.equipo5.pillsense;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;


public final class AlarmasToma {


    public static final String CANAL = "avisos_tomas";


    public static final String EXTRA_POSPUESTA = "pospuesta";

    private AlarmasToma() { }


    public static void programar(Context ctx, String idToma, String hora, String medicamento, int cajon) {
        Calendar cuando = proximaVez(hora);
        if (idToma == null || cuando == null) return;
        programarEn(ctx, idToma, hora, medicamento, cajon, cuando.getTimeInMillis(), false);
    }


    public static void posponer(Context ctx, String idToma, String hora, String medicamento,
                                int cajon, int minutos) {
        if (idToma == null) return;
        long cuando = System.currentTimeMillis() + minutos * 60_000L;
        programarEn(ctx, idToma, hora, medicamento, cajon, cuando, true);
    }


    public static void cancelar(Context ctx, String idToma) {
        cancelarAlarma(ctx, idToma, false);
        cancelarAlarma(ctx, idToma, true);
    }

    public static void cancelarPospuesta(Context ctx, String idToma) {
        cancelarAlarma(ctx, idToma, true);
    }

    /** Al encender el móvil: vuelve a programar las tomas guardadas  */
    public static void reprogramarGuardadas(Context ctx) {
        android.content.SharedPreferences prefs =
                ctx.getSharedPreferences("PillSenseAlarmas", Context.MODE_PRIVATE);
        for (String id : prefs.getStringSet("ids", new java.util.HashSet<>())) {

            String datos = prefs.getString("toma_" + id, null);
            if (datos == null) continue;
            String[] partes = datos.split("\\|", 3);
            if (partes.length < 3) continue;
            try {
                programar(ctx, id, partes[0], partes[2], Integer.parseInt(partes[1]));
            } catch (NumberFormatException e) {

            }
        }
    }


    public static void crearCanal(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;   // antes de Android 8 no hay canales
        NotificationChannel canal = new NotificationChannel(
                CANAL, "Avisos de las pastillas", NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription("Te avisa a la hora de cada pastilla");
        ctx.getSystemService(NotificationManager.class).createNotificationChannel(canal);
    }



    private static void programarEn(Context ctx, String idToma, String hora, String medicamento,
                                    int cajon, long cuandoMillis, boolean pospuesta) {
        Intent intent = new Intent(ctx, AlarmaTomaReceiver.class);
        intent.putExtra(AvisoTomaActivity.EXTRA_ID_TOMA, idToma);
        intent.putExtra(AvisoTomaActivity.EXTRA_HORA, hora);
        intent.putExtra(AvisoTomaActivity.EXTRA_MEDICAMENTO, medicamento);
        intent.putExtra(AvisoTomaActivity.EXTRA_CAJON, cajon);
        intent.putExtra(EXTRA_POSPUESTA, pospuesta);

        PendingIntent pi = PendingIntent.getBroadcast(ctx, codigo(idToma, pospuesta), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Sin permiso de alarma exacta: suena igual, pero puede retrasarse unos minutos
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuandoMillis, pi);
        } else {
            // A la hora justa, aunque el móvil esté en reposo
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuandoMillis, pi);
        }
        android.util.Log.d("PillSenseAlarma", "Programada " + hora + " (" + medicamento + ") para "
                + new java.util.Date(cuandoMillis) + (pospuesta ? " [pospuesta]" : ""));
    }

    private static void cancelarAlarma(Context ctx, String idToma, boolean pospuesta) {
        if (idToma == null) return;
        Intent intent = new Intent(ctx, AlarmaTomaReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, codigo(idToma, pospuesta), intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {   // null = no había ninguna programada
            ctx.getSystemService(AlarmManager.class).cancel(pi);
            pi.cancel();
        }
    }


    private static int codigo(String idToma, boolean pospuesta) {
        return (pospuesta ? idToma + "_pospuesta" : idToma).hashCode();
    }


    static Calendar proximaVez(String hora) {
        if (hora == null || !hora.contains(":")) return null;
        String[] partes = hora.split(":");
        int h, m;
        try {
            h = Integer.parseInt(partes[0].trim());
            m = Integer.parseInt(partes[1].trim());
        } catch (NumberFormatException e) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, h);
        c.set(Calendar.MINUTE, m);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        if (c.getTimeInMillis() <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_YEAR, 1);   // ya ha pasado hoy → mañana
        }
        return c;
    }
}