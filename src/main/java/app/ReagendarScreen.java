package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.example.OracleWalletConnector;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class ReagendarScreen {

    // Estilos
    private static final String COLOR_FONDO_CELDA = "#E9EEF5";
    private static final String COLOR_TEXTO_NORMAL = "#1F355E";
    private static final String COLOR_SELECCION = "#1F355E";
    private static final String COLOR_TEXTO_SELECCION = "#FFFFFF";
    private static final String COLOR_BORDE_AZUL = "#1F355E";
    private static final String BASE_ASSETS = "/images/mainPage/";

    // Rango de horas
    private static final LocalTime HORA_INICIO = LocalTime.of(9, 0);
    private static final LocalTime HORA_FIN = LocalTime.of(14, 0);
    private static final int MINUTOS_INTERVALO = 30;

    // Estado
    private final ToggleGroup grupoHorarios = new ToggleGroup();
    private LocalDateTime fechaHoraSeleccionada;

    private Doctor doctor;
    private String especialidad;
    private String matriculaPaciente;
    private Integer citaAnteriorId;

    private BorderPane screenRoot;
    private Stage dialogRef;
    private LocalDate semanaBase;
    private final Map<LocalDate, Set<LocalTime>> ocupadosPorDia = new HashMap<>();

    private ReagendarScreen() {}

    public static void show(Stage stage, Doctor doctor, String especialidad, String matricula, Integer idCitaAnterior) {
        ReagendarScreen instance = new ReagendarScreen();
        instance.doctor = doctor;
        instance.especialidad = especialidad;
        instance.matriculaPaciente = matricula;
        instance.citaAnteriorId = idCitaAnterior;
        instance.semanaBase = instance.obtenerLunes(LocalDate.now());
        instance.render(stage);
    }

    private void render(Stage stage) {
        ScreenRouter.initIfNeeded(stage);

        screenRoot = new BorderPane();
        screenRoot.setStyle("-fx-background-color:white;");

        screenRoot.setTop(crearBarraSuperior());

        VBox content = new VBox(16);
        content.setPadding(new Insets(12, 32, 12, 32));
        VBox.setVgrow(content, Priority.ALWAYS);

        Node barraSemana = construirBarraSemana();
        Node tablaHorarios = construirTablaSemana();
        VBox.setVgrow(tablaHorarios, Priority.ALWAYS);

        content.getChildren().addAll(barraSemana, tablaHorarios);

        screenRoot.setCenter(content);
        screenRoot.setBottom(construirFooter());

        ScreenRouter.setView(screenRoot);
        recargarOcupadosYRefrescar();
    }

    private void reagendarCitaEnTransaccion(int idCitaAnterior, String matricula, String idMedico, Timestamp nuevaFechaHora) throws SQLException {
        Connection con = null;
        try {
            con = OracleWalletConnector.getConnection();
            con.setAutoCommit(false);

            long nuevoIdCita = -1;
            Long idPacienteNumber = obtenerIdPacientePorMatricula(matricula);
            if (idPacienteNumber == null) {
                throw new SQLException("No se encontró la matrícula del paciente.");
            }

            String sqlIns = "INSERT INTO CITA (ID_CITA, ID_PACIENTE, ID_MEDICO, FECHA_HORA) VALUES (SEQ_CITA.NEXTVAL, ?, ?, ?)";
            try (PreparedStatement psIns = con.prepareStatement(sqlIns, new String[]{"ID_CITA"})) {
                psIns.setLong(1, idPacienteNumber);
                psIns.setString(2, idMedico);
                psIns.setTimestamp(3, nuevaFechaHora);
                psIns.executeUpdate();

                try (Statement stmt = con.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT SEQ_CITA.CURRVAL FROM DUAL")) {
                    if (rs.next()) {
                        nuevoIdCita = rs.getLong(1);
                    } else {
                        throw new SQLException("No se pudo obtener el ID de la nueva cita creada.");
                    }
                }
            }

            String sqlUpdateExp = "UPDATE EXPEDIENTE_MEDICO SET ID_CITA = ? WHERE ID_CITA = ?";
            try (PreparedStatement psUpd = con.prepareStatement(sqlUpdateExp)) {
                psUpd.setLong(1, nuevoIdCita);
                psUpd.setInt(2, idCitaAnterior);
                psUpd.executeUpdate();
            }

            String sqlDel = "DELETE FROM CITA WHERE ID_CITA = ?";
            try (PreparedStatement psDel = con.prepareStatement(sqlDel)) {
                psDel.setInt(1, idCitaAnterior);
                int deletedRows = psDel.executeUpdate();
                if (deletedRows == 0) {
                    throw new SQLException("Error: No se encontró la cita anterior (ID: " + idCitaAnterior + ") para borrar.");
                }
            }

            con.commit();

        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { System.err.println("Error CRÍTICO al hacer rollback: " + ex.getMessage()); }
            }
            throw e;
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); con.close(); } catch (SQLException e) { System.err.println("Error al cerrar la conexión: " + e.getMessage()); }
            }
        }
    }

    private HBox crearBarraSuperior() {
        HBox top = new HBox();
        top.setStyle("-fx-background-color:#FFFFFF; -fx-border-color: transparent transparent #E9EEF5 transparent; -fx-border-width:0 0 1 0;");
        top.setPadding(new Insets(8, 32, 8, 32));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setSpacing(12);
        ImageView simed = icon("Logo.png", 100, 100);
        Button bInicio = new Button("Inicio", icon("Inicio.png", 22, 22));
        bInicio.setContentDisplay(ContentDisplay.LEFT);
        bInicio.setGraphicTextGap(8);
        bInicio.setStyle("-fx-background-color:#D0E1F9; -fx-text-fill:#1F355E; -fx-font-weight:bold; -fx-background-radius:10; -fx-padding:8 16;");
        bInicio.setOnAction(e -> new MenuScreen().show(ScreenRouter.getStage()));

        Region spL = new Region(); HBox.setHgrow(spL, Priority.ALWAYS);
        HBox middle = new HBox(bInicio);
        middle.setAlignment(Pos.CENTER);
        Region spR = new Region(); HBox.setHgrow(spR, Priority.ALWAYS);
        Label user = new Label(Sesion.getNombreUsuario() == null ? "Usuario" : Sesion.getNombreUsuario(), icon("User.png", 22, 22));
        user.setContentDisplay(ContentDisplay.LEFT);
        user.setTextFill(Color.web(COLOR_TEXTO_NORMAL));
        user.setFont(Font.font(13));
        Button salir = new Button("", icon("Close.png", 22, 22));
        salir.setStyle("-fx-background-color:#1F355E;");
        salir.setOnAction(e -> new org.example.Main().start(ScreenRouter.getStage()));
        top.getChildren().addAll(simed, spL, middle, spR, user, salir);
        return top;
    }

    private Node construirBarraSemana() {
        HBox barra = new HBox(12);
        barra.setAlignment(Pos.CENTER);
        Button btnPrev = new Button("← Semana");
        Button btnNext = new Button("Semana →");
        DatePicker dp = new DatePicker(semanaBase);
        styleBotonSemana(btnPrev);
        styleBotonSemana(btnNext);
        LocalDate lunesDeHoy = obtenerLunes(LocalDate.now());
        btnPrev.setDisable(semanaBase.isEqual(lunesDeHoy) || semanaBase.isBefore(lunesDeHoy));
        btnPrev.setOnAction(e -> { semanaBase = semanaBase.minusWeeks(1); recargarOcupadosYRefrescar(); });
        btnNext.setOnAction(e -> { semanaBase = semanaBase.plusWeeks(1); recargarOcupadosYRefrescar(); });
        dp.setDayCellFactory(picker -> new DateCell() {
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                }
            }
        });
        dp.setOnAction(e -> {
            if (dp.getValue() != null) {
                semanaBase = obtenerLunes(dp.getValue());
                recargarOcupadosYRefrescar();
            }
        });
        barra.getChildren().addAll(btnPrev, dp, btnNext);
        return barra;
    }

    private Node construirTablaSemana() {
        StackPane wrapper = new StackPane();
        wrapper.setPadding(new Insets(10));
        wrapper.setStyle("-fx-background-color: " + COLOR_BORDE_AZUL + "; -fx-background-radius: 12;");
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        GridPane tabla = new GridPane();
        tabla.setHgap(8);
        tabla.setVgap(8);
        tabla.setPadding(new Insets(14));
        tabla.setAlignment(Pos.TOP_CENTER);
        tabla.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        List<LocalDate> dias = diasDeSemana(semanaBase);
        List<String> nombres = Arrays.asList("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom");
        final LocalDateTime ahora = LocalDateTime.now();
        tabla.add(makeHeader("Hora"), 0, 0);
        for (int c = 0; c < 7; c++) {
            String titulo = nombres.get(c) + " " + dias.get(c).format(DateTimeFormatter.ofPattern("dd/MM"));
            tabla.add(makeHeader(titulo), c + 1, 0);
        }
        int row = 1;
        for (LocalTime t = HORA_INICIO; !t.isAfter(HORA_FIN); t = t.plusMinutes(MINUTOS_INTERVALO)) {
            Label labHora = new Label(t.toString());
            labHora.setPadding(new Insets(6));
            labHora.setStyle("-fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-font-weight: bold;");
            tabla.add(labHora, 0, row);
            for (int c = 0; c < 7; c++) {
                LocalDate date = dias.get(c);
                Node celda = crearCelda(date, t, ahora);
                tabla.add(celda, c + 1, row);
                GridPane.setHgrow(celda, Priority.ALWAYS);
            }
            row++;
        }
        wrapper.getChildren().add(tabla);
        return wrapper;
    }

    private Node construirFooter() {
        HBox bottom = new HBox(16);
        bottom.setPadding(new Insets(10, 32, 20, 32));
        bottom.setAlignment(Pos.CENTER);
        Button btnAtras = new Button("Atrás");
        styleBotonSecundario(btnAtras);

        // --- INICIO DE LA CORRECCIÓN ---
        btnAtras.setOnAction(e -> {
            MenuScreen menu = new MenuScreen();
            menu.show(ScreenRouter.getStage());
            menu.showCitasProximas();
        });
        // --- FIN DE LA CORRECCIÓN ---

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox leyenda = new HBox(12,
                etiquetaColor("Disponible", COLOR_FONDO_CELDA, COLOR_TEXTO_NORMAL),
                etiquetaColor("Seleccionado", COLOR_SELECCION, COLOR_TEXTO_SELECCION),
                etiquetaColor("Ocupado", "#C9CED6", "#6B7280")
        );
        leyenda.setAlignment(Pos.CENTER);
        Button btnGuardar = new Button("Reagendar Cita");
        styleBotonPrimario(btnGuardar);
        btnGuardar.setOnAction(e -> onGuardar());
        bottom.getChildren().addAll(btnAtras, spacer, leyenda, btnGuardar);
        return bottom;
    }

    private Node crearCelda(LocalDate fecha, LocalTime hora, LocalDateTime ahora) {
        ToggleButton btn = new ToggleButton("");
        btn.setToggleGroup(grupoHorarios);
        btn.setUserData(LocalDateTime.of(fecha, hora));
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(36);
        btn.setAlignment(Pos.CENTER);
        btn.setStyle("-fx-background-color: " + COLOR_FONDO_CELDA + "; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;");
        Tooltip tip = new Tooltip("Hora: " + hora.format(DateTimeFormatter.ofPattern("HH:mm")) + "\nFecha: " + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        btn.setTooltip(tip);
        LocalDateTime fechaHoraCelda = LocalDateTime.of(fecha, hora);
        boolean ocupado = estaOcupado(fecha, hora);
        boolean esPasado = fechaHoraCelda.isBefore(ahora);
        if (ocupado || esPasado) {
            btn.setText(ocupado ? "Ocupado" : "");
            tip.setText(tip.getText() + (ocupado ? "\nEstado: Ocupado" : "\nEstado: No disponible"));
            btn.setStyle("-fx-background-color: #D1D5DB; -fx-background-radius: 8;");
            btn.setDisable(true);
        } else {
            btn.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> { if (!btn.isSelected()) btn.setStyle("-fx-background-color: #DDE6F3; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;"); });
            btn.addEventFilter(MouseEvent.MOUSE_EXITED, e -> { if (!btn.isSelected()) btn.setStyle("-fx-background-color: " + COLOR_FONDO_CELDA + "; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;"); });
            btn.selectedProperty().addListener((obs, was, isSel) -> btn.setStyle(isSel ? "-fx-background-color: #1F355E; -fx-text-fill: white; -fx-background-radius: 8;" : "-fx-background-color: " + COLOR_FONDO_CELDA + "; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 8;"));
        }
        grupoHorarios.selectedToggleProperty().addListener((obs, oldT, newT) -> fechaHoraSeleccionada = (newT == null) ? null : (LocalDateTime) newT.getUserData());
        return btn;
    }

    private void onGuardar() {
        if (grupoHorarios.getSelectedToggle() == null || fechaHoraSeleccionada == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Selecciona un horario disponible antes de guardar.");
            return;
        }
        mostrarPopupConfirmacion();
    }

    private void mostrarPopupConfirmacion() {
        GaussianBlur blur = new GaussianBlur(14);
        screenRoot.setEffect(blur);
        Stage owner = ScreenRouter.getStage();
        Button btnAceptar = new Button("Aceptar");
        btnAceptar.setOnAction(e -> {
            try {
                reagendarCitaEnTransaccion(citaAnteriorId, matriculaPaciente, doctor.getId(), Timestamp.valueOf(fechaHoraSeleccionada));
                dialogRef.close();
                mostrarPopupExito();
            } catch (SQLException ex) {
                ex.printStackTrace();
                dialogRef.close();
                screenRoot.setEffect(null);
                mostrarAlerta(Alert.AlertType.ERROR, "Error al reagendar la cita: " + ex.getMessage());
            }
        });
        final String AZUL_OSCURO = "#1F355E";
        final String AZUL_SUAVE = "#E9EEF5";
        Locale esMX = new Locale("es", "MX");
        String fechaLarga = fechaHoraSeleccionada.toLocalDate().format(DateTimeFormatter.ofPattern("d 'de' MMMM", esMX));
        String horaAmPm = fechaHoraSeleccionada.toLocalTime().format(DateTimeFormatter.ofPattern("hh:mm a", esMX)).toLowerCase();
        String subtitulo = "La nueva cita sería el día " + fechaLarga + " a las " + horaAmPm + ".";
        Label titulo = new Label("¿Estás seguro que deseas\n reagendar esta cita?");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: " + AZUL_OSCURO + ";");
        titulo.setWrapText(true);
        titulo.setAlignment(Pos.CENTER);
        Label sub = new Label(subtitulo);
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7A99;");
        sub.setWrapText(true);
        sub.setAlignment(Pos.CENTER);
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setStyle("-fx-background-color: #EEF2F7; -fx-text-fill: " + AZUL_OSCURO + "; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 10 18;");
        btnAceptar.setStyle("-fx-background-color: " + AZUL_OSCURO + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 10 22;");
        btnCancelar.setOnAction(ev -> { dialogRef.close(); screenRoot.setEffect(null); });
        BorderPane bottom = new BorderPane();
        bottom.setLeft(btnCancelar);
        bottom.setRight(btnAceptar);
        VBox card = new VBox(18, new Label("?"), titulo, sub, bottom);
        card.getChildren().get(0).setStyle("-fx-font-size: 56px; -fx-font-weight: bold; -fx-text-fill: " + AZUL_OSCURO + ";");
        card.setPadding(new Insets(28));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 18, 0, 0, 4); -fx-border-color: " + AZUL_SUAVE + "; -fx-border-radius: 16;");
        card.setMinWidth(430);
        StackPane overlay = new StackPane(card);
        overlay.setPadding(new Insets(12));
        Scene scene = new Scene(overlay, Color.TRANSPARENT);
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setScene(scene);
        this.dialogRef = dialog;
        dialog.show();
    }

    private void mostrarPopupExito() {
        screenRoot.setEffect(new GaussianBlur(14));
        Stage owner = ScreenRouter.getStage();
        Button btnOk = new Button("Aceptar");

        // --- INICIO DE LA CORRECCIÓN ---
        btnOk.setOnAction(e -> {
            dialogRef.close();
            screenRoot.setEffect(null);
            MenuScreen menu = new MenuScreen();
            menu.show(ScreenRouter.getStage());
            menu.showCitasProximas();
        });
        // --- FIN DE LA CORRECCIÓN ---

        final String AZUL_OSCURO = "#1F355E";
        final String AZUL_SUAVE = "#E9EEF5";
        Locale esMX = new Locale("es", "MX");
        String fechaLarga = fechaHoraSeleccionada.toLocalDate().format(DateTimeFormatter.ofPattern("d 'de' MMMM", esMX));
        String hora24 = fechaHoraSeleccionada.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm", esMX));
        String subtitulo = "La cita será el " + fechaLarga + " a las " + hora24 + " horas";
        Circle circle = new Circle(36, Color.TRANSPARENT);
        circle.setStroke(Color.web(AZUL_OSCURO));
        circle.setStrokeWidth(4);
        Label check = new Label("✔");
        check.setStyle("-fx-font-size: 30px; -fx-text-fill: " + AZUL_OSCURO + ";");
        StackPane icono = new StackPane(circle, check);
        Label titulo = new Label("Tu cita ha sido reagendada\nexitosamente.");
        titulo.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: " + AZUL_OSCURO + ";");
        titulo.setWrapText(true);
        titulo.setAlignment(Pos.CENTER);
        Label sub = new Label(subtitulo);
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7A99;");
        sub.setWrapText(true);
        sub.setAlignment(Pos.CENTER);
        btnOk.setStyle("-fx-background-color: " + AZUL_OSCURO + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 10 24;");
        VBox card = new VBox(22, icono, titulo, sub, btnOk);
        card.setPadding(new Insets(30));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: " + AZUL_SUAVE + "; -fx-background-radius: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 16, 0, 0, 4);");
        card.setMinWidth(460);
        StackPane overlay = new StackPane(card);
        overlay.setPadding(new Insets(12));
        Scene scene = new Scene(overlay, Color.TRANSPARENT);
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setScene(scene);
        this.dialogRef = dialog;
        dialog.show();
    }

    private void recargarOcupadosYRefrescar() {
        try {
            cargarOcupadosSemana();
            VBox content = (VBox) screenRoot.getCenter();
            // Se actualiza el contenido en las posiciones correctas (0 y 1)
            content.getChildren().set(0, construirBarraSemana());
            content.getChildren().set(1, construirTablaSemana());
        } catch (Exception ex) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar horarios ocupados: " + ex.getMessage());
        }
    }

    private void cargarOcupadosSemana() throws SQLException {
        ocupadosPorDia.clear();
        List<LocalDate> dias = diasDeSemana(semanaBase);
        LocalDate desde = dias.get(0);
        LocalDate hasta = dias.get(6);
        Long idPacienteNumber = obtenerIdPacientePorMatricula(this.matriculaPaciente);
        if (idPacienteNumber == null) {
            throw new SQLException("La matrícula del paciente no fue encontrada.");
        }
        final String sqlCitas = "SELECT FECHA_HORA FROM CITA WHERE (ID_MEDICO = ? OR ID_PACIENTE = ?) AND TRUNC(CAST(FECHA_HORA AS DATE)) BETWEEN ? AND ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlCitas)) {
            ps.setString(1, doctor.getId());
            ps.setLong(2, idPacienteNumber);
            ps.setDate(3, java.sql.Date.valueOf(desde));
            ps.setDate(4, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    addOcupado(rs.getTimestamp(1).toLocalDateTime());
                }
            }
        }
    }

    private void addOcupado(LocalDateTime ldt) {
        ocupadosPorDia.computeIfAbsent(ldt.toLocalDate(), k -> new HashSet<>()).add(ldt.toLocalTime().withSecond(0).withNano(0));
    }

    private boolean estaOcupado(LocalDate fecha, LocalTime hora) {
        Set<LocalTime> set = ocupadosPorDia.get(fecha);
        return set != null && set.contains(hora.withSecond(0).withNano(0));
    }

    private LocalDate obtenerLunes(LocalDate anyDay) {
        return anyDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private List<LocalDate> diasDeSemana(LocalDate lunes) {
        List<LocalDate> dias = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) dias.add(lunes.plusDays(i));
        return dias;
    }

    private Long obtenerIdPacientePorMatricula(String matricula) throws SQLException {
        if (matricula == null || matricula.isBlank()) return null;
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

    private void mostrarAlerta(Alert.AlertType tipo, String msg) {
        Alert a = new Alert(tipo, msg);
        a.initOwner(ScreenRouter.getStage());
        a.setHeaderText(null);
        a.showAndWait();
    }

    private static ImageView icon(String file, double w, double h) {
        try {
            var url = ReagendarScreen.class.getResource(BASE_ASSETS + file);
            if (url != null) return new ImageView(new Image(url.toExternalForm(), w, h, true, true));
        } catch (Exception e) { /* ignored */ }
        return new ImageView();
    }

    private static Label makeHeader(String text) {
        Label l = new Label(text);
        l.setPadding(new Insets(8));
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-background-color: #F7F9FC; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-font-weight: bold; -fx-background-radius: 6;");
        return l;
    }

    private static Label etiquetaColor(String texto, String fondo, String textoColor) {
        Label l = new Label(texto);
        l.setPadding(new Insets(6, 10, 6, 10));
        l.setStyle("-fx-background-color: " + fondo + "; -fx-text-fill: " + textoColor + "; -fx-background-radius: 6;");
        return l;
    }

    private void styleBotonPrimario(Button b) {
        b.setPadding(new Insets(8, 18, 8, 18));
        b.setStyle("-fx-background-color: " + COLOR_SELECCION + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold;");
    }

    private void styleBotonSecundario(Button b) {
        b.setPadding(new Insets(8, 18, 8, 18));
        b.setStyle("-fx-background-color: #EEF2F7; -fx-text-fill: " + COLOR_TEXTO_NORMAL + "; -fx-background-radius: 10; -fx-font-weight: bold;");
    }

    private void styleBotonSemana(Button b) {
        b.setPadding(new Insets(8, 16, 8, 16));
        b.setStyle("-fx-background-color: " + COLOR_SELECCION + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + COLOR_BORDE_AZUL + "; -fx-border-radius: 10;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #2D4A80; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + COLOR_BORDE_AZUL + "; -fx-border-radius: 10;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: " + COLOR_SELECCION + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-color: " + COLOR_BORDE_AZUL + "; -fx-border-radius: 10;"));
    }
}