package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import org.example.OracleWalletConnector;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

public class ReagendarScreen {

    // Estilos
    private static final String COLOR_FONDO_CELDA = "#E9EEF5";
    private static final String COLOR_TEXTO_NORMAL = "#1F355E";
    private static final String COLOR_SELECCION = "#1F355E";
    private static final String COLOR_TEXTO_SELECCION = "#FFFFFF";
    private static final String COLOR_BORDE_AZUL = "#1F355E";

    // Rango de horas
    private static final LocalTime HORA_INICIO = LocalTime.of(9, 0);
    private static final LocalTime HORA_FIN = LocalTime.of(14, 0);
    private static final int MINUTOS_INTERVALO = 30;

    // Estado
    private final ToggleGroup grupoHorarios = new ToggleGroup();
    private LocalDateTime fechaHoraSeleccionada;

    private Doctor doctor;            // doctor.getId() -> String, lo convertimos a int
    private String especialidad;
    private String idPaciente;        // <-- VARCHAR2 en BD
    private Integer citaAnteriorId = null;   // ⬅️ NUEVO: id de la cita que se va a borrar (si aplica)


    private BorderPane root;
    private GridPane tabla;
    private Pane hostContainer;

    private LocalDate semanaBase;
    private final Map<LocalDate, Set<LocalTime>> ocupadosPorDia = new HashMap<>();

    private ReagendarScreen() {}

    // 1) Crear cita (SIN id de cita anterior)
    public static void mostrarHorario(Doctor doctor,
                                      String especialidad,
                                      javafx.scene.layout.Pane centerContainer,
                                      String idPaciente) {
        ReagendarScreen hs = new ReagendarScreen();
        hs.doctor = doctor;
        hs.especialidad = especialidad;
        hs.idPaciente = idPaciente;
        hs.hostContainer = centerContainer;
        hs.semanaBase = hs.obtenerLunes(java.time.LocalDate.now());
        hs.render();
    }

    // 2) Reagendar (CON id de cita anterior)
    public static void mostrarHorario(Doctor doctor,
                                      String especialidad,
                                      javafx.scene.layout.Pane centerContainer,
                                      String idPaciente,
                                      Integer citaAnteriorId) {
        ReagendarScreen hs = new ReagendarScreen();
        hs.doctor = doctor;
        hs.especialidad = especialidad;
        hs.idPaciente = idPaciente;
        hs.citaAnteriorId = citaAnteriorId;
        hs.hostContainer = centerContainer;
        hs.semanaBase = hs.obtenerLunes(java.time.LocalDate.now());
        hs.render();
    }


    /* ===================== RENDER ===================== */

    private void render() {
        root = new BorderPane();
        root.setPadding(new Insets(16));

        root.setTop(construirHeader());

        VBox center = new VBox(16);
        center.setAlignment(Pos.TOP_CENTER);
        center.getChildren().addAll(construirBarraSemana(), construirTablaSemana());
        root.setCenter(center);

        root.setBottom(construirFooter());

        hostContainer.getChildren().setAll(root);
        recargarOcupadosYRefrescar();
    }

    private Node construirHeader() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 0, 0, 12));

        Label breadcrumb = new Label("Especialidad: " + especialidad + "  >  Doctor: " + doctor.getNombre());
        breadcrumb.setFont(Font.font(15));
        breadcrumb.setStyle("-fx-text-fill: " + COLOR_TEXTO_NORMAL + ";");

        box.getChildren().addAll(breadcrumb);
        return box;
    }


    private Node construirBarraSemana() {
        HBox barra = new HBox(12);
        barra.setAlignment(Pos.CENTER);

        Button btnPrev = new Button("← Semana anterior");
        Button btnNext = new Button("Semana siguiente →");
        DatePicker dp = new DatePicker(semanaBase);

        styleBotonSemana(btnPrev);
        styleBotonSemana(btnNext);

        btnPrev.setOnAction(e -> { semanaBase = semanaBase.minusWeeks(1); recargarOcupadosYRefrescar(); });
        btnNext.setOnAction(e -> { semanaBase = semanaBase.plusWeeks(1); recargarOcupadosYRefrescar(); });

        dp.setConverter(new StringConverter<LocalDate>() {
            private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override public String toString(LocalDate d) { return d == null ? "" : fmt.format(d); }
            @Override public LocalDate fromString(String s) { return (s == null || s.isEmpty()) ? null : LocalDate.parse(s, fmt); }
        });
        dp.setOnAction(e -> {
            if (dp.getValue() != null) {
                semanaBase = obtenerLunes(dp.getValue());
                recargarOcupadosYRefrescar();
            }
        });

        barra.getChildren().addAll(btnPrev, new Label("Ir a semana:"), dp, btnNext);
        return barra;
    }

    private Node construirTablaSemana() {
        StackPane wrapper = new StackPane();
        wrapper.setPadding(new Insets(10));
        wrapper.setStyle("-fx-background-color: " + COLOR_BORDE_AZUL + "; -fx-background-radius: 12;");

        tabla = new GridPane();
        tabla.setHgap(8);
        tabla.setVgap(8);
        tabla.setPadding(new Insets(14));
        tabla.setAlignment(Pos.TOP_CENTER);
        tabla.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        List<LocalDate> dias = diasDeSemana(semanaBase);
        List<String> nombres = Arrays.asList("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom");

        // encabezados
        tabla.add(makeHeader("Hora"), 0, 0);
        for (int c = 0; c < 7; c++) {
            String titulo = nombres.get(c) + " " + dias.get(c).format(DateTimeFormatter.ofPattern("dd/MM"));
            tabla.add(makeHeader(titulo), c + 1, 0);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(100.0 / 8.0);
            tabla.getColumnConstraints().add(cc);
        }

        // filas
        int row = 1;
        for (LocalTime t = HORA_INICIO; !t.isAfter(HORA_FIN); t = t.plusMinutes(MINUTOS_INTERVALO)) {
            Label labHora = new Label(t.toString());
            labHora.setPadding(new Insets(6));
            labHora.setStyle("-fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-font-weight: bold;");
            tabla.add(labHora, 0, row);

            for (int c = 0; c < 7; c++) {
                LocalDate date = dias.get(c);
                Node celda = crearCelda(date, t);
                tabla.add(celda, c + 1, row);
                GridPane.setHgrow(celda, Priority.ALWAYS);
            }
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.NEVER);
            tabla.getRowConstraints().add(rc);
            row++;
        }

        wrapper.getChildren().add(tabla);
        return wrapper;
    }

    private Node construirFooter() {
        HBox bottom = new HBox(16);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(10, 0, 0, 0));

        Region leftSpacer = new Region(), rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        Button btnAtras = new Button("Atrás");
        Button btnGuardar = new Button("Guardar");
        styleBotonPrimario(btnGuardar);
        styleBotonSecundario(btnAtras);

        btnAtras.setOnAction(e -> hostContainer.getChildren().clear());
        btnGuardar.setOnAction(e -> onGuardar());

        HBox leyenda = new HBox(12);
        leyenda.setAlignment(Pos.CENTER);
        leyenda.getChildren().addAll(
                etiquetaColor("Disponible", COLOR_FONDO_CELDA, COLOR_TEXTO_NORMAL),
                etiquetaColor("Seleccionado", COLOR_SELECCION, COLOR_TEXTO_SELECCION),
                etiquetaColor("Ocupado", "#C9CED6", "#6B7280")
        );

        bottom.getChildren().addAll(btnAtras, leftSpacer, leyenda, rightSpacer, btnGuardar);
        return bottom;
    }

    /* ===================== CELDAS ===================== */

    private Node crearCelda(LocalDate fecha, LocalTime hora) {
        ToggleButton btn = new ToggleButton("");
        btn.setToggleGroup(grupoHorarios);
        btn.setUserData(LocalDateTime.of(fecha, hora));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(36);
        btn.setAlignment(Pos.CENTER);
        btn.setStyle("-fx-background-color: " + COLOR_FONDO_CELDA + "; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;");

        DateTimeFormatter fHora  = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter fFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Tooltip tip = new Tooltip("Hora: " + fHora.format(hora) + "\nFecha: " + fFecha.format(fecha));
        btn.setTooltip(tip);

        if (estaOcupado(fecha, hora)) {
            btn.setText("Ocupado");
            btn.setTextFill(Color.web("#A0A0A0"));
            String colorOcupado = "#6B85A3";
            btn.setStyle("-fx-background-color: " + colorOcupado + "; -fx-background-radius: 8;");
            btn.setDisable(true);

            tip.setText(tip.getText() + "\nEstado: Ocupado");
        } else {
            btn.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
                if (!btn.isSelected()) {
                    btn.setStyle("-fx-background-color: #DDE6F3; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;");
                }
            });
            btn.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
                if (!btn.isSelected()) {
                    btn.setStyle("-fx-background-color: " + COLOR_FONDO_CELDA + "; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;");
                }
            });

            btn.selectedProperty().addListener((obs, was, isSel) -> {
                if (isSel) {
                    btn.setStyle("-fx-background-color: #1F355E; -fx-text-fill: white; -fx-background-radius: 8;");
                } else {
                    btn.setStyle("-fx-background-color: " + COLOR_FONDO_CELDA + "; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;");
                }
            });
        }

        grupoHorarios.selectedToggleProperty().addListener((obs, oldT, newT) ->
                fechaHoraSeleccionada = (newT == null) ? null : (LocalDateTime) newT.getUserData());

        return btn;
    }

    /* ===================== GUARDAR ===================== */

    private void onGuardar() {
        if (grupoHorarios.getSelectedToggle() == null || fechaHoraSeleccionada == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Selecciona un horario disponible antes de guardar.");
            return;
        }

        String idMedico = doctor.getId(); // VARCHAR2 en CITA
        mostrarPopupConfirmacion(idMedico);
    }


    public void insertarCita(String matriculaSesion, String idMedicoVarchar, Timestamp fechaHora) throws SQLException {
        // Resolver ID_PACIENTE (NUMBER) desde la matrícula
        Long idPacienteNumber = obtenerIdPacientePorMatricula(matriculaSesion);
        if (idPacienteNumber == null) {
            throw new SQLException("No se encontró la matrícula del paciente.");
        }

        final String sql = "INSERT INTO CITA (ID_CITA, ID_PACIENTE, ID_MEDICO, FECHA_HORA) " +
                "VALUES (SEQ_CITA.NEXTVAL, ?, ?, ?)";

        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idPacienteNumber);   // NUMBER
            ps.setString(2, idMedicoVarchar);  // VARCHAR2(20)
            ps.setTimestamp(3, fechaHora);     // TIMESTAMP
            ps.executeUpdate();
        }
    }

    /* ===================== POPUPS ===================== */
    private void mostrarPopupConfirmacion(String idMedico) {
        GaussianBlur blur = new GaussianBlur(14);
        root.setEffect(blur);
        Stage owner = (Stage) root.getScene().getWindow();

        Locale esMX = new Locale("es", "MX");
        String fechaLarga = fechaHoraSeleccionada.toLocalDate()
                .format(DateTimeFormatter.ofPattern("d 'de' MMMM", esMX));
        String horaAmPm = fechaHoraSeleccionada.toLocalTime()
                .format(DateTimeFormatter.ofPattern("hh:mm a", esMX)).toLowerCase();
        String subtitulo = "La cita sería el día " + fechaLarga + " a las " + horaAmPm + ".";

        Label titulo = new Label("¿Deseas confirmar la cita?");
        titulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        titulo.setWrapText(true);
        titulo.setAlignment(Pos.CENTER);

        Label sub = new Label(subtitulo);
        sub.setWrapText(true);
        sub.setAlignment(Pos.CENTER);

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setOnAction(e -> { dialogRef.close(); root.setEffect(null); });

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.setOnAction(e -> {
            try {
                Long idPacienteNumber = Long.valueOf(idPaciente);

                // Si hay cita anterior, eliminarla
                if (citaAnteriorId != null) {
                    String sqlDel = "DELETE FROM CITA WHERE ID_CITA = ?";
                    try (Connection con = OracleWalletConnector.getConnection();
                         PreparedStatement ps = con.prepareStatement(sqlDel)) {
                        ps.setInt(1, citaAnteriorId);
                        ps.executeUpdate();
                    }
                }

                // Insertar nueva cita
                insertarCita(idPacienteNumber.toString(), idMedico, Timestamp.valueOf(fechaHoraSeleccionada));

                dialogRef.close();
                root.setEffect(null);

                // Mostrar popup de éxito
                mostrarPopupExito("Cita agendada", "Tu cita ha sido registrada exitosamente.");

            } catch (SQLException ex) {
                dialogRef.close();
                root.setEffect(null);
                mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar la cita: " + ex.getMessage());
            }
        });

        HBox botones = new HBox(16, btnCancelar, btnAceptar);
        botones.setAlignment(Pos.CENTER);

        VBox card = new VBox(16, titulo, sub, botones);
        card.setPadding(new Insets(24));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10,0,0,4);");

        StackPane overlay = new StackPane(card);
        overlay.setPadding(new Insets(12));

        Scene scene = new Scene(overlay);
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setScene(scene);
        dialog.setResizable(false);

        this.dialogRef = dialog;
        dialog.show();
    }

    // añade este campo en la clase para cerrar el diálogo desde handlers
    private Stage dialogRef;

    private void mostrarPopupExito(String tituloIgnorado, String mensajeIgnorado) {
        GaussianBlur blur = new GaussianBlur(14);
        root.setEffect(blur);
        Stage owner = (Stage) root.getScene().getWindow();

        final String AZUL_OSCURO = "#1F355E";
        final String AZUL_SUAVE  = "#E9EEF5";
        final String TEXTO_SUAVE = "#6B7A99";

        Locale esMX = new Locale("es", "MX");
        String fechaLarga = fechaHoraSeleccionada.toLocalDate()
                .format(DateTimeFormatter.ofPattern("d 'de' MMMM", esMX));
        String hora24 = fechaHoraSeleccionada.toLocalTime()
                .format(DateTimeFormatter.ofPattern("HH:mm", esMX));
        String subtitulo = "La cita será el " + fechaLarga + " a las " + hora24 + " horas";

        // Icono circular con check
        Circle circle = new Circle(36);
        circle.setFill(Color.TRANSPARENT);
        circle.setStroke(Color.web(AZUL_OSCURO));
        circle.setStrokeWidth(4);
        Label check = new Label("✔");
        check.setStyle("-fx-font-size: 30px; -fx-text-fill: " + AZUL_OSCURO + ";");
        StackPane icono = new StackPane(circle, check);

        Label titulo = new Label("Tu cita ha sido agendada\nexitosamente.");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: " + AZUL_OSCURO + ";");
        titulo.setWrapText(true);
        titulo.setAlignment(Pos.CENTER);

        Label sub = new Label(subtitulo);
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: " + TEXTO_SUAVE + ";");
        sub.setWrapText(true);
        sub.setAlignment(Pos.CENTER);

        Button btnOk = new Button("Aceptar");
        btnOk.setStyle(
                "-fx-background-color: " + AZUL_OSCURO + "; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 10 24;"
        );
        btnOk.setOnAction(e -> {
            dialogRef.close();
            root.setEffect(null);
            // En vez de solo recargar la tabla, mostramos Mis Citas:
            CitasAgendadasScreen.show(hostContainer, idPaciente);  // idPaciente = matrícula guardada en sesión
        });


        VBox card = new VBox(22, icono, titulo, sub, btnOk);
        card.setPadding(new Insets(30));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: " + AZUL_SUAVE + "; -fx-background-radius: 16;" + // fondo claro como el mock
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);"
        );
        card.setMinWidth(460);

        StackPane overlay = new StackPane(card);
        overlay.setPadding(new Insets(12));

        Scene scene = new Scene(overlay);
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.setTitle("¡Cita agendada!");

        this.dialogRef = dialog;
        dialog.show();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String msg) {
        Alert a = new Alert(tipo, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /* ===================== OCUPADOS ===================== */

    private void recargarOcupadosYRefrescar() {
        try {
            cargarOcupadosSemana();
            VBox center = new VBox(16);
            center.setAlignment(Pos.TOP_CENTER);
            center.getChildren().addAll(construirBarraSemana(), construirTablaSemana());
            root.setCenter(center);
        } catch (SQLException ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar horarios ocupados: " + ex.getMessage());
        }
    }

    private void cargarOcupadosSemana() throws SQLException {
        ocupadosPorDia.clear();
        List<LocalDate> dias = diasDeSemana(semanaBase);
        LocalDate desde = dias.get(0);
        LocalDate hasta = dias.get(6);

        // Con CITA: la columna sí es FECHA_HORA
        final String sqlCitas =
                "SELECT FECHA_HORA FROM CITA " +
                        "WHERE ID_MEDICO = ? " +
                        "AND TRUNC(CAST(FECHA_HORA AS DATE)) BETWEEN ? AND ?";

        // Con HORARIO_MEDICO: expandimos intervalos [HORA_INICIO, HORA_FIN) en pasos de 30 min
        final String sqlBloques =
                "SELECT DIA_SEMANA, HORA_INICIO, HORA_FIN " +
                        "FROM HORARIO_MEDICO " +
                        "WHERE ID_MEDICO = ? " +
                        "AND TRUNC(CAST(HORA_INICIO AS DATE)) BETWEEN ? AND ?";

        try (Connection con = OracleWalletConnector.getConnection()) {

            // 1) Citas reales
            try (PreparedStatement ps = con.prepareStatement(sqlCitas)) {
                ps.setString(1, doctor.getId());                // ahora es VARCHAR
                ps.setDate(2, java.sql.Date.valueOf(desde));
                ps.setDate(3, java.sql.Date.valueOf(hasta));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LocalDateTime ldt = rs.getTimestamp(1).toLocalDateTime();
                        addOcupado(ldt);
                    }
                }
            }

            // 2) Bloqueos/agenda base por intervalos
            try (PreparedStatement ps = con.prepareStatement(sqlBloques)) {
                ps.setString(1, doctor.getId());                // ahora es VARCHAR
                ps.setDate(2, java.sql.Date.valueOf(desde));
                ps.setDate(3, java.sql.Date.valueOf(hasta));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp tin = rs.getTimestamp("HORA_INICIO");
                        Timestamp tfi = rs.getTimestamp("HORA_FIN");
                        if (tin == null || tfi == null) continue;

                        LocalDateTime ini = tin.toLocalDateTime().withSecond(0).withNano(0);
                        LocalDateTime fin = tfi.toLocalDateTime().withSecond(0).withNano(0);

                        for (LocalDateTime cur = ini; cur.isBefore(fin); cur = cur.plusMinutes(MINUTOS_INTERVALO)) {
                            addOcupado(cur);
                        }
                    }
                }
            }
        }
    }


    private void addOcupado(LocalDateTime ldt) {
        ocupadosPorDia
                .computeIfAbsent(ldt.toLocalDate(), k -> new HashSet<>())
                .add(ldt.toLocalTime().withSecond(0).withNano(0));
    }

    private boolean estaOcupado(LocalDate fecha, LocalTime hora) {
        Set<LocalTime> set = ocupadosPorDia.get(fecha);
        if (set == null) return false;
        return set.contains(hora.withSecond(0).withNano(0));
    }

    /* ===================== UTILES ===================== */

    private List<LocalDate> diasDeSemana(LocalDate lunes) {
        List<LocalDate> dias = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) dias.add(lunes.plusDays(i));
        return dias;
    }

    private LocalDate obtenerLunes(LocalDate anyDay) {
        WeekFields wf = WeekFields.of(Locale.getDefault());
        return anyDay.with(wf.dayOfWeek(), 1);
    }

    private Label makeHeader(String text) {
        Label l = new Label(text);
        l.setPadding(new Insets(8));
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-background-color: #F7F9FC; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-font-weight: bold; -fx-background-radius: 6;");
        return l;
    }

    private Label etiquetaColor(String texto, String fondo, String textoColor) {
        Label l = new Label(texto);
        l.setPadding(new Insets(6, 10, 6, 10));
        l.setStyle("-fx-background-color: " + fondo + "; -fx-text-fill: " + textoColor + "; -fx-background-radius: 6;");
        return l;
    }

    private void styleBotonHeader(Button b) {
        b.setPadding(new Insets(8, 14, 8, 14));
        b.setStyle("-fx-background-color: white; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 20; -fx-border-color: " + COLOR_BORDE_AZUL + "; -fx-border-radius: 20;");
    }

    private void styleBotonPrimario(Button b) {
        b.setPadding(new Insets(8, 18, 8, 18));
        b.setStyle("-fx-background-color: " + COLOR_SELECCION + "; -fx-text-fill: white; -fx-background-radius: 10;");
    }

    private void styleBotonSecundario(Button b) {
        b.setPadding(new Insets(8, 18, 8, 18));
        b.setStyle("-fx-background-color: #EEF2F7; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 10;");
    }

    private Long obtenerIdPacientePorMatricula(String matricula) throws SQLException {
        if (matricula == null || matricula.isBlank()) {
            return null; // devolvemos null y que el caller avise al usuario
        }
        final String sql = "SELECT ID_PACIENTE FROM ADMIN.PACIENTE WHERE UPPER(MATRICULA) = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, matricula.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return null;
    }


    private void styleBotonSemana(Button b) {
        // Azul oscuro de tu paleta + texto blanco + bordes redondeados
        b.setPadding(new Insets(8, 16, 8, 16));
        b.setStyle(
                "-fx-background-color: " + COLOR_SELECCION + ";" +   // #1F355E
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + COLOR_BORDE_AZUL + ";" +      // #1F355E
                        "-fx-border-radius: 10;"
        );

        // hover: un azul un poco más claro
        b.setOnMouseEntered(e -> b.setStyle(
                "-fx-background-color: #2D4A80;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + COLOR_BORDE_AZUL + ";" +
                        "-fx-border-radius: 10;"
        ));
        b.setOnMouseExited(e -> b.setStyle(
                "-fx-background-color: " + COLOR_SELECCION + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + COLOR_BORDE_AZUL + ";" +
                        "-fx-border-radius: 10;"
        ));
    }

}
