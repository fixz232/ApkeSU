<div align="center">
  <img src=".github/assets/apkesu-avatar.jpg" width="180" alt="Avatar del proyecto ApkeSU">
  <h1>ApkeSU</h1>
  <p>Gestor root basado en el kernel para dispositivos Android GKI</p>
  <p>
    <a href="README.md">简体中文</a> ·
    <a href="README.en.md">English</a> ·
    <a href="README.fr.md">Français</a> ·
    <a href="README.ru.md">Русский</a> ·
    <a href="README.ja.md">日本語</a> ·
    <a href="README.ko.md">한국어</a> ·
    <strong>Español</strong>
  </p>
  <p>
    <a href="#descripción-del-proyecto">Descripción</a> ·
    <a href="#proyecto-principal">Proyecto principal</a> ·
    <a href="#cumplimiento-de-las-licencias">Licencias</a> ·
    <a href="#aviso-legal">Aviso legal</a>
  </p>
  <p>
    <a href="https://t.me/+LkrMQKXtXvpmYmNl">Telegram</a> ·
    <a href="https://qm.qq.com/q/8O7qvLM3zq">Grupo de QQ</a>
  </p>
</div>

---

ApkeSU es un proyecto derivado de código abierto basado en el repositorio oficial de [KernelSU](https://github.com/tiann/KernelSU) y en [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra). Es un gestor root basado en el kernel para dispositivos Android GKI, centrado en la coincidencia KMI, la fiabilidad de la configuración de SuSFS, el mantenimiento recuperable, la experiencia de KernelSU Manager, las extensiones de interfaz, el parcheo de LKM y la depuración de dispositivos personales.

## Descripción del proyecto

Este proyecto hereda la estructura de licencias de KernelSU: el directorio `kernel/` se distribuye bajo **GPL-2.0-only**, de acuerdo con KernelSU y el kernel de Linux; el código derivado de KernelSU situado fuera de `kernel/` se distribuye bajo **GPL-3.0-or-later**. Las dependencias de terceros conservan sus respectivas licencias de origen. Consulta [THIRD_PARTY_NOTICES.md](./THIRD_PARTY_NOTICES.md) para obtener más información.

## Proyecto principal

Proyecto principal de origen: KernelSU

Repositorio de origen: https://github.com/tiann/KernelSU

Este proyecto incorpora periódicamente actualizaciones del código fuente, parches de seguridad y mejoras funcionales del proyecto principal.

## Cumplimiento de las licencias

1. Todas las modificaciones y todo el código derivado permanecen abiertos. El código fuente correspondiente completo se proporciona con cada compilación distribuida, incluidos kernels, APK y módulos.
2. Cualquier persona puede obtener, modificar y redistribuir este proyecto según las condiciones de las licencias aplicables. Las redistribuciones deben conservar la atribución al proyecto de origen, los avisos de licencia y el código fuente correspondiente completo.
3. Si distribuyes públicamente una versión modificada de este proyecto, también debes publicar el código fuente modificado completo e indicar su procedencia.

> Importante: este proyecto amplía las funciones del proyecto original, pero mantiene el modelo de permisos nativo de KernelSU.
> El tema de estilo MIUI es una implementación visual independiente y no utiliza código fuente oficial de Xiaomi.

## Uso previsto

Esta herramienta está destinada exclusivamente a la investigación técnica local sobre Android, el aprendizaje y la depuración de dispositivos de tu propiedad. No la utilices para alterar ilegalmente permisos de aplicaciones, eludir controles de riesgo, obtener acceso no autorizado ni realizar ninguna otra actividad ilegal.

## Aviso legal

1. Este proyecto está destinado únicamente al aprendizaje de tecnologías Android de bajo nivel y al intercambio técnico de código abierto. Todas las herramientas y el código fuente se proporcionan exclusivamente para investigación personal lícita. Los desarrolladores no asumen responsabilidad alguna por reparaciones, indemnizaciones o asistencia posventa ante fallos de arranque, reinicios continuos, dispositivos inutilizados, daños de hardware u otros fallos causados por flashear una ROM o un kernel o por instalar archivos relacionados con el proyecto. El usuario asume todos estos riesgos.<br>

2. Las aplicaciones financieras, de juegos en línea, empresariales y gubernamentales suelen utilizar controles de riesgo sobre entornos Root y privilegios del kernel. El usuario asume todos los riesgos de suspensión de cuentas, bloqueo de dispositivos, restricciones de funciones o pérdidas económicas derivados del uso de esta herramienta. Los desarrolladores no ayudan con apelaciones de cuentas ni con la elusión de controles de riesgo.<br>

3. Está prohibido utilizar el código fuente o las herramientas derivadas de este proyecto para modificar permisos sin autorización del propietario del dispositivo, piratear aplicaciones, robar datos, incluir software malicioso, desarrollar trampas o infringir cualquier ley aplicable de ciberseguridad, derechos de autor u otra normativa. La persona que cometa dicho uso indebido asumirá por sí sola toda responsabilidad civil, administrativa y penal; los desarrolladores del proyecto no serán responsables.<br>

4. Este proyecto se distribuye gratuitamente solo a través de comunidades legítimas de código abierto y no ofrece ventas de pago ni servicios oficiales de personalización. Los paquetes de pago o las versiones modificadas ofrecidos por plataformas o personas ajenas no guardan relación con este proyecto, y no se garantiza su seguridad ni integridad. El usuario asume los riesgos de robo de cuentas, filtración de datos personales o infección por software malicioso derivados de compilaciones de terceros.<br>

5. Al descargar, compilar o flashear archivos de este proyecto, confirmas que has leído, comprendido y aceptado íntegramente este aviso legal. Si no lo aceptas, elimina inmediatamente el código fuente y los archivos y deja de utilizar el proyecto.<br>

## Agradecimientos

- [KernelSU](https://github.com/tiann/KernelSU), con agradecimiento al autor weishu y a todos los colaboradores
- [SukiSU-Ultra](https://github.com/SukiSU-Ultra/SukiSU-Ultra), referencia para la solución SuSFS
- [FolkPatch](https://github.com/LyraVoid/FolkPatch), código del framework de interfaz utilizado como referencia
- [kowsu](https://github.com/KOWX712/KernelSU.git), asistencia técnica
- [Kernel-Assisted Superuser](https://git.zx2c4.com/kernel-assisted-superuser/about/), inspiración para el diseño de KernelSU
- [Magisk](https://github.com/topjohnwu/Magisk), conocida solución root de código abierto
- [genuine](https://github.com/brevent/genuine/), verificación de firmas APK
- [Diamorphine](https://github.com/m0nad/Diamorphine), referencia para técnicas de ocultación de bajo nivel
