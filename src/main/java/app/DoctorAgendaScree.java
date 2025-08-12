package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.OracleWalletConnector;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

public class DoctorAgendaScree {

    // Colores (tu paleta)
    private static final String AZUL_OSCURO = "#1F355E";
    private static final String AZUL_BORDE = "#0F274A";
    private static final String AZUL_OCUPADO = "#6D84A2"; // mismo bloqueado
    private static final String FONDO_TABLA = "#FFFFFF";

    // rango e intervalo
    private static final LocalTime HORA_INICIO = LocalTime.of(9, 0);
    private static final LocalTime HORA_FIN = LocalTime.of(14, 0);
    private static final int INTERVALO = 30;

    private final Map<LocalDate, Map<LocalTime, String>> citas = new HashMap<>();
    private LocalDate semanaBase;
    private String idMedico;

    private BorderPane root;
    private VBox centerContent;
    private Stage stage;

    public void show(Stage stage, String idMedico, String nombreMedico) {
        this.stage = stage;

        // root y center
        root = new BorderPane();
        root.setPadding(new Insets(16));
        centerContent = new VBox(16);
        centerContent.setAlignment(Pos.TOP_CENTER);
        root.setCenter(centerContent);

        // Inicializa semanaBase al lunes de la semana actual
        this.semanaBase = startOfWeek(LocalDate.now());  // <<<<<<

        root.setTop(buildTopBar(stage));

        // pinta semana
        renderWeek();

        Scene sc = stage.getScene();
        if (sc == null) {
            sc = new Scene(root, 1200, 800);
            stage.setScene(sc);
        } else {
            sc.setRoot(root);
        }

        stage.setMaximized(true);
        javafx.application.Platform.runLater(() -> stage.setMaximized(true));
        stage.show();
    }



    private Node buildWeekView() {
        cargarCitasSemana(); // rellena el mapa 'citas' con la semana actual

        StackPane wrapper = new StackPane();
        wrapper.setPadding(new Insets(10));
        wrapper.setStyle("-fx-background-color: #0F274A; -fx-background-radius: 10;");

        GridPane tabla = new GridPane();
        tabla.setHgap(6);
        tabla.setVgap(6);
        tabla.setPadding(new Insets(14));
        tabla.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8;");
        wrapper.getChildren().add(tabla);

        List<LocalDate> dias = diasDeSemana(semanaBase);
        var nombres = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");

        // encabezados
        tabla.add(makeHeader("Horario"), 0, 0);
        for (int c = 0; c < 7; c++) {
            Label hd = makeHeader(nombres.get(c));
            final LocalDate day = dias.get(c);
            hd.setOnMouseClicked(e -> mostrarDia(day)); // ← CLICK EN EL DÍA
            tabla.add(hd, c + 1, 0);

            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 8.0);
            cc.setHgrow(Priority.ALWAYS);
            tabla.getColumnConstraints().add(cc);
        }

        int row = 1;
        DateTimeFormatter hfmt = DateTimeFormatter.ofPattern("h:mm a", new Locale("es", "MX"));
        for (LocalTime t = HORA_INICIO; !t.isAfter(HORA_FIN); t = t.plusMinutes(INTERVALO)) {
            Label lhora = new Label(t.format(hfmt).toLowerCase());
            lhora.setStyle("-fx-text-fill: #1F355E; -fx-font-weight: bold;");
            tabla.add(lhora, 0, row);

            for (int c = 0; c < 7; c++) {
                LocalDate d = dias.get(c);
                StackPane celda = new StackPane();
                celda.setMinHeight(38);
                celda.setStyle("-fx-background-color: #EFF4FA; -fx-background-radius: 6;");
                celda.setBorder(new Border(new BorderStroke(Color.web("#CAD3E0"),
                        BorderStrokeStyle.SOLID, new CornerRadii(6), BorderWidths.DEFAULT)));

                // Si hay cita, pintamos azul y agregamos tooltip con nombre
                String paciente = getPacienteEn(d, t);
                if (paciente != null) {
                    celda.setStyle("-fx-background-color: #6D84A2; -fx-background-radius: 6;");
                    Tooltip.install(celda, new Tooltip("Cita: " + paciente));
                }

                // CLICK EN CUALQUIER CELDA DEL DÍA → vista por día
                celda.setOnMouseClicked(e -> mostrarDia(d));

                tabla.add(celda, c + 1, row);
                GridPane.setHgrow(celda, Priority.ALWAYS);
            }

            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.NEVER);
            tabla.getRowConstraints().add(rc);
            row++;
        }

        return wrapper;
    }

    private void renderDay(LocalDate dia) {
        Node dayView = buildDayView(dia); // Devuelve un VBox/GridPane (no ScrollPane)

        centerContent.getChildren().setAll(dayView);
        root.setBottom(buildBackBarToWeek()); // muestra "Atrás" SOLO en día
    }

    private HBox buildBackBarToWeek() {
        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle("-fx-background-color:#EEF2F7; -fx-text-fill:#1F355E; -fx-background-radius:8; -fx-padding:8 18;");
        btnAtras.setOnAction(e -> renderWeek()); // Vuelve a la semana del MÉDICO

        HBox bar = new HBox(btnAtras);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 0, 18, 14));
        return bar;
    }

    // ==== BD ====
    private void cargarCitasSemana() {
        citas.clear();
        List<LocalDate> dias = diasDeSemana(semanaBase);
        LocalDate desde = dias.get(0);
        LocalDate hasta = dias.get(6);

        final String sql =
                "SELECT c.FECHA_HORA, p.NOMBRE || ' ' || p.APELLIDOS AS PACIENTE " +
                        "FROM   ADMIN.CITA c " +
                        "JOIN   ADMIN.PACIENTE p ON p.ID_PACIENTE = c.ID_PACIENTE " +
                        "WHERE  c.ID_MEDICO = ? " +
                        "AND    TRUNC(CAST(c.FECHA_HORA AS DATE)) BETWEEN ? AND ?";

        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idMedico);
            ps.setDate(2, java.sql.Date.valueOf(desde));
            ps.setDate(3, java.sql.Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime ldt = rs.getTimestamp(1).toLocalDateTime().withSecond(0).withNano(0);
                    String paciente = rs.getString(2);
                    citas.computeIfAbsent(ldt.toLocalDate(), k -> new HashMap<>())
                            .put(ldt.toLocalTime(), paciente);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getPacienteEn(LocalDate d, LocalTime t) {
        Map<LocalTime, String> m = citas.get(d);
        return (m == null) ? null : m.get(t.withSecond(0).withNano(0));
    }

    // ==== util ====
    private static Label makeHeader(String text) {
        Label l = new Label(text);
        l.setAlignment(Pos.CENTER);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle("-fx-background-color: #F7F9FC; -fx-text-fill: " + AZUL_OSCURO + "; -fx-font-weight: bold; -fx-background-radius: 6;");
        l.setPadding(new Insets(8, 6, 8, 6));
        return l;
    }

    private static LocalDate obtenerLunes(LocalDate any) {
        var wf = java.time.temporal.WeekFields.of(Locale.getDefault());
        return any.with(wf.dayOfWeek(), 1);
    }

    // ===== Barra superior estilo MenuScreen =====
    private HBox buildTopBar(Stage stage) {
        HBox menuBar = new HBox();
        menuBar.setStyle("-fx-background-color: #FFFFFF;");
        menuBar.setPadding(new Insets(0, 40, 0, 40));
        menuBar.setSpacing(10);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        ImageView simedIcon = icon("Logo.png", 120, 120);

        String estiloBoton = "-fx-background-color: #D0E1F9; " +
                "-fx-text-fill: #1F355E; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-padding: 10 20 10 20;";

        String estiloEmergencia = "-fx-background-color: #B1361E; " +
                "-fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-padding: 10 20 10 20;";

        Button btnInicio = new Button("Inicio", icon("Inicio.png", 24, 24));
        btnInicio.setContentDisplay(ContentDisplay.LEFT);
        btnInicio.setGraphicTextGap(8);
        btnInicio.setStyle(estiloBoton);
        btnInicio.setMinHeight(40);
        // Para médico, simplemente recarga su agenda
        btnInicio.setOnAction(e -> {
            Stage st = (Stage) btnInicio.getScene().getWindow();
            // volver a la agenda del médico (no al menú)
            renderWeek();
            st.setMaximized(true);
            javafx.application.Platform.runLater(() -> st.setMaximized(true));
        });




        Button btnEmergencia = new Button("EMERGENCIA");
        btnEmergencia.setStyle(estiloEmergencia);
        btnEmergencia.setMinHeight(40);

        HBox centerButtons = new HBox(btnInicio, btnEmergencia);
        centerButtons.setSpacing(60);
        centerButtons.setAlignment(Pos.CENTER);
        centerButtons.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(centerButtons, Priority.ALWAYS);

        Label lblUsuario = new Label(app.Sesion.getNombreUsuario(), icon("User.png", 24, 24));
        lblUsuario.setContentDisplay(ContentDisplay.LEFT);
        lblUsuario.setGraphicTextGap(8);
        lblUsuario.setTextFill(Color.web("#1F355E"));

        Button btnSalir = new Button("", icon("Close.png", 24, 24));
        btnSalir.setStyle("-fx-background-color: #1F355E;");
        btnSalir.setOnAction(e -> {
            // Cerrar y volver al login
            Stage current = (Stage) menuBar.getScene().getWindow();
            current.close();
            Stage login = new Stage();
            try {
                new org.example.Main().start(login);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Region spacerL = new Region();
        Region spacerR = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        menuBar.getChildren().addAll(simedIcon, spacerL, centerButtons, spacerR, lblUsuario, btnSalir);
        return menuBar;
    }

    // Cargar iconos como en MenuScreen
    private ImageView icon(String fileName, double w, double h) {
        String path = "/images/mainPage/" + fileName;
        Image img = new Image(getClass().getResource(path).toExternalForm());
        ImageView iv = new ImageView(img);
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        return iv;
    }

    private void mostrarDia(LocalDate dia) {
        centerContent.getChildren().setAll(buildDayView(dia));
    }

    private Node buildDayView(LocalDate dia) {
        // Traemos las citas del día con nombres de paciente
        Map<LocalTime, String> citasDelDia = cargarCitasDia(dia);

        StackPane wrapper = new StackPane();
        wrapper.setPadding(new Insets(10));
        wrapper.setStyle("-fx-background-color: #0F274A; -fx-background-radius: 10;");

        GridPane tabla = new GridPane();
        tabla.setHgap(6);
        tabla.setVgap(6);
        tabla.setPadding(new Insets(14));
        tabla.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8;");
        wrapper.getChildren().add(tabla);

        // Encabezados
        tabla.add(makeHeader("Horario"), 0, 0);
        Label hd = makeHeader("Citas de hoy");
        tabla.add(hd, 1, 0);

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(18);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(82);
        c1.setHgrow(Priority.ALWAYS);
        tabla.getColumnConstraints().addAll(c0, c1);

        // Filas por horario
        DateTimeFormatter hfmt = DateTimeFormatter.ofPattern("h:mm a", new Locale("es", "MX"));
        int row = 1;
        for (LocalTime t = HORA_INICIO; !t.isAfter(HORA_FIN); t = t.plusMinutes(INTERVALO)) {
            Label lhora = new Label(t.format(hfmt).toLowerCase());
            lhora.setStyle("-fx-text-fill: #1F355E; -fx-font-weight: bold;");
            tabla.add(lhora, 0, row);

            StackPane celda = new StackPane();
            celda.setMinHeight(38);
            celda.setBorder(new Border(new BorderStroke(Color.web("#CAD3E0"),
                    BorderStrokeStyle.SOLID, new CornerRadii(6), BorderWidths.DEFAULT)));

            String paciente = citasDelDia.get(t.withSecond(0).withNano(0));
            if (paciente != null) {
                // OCUPADA: azul con nombre
                celda.setStyle("-fx-background-color: #6D84A2; -fx-background-radius: 6;");
                Label name = new Label(paciente);
                name.setStyle("-fx-text-fill: white; -fx-font-weight: normal;");
                celda.getChildren().add(name);
                StackPane.setAlignment(name, Pos.CENTER_LEFT);
                StackPane.setMargin(name, new Insets(0, 0, 0, 14));
            } else {
                // DISPONIBLE: blanca
                celda.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 6;");
            }

            tabla.add(celda, 1, row);

            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.NEVER);
            tabla.getRowConstraints().add(rc);
            row++;
        }

        // Botón Atrás
        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle("-fx-background-color: #EEF2F7; -fx-text-fill: #1F355E; -fx-background-radius: 8; -fx-padding: 8 18;");
        btnAtras.setOnAction(e -> centerContent.getChildren().setAll(buildWeekView()));

        HBox footer = new HBox(btnAtras);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(12, 0, 0, 14));

        VBox content = new VBox(wrapper, footer);
        content.setSpacing(10);
        return content;
    }

    private Map<LocalTime, String> cargarCitasDia(LocalDate dia) {
        Map<LocalTime, String> map = new HashMap<>();
        final String sql =
                "SELECT c.FECHA_HORA, p.NOMBRE || ' ' || p.APELLIDOS AS PACIENTE " +
                        "FROM   ADMIN.CITA c " +
                        "JOIN   ADMIN.PACIENTE p ON p.ID_PACIENTE = c.ID_PACIENTE " +
                        "WHERE  c.ID_MEDICO = ? AND TRUNC(CAST(c.FECHA_HORA AS DATE)) = ?";

        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idMedico);
            ps.setDate(2, java.sql.Date.valueOf(dia));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime ldt = rs.getTimestamp(1).toLocalDateTime().withSecond(0).withNano(0);
                    String paciente = rs.getString(2);
                    map.put(ldt.toLocalTime(), paciente);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }
    private void renderWeek() {
        Node weekGrid = buildWeekView(); // tu método que pinta la semana

        ScrollPane sp = new ScrollPane(weekGrid);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");

        centerContent.getChildren().setAll(sp);
        root.setBottom(null); // NO mostrar "Atrás" en semana
    }

    private static LocalDate startOfWeek(LocalDate date) {
        if (date == null) date = LocalDate.now();
        WeekFields wf = WeekFields.of(Locale.getDefault());
        return date.with(wf.dayOfWeek(), 1); // lunes
    }

    private List<LocalDate> diasDeSemana(LocalDate base) {
        LocalDate start = startOfWeek(base); // nunca null
        List<LocalDate> dias = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) dias.add(start.plusDays(i));
        return dias;
    }

}