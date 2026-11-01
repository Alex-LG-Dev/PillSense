package com.equipo5.pillsense;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Pantalla para añadir un medicamento. Se guarda en medicamentos/{uid}/{id} de Realtime Database. */
public class MedicamentoActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "idMedicamento";

    private static final String DB_URL =
            "https://pillsense-d14cb-default-rtdb.europe-west1.firebasedatabase.app/";

    // Medicamentos del usuario que ha iniciado sesión
    private DatabaseReference refMedicamentos;

    private EditText etNombre;
    private EditText etDosis;

    private Button btnGuardar;

    // La foto en Base64 (la rellenaremos en el paso 4)
    private String imagenBase64 = "";

    private String idEditando;
    private ImageView ivFoto;

    // Abre la cámara y devuelve la foto (en pequeño)
    private final ActivityResultLauncher<Void> lanzarCamara =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), foto -> {
                if (foto != null) ponerFoto(foto);
            });

    // Abre el selector de fotos de Android (no necesita permisos)
    private final ActivityResultLauncher<PickVisualMediaRequest> lanzarGaleria =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) ponerFoto(FotoUtils.leerDeGaleria(this, uri));
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BarraAcciones.prepararVentana(this);
        setContentView(R.layout.activity_medicamento);
        // Si nos pasan un id, estamos editando; si no, es uno nuevo
        idEditando = getIntent().getStringExtra(EXTRA_ID);
        BarraAcciones.configurar(this,
                idEditando == null ? R.string.med_titulo_nuevo : R.string.med_titulo_editar,
                true, 0);

        // Sin sesión no sabemos de quién es el medicamento
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario == null) {
            Toast.makeText(this, R.string.med_sin_sesion, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        refMedicamentos = FirebaseDatabase.getInstance(DB_URL)
                .getReference("medicamentos")
                .child(usuario.getUid());

        etNombre = findViewById(R.id.etNombreMed);
        etDosis = findViewById(R.id.etDosisMed);
        btnGuardar = findViewById(R.id.btnGuardarMed);
        ivFoto = findViewById(R.id.ivFotoMed);

        findViewById(R.id.btnCamara).setOnClickListener(v -> lanzarCamara.launch(null));
        findViewById(R.id.btnGaleria).setOnClickListener(v ->
                lanzarGaleria.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        btnGuardar.setOnClickListener(v -> guardar());
        if (idEditando != null) {
            cargarMedicamento();

            // El botón de borrar solo tiene sentido si el medicamento ya existe
            Button btnBorrar = findViewById(R.id.btnBorrarMed);
            btnBorrar.setVisibility(View.VISIBLE);
            btnBorrar.setOnClickListener(v -> confirmarBorrado());
        }
    }

    private void guardar() {
        String nombre = etNombre.getText().toString().trim();
        String dosis = etDosis.getText().toString().trim();

        // Comprobamos que no falte nada
        if (nombre.isEmpty()) {
            etNombre.setError(getString(R.string.med_error_nombre));
            etNombre.requestFocus();
            return;
        }
        if (dosis.isEmpty()) {
            etDosis.setError(getString(R.string.med_error_dosis));
            etDosis.requestFocus();
            return;
        }

        // push() crea un id único para el medicamento nuevo
        // Si editamos usamos el mismo id (se sobrescribe); si es nuevo, push() crea uno
        String id = (idEditando != null) ? idEditando : refMedicamentos.push().getKey();
        Medicamento med = new Medicamento(id, nombre, dosis, imagenBase64);

        btnGuardar.setEnabled(false); // evita guardarlo dos veces si se pulsa rápido
        refMedicamentos.child(id).setValue(med)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, R.string.med_guardado, Toast.LENGTH_SHORT).show();
                    finish(); // volvemos a la lista
                })
                .addOnFailureListener(e -> {
                    btnGuardar.setEnabled(true);
                    Toast.makeText(this, getString(R.string.med_error_guardar, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }


    /** Guarda la foto elegida en Base64 y la enseña en la tarjeta. */
    private void ponerFoto(Bitmap foto) {
        if (foto == null) {
            Toast.makeText(this, R.string.med_error_foto, Toast.LENGTH_SHORT).show();
            return;
        }
        imagenBase64 = FotoUtils.aBase64(foto);
        mostrarFoto(foto);
    }

    /** Quita el icono gris de ejemplo y pone la foto ocupando toda la tarjeta. */
    private void mostrarFoto(Bitmap foto) {
        ivFoto.setImageTintList(null);
        ivFoto.setPadding(0, 0, 0, 0);
        ivFoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivFoto.setImageBitmap(foto);
    }
    /** Lee el medicamento de Firebase y rellena el formulario con sus datos. */
    private void cargarMedicamento() {
        refMedicamentos.child(idEditando).get()
                .addOnSuccessListener(datos -> {
                    Medicamento med = datos.getValue(Medicamento.class);
                    if (med == null) {
                        Toast.makeText(this, R.string.med_error_cargar, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    etNombre.setText(med.getNombre());
                    etDosis.setText(med.getDosis());


                    // Conservamos la foto que ya tenía (si no se cambia, se vuelve a guardar la misma)
                    imagenBase64 = med.getImagenBase64() != null ? med.getImagenBase64() : "";
                    Bitmap foto = FotoUtils.deBase64(imagenBase64);
                    if (foto != null) mostrarFoto(foto);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.med_error_cargar, Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /** Pregunta antes de borrar, porque no se puede deshacer. */
    private void confirmarBorrado() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.med_borrar_titulo)
                .setMessage(getString(R.string.med_borrar_mensaje, etNombre.getText().toString().trim()))
                .setNegativeButton(R.string.med_cancelar, null)
                .setPositiveButton(R.string.med_borrar_si, (dialogo, boton) -> borrar())
                .show();
    }

    /** Borra el medicamento de Firebase y vuelve a la lista. */
    private void borrar() {
        refMedicamentos.child(idEditando).removeValue()
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, R.string.med_borrado, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.med_error_borrar, e.getMessage()),
                                Toast.LENGTH_LONG).show());
    }
}