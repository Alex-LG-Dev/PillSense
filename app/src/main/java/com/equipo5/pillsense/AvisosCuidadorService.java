package com.equipo5.pillsense;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Recibe los avisos de Firebase Cloud Messaging (T071).
 * El servidor manda mensajes solo de datos (sin "notification"), así que esto se
 * ejecuta también con la app en segundo plano o cerrada, y la notificación la
 * construimos aquí para que al pulsarla se abra la pestaña Hoy del paciente.
 */
public class AvisosCuidadorService extends FirebaseMessagingService {

    public static final String TIPO_TOMA_OLVIDADA = "toma_olvidada";

    @Override
    public void onNewToken(@NonNull String token) {
        TokenFcm.guardar(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage mensaje) {
        Map<String, String> datos = mensaje.getData();
        if (!TIPO_TOMA_OLVIDADA.equals(datos.get("tipo"))) return;

        String uidPaciente = datos.get("uidPaciente");
        String nombrePaciente = datos.get("nombrePaciente");
        String medicamento = datos.get("medicamento");
        String hora = datos.get("hora");
        String idToma = datos.get("idToma");
        String fecha = datos.get("fecha");
        if (uidPaciente == null) return;
        if (nombrePaciente == null || nombrePaciente.isEmpty()) nombrePaciente = "Tu paciente";

        // Al pulsar: pantalla principal en la pestaña Hoy de ese paciente
        Intent abrir = new Intent(this, PrincipalActivity.class);
        abrir.putExtra(PrincipalActivity.EXTRA_UID_PACIENTE, uidPaciente);
        abrir.putExtra(PrincipalActivity.EXTRA_NOMBRE_PACIENTE, nombrePaciente);
        abrir.putExtra(PrincipalActivity.EXTRA_PESTANA, PrincipalActivity.PESTANA_HOY);
        abrir.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Mismo id para la misma toma del mismo día: si llegara repetida, se sustituye
        int idNotificacion = (uidPaciente + "/" + fecha + "/" + idToma).hashCode();
        PendingIntent alPulsar = PendingIntent.getActivity(this, idNotificacion, abrir,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmasToma.crearCanal(this);
        NotificationCompat.Builder notificacion = new NotificationCompat.Builder(this, AlarmasToma.CANAL)
                .setSmallIcon(R.drawable.ic_campana)
                .setContentTitle(nombrePaciente + " no ha tomado su pastilla")
                .setContentText((medicamento != null ? medicamento : "Toma")
                        + (hora != null ? " de las " + hora : "") + " · Olvidada")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(alPulsar)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true);

        boolean conPermiso = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        if (conPermiso) {
            NotificationManagerCompat.from(this).notify(idNotificacion, notificacion.build());
        }
    }
}
