# Plan de mejoras UI — Gravity Defied Android

> Alcance: paquete `Menu/`, `GDActivity` (capa de presentación), recursos (`res/`).
> El paquete `Game/` y la lógica de física/motor gráfico **no se tocan**.

---

## Contexto del estado actual

| Aspecto | Estado actual |
|---|---|
| Tema | `android:Theme.Holo.Light.NoActionBar` (Holo, 2011) |
| Colores | Literales hex hardcodeados en Java y XML — no existe `colors.xml` |
| Layouts | Toda la jerarquía se construye en Java dentro de `GDActivity.onCreate()` |
| Touch feedback | Casco giratorio custom; sin ripple Material |
| `minSdk` / `targetSdk` | 21 / 36 — Material3 disponible |
| APIs deprecadas | `getColorStateList(int)`, `Html.fromHtml(String)`, tema Holo |
| Bug de rendimiento | `MenuHelmetView.onDraw()` llama `invalidate()` siempre, incluso oculto |

---

## Nivel 1 — Quick wins (bajo riesgo, alto impacto)

### 1.1 Centralizar colores en `colors.xml`

Crear `app/src/main/res/values/colors.xml` con la paleta completa:

```xml
<!-- Fondo general -->
<color name="menu_background">#FFFFFFFF</color>
<!-- Texto primario -->
<color name="menu_text_primary">#FF000000</color>
<!-- Texto deshabilitado -->
<color name="menu_text_disabled">#FF999999</color>
<!-- Color de selección/highlight -->
<color name="menu_highlight">#FF00A000</color>
<!-- Fondo teclado numérico -->
<color name="keyboard_background">#C6FFFFFF</color>
```

Reemplazar en código Java:
- `GDActivity`: `0xffffffff`, `0x00ffffff`, `0xc6ffffff`, `0xff000000`
- `TextMenuElement`: `TEXT_COLOR = 0xff000000`
- `ActionMenuElement`: `DISABLED_COLOR = 0xff999999`
- `menu_item_color.xml`: `#00a000`, `#000000`

**Archivos afectados:**
- `res/values/colors.xml` (crear)
- `res/drawable/menu_item_color.xml`
- `GDActivity.java` (~líneas 186, 204, 207, 214, 249)
- `TextMenuElement.java` (constante `TEXT_COLOR`)
- `ActionMenuElement.java` (constante `DISABLED_COLOR`)

---

### 1.2 Migrar tema a Material3

Reemplazar el tema Holo heredado por Material3. Afecta automáticamente `AlertDialog`, barras del sistema, y colores de sistema.

**`app/build.gradle`** — añadir dependencia:
```groovy
implementation 'com.google.android.material:material:1.12.0'
```

**`res/values/styles.xml`**:
```xml
<style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
    <item name="colorPrimary">@color/menu_highlight</item>
    <item name="colorOnPrimary">@color/menu_background</item>
    <item name="colorSurface">@color/menu_background</item>
    <item name="colorOnSurface">@color/menu_text_primary</item>
    <item name="android:windowContentOverlay">@null</item>
</style>
```

**`res/values-v14/styles.xml`** — se puede eliminar (ya cubierto por Material3).

**Archivos afectados:**
- `app/build.gradle`
- `res/values/styles.xml`
- `res/values-v14/styles.xml` (eliminar o vaciar)
- `AndroidManifest.xml` (verificar que usa `@style/AppTheme`)

---

### 1.3 Fix del bug de redibujado en `MenuHelmetView`

**Archivo:** `Menu/Views/MenuHelmetView.java`, `onDraw()` línea 57.

```java
// ANTES — invalida siempre, bucle infinito de redibujado
@Override
public void onDraw(Canvas canvas) {
    canvas.save();
    canvas.scale(Global.density, Global.density);
    drawHelmet(canvas);
    canvas.restore();
    invalidate();  // ← BUG: siempre, aunque show=false
}

// DESPUÉS — solo invalida cuando el casco está visible
@Override
public void onDraw(Canvas canvas) {
    canvas.save();
    canvas.scale(Global.density, Global.density);
    drawHelmet(canvas);
    canvas.restore();
    if (show) {
        invalidate();
    }
}
```

Impacto: elimina un bucle de redibujado permanente en toda la pantalla de menú. Sin cambio visual.

---

### 1.4 Ripple Material en filas clicables

Añadir feedback táctil estándar de Material a `ClickableMenuElement`.

**Archivo:** `Menu/ClickableMenuElement.java`, método `createAllViews()`.

```java
// Añadir tras construir layout:
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    TypedValue outValue = new TypedValue();
    context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
    layout.setForeground(context.getDrawable(outValue.resourceId));
}
layout.setClickable(true);
```

O bien crear un drawable `res/drawable/menu_item_ripple.xml`:
```xml
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="?attr/colorControlHighlight">
    <item android:id="@android:id/mask">
        <color android:color="#FF000000" />
    </item>
</ripple>
```
Y asignarlo con `layout.setBackground(...)`.

---

### 1.5 Tipografía y espaciado en recursos

Mover los números mágicos a `res/values/dimens.xml`:

```xml
<!-- Menú principal -->
<dimen name="menu_text_size">20sp</dimen>
<dimen name="menu_text_size_small">15sp</dimen>
<dimen name="menu_title_text_size">28sp</dimen>
<dimen name="menu_item_padding_vertical">5dp</dimen>
<dimen name="menu_layout_padding_horizontal">30dp</dimen>
<dimen name="menu_layout_padding_bottom">15dp</dimen>

<!-- Teclado numérico -->
<dimen name="keyboard_button_height_phone">55dp</dimen>
<dimen name="keyboard_button_height_tablet">85dp</dimen>
```

Reemplazar las constantes Java (`TEXT_SIZE`, `PADDING_TOP`, `LAYOUT_LEFT_PADDING`, etc.) por `context.getResources().getDimension(R.dimen.*)`.

---

## Nivel 2 — Modernización media (riesgo moderado)

### 2.1 Migrar APIs deprecadas a AndroidX

| API actual | Reemplazo |
|---|---|
| `getResources().getColorStateList(int)` | `ContextCompat.getColorStateList(ctx, int)` |
| `Html.fromHtml(String)` | `HtmlCompat.fromHtml(String, HtmlCompat.FROM_HTML_MODE_LEGACY)` |
| `getColor(int)` sin contexto | `ContextCompat.getColor(ctx, int)` |

**Archivos afectados:**
- `ClickableMenuElement.java` (`defaultColorStateList()`)
- `TextMenuElement.java` (`setText()`)
- `GDActivity.java` (línea ~299)
- Cualquier otro uso de `Html.fromHtml`

Añadir a `build.gradle` si no están:
```groovy
implementation 'androidx.core:core:1.13.1'
```

---

### 2.2 Limpiar `activity_gdtr.xml` o darle uso

El archivo `res/layout/activity_gdtr.xml` está vacío y no se usa (`GDActivity` hace `setContentView(frame)` con un `FrameLayout` construido en Java). Opciones:

- **Opción A (mínima):** Eliminar el archivo si `GDTRActivity` ya no existe o no se usa.
- **Opción B (preferida):** Extraer gradualmente la jerarquía de menú a un layout XML inflable, empezando por el `menuLayout` (título + scroll), para poder previsualizar y tematizar en el editor.

Estructura objetivo del layout XML:
```xml
<!-- res/layout/menu_main.xml -->
<LinearLayout vertical>
    <org.happysanta.gd.Menu.Views.MenuTitleLinearLayout
        android:id="@+id/titleLayout" />
    <org.happysanta.gd.Menu.Views.ObservableScrollView
        android:id="@+id/scrollView" />
</LinearLayout>
```

---

### 2.3 Edge-to-edge e insets correctos

Ya existe un `setOnApplyWindowInsetsListener` en `GDActivity`. Verificar y refinar:

```java
// En GDActivity, asegurar que se consume el inset de barras del sistema
ViewCompat.setOnApplyWindowInsetsListener(frame, (v, insets) -> {
    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
    // Aplicar solo al contenido de menú, no al GameView
    menuLayout.setPadding(0, systemBars.top, 0, 0);
    return WindowInsetsCompat.CONSUMED;
});
```

Requiere:
```groovy
implementation 'androidx.activity:activity:1.9.0'
```

---

## Nivel 3 — Rediseño mayor (mayor esfuerzo, solo si se quiere)

> Estos puntos son opcionales y tienen mayor riesgo. Se documentan para referencia.

### 3.1 Reemplazar el framework de menú custom por RecyclerView

El sistema actual usa `Vector` crudo + `ScrollView` + highlight manual. `RecyclerView` daría:
- Reciclado de vistas (importante en listas de niveles largas)
- `DiffUtil` para actualizaciones eficientes
- `ItemDecoration` para separadores
- Compatibilidad con `RecyclerView.Adapter` tipado

**Riesgo:** La lógica de navegación por teclado numérico (KeyboardController → `performAction(KEY_UP/DOWN/FIRE)`) está acoplada a `MenuScreen.selectedIndex`. Necesitaría reasignarse al estado del Adapter.

### 3.2 Reemplazar el menú con Jetpack Compose

Solo la capa de menú (`MenuScreen` y sus vistas), dejando `GameView` (Canvas) intacto. Interoperabilidad via `ComposeView` dentro del `FrameLayout` existente.

Ventajas: animaciones declarativas, dark mode trivial, preview en Android Studio.
Riesgo: requiere Kotlin (actualmente todo Java) o interop Java↔Compose más complejo.

---

## Archivos que NO se tocan

- Todo `Game/` (physics, GameView, FPMath, Bitmap, etc.)
- `Levels/Loader`, `Levels/Reader`, `Levels/Level` (decompilados)
- `GDApplication.java` (ACRA)
- Clases con identificadores ofuscados (`k`, `_avJ()`, `_dovI()`, etc.)
- `assets/levels.mrg` y packs de niveles

---

## Orden de implementación recomendado

```
1.3 Fix invalidate()       — sin riesgo, impacto rendimiento
1.1 colors.xml             — base para todo lo demás
1.2 Tema Material3         — visual inmediato
1.5 dimens.xml             — limpieza de constantes
1.4 Ripple en filas        — touch feedback
2.1 APIs deprecadas        — deuda técnica
2.2 Layout XML             — mantenibilidad
2.3 Edge-to-edge           — compatibilidad moderna
--- (pausa, evaluar) ---
3.1 RecyclerView           — si se necesita rendimiento en listas
3.2 Compose                — si se decide reescribir la UI
```
