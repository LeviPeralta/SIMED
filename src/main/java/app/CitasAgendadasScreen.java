package app;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.example.OracleWalletConnector;
import javafx.stage.Stage;
import javafx.scene.Cursor;

import java.sql.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CitasAgendadasScreen {

    private static final String AZUL_OSCURO = "#1F355E";
    private static final String AZUL_SUAVE  = "#E9EEF5";
    private static final String BORDE       = "#0F274A";


    public static void show(Pane hostContainer, String matriculaSesion) {
        VBox root = new VBox(18);
        root.setPadding(new Insets(16, 24, 24, 24));
        root.setStyle("-fx-background-color: white;");
        root.setAlignment(Pos.TOP_LEFT);

        // Breadcrumb
        HBox bc = new HBox(6);
        Label t1 = new Label("Inicio");
        Label dot = new Label("•");
        Label t2 = new Label("Mis Citas");

        t1.setStyle("-fx-text-fill: " + AZUL_OSCURO + "; -fx-underline: true;");
        dot.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");
        t2.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");

        t1.setCursor(Cursor.HAND);
        t1.setOnMouseClicked(e -> {
            Stage stage = (Stage) hostContainer.getScene().getWindow();
            new MenuScreen().show(stage);
        });

        bc.getChildren().addAll(t1, dot, t2);

        // Título centrado
        Label titulo = new Label("Citas agendadas");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 16));
        titulo.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");
        HBox tituloBox = new HBox(titulo);
        tituloBox.setAlignment(Pos.CENTER);

        // Contenedor de tarjetas
        VBox lista = new VBox(16);
        lista.setFillWidth(true);

        // Botón (placeholder) "Citas anteriores"
        Button btnPrev = new Button("Citas anteriores");
        btnPrev.setStyle("-fx-background-color: " + AZUL_OSCURO + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 14;");

        root.getChildren().addAll(bc, btnPrev, tituloBox, lista);

        // Cargar datos
        cargarCitas(matriculaSesion, lista);

        // --- Scroll vertical minimalista ---
        ScrollPane scroller = new ScrollPane(root);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setPannable(true); // permite arrastrar con el mouse/trackpad
        scroller.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");

        applyMinimalScrollBar(scroller);
        hostContainer.getChildren().setAll(scroller);

    }

    private static void cargarCitas(String matricula, VBox lista) {
        final String OWNER = "ADMIN"; // tu esquema

        // SQL: PACIENTE por matrícula
        final String qId =
                "SELECT ID_PACIENTE " +
                        "FROM " + OWNER + ".PACIENTE " +
                        "WHERE UPPER(MATRICULA)=?";

        // SQL: Citas + Médico (tabla MEDICOS) + Consultorio (CONSULTORIOS)
        final String qCitas =
                "SELECT c.ID_CITA, c.FECHA_HORA, " +
                        "       m.NOMBRE || ' ' || m.APELLIDOS AS MEDICO, " +
                        "       co.NOMBRE AS CONSULTORIO " +
                        "FROM " + OWNER + ".CITA c " +
                        "JOIN " + OWNER + ".MEDICOS m " +
                        "  ON m.ID_MEDICO = c.ID_MEDICO " +       // ambos VARCHAR2(20)
                        "LEFT JOIN " + OWNER + ".CONSULTORIOS co " +
                        "  ON co.ID_CONSULTORIO = m.ID_CONSULTORIO " +
                        "WHERE c.ID_PACIENTE = ? " +
                        "ORDER BY c.FECHA_HORA DESC";

        try (Connection con = OracleWalletConnector.getConnection()) {
            try (Statement st = con.createStatement()) {
                st.execute("ALTER SESSION SET CURRENT_SCHEMA=" + OWNER);
            }

            // 1) ID_PACIENTE
            Long idPac = null;
            try (PreparedStatement ps = con.prepareStatement(qId)) {
                ps.setString(1, matricula.toUpperCase().trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) idPac = rs.getLong(1);
                }
            }
            if (idPac == null) {
                lista.getChildren().add(mensaje("No se encontró el paciente para la matrícula: " + matricula));
                return;
            }

            // 2) Citas + Médico + Consultorio
            try (PreparedStatement ps = con.prepareStatement(qCitas)) {
                ps.setLong(1, idPac);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean hay = false;
                    while (rs.next()) {
                        hay = true;
                        long idCita = rs.getLong("ID_CITA");
                        Timestamp ts = rs.getTimestamp("FECHA_HORA");
                        String medico = rs.getString("MEDICO");
                        String consultorio = rs.getString("CONSULTORIO"); // nombre del consultorio
                        lista.getChildren().add(tarjetaCita(idCita, idPac, ts, medico, consultorio, lista));

                    }
                    if (!hay) lista.getChildren().add(mensaje("No tienes citas agendadas."));
                }
            }

        } catch (SQLException e) {
            lista.getChildren().add(mensaje("Error al cargar citas: " + e.getMessage()));
        }
    }

    private static Node mensaje(String txt) {
        Label l = new Label(txt);
        l.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");
        return l;
    }

    private static Node tarjetaCita(long idCita,
                                    long idPaciente,
                                    Timestamp ts,
                                    String medico,
                                    String consultorio,
                                    Pane hostContainer) {
        // Formateo de fecha/hora
        Locale esMX = new Locale("es", "MX");
        DateTimeFormatter fDia = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'del' yyyy", esMX);
        DateTimeFormatter fHora = DateTimeFormatter.ofPattern("h:mm a", esMX);
        String dia = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(fDia);
        String hora = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().format(fHora).toLowerCase();

        // Labels
        Label ld = new Label("Día: " + capitalize(dia));
        Label lh = new Label("Hora: " + hora);
        Label lm = new Label("Doctor: " + medico);
        Label lc = new Label("Consultorio: " + (consultorio == null ? "-" : consultorio));

        for (Label l: new Label[]{ld, lh, lm, lc}) {
            l.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");
        }
        ld.setFont(Font.font("System", FontWeight.BOLD, 12));
        lh.setFont(Font.font("System", FontWeight.BOLD, 12));

        // Botones
        Button btnCancelar = new Button("Cancelar cita");
        btnCancelar.setStyle("-fx-background-color: " + AZUL_SUAVE + "; -fx-text-fill: " + AZUL_OSCURO + "; -fx-background-radius: 8; -fx-padding: 8 12;");
        btnCancelar.setOnAction(e -> {
            cancelarCita(idCita, (VBox) hostContainer, String.valueOf(idPaciente));
        });

        Button btnReagendar = new Button("Reagendar cita");
        btnReagendar.setStyle("-fx-background-color: " + AZUL_SUAVE + "; -fx-text-fill: " + AZUL_OSCURO + "; -fx-background-radius: 8; -fx-padding: 8 12;");


        btnReagendar.setOnAction(e -> {
            Doctor doc = obtenerDoctorPorCita(idCita);

            if (doc == null) {
                System.err.println("No se encontró el doctor para la cita " + idCita);
                return;
            }

            ReagendarScreen.mostrarHorario(
                    doc,
                    doc.getEspecialidad(),
                    hostContainer,
                    String.valueOf(idPaciente),
                    (int) idCita
            );
        });



        HBox acciones = new HBox(10, btnCancelar, btnReagendar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(8);
        grid.add(ld, 0, 0);
        grid.add(lh, 1, 0);
        grid.add(lm, 0, 1, 2, 1);
        grid.add(lc, 0, 2, 2, 1);
        GridPane.setHgrow(acciones, Priority.ALWAYS);

        BorderPane card = new BorderPane();
        card.setPadding(new Insets(16));
        card.setLeft(grid);
        card.setRight(acciones);
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: " + BORDE + "; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10;"
        );

        VBox wrap = new VBox(card);
        wrap.setPadding(new Insets(4, 0, 0, 0));
        return wrap;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static boolean tableExists(Connection con, String owner, String table) throws SQLException {
        final String sql = "SELECT 1 FROM ALL_TABLES WHERE OWNER = ? AND TABLE_NAME = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, owner.toUpperCase());
            ps.setString(2, table.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }


    private static void applyMinimalScrollBar(ScrollPane sp) {
        // Cuando ya hay skin/render, personalizamos barras
        sp.skinProperty().addListener((obs, oldSkin, newSkin) -> Platform.runLater(() -> styleBars(sp)));
        // Si ya tenía skin, aplica de una vez
        Platform.runLater(() -> styleBars(sp));
    }

    private static void styleBars(ScrollPane sp) {
        // Oculta por completo la barra horizontal
        Node hbar = sp.lookup(".scroll-bar:horizontal");
        if (hbar != null) {
            hbar.setVisible(false);
            hbar.setManaged(false);
        }

        // Barra vertical ultra minimal
        Node vbar = sp.lookup(".scroll-bar:vertical");
        if (vbar != null) {
            // Delgadita y casi transparente, aparece un poco al pasar el mouse
            vbar.setStyle("-fx-background-color: transparent; -fx-pref-width: 6; -fx-opacity: 0.12;");
            vbar.hoverProperty().addListener((o, was, is) -> {
                vbar.setStyle("-fx-background-color: transparent; -fx-pref-width: 6; -fx-opacity: " + (is ? "0.55" : "0.12") + ";");
            });

            Node track = vbar.lookup(".track");
            if (track != null) track.setStyle("-fx-background-color: transparent;");

            Node thumb = vbar.lookup(".thumb");
            if (thumb != null) {
                // Color suave, redondeado
                thumb.setStyle("-fx-background-color: #A6B4CC; -fx-background-insets: 0; -fx-background-radius: 3;");
            }
        }
    }

    private static Doctor obtenerDoctorPorCita(long idCita) {
        Doctor doctor = null;
        final String OWNER = "ADMIN";

        String sql = "SELECT m.ID_MEDICO, m.NOMBRE, m.APELLIDOS, e.NOMBRE AS ESPECIALIDAD " +
                "FROM " + OWNER + ".CITA c " +
                "JOIN " + OWNER + ".MEDICOS m ON c.ID_MEDICO = m.ID_MEDICO " +
                "LEFT JOIN " + OWNER + ".ESPECIALIDADES e ON m.ID_ESPECIALIDAD = e.ID_ESPECIALIDAD " +
                "WHERE c.ID_CITA = ?";

        try (Connection conn = OracleWalletConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idCita);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                doctor = new Doctor(
                        rs.getString("ID_MEDICO"),
                        rs.getString("NOMBRE") + " " + rs.getString("APELLIDOS"),
                        null, // horario
                        null, // imagen
                        rs.getString("ESPECIALIDAD") // nombre de la especialidad
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return doctor;
    }

    private static void cancelarCita(long idCita, VBox lista, String matricula) {
        final String sqlDel = "DELETE FROM ADMIN.CITA WHERE ID_CITA = ?";

        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlDel)) {

            ps.setLong(1, idCita);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Cita cancelada correctamente: " + idCita);
                // Refrescar lista
                lista.getChildren().clear();
                cargarCitas(matricula, lista);
            } else {
                System.out.println("No se encontró la cita con ID: " + idCita);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }


}
