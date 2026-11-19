package com.equipo5.pillsense;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Adapter de la lista "Tomas de hoy": cada fila con su hora, nombre y estado con su color. */
public class TomaAdapter extends RecyclerView.Adapter<TomaAdapter.TomaViewHolder> {

    /** Lo que hace el fragment cuando se toca una fila o su icono. */
    public interface OnTomaListener {
        void alTocarIcono(Toma toma, boolean marcarComoTomada);
        void alTocarFila(Toma toma);
    }

    private final List<Toma> tomas = new ArrayList<>();
    private final Set<String> tomadasHoy = new HashSet<>();
    private final OnTomaListener listener;

    public TomaAdapter(OnTomaListener listener) {
        this.listener = listener;
    }

    /** Se llama cada vez que Firebase avisa de un cambio: cambia los datos y repinta la lista. */
    public void actualizar(List<Toma> nuevasTomas, Set<String> nuevasTomadas) {
        tomas.clear();
        tomas.addAll(nuevasTomas);
        tomadasHoy.clear();
        tomadasHoy.addAll(nuevasTomadas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TomaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View fila = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_toma, parent, false);
        return new TomaViewHolder(fila);
    }

    @Override
    public void onBindViewHolder(@NonNull TomaViewHolder holder, int position) {
        Context ctx = holder.itemView.getContext();
        Toma toma = tomas.get(position);

        holder.tvHora.setText(toma.getHora());
        holder.tvNombre.setText(toma.getNombreMedicamento());
        holder.tvEstado.setText(ctx.getString(R.string.toma_detalle,
                toma.getCajon(), toma.getHoraLimite()));

        // Estado de la toma: tomada (verde), olvidada (azul) o pendiente (gris)
        String ahora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        boolean tomada = toma.getId() != null && tomadasHoy.contains(toma.getId());
        boolean olvidada = !tomada && toma.getHoraLimite() != null
                && ahora.compareTo(toma.getHoraLimite()) > 0;

        if (tomada) {
            holder.ivEstado.setImageResource(R.drawable.ic_toma_hecha);
            holder.ivEstado.setColorFilter(ContextCompat.getColor(ctx, R.color.verde_menta));
            holder.ivEstado.setContentDescription("Tomada");
        } else if (olvidada) {
            holder.ivEstado.setImageResource(R.drawable.ic_toma_olvidada);
            holder.ivEstado.setColorFilter(ContextCompat.getColor(ctx, R.color.toma_olvidada));
            holder.ivEstado.setContentDescription("Olvidada");
        } else {
            holder.ivEstado.setImageResource(R.drawable.ic_toma_pendiente);
            holder.ivEstado.setColorFilter(ContextCompat.getColor(ctx, R.color.pestana_inactiva));
            holder.ivEstado.setContentDescription("Pendiente");
        }

        // Tocando el icono se marca como tomada, o se desmarca si fue sin querer
        holder.ivEstado.setOnClickListener(v -> listener.alTocarIcono(toma, !tomada));

        // Tocando la fila se abre el formulario para editar la toma
        holder.itemView.setOnClickListener(v -> listener.alTocarFila(toma));

        // La última fila no lleva línea de separación
        holder.separador.setVisibility(
                position == tomas.size() - 1 ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return tomas.size();
    }

    /** Guarda las vistas de una fila para no buscarlas cada vez. */
    static class TomaViewHolder extends RecyclerView.ViewHolder {
        final TextView tvHora, tvNombre, tvEstado;
        final ImageView ivEstado;
        final View separador;

        TomaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHora = itemView.findViewById(R.id.tvHora);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            ivEstado = itemView.findViewById(R.id.ivEstado);
            separador = itemView.findViewById(R.id.separador);
        }
    }
}