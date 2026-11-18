package com.example.pasarela;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Pasarela de PillSense que corre en la Raspberry.
// 1) Cuando la base avisa por MQTT de que se abre un cajón, marca la toma como tomada en Firebase.
// 2) Cada minuto mira si alguna toma ha pasado su hora límite y avisa al avisador (T069).
// 3) Cuando cambian las tomas en la App, manda los horarios a la base por MQTT (T066).
// 4) Avisa de las tomas olvidadas al móvil de los cuidadores con FCM (T071).
public class Pasarela {

    private static final String BROKER = "tcp://localhost:1883";
    private static final String TEMA = "pillsense/+/cajon/+";
    private static final String CLAVE = "clave-firebase.json";      // tiene que estar al lado del .jar
    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private static DatabaseReference bd;
    private static MqttClient mqtt;

    // Guardo las tomas que ya he avisado para no repetir el aviso cada minuto
    private static final Set<String> avisadas = new HashSet<>();
    // Si reinicio la Raspberry, así no avisa de tomas olvidadas de hace horas
    private static final int MARGEN_MINUTOS = 60;

    // Opcional (pruebas): si existe PILL_TEST_UID solo se procesa ese paciente
    // (tomas, registros, MQTT y FCM). Sin la variable se procesan todos.
    private static final String UID_PRUEBA = leerUidPrueba();

    private static String leerUidPrueba() {
        String uid = System.getenv("PILL_TEST_UID");
        return (uid == null || uid.trim().isEmpty()) ? null : uid.trim();
    }

    private static boolean seProcesa(String uid) {
        return UID_PRUEBA == null || UID_PRUEBA.equals(uid);
    }

    public static void main(String[] args) throws Exception {
        conectarFirebase();
        conectarMqtt();
        escucharHorarios();
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(Pasarela::revisarOlvidadas, 10, 60, TimeUnit.SECONDS);
        if (UID_PRUEBA != null) System.out.println("Modo prueba: solo el paciente " + UID_PRUEBA);
        System.out.println("Pasarela en marcha. Escuchando " + TEMA + " (Ctrl+C para salir)");
        Thread.currentThread().join();   // para que el programa no se cierre
    }

    private static void conectarFirebase() throws Exception {
        FirebaseOptions opciones = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new FileInputStream(CLAVE)))
                .setDatabaseUrl(DB_URL)
                .build();
        FirebaseApp.initializeApp(opciones);
        bd = FirebaseDatabase.getInstance().getReference();
        System.out.println("Conectado a Firebase");
    }

    private static void conectarMqtt() throws Exception {
        MqttClient cliente = new MqttClient(BROKER, "pasarela-raspberry", new MemoryPersistence());
        mqtt = cliente;
        MqttConnectOptions opciones = new MqttConnectOptions();
        opciones.setAutomaticReconnect(true);
        opciones.setCleanSession(true);

        cliente.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconexion, String broker) {
                try {
                    cliente.subscribe(TEMA, 1);   // si se reconecta hay que suscribirse otra vez
                    System.out.println("Conectado a Mosquitto");
                } catch (Exception e) {
                    System.out.println("No se pudo suscribir: " + e.getMessage());
                }
            }

            @Override
            public void connectionLost(Throwable causa) {
                System.out.println("Se perdió la conexión con Mosquitto, reintentando...");
            }

            @Override
            public void messageArrived(String tema, MqttMessage mensaje) {
                procesar(tema, new String(mensaje.getPayload()).trim());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) { }
        });

        cliente.connect(opciones);
    }

    // El tema llega como pillsense/{uid}/cajon/{n}
    private static void procesar(String tema, String texto) {
        System.out.println("Recibido " + tema + " -> " + texto);
        String[] partes = tema.split("/");
        if (partes.length != 4 || !"abierto".equalsIgnoreCase(texto)) return;

        String uid = partes[1];
        if (!seProcesa(uid)) return;
        int cajon;
        try {
            cajon = Integer.parseInt(partes[3]);
        } catch (NumberFormatException e) {
            System.out.println("Número de cajón no válido: " + partes[3]);
            return;
        }
        marcarTomada(uid, cajon);
    }

    // Si hay varias tomas en el mismo cajón, marco la que esté más cerca de la hora actual
    private static void marcarTomada(String uid, int cajon) {
        String hoy = LocalDate.now().format(FECHA);
        LocalTime ahora = LocalTime.now();

        bd.child("tomas").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot tomas) {
                bd.child("registros").child(uid).child(hoy)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot registros) {
                                String mejor = null;
                                long mejorDiferencia = Long.MAX_VALUE;

                                for (DataSnapshot toma : tomas.getChildren()) {
                                    Integer c = toma.child("cajon").getValue(Integer.class);
                                    String hora = toma.child("hora").getValue(String.class);
                                    if (c == null || c != cajon || hora == null) continue;
                                    if ("tomada".equals(registros.child(toma.getKey()).child("estado").getValue(String.class))) continue;

                                    long diferencia = Math.abs(java.time.Duration.between(LocalTime.parse(hora), ahora).toMinutes());
                                    if (diferencia < mejorDiferencia) {
                                        mejorDiferencia = diferencia;
                                        mejor = toma.getKey();
                                    }
                                }

                                if (mejor == null) {
                                    System.out.println("No hay ninguna toma pendiente hoy en el cajón " + cajon);
                                    return;
                                }

                                Map<String, Object> registro = new HashMap<>();
                                registro.put("estado", "tomada");
                                registro.put("horaReal", ahora.format(HORA));
                                registro.put("origen", "pastillero");   // para distinguirlo de cuando se marca desde la App

                                String idToma = mejor;
                                bd.child("registros").child(uid).child(hoy).child(idToma)
                                        .setValue(registro, (error, ref) -> {
                                            if (error == null) {
                                                System.out.println("Toma " + idToma + " del cajón " + cajon + " marcada como tomada");
                                            } else {
                                                System.out.println("Error al guardar: " + error.getMessage());
                                            }
                                        });
                            }

                            @Override
                            public void onCancelled(DatabaseError error) {
                                System.out.println("Error leyendo registros: " + error.getMessage());
                            }
                        });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.out.println("Error leyendo tomas: " + error.getMessage());
            }
        });
    }
    // T069. No escribo nada en Firebase porque la App ya la muestra como olvidada
    private static void revisarOlvidadas() {
        String hoy = LocalDate.now().format(FECHA);
        LocalTime ahora = LocalTime.now();

        bd.child("tomas").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot todas) {
                for (DataSnapshot paciente : todas.getChildren()) {
                    String uid = paciente.getKey();
                    if (!seProcesa(uid)) continue;
                    bd.child("registros").child(uid).child(hoy)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot registros) {
                                    for (DataSnapshot toma : paciente.getChildren()) {
                                        String limite = toma.child("horaLimite").getValue(String.class);
                                        if (limite == null) continue;

                                        LocalTime horaLimite;
                                        try {
                                            horaLimite = LocalTime.parse(limite);
                                        } catch (Exception e) {
                                            continue;
                                        }
                                        if (!ahora.isAfter(horaLimite)) continue;
                                        if (java.time.Duration.between(horaLimite, ahora).toMinutes() > MARGEN_MINUTOS) continue;

                                        String estado = registros.child(toma.getKey()).child("estado").getValue(String.class);
                                        if ("tomada".equals(estado)) continue;

                                        String clave = hoy + "/" + uid + "/" + toma.getKey();
                                        String nombre = toma.child("nombreMedicamento").getValue(String.class);
                                        String horaToma = toma.child("hora").getValue(String.class);

                                        // T071: aviso al móvil de los cuidadores. Se intenta cada minuto
                                        // dentro del margen; solo se envía a los móviles que aún no lo tienen.
                                        avisarCuidadores(clave, uid, toma.getKey(), nombre, horaToma, hoy);

                                        boolean nueva;
                                        synchronized (avisadas) {
                                            nueva = avisadas.add(clave);
                                        }
                                        if (!nueva) continue;

                                        Integer cajon = toma.child("cajon").getValue(Integer.class);
                                        String texto = "Toma olvidada: " + (nombre != null ? nombre : "")
                                                + " (cajon " + cajon + ", " + horaToma + ")";
                                        avisar(uid, texto);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    System.out.println("Error leyendo registros: " + error.getMessage());
                                }
                            });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.out.println("Error leyendo tomas: " + error.getMessage());
            }
        });
    }

    // ---------- T071: notificación FCM de toma olvidada a los cuidadores ----------

    // Las lecturas y envíos son bloqueantes: no se hacen en el hilo de eventos de Firebase
    private static final ExecutorService envios = Executors.newSingleThreadExecutor();
    // Tomas que se están enviando ahora mismo, para no lanzar el mismo envío dos veces
    private static final Set<String> enCurso = new HashSet<>();

    private static void avisarCuidadores(String clave, String uidPaciente, String idToma,
                                         String medicamento, String hora, String fecha) {
        synchronized (enCurso) {
            if (!enCurso.add(clave)) return;
        }
        envios.submit(() -> {
            try {
                enviarACuidadores(uidPaciente, idToma, medicamento, hora, fecha);
            } catch (Exception e) {
                System.out.println("FCM: error con la toma " + clave + ", se reintenta: " + e.getMessage());
            } finally {
                synchronized (enCurso) {
                    enCurso.remove(clave);
                }
            }
        });
    }

    /**
     * Manda el aviso solo a los móviles (usuarios/{cuidador}/fcmTokens) de los cuidadores
     * vinculados al paciente (usuarios/{paciente}/cuidadores).
     * Los envíos correctos se guardan en avisosOlvidadas/{paciente}/{fecha}/{idToma}/{token}
     * y no se repiten; los fallidos se vuelven a intentar en la siguiente revisión.
     */
    private static void enviarACuidadores(String uidPaciente, String idToma, String medicamento,
                                          String hora, String fecha) throws Exception {
        DataSnapshot paciente = leer(bd.child("usuarios").child(uidPaciente));
        DataSnapshot cuidadores = paciente.child("cuidadores");
        if (!cuidadores.hasChildren()) return;

        String rutaEnviados = "avisosOlvidadas/" + uidPaciente + "/" + fecha + "/" + idToma;
        DataSnapshot yaEnviados = leer(bd.child(rutaEnviados));
        String nombrePaciente = texto(paciente.child("nombre").getValue(String.class));

        for (DataSnapshot cuidador : cuidadores.getChildren()) {
            String uidCuidador = cuidador.getKey();
            List<String> pendientes = new ArrayList<>();
            for (DataSnapshot token : leer(bd.child("usuarios").child(uidCuidador).child("fcmTokens")).getChildren()) {
                if (!yaEnviados.hasChild(token.getKey())) pendientes.add(token.getKey());
            }
            if (pendientes.isEmpty()) continue;

            // Solo datos y prioridad alta: la App construye la notificación aunque esté cerrada
            MulticastMessage mensaje = MulticastMessage.builder()
                    .addAllTokens(pendientes)
                    .putData("tipo", "toma_olvidada")
                    .putData("uidPaciente", uidPaciente)
                    .putData("nombrePaciente", nombrePaciente)
                    .putData("idToma", idToma)
                    .putData("medicamento", medicamento != null ? medicamento : "")
                    .putData("hora", hora != null ? hora : "")
                    .putData("fecha", fecha)
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH).build())
                    .build();
            BatchResponse respuesta = FirebaseMessaging.getInstance().sendEachForMulticast(mensaje);

            Map<String, Object> cambios = new HashMap<>();
            for (int i = 0; i < pendientes.size(); i++) {
                String token = pendientes.get(i);
                SendResponse r = respuesta.getResponses().get(i);
                if (r.isSuccessful()) {
                    cambios.put(rutaEnviados + "/" + token, ServerValue.TIMESTAMP);
                } else if (r.getException() != null
                        && r.getException().getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    cambios.put("usuarios/" + uidCuidador + "/fcmTokens/" + token, null);   // móvil que ya no existe
                } else {
                    System.out.println("FCM: fallo enviando a " + uidCuidador + ", se reintenta: "
                            + (r.getException() != null ? r.getException().getMessage() : "desconocido"));
                }
            }
            if (!cambios.isEmpty()) bd.updateChildrenAsync(cambios).get(10, TimeUnit.SECONDS);
            System.out.println("FCM: toma " + idToma + " de " + uidPaciente + " -> "
                    + respuesta.getSuccessCount() + "/" + pendientes.size() + " avisos a " + uidCuidador);
        }
    }

    /** Lectura de una sola vez, esperando el resultado (como mucho 10 s). */
    private static DataSnapshot leer(DatabaseReference ref) throws Exception {
        CompletableFuture<DataSnapshot> resultado = new CompletableFuture<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot datos) {
                resultado.complete(datos);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                resultado.completeExceptionally(error.toException());
            }
        });
        return resultado.get(10, TimeUnit.SECONDS);
    }

    private static void avisar(String uid, String texto) {
        try {
            mqtt.publish("pillsense/" + uid + "/aviso", new MqttMessage(texto.getBytes()));
            System.out.println("Aviso enviado a " + uid + ": " + texto);
        } catch (Exception e) {
            System.out.println("No se pudo enviar el aviso: " + e.getMessage());
        }
    }
    // T066. addValueEventListener se ejecuta al empezar y cada vez que cambian las tomas
    private static void escucharHorarios() {
        bd.child("tomas").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot todas) {
                for (DataSnapshot paciente : todas.getChildren()) {
                    if (!seProcesa(paciente.getKey())) continue;
                    List<String[]> tomas = new ArrayList<>();   // guardo la hora para poder ordenarlas
                    for (DataSnapshot toma : paciente.getChildren()) {
                        String hora = toma.child("hora").getValue(String.class);
                        if (hora == null) continue;
                        Integer cajon = toma.child("cajon").getValue(Integer.class);
                        String json = "{\"cajon\":" + (cajon != null ? cajon : 0)
                                + ",\"hora\":\"" + hora + "\""
                                + ",\"limite\":\"" + texto(toma.child("horaLimite").getValue(String.class)) + "\""
                                + ",\"medicamento\":\"" + texto(toma.child("nombreMedicamento").getValue(String.class)) + "\"}";
                        tomas.add(new String[]{hora, json});
                    }
                    tomas.sort((a, b) -> a[0].compareTo(b[0]));

                    StringBuilder lista = new StringBuilder("[");
                    for (int i = 0; i < tomas.size(); i++) {
                        if (i > 0) lista.append(",");
                        lista.append(tomas.get(i)[1]);
                    }
                    lista.append("]");
                    enviarHorarios(paciente.getKey(), lista.toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.out.println("Error escuchando horarios: " + error.getMessage());
            }
        });
    }

    private static void enviarHorarios(String uid, String json) {
        try {
            MqttMessage mensaje = new MqttMessage(json.getBytes("UTF-8"));
            mensaje.setRetained(true);   // Mosquitto lo guarda y se lo da a la base aunque se conecte más tarde
            mensaje.setQos(1);
            mqtt.publish("pillsense/" + uid + "/horarios", mensaje);
            System.out.println("Horarios enviados a " + uid + ": " + json);
        } catch (Exception e) {
            System.out.println("No se pudieron enviar los horarios: " + e.getMessage());
        }
    }
    private static String texto(String s) {
        return s == null ? "" : s.replace("\"", "'").replace("\\", "");
    }
}
