# Propuesta: Modo oscuro para Gravity Defied

Fecha: 2026-05-23
Branch: `claude/dark-mode-proposal-dRLsQ`

## 1. Objetivo

Permitir que la interfaz de menús, títulos, fondo y panel de teclado se pinten
en variantes oscuras, sin retocar ninguno de los PNG del juego (sprites,
botones del keypad, íconos del navegador de mods, etc.).

El game canvas (`Game/GameView`) queda **fuera de alcance**: lo dibuja el motor
físico decompilado del J2ME original — tocarlo rompe la paridad con el juego
2004 que `CLAUDE.md` exige preservar.

## 2. Análisis del estado actual

### 2.1 Capa de color ya está casi centralizada

Todos los colores de UI viven en `app/src/main/res/values/colors.xml`:

| Token              | Valor       | Dónde se usa                                         |
| ------------------ | ----------- | ---------------------------------------------------- |
| `menu_background`  | `#FFFFFFFF` | `frame` (fondo del menú) + `windowBackground`        |
| `menu_text_primary`| `#FF000000` | Textos de menú, opciones, name input                 |
| `menu_text_disabled`| `#FF999999`| Items deshabilitados (`ActionMenuElement`)           |
| `menu_highlight`   | `#FF00A000` | Color verde de selección/pressed + `colorAccent`     |
| `keyboard_background`| `#C6FFFFFF` | Fondo semi-transparente del panel 3×3              |
| `keyboard_button_text`| `#FF000000` | Números 1-9 del keypad                            |
| `title_text`       | `#FF000000` | Título de cada `MenuScreen`                          |

Esto es lo que hace la propuesta viable: cambiando estos 7 tokens cambia el
~95% de la UI sin tocar Java.

### 2.2 Puntos donde hay colores hardcodeados

Sólo hay **un** color hardcodeado en código de menú:

- `app/src/main/java/cl/figonzal/gravitydefied/Menu/LevelsMenuScreen.java:40`
  ```java
  protected final static int ERROR_COLOR = 0xff777777;
  ```
  Se usa para el texto de error cuando falla la descarga de mods. Debe
  migrar a `R.color.menu_text_error`.

El resto de referencias a color en Java ya pasan por `R.color.*`:
- `GDActivity.java:128,142,177,182`
- `Menu/SimpleMenuElement.java:65`
- `Menu/TextMenuElement.java:40`
- `Menu/NameInputMenuScreen.java:72`
- `Menu/ActionMenuElement.java:134`
- `Menu/LevelMenuElement.java:77` (vía `menu_item_color.xml`)

### 2.3 Tema actual

```xml
<style name="AppTheme" parent="android:Theme.Material.Light.NoActionBar">
```

`Material.Light` no es un blocker — Android selecciona automáticamente el
qualifier `-night` cuando el sistema está en modo oscuro, sin necesidad de
cambiar a `DayNight` (que pertenece a AppCompat, librería que el proyecto
**no** usa y no debería sumar para mantener el peso).

### 2.4 PNGs que conviven con el fondo oscuro

Aunque la consigna es no retocar PNG, hay íconos pequeños dentro de menús
que sí se verían encima del nuevo fondo oscuro:

| Drawable                         | Contexto                              | Solución sin tocar PNG       |
| -------------------------------- | ------------------------------------- | ---------------------------- |
| `ic_sort_up/down`                | Botón de ordenar en `DownloadLevels`  | `setColorFilter` runtime     |
| `ic_downloaded`, `ic_installed`  | Estado del nivel en `LevelMenuElement`| `setColorFilter` runtime     |
| `levels_wheel0..2`               | Rueda en `MenuHelmetView` (mod list)  | `setColorFilter` runtime     |
| `ic_menu_up/down`                | Botón de menú **dentro** del juego    | Sin cambio (overlay sobre canvas) |
| `btn_b/br/n/r_up/down.9.png`     | Keypad 3×3 **dentro** del juego       | Sin cambio (overlay sobre canvas) |
| `gd.png`, `codebrew.png`, sprites| Game canvas                           | Sin cambio                   |

Los íconos del top de la tabla son tintables vía `ImageView.setColorFilter(
new PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN))` — la imagen se
queda igual en disco, sólo cambia el color cuando se compone en pantalla.

## 3. Estrategia recomendada

**Opción híbrida**: seguir el modo del sistema *y* exponer una opción
manual "System / Light / Dark" en el menú Options (siguiendo el patrón
de `OptionsMenuElement` que ya usa Perspective, Shadows, Vibrate, etc.).

### 3.1 ¿Por qué híbrida y no sólo automática?

- **Auto vía `values-night/`** es la solución más limpia: Android conmuta
  los recursos por sí solo cuando el usuario activa modo oscuro en el OS.
  Sin código.
- Pero algunos usuarios quieren forzar un modo distinto al sistema. Como
  el proyecto no usa AppCompat (`AppCompatDelegate.setDefaultNightMode`
  no está disponible), el override manual se hace por `Configuration`
  en `attachBaseContext()`.
- El usuario ya tiene infraestructura de reinicio (`doRestartApp()` con
  `AlarmManager` en `GDActivity.java:1077`) que evita lidiar con la
  recreación parcial — re-arrancar la activity es coherente con cómo
  ya se aplica `Full Reset`.

### 3.2 Flujo

1. Usuario abre Options → ve "Theme" con valores `System`, `Light`, `Dark`.
2. Al cambiar, `Settings.setThemeMode(...)` persiste el valor.
3. `doRestartApp()` reinicia la activity.
4. `attachBaseContext()` lee la preferencia y, si es Light/Dark explícito,
   sobreescribe `Configuration.uiMode` antes de inflar recursos.
5. Android resuelve `values-night/` o `values/` según corresponde.

## 4. Paleta propuesta

Manteniendo el feel "minimalista J2ME" del original:

### Light (sin cambio respecto al actual)

```xml
<color name="menu_background">#FFFFFFFF</color>
<color name="menu_text_primary">#FF000000</color>
<color name="menu_text_disabled">#FF999999</color>
<color name="menu_highlight">#FF00A000</color>
<color name="keyboard_background">#C6FFFFFF</color>
<color name="keyboard_button_text">#FF000000</color>
<color name="title_text">#FF000000</color>
<color name="menu_text_error">#FF777777</color>      <!-- nuevo (migra ERROR_COLOR) -->
<color name="menu_icon_tint">#FF000000</color>       <!-- nuevo (tint para íconos PNG en menús) -->
```

### Dark (`values-night/colors.xml`)

```xml
<color name="menu_background">#FF121212</color>       <!-- gris muy oscuro, no negro puro -->
<color name="menu_text_primary">#FFEDEDED</color>     <!-- blanco apagado, menos fatiga -->
<color name="menu_text_disabled">#FF6E6E6E</color>
<color name="menu_highlight">#FF66D266</color>        <!-- verde más luminoso para contraste AA -->
<color name="keyboard_background">#C6121212</color>   <!-- mismo alpha, base oscura -->
<color name="keyboard_button_text">#FFEDEDED</color>
<color name="title_text">#FFEDEDED</color>
<color name="menu_text_error">#FFB87575</color>       <!-- rojizo apagado, no agresivo -->
<color name="menu_icon_tint">#FFEDEDED</color>
```

Criterios:
- Fondo `#121212` y texto `#EDEDED` cumplen contraste WCAG AA (~14:1).
- `menu_highlight` sube de `#00A000` (4.2:1 sobre blanco) a `#66D266`
  (~6.5:1 sobre `#121212`) para mantenerse visible.
- `ic_launcher_background` queda fuera — el icono del launcher debería
  verse igual en cualquier launcher.

## 5. Cambios concretos por archivo

### 5.1 Recursos nuevos

- `app/src/main/res/values-night/colors.xml` *(nuevo)* — paleta dark de §4.
- `app/src/main/res/values-night/styles.xml` *(nuevo)*:
  ```xml
  <style name="AppTheme" parent="android:Theme.Material.NoActionBar">
      <item name="android:colorPrimary">@color/menu_highlight</item>
      <item name="android:colorPrimaryDark">@color/menu_highlight</item>
      <item name="android:colorAccent">@color/menu_highlight</item>
      <item name="android:windowBackground">@color/menu_background</item>
      <item name="android:windowContentOverlay">@null</item>
  </style>
  ```
  Nota: `Material` (no `Material.Light`) hace que widgets nativos no
  tematizados también se vean coherentes.

### 5.2 Recursos existentes a modificar

- `app/src/main/res/values/colors.xml` — agregar `menu_text_error` y
  `menu_icon_tint`.
- `app/src/main/res/values/strings.xml` — agregar:
  ```xml
  <string name="theme">Theme</string>
  <string name="theme_system">System</string>
  <string name="theme_light">Light</string>
  <string name="theme_dark">Dark</string>
  ```

### 5.3 Java

- `Settings.java`: agregar `THEME_MODE` (int: 0=system, 1=light, 2=dark),
  con `getThemeMode()` / `setThemeMode()` siguiendo el patrón de
  `InputOption`. Incluirlo en `resetAll()`.
- `GDActivity.java`: override de `attachBaseContext(Context base)` que
  lee `Settings.getThemeMode()` y, si es light/dark, construye un nuevo
  `Configuration` con `uiMode` forzado:
  ```java
  @Override
  protected void attachBaseContext(Context base) {
      int mode = base.getSharedPreferences("GDSettings", MODE_PRIVATE)
              .getInt("theme_mode", 0);
      if (mode == 0) {
          super.attachBaseContext(base);
          return;
      }
      Configuration cfg = new Configuration(base.getResources().getConfiguration());
      int nightFlag = (mode == 2)
              ? Configuration.UI_MODE_NIGHT_YES
              : Configuration.UI_MODE_NIGHT_NO;
      cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightFlag;
      super.attachBaseContext(base.createConfigurationContext(cfg));
  }
  ```
  (Nota: `Settings.preferences` se inicializa en static block que depende
  de `GDActivity.shared` — por eso el `attachBaseContext` lee de
  `SharedPreferences` directo, evitando dependencia circular.)
- `Menu/Menu.java`: agregar `themeOptionItem = new OptionsMenuElement(
  getString(R.string.theme), Settings.getThemeMode(), this, themeStrings,
  false, optionsMenu);` con `themeStrings = { theme_system, theme_light,
  theme_dark }`. En el handler de selección guardar y llamar a
  `gd.doRestartApp()` (hoy es `private`, hay que abrirlo a package o
  exponer un método público `restartForThemeChange()`).
- `Menu/LevelsMenuScreen.java:40`: cambiar
  ```java
  protected final static int ERROR_COLOR = 0xff777777;
  ```
  a leer `R.color.menu_text_error` en el sitio de uso (línea 78).

### 5.4 Tints runtime de íconos PNG

En los puntos donde se hace `setImageResource` sobre íconos de menú:

- `LevelMenuElement.java:124,127,159,185` (`ic_downloaded`, `ic_installed`)
- `DownloadLevelsMenuScreen.java:44` (`ic_sort`)
- `MenuHelmetView` (rueda)

Agregar después del `setImageResource`:
```java
int tint = ctx.getResources().getColor(R.color.menu_icon_tint);
img.setColorFilter(tint, android.graphics.PorterDuff.Mode.SRC_IN);
```

Esto reutiliza el mismo PNG en blanco y negro, pero lo pinta del color
correcto según el tema activo. Sigue cumpliendo "no retocar PNG".

## 6. Lo que queda igual

- Todos los assets `.png` y `.9.png` — sin cambios.
- `Game/GameView`, `Game/Physics`, `Game/Bitmap` — sin cambios.
- Keypad 3×3 del juego — los botones (`btn_b/br/n/r_up/down.9.png`) son
  un overlay sobre el game canvas, no sobre el menú; la consigna del
  CLAUDE.md de no tocar el feel del original aplica fuerte aquí.
- Comportamiento del menú, navegación, persistencia de highscores, mod
  browser.

## 7. Riesgos y mitigaciones

| Riesgo                                                          | Mitigación                                                        |
| --------------------------------------------------------------- | ------------------------------------------------------------------ |
| Iconos PNG (sort/downloaded/installed) quedan negros sobre dark | `setColorFilter` runtime con `menu_icon_tint` (§5.4)               |
| `Settings` depende de `getGDActivity()` en static init          | `attachBaseContext` lee `SharedPreferences` directo (§5.3)         |
| Cambio de tema requiere recrear la activity                     | Reutilizar `doRestartApp()` existente (`AlarmManager`)             |
| `Theme.Material` (no Light) cambia diálogos nativos sutilmente  | El proyecto casi no usa diálogos; revisar `FileDialog.java` en QA  |
| `menu_highlight` verde sobre dark puede quedar saturado         | Variante `#66D266` testeada para AA contrast; ajustable en review  |

## 8. Estimación

- Recursos (`values-night/` + tokens nuevos): ~30 min.
- Cambios Java (`Settings`, `attachBaseContext`, opción en menú, tint de
  íconos, `ERROR_COLOR` → resource): ~1.5 h.
- QA manual recorriendo todas las pantallas del menú en ambos modos: ~1 h.

**Total estimado: medio día de trabajo.**

## 9. Fuera de alcance (futuras iteraciones)

- Tema dinámico Material You (Android 12+) — requeriría agregar
  androidx.core, fuera de la filosofía del repo (no AndroidX).
- Tematización del game canvas (cielo, montañas, sombras) — implicaría
  tocar `Game/` que está fuera del scope autorizado.
- Variantes alternativas (high contrast, sepia) — sumar como modos
  extra una vez que la opción "Theme" exista y funcione.

## 10. Próximo paso sugerido

Confirmar la paleta de §4 (especialmente `menu_background #121212` y
`menu_highlight #66D266`) y dar luz verde para abrir un PR aparte con la
implementación bajo el plan de §5.
