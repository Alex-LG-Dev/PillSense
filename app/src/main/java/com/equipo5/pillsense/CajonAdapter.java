package com.equipo5.pillsense;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.ArrayList;
import java.util.List;


public class CajonAdapter extends RecyclerView.Adapter<CajonAdapter.CajonViewHolder> {

    private static final int TAMANO_FOTO_DP = 40;   // tamaño de cada foto pequeña
    private static final int SEPARACION_DP = 8;     // espacio entre fotos

    private final List<Cajon> cajones = new ArrayList<>();

    /** Cambia los cajones que se muestran y repinta la lista. */
    public void setCajones(List<Cajon> nuevos) {
        cajones.clear();
        cajones.addAll(nuevos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CajonViewHolder onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
        View vista = LayoutInflater.from(padre.getContext())
                .inflate(R.layout.item_cajon, padre, false);
        return new CajonViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull CajonViewHolder holder, int posicion) {
        Cajon cajon = cajones.get(posicion);
        int total = cajon.getMedicamentos().size();

        holder.tvNumero.setText(String.valueOf(cajon.getNumero()));
        holder.tvMomento.setText(cajon.getMomento());
        holder.tvEstado.setText(total == 0 ? "Vacío"
                : total == 1 ? "1 medicamento"
                  : total + " medicamentos");

        // Una foto pequeña por cada medicamento del cajón
        holder.llFotos.removeAllViews();
        for (Medicamento med : cajon.getMedicamentos()) {
            holder.llFotos.addView(crearFoto(holder.llFotos, med));
        }
    }

    @Override
    public int getItemCount() {
        return cajones.size();
    }

    /** Crea la foto redondeada de un medicamento (o el icono de pastilla si no tiene foto). */
    private ImageView crearFoto(LinearLayout padre, Medicamento med) {
        float densidad = padre.getResources().getDisplayMetrics().density;
        int tamano = Math.round(TAMANO_FOTO_DP * densidad);

        ShapeableImageView iv = new ShapeableImageView(padre.getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(tamano, tamano);
        params.setMarginEnd(Math.round(SEPARACION_DP * densidad));
        iv.setLayoutParams(params);

        // Esquinas redondeadas, como la foto de la lista de Pastillas
        iv.setShapeAppearanceModel(ShapeAppearanceModel.builder()
                .setAllCornerSizes(10 * densidad)
                .build());
        iv.setBackgroundColor(ContextCompat.getColor(padre.getContext(), R.color.fondo_pantalla));
        iv.setContentDescription(med.getNombre());

        // La foto está guardada en Base64 dentro de Realtime Database
        Bitmap foto = FotoUtils.deBase64(med.getImagenBase64());
        if (foto != null) {
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(foto);
        } else {
            int hueco = Math.round(8 * densidad);
            iv.setPadding(hueco, hueco, hueco, hueco);
            iv.setImageResource(R.drawable.ic_pestana_pastillas);
            iv.setColorFilter(ContextCompat.getColor(padre.getContext(), R.color.texto_secundario));
        }
        return iv;
    }

    /** Guarda las vistas de una tarjeta para no buscarlas cada vez. */
    static class CajonViewHolder extends RecyclerView.ViewHolder {
        final TextView tvNumero, tvMomento, tvEstado;
        final LinearLayout llFotos;

        CajonViewHolder(@NonNull View vista) {
            super(vista);
            tvNumero = vista.findViewById(R.id.tvNumero);
            tvMomento = vista.findViewById(R.id.tvMomento);
            tvEstado = vista.findViewById(R.id.tvEstadoCajon);
            llFotos = vista.findViewById(R.id.llFotosMedicamentos);
        }
    }
}