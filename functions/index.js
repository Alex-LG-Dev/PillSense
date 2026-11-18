/**
 * T071 – Aviso de toma olvidada al móvil del cuidador (H016).
 *
 * Cada 5 minutos revisa las tomas de todos los pacientes. Una toma pasa a
 * "olvidada" cuando ya ha pasado su horaLimite de hoy y no hay registro
 * con estado "tomada" (misma regla que HistorialFragment). Entonces se manda
 * un mensaje FCM solo de datos a los móviles de los cuidadores de ese paciente.
 *
 * Para no repetir avisos se marca avisosOlvidadas/{uidPaciente}/{fecha}/{idToma}
 * con una transacción antes de enviar.
 */
const { onSchedule } = require("firebase-functions/v2/scheduler");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp({
  databaseURL: "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app",
});

// Zona horaria en la que se guardan las horas de las tomas ("HH:mm")
const ZONA = "Europe/Madrid";

/** Fecha "yyyy-MM-dd" y hora "HH:mm" de ahora en la zona de la app. */
function ahoraLocal() {
  const partes = Object.fromEntries(
    new Intl.DateTimeFormat("en-GB", {
      timeZone: ZONA, year: "numeric", month: "2-digit", day: "2-digit",
      hour: "2-digit", minute: "2-digit", hourCycle: "h23",
    }).formatToParts(new Date()).map((p) => [p.type, p.value]),
  );
  return {
    fecha: `${partes.year}-${partes.month}-${partes.day}`,
    hora: `${partes.hour}:${partes.minute}`,
  };
}

/** Fecha y hora locales en que se creó una toma, sacadas de su id de Firebase. */
function creacionLocal(id) {
  const ALFABETO = "-0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";
  if (!id || id.length < 8 || !id.startsWith("-")) return null;
  let milis = 0;
  for (let i = 0; i < 8; i++) {
    const v = ALFABETO.indexOf(id.charAt(i));
    if (v < 0) return null;
    milis = milis * 64 + v;
  }
  return new Intl.DateTimeFormat("sv-SE", {
    timeZone: ZONA, year: "numeric", month: "2-digit", day: "2-digit",
    hour: "2-digit", minute: "2-digit", hourCycle: "h23",
  }).format(new Date(milis)); // "yyyy-MM-dd HH:mm"
}

exports.avisarTomasOlvidadas = onSchedule(
  { schedule: "every 5 minutes", timeZone: ZONA, region: "europe-west1" },
  async () => {
    const db = admin.database();
    const { fecha, hora } = ahoraLocal();
    const tomasPorPaciente = (await db.ref("tomas").get()).val() || {};

    for (const [uidPaciente, tomas] of Object.entries(tomasPorPaciente)) {
      for (const [idToma, toma] of Object.entries(tomas || {})) {
        const limite = toma.horaLimite || toma.hora;
        if (!limite || hora < limite) continue; // todavía no está olvidada

        // Una toma creada hoy después de su hora límite no cuenta como olvidada
        const creada = creacionLocal(idToma);
        if (creada && creada > `${fecha} ${limite}`) continue;

        const estado = (await db.ref(`registros/${uidPaciente}/${fecha}/${idToma}/estado`).get()).val();
        if (estado === "tomada") continue;

        // Marca de "ya avisado": solo la primera ejecución consigue ponerla
        const marca = db.ref(`avisosOlvidadas/${uidPaciente}/${fecha}/${idToma}`);
        const { committed } = await marca.transaction((actual) => (actual ? undefined : Date.now()));
        if (!committed) continue;

        await avisarCuidadores(db, uidPaciente, idToma, toma, fecha);
      }
    }
  },
);

async function avisarCuidadores(db, uidPaciente, idToma, toma, fecha) {
  const paciente = (await db.ref(`usuarios/${uidPaciente}`).get()).val() || {};
  const cuidadores = Object.keys(paciente.cuidadores || {});
  if (cuidadores.length === 0) return;

  for (const uidCuidador of cuidadores) {
    const tokens = Object.keys(
      (await db.ref(`usuarios/${uidCuidador}/fcmTokens`).get()).val() || {},
    );
    if (tokens.length === 0) continue;

    const respuesta = await admin.messaging().sendEachForMulticast({
      tokens,
      // Solo datos: la app construye la notificación también en segundo plano
      data: {
        tipo: "toma_olvidada",
        uidPaciente,
        nombrePaciente: String(paciente.nombre || ""),
        idToma,
        medicamento: String(toma.nombreMedicamento || ""),
        hora: String(toma.hora || ""),
        fecha,
      },
      android: { priority: "high" },
    });

    // Borra los tokens que ya no sirven (app desinstalada, etc.)
    respuesta.responses.forEach((r, i) => {
      const codigo = r.error && r.error.code;
      if (codigo === "messaging/registration-token-not-registered" ||
          codigo === "messaging/invalid-registration-token") {
        db.ref(`usuarios/${uidCuidador}/fcmTokens/${tokens[i]}`).remove();
      }
    });
    logger.info(`Toma ${idToma} de ${uidPaciente}: ${respuesta.successCount} avisos a ${uidCuidador}`);
  }
}
