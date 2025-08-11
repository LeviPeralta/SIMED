package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
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

public class HorarioScreen {

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
    private Integer idMedicoIntResuelto = null;

    private BorderPane root;
    private GridPane tabla;
    private Pane hostContainer;

    private LocalDate semanaBase;
    private final Map<LocalDate, Set<LocalTime>> ocupadosPorDia = new HashMap<>();

    private HorarioScreen() {}

    /** Punto de entrada */
    public static void mostrarHorario(Doctor doctor, String especialidad, Pane centerContainer, String idPaciente) {
        HorarioScreen hs = new HorarioScreen();
        hs.doctor = doctor;
        hs.especialidad = especialidad;
        hs.idPaciente = idPaciente;      // ahora String
        hs.hostContainer = centerContainer;
        hs.semanaBase = hs.obtenerLunes(LocalDate.now());
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
        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER);

        Button btnInicio = new Button("Inicio");
        Button btnMisCitas = new Button("Mis Citas");
        Button btnEmergencia = new Button("Emergencia");
        styleBotonHeader(btnInicio);
        styleBotonHeader(btnMisCitas);
        styleBotonHeader(btnEmergencia);
        top.getChildren().addAll(btnInicio, btnMisCitas, btnEmergencia);

        Label breadcrumb = new Label("Especialidad: " + especialidad + "  >  Doctor: " + doctor.getNombre());
        breadcrumb.setFont(Font.font(15));
        breadcrumb.setStyle("-fx-text-fill: " + COLOR_TEXTO_NORMAL + ";");

        box.getChildren().addAll(top, breadcrumb);
        return box;
    }

    private Node construirBarraSemana() {
        HBox barra = new HBox(12);
        barra.setAlignment(Pos.CENTER);

        Button btnPrev = new Button("⟵ Semana anterior");
        Button btnNext = new Button("Semana siguiente ⟶");
        DatePicker dp = new DatePicker(semanaBase);

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
        ToggleButton btn = new ToggleButton(hora.toString());
        btn.setToggleGroup(grupoHorarios);
        btn.setUserData(LocalDateTime.of(fecha, hora));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(36);
        btn.setAlignment(Pos.CENTER);
        btn.setStyle("-fx-background-color: " + COLOR_FONDO_CELDA + "; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;");

        if (estaOcupado(fecha, hora)) {
            btn.setDisable(true);
            btn.setText(hora + "  (ocupado)");
            btn.setStyle("-fx-background-color: #C9CED6; -fx-text-fill: #6B7280; -fx-background-radius: 8;");
        } else {
            btn.addEventFilter(MouseEvent.MOUSE_ENTERED, e ->
                    btn.setStyle("-fx-background-color: #DDE6F3; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;"));
            btn.addEventFilter(MouseEvent.MOUSE_EXITED, e ->
                    btn.setStyle("-fx-background-color: " + COLOR_FONDO_CELDA + "; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;"));

            btn.selectedProperty().addListener((obs, was, isSel) -> {
                if (isSel)  btn.setStyle("-fx-background-color: " + COLOR_SELECCION + "; -fx-text-fill: " + COLOR_TEXTO_SELECCION + "; -fx-background-radius: 8;");
                else        btn.setStyle("-fx-background-color: " + COLOR_FONDO_CELDA + "; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;");
            });
        }

        // guarda la selección real
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

        // Convertimos el id del doctor (String) a int porque ID_MEDICO es NUMBER
        Integer idMedicoInt = tryParseInt(doctor.getId());
        if (idMedicoInt == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "El ID del doctor no es numérico: " + doctor.getId());
            return;
        }

        mostrarPopupConfirmacion(
                "Confirmar cita",
                "¿Deseas agendar la cita?\n\nDoctor: " + doctor.getNombre() +
                        "\nFecha: " + fechaHoraSeleccionada.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                        "\nHora: " + fechaHoraSeleccionada.toLocalTime(),
                idMedicoInt
        );
    }

    private void insertarCita(String idPacienteVarchar, int idMedicoNumber, Timestamp fechaHora) throws SQLException {
        final String sql = "INSERT INTO CITA (ID_CITA, ID_PACIENTE, ID_MEDICO, FECHA_HORA) " +
                "VALUES (SEQ_CITA.NEXTVAL, ?, ?, ?)";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idPacienteVarchar);       // VARCHAR2
            ps.setInt(2, idMedicoNumber);             // NUMBER
            ps.setTimestamp(3, fechaHora);            // TIMESTAMP
            ps.executeUpdate();
        }
    }

    /* ===================== POPUPS ===================== */

    private void mostrarPopupConfirmacion(String titulo, String mensaje, int idMedicoInt) {
        GaussianBlur blur = new GaussianBlur(14);
        root.setEffect(blur);

        Stage stage = (Stage) root.getScene().getWindow();

        VBox content = new VBox(12);
        content.setPadding(new Insets(18));
        content.setAlignment(Pos.CENTER_LEFT);
        Label lt = new Label(titulo);
        lt.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXTO_NORMAL + ";");
        Label lm = new Label(mensaje);
        lm.setWrapText(true);

        HBox acciones = new HBox(12);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancelar = new Button("Cancelar");
        Button btnAceptar = new Button("Aceptar");
        styleBotonSecundario(btnCancelar);
        styleBotonPrimario(btnAceptar);

        acciones.getChildren().addAll(btnCancelar, btnAceptar);
        content.getChildren().addAll(lt, lm, new Separator(), acciones);

        Scene sc = new Scene(content);
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setScene(sc);
        dialog.setResizable(false);
        dialog.setTitle(titulo);

        btnCancelar.setOnAction(e -> { dialog.close(); root.setEffect(null); });

        btnAceptar.setOnAction(e -> {
            try {
                insertarCita(idPaciente, idMedicoInt, Timestamp.valueOf(fechaHoraSeleccionada));
                dialog.close();
                mostrarPopupExito("¡Cita agendada!", "Tu cita fue agendada exitosamente.");
            } catch (SQLException ex) {
                dialog.close();
                root.setEffect(null);
                mostrarAlerta(Alert.AlertType.ERROR, "Error al guardar la cita: " + ex.getMessage());
            }
        });

        dialog.show();
    }

    private void mostrarPopupExito(String titulo, String mensaje) {
        GaussianBlur blur = new GaussianBlur(14);
        root.setEffect(blur);

        Stage stage = (Stage) root.getScene().getWindow();

        VBox content = new VBox(16);
        content.setPadding(new Insets(22));
        content.setAlignment(Pos.CENTER);
        Label lt = new Label(titulo);
        lt.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + COLOR_TEXTO_NORMAL + ";");
        Label lm = new Label(mensaje);
        lm.setWrapText(true);

        Button btnOk = new Button("Aceptar");
        styleBotonPrimario(btnOk);

        VBox tarjeta = new VBox(12, lt, lm, btnOk);
        tarjeta.setPadding(new Insets(24));
        tarjeta.setAlignment(Pos.CENTER);
        tarjeta.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 4);");

        StackPane overlay = new StackPane(tarjeta);
        overlay.setPadding(new Insets(18));

        Scene sc = new Scene(overlay);
        Stage dialog = new Stage();
        dialog.initOwner(stage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setScene(sc);
        dialog.setResizable(false);
        dialog.setTitle(titulo);

        btnOk.setOnAction(e -> {
            dialog.close();
            root.setEffect(null);
            recargarOcupadosYRefrescar();
        });

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

        final String sqlCitas =
                "SELECT FECHA_HORA FROM CITA " +
                        "WHERE ID_MEDICO = ? " +
                        "AND TRUNC(CAST(FECHA_HORA AS DATE)) BETWEEN ? AND ?";

        final String sqlBloques =
                "SELECT FECHA_HORA FROM HORARIO_MEDICO " +
                        "WHERE ID_MEDICO = ? " +
                        "AND TRUNC(CAST(FECHA_HORA AS DATE)) BETWEEN ? AND ?";

        Integer idMedicoInt = tryParseInt(doctor.getId());
        if (idMedicoInt == null) return; // evita NPE si el id no es numérico

        try (Connection con = OracleWalletConnector.getConnection()) {

            // Citas
            try (PreparedStatement ps = con.prepareStatement(sqlCitas)) {
                ps.setInt(1, idMedicoInt);
                ps.setDate(2, java.sql.Date.valueOf(desde));
                ps.setDate(3, java.sql.Date.valueOf(hasta));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) addOcupado(rs.getTimestamp(1).toLocalDateTime());
                }
            }

            // Bloqueos (si usas esa tabla)
            try (PreparedStatement ps = con.prepareStatement(sqlBloques)) {
                ps.setInt(1, idMedicoInt);
                ps.setDate(2, java.sql.Date.valueOf(desde));
                ps.setDate(3, java.sql.Date.valueOf(hasta));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) addOcupado(rs.getTimestamp(1).toLocalDateTime());
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

    private Integer tryParseInt(String s) {
        try { return Integer.valueOf(s); } catch (Exception e) { return null; }
    }

    private Integer resolverIdMedicoNumerico(Doctor doctor) {
        // 1) Intenta parsear si por alguna razón ya viene numérico
        try { return Integer.valueOf(doctor.getId()); } catch (Exception ignore) {}

        // 2) Busca por CODIGO (recomendado)
        final String sqlCodigo = "SELECT ID_MEDICO FROM MEDICO WHERE CODIGO = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlCodigo)) {
            ps.setString(1, doctor.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al resolver ID del médico: " + e.getMessage());
            return null;
        }

        // 3) Fallback por nombre (si no tienes CODIGO)
        final String sqlNombre = "SELECT ID_MEDICO FROM MEDICO WHERE UPPER(NOMBRE) = UPPER(?)";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlNombre)) {
            ps.setString(1, doctor.getNombre());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al resolver ID del médico por nombre: " + e.getMessage());
            return null;
        }

        return null; // no encontrado
    }

}
