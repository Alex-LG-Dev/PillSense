package com.equipo5.pillsense;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class PestanasAdapter extends FragmentStateAdapter {

    public PestanasAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    // Qué pantalla va en cada posición (0 = la primera pestaña)
    @NonNull
    @Override
    public Fragment createFragment(int posicion) {
        switch (posicion) {
            case 1:  return new PastilleroFragment();
            case 2:  return new PastillasFragment();
            case 3:  return new HistorialFragment();
            default: return new HoyFragment();
        }
    }

    // Cuántas pestañas hay
    @Override
    public int getItemCount() {
        return 4;
    }
}