package com.equipo5.pillsense;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Pasa las fotos de los medicamentos a texto Base64 (para Realtime Database) y al revés. */
public final class FotoUtils {

    // Lado más largo de la foto guardada, en píxeles (pequeña para que el Base64 no pese mucho)
    private static final int LADO_MAX = 400;
    private static final int CALIDAD_JPEG = 70;

    private FotoUtils() { }

    /** Foto → texto Base64. */
    public static String aBase64(Bitmap foto) {
        Bitmap pequena = reducir(foto);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        pequena.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida);
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP);
    }

    /** Texto Base64 → foto. Devuelve null si no hay foto. */
    public static Bitmap deBase64(String texto) {
        if (texto == null || texto.isEmpty()) return null;
        try {
            byte[] bytes = Base64.decode(texto, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Lee una foto elegida de la galería, ya reducida. Devuelve null si falla. */
    public static Bitmap leerDeGaleria(Context context, Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Android 9+: ImageDecoder ya gira la foto si hace falta y la reduce al leerla
                ImageDecoder.Source origen = ImageDecoder.createSource(context.getContentResolver(), uri);
                return ImageDecoder.decodeBitmap(origen, (decoder, info, src) -> {
                    int ancho = info.getSize().getWidth();
                    int alto = info.getSize().getHeight();
                    int lado = Math.max(ancho, alto);
                    if (lado > LADO_MAX) {
                        float k = (float) LADO_MAX / lado;
                        decoder.setTargetSize(Math.round(ancho * k), Math.round(alto * k));
                    }
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
            }
            // Android 7 y 8
            try (InputStream entrada = context.getContentResolver().openInputStream(uri)) {
                return reducir(BitmapFactory.decodeStream(entrada));
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static Bitmap reducir(Bitmap foto) {
        if (foto == null) return null;
        int lado = Math.max(foto.getWidth(), foto.getHeight());
        if (lado <= LADO_MAX) return foto;
        float k = (float) LADO_MAX / lado;
        return Bitmap.createScaledBitmap(foto,
                Math.round(foto.getWidth() * k), Math.round(foto.getHeight() * k), true);
    }
}