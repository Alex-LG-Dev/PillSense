package com.equipo5.pillsense;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ArranqueReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        android.util.Log.d("PillSenseAlarma", "Reprogramando alarmas por: " + intent.getAction());
        AlarmasToma.reprogramarGuardadas(ctx);
    }
}