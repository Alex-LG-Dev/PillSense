# Barra de acciones de PillSense

Barra superior con el nombre de la app y el menú de tres puntos (H002 / H003).
Se pone en cualquier pantalla con **3 pasos** y **sin tocar colores ni temas**.

## Cómo ponerla en una pantalla

**1. Layout.** El contenedor raíz debe tener `android:id="@+id/main"` y orientación vertical. Pon la barra arriba del todo:

```xml
<LinearLayout android:id="@+id/main" android:orientation="vertical" ...>
    <include layout="@layout/app_bar" />
    <!-- el resto de la pantalla -->
</LinearLayout>
```

**2. Activity.** Antes de `setContentView` prepara la ventana y después configura la barra:

```java
BarraAcciones.prepararVentana(this);
setContentView(R.layout.activity_mi_pantalla);

// pantalla principal: título = nombre de la app, con menú
MaterialToolbar barra = BarraAcciones.configurar(this, R.string.app_name, false, R.menu.menu_principal);
barra.setOnMenuItemClickListener(item -> {
    if (item.getItemId() == R.id.accion_acerca_de) {
        startActivity(new Intent(this, AcercaDeActivity.class));
        return true;
    }
    return false;
});

// pantalla secundaria: con flecha de volver y sin menú
BarraAcciones.configurar(this, R.string.mi_titulo, true, 0);
```

**3. Manifest.** Registra la pantalla como `<activity android:name=".MiPantalla" />` (ya está `AcercaDeActivity`).

## Qué hace por ti

- Barra azul PillSense (`azul_pillsense`) con título y iconos en blanco.
- Zona de la barra de estado (hora, batería, notificaciones) en azul noche (`azul_noche`) con iconos blancos, para que no se mezclen con la barra.
- Márgenes del sistema (notch y botones de navegación) ya resueltos.
- Fuente Atkinson Hyperlegible y tamaños del equipo (`texto_boton`).
- Funciona desde **minSdk 24**.

## Archivos

| Archivo | Para qué |
| --- | --- |
| `BarraAcciones.java` | Ayudante: `prepararVentana(...)` y `configurar(...)` |
| `res/layout/app_bar.xml` | La barra (se incluye con `<include>`) |
| `res/menu/menu_principal.xml` | Menú de tres puntos (ahora solo "Acerca de") |
| `res/values/styles_barra.xml` | Estilo del título |
| `AcercaDeActivity.java` + `activity_acerca_de.xml` | Pantalla Acerca de |
| `res/values/strings_acerca_de.xml` | Textos de Acerca de y nombres del equipo (`integrantes_equipo`) |

Para añadir otra opción al menú, añade un `<item>` en `menu_principal.xml` y trátala en el `setOnMenuItemClickListener`.
