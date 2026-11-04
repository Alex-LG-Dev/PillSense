package com.equipo5.pillsense;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;


public class AlarmaTomaReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        android.util.Log.d("PillSenseAlarma", "HA SONADO la alarma de las "
                + intent.getStringExtra(AvisoTomaActivity.EXTRA_HORA));
        String idToma = intent.getStringExtra(AvisoTomaActivity.EXTRA_ID_TOMA);
        String hora = intent.getStringExtra(AvisoTomaActivity.EXTRA_HORA);
        String medicamento = intent.getStringExtra(AvisoTomaActivity.EXTRA_MEDICAMENTO);
        int cajon = intent.getIntExtra(AvisoTomaActivity.EXTRA_CAJON, 0);
        boolean pospuesta = intent.getBooleanExtra(AlarmasToma.EXTRA_POSPUESTA, false);
        if (idToma == null) return;


        Intent abrir = new Intent(ctx, AvisoTomaActivity.class);
        abrir.putExtra(AvisoTomaActivity.EXTRA_ID_TOMA, idToma);
        abrir.putExtra(AvisoTomaActivity.EXTRA_HORA, hora);
        abrir.putExtra(AvisoTomaActivity.EXTRA_MEDICAMENTO, medicamento);
        abrir.putExtra(AvisoTomaActivity.EXTRA_CAJON, cajon);
        abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent alPulsar = PendingIntent.getActivity(ctx, idToma.hashCode(), abrir,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmasToma.crearCanal(ctx);
        NotificationCompat.Builder notificacion = new NotificationCompat.Builder(ctx, AlarmasToma.CANAL)
                .setSmallIcon(R.drawable.ic_campana)
                .setContentTitle("Es hora de tu pastilla · " + hora)
                .setContentText("Abre el cajón " + cajon + ": " + medicamento)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(alPulsar)
                .setAutoCancel(true);   // desaparece al pulsarla

        // Solo si el paciente nos dio permiso de notificaciones
        boolean conPermiso = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        android.util.Log.d("PillSenseAlarma", "¿Permiso de notificaciones? " + conPermiso);
        if (conPermiso) {
            NotificationManagerCompat.from(ctx).notify(idToma.hashCode(), notificacion.build());
        }


        if (!pospuesta) {
            AlarmasToma.programar(ctx, idToma, hora, medicamento, cajon);
        }
    }
}