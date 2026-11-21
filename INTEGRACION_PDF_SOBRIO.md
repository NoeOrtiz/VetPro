# VetPRO híbrido: informes PDF sobrios y sin naranja

Rama de integración: `integracion/pdf-sobrio-historia`.
Referencias visuales: PDF de Clientes e Historia Clínica entregados por la usuaria y pantalla de Historia Clínica del compañero.

- PDFBox permanece como generador; no se añade JasperReports.
- Se preservan todas las reglas del original, incluidos filtros de Clientes, eventos de Historia Clínica y formato de importes de peluquería.
- Se adopta la presentación institucional del compañero: logo a izquierda, títulos, fecha de emisión, separador fino, pie con usuario y páginas, bandas suaves y tablas legibles.
- La paleta cambia de naranja a azul petróleo `#177682`, azul oscuro `#0E4D5B`, fondo `#E8F5F5` y gris `#F6F9FA`.
- Las pantallas Swing siguen en el proyecto original: casillas de selección, detalle, impresión, paginación y búsquedas no se reemplazan por el funcionamiento del compañero.
- Antes de uso real: ejecutar `mvn clean test`, verificar los PDFs generados y revisar la legibilidad cuando hay muchas páginas.

No se modificó la rama `redisenio-vetpro` ni `VetPRO_Companero`.
