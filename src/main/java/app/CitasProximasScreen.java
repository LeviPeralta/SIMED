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

// RENOMBRADO DE CitasAgendadasScreen a CitasProximasScreen
public class CitasProximasScreen {

    private static final String AZUL_OSCURO = "#1F355E";
    private static final String AZUL_SUAVE  = "#E9EEF5";
    private static final String BORDE       = "#0F274A";

    public static void show(Pane hostContainer, String matriculaSesion) {
        VBox root = new VBox(18);
        root.setPadding(new Insets(16, 24, 24, 24));
        root.setStyle("-fx-background-color: white;");
        root.setAlignment(Pos.TOP_LEFT);

        HBox bc = new HBox(6);
        Label t1 = new Label("Inicio");
        Label dot = new Label("•");
        Label t2 = new Label("Mis Citas");

        t1.setStyle("-fx-text-fill: " + AZUL_OSCURO + "; -fx-underline: true;");
        dot.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");
        t2.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");

        t1.setCursor(Cursor.HAND);
        t1.setOnMouseClicked(e -> new MenuScreen().show((Stage) hostContainer.getScene().getWindow()));
        bc.getChildren().addAll(t1, dot, t2);

        Label titulo = new Label("Próximas Citas");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 18));
        titulo.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");

        Button btnAnteriores = new Button("Citas anteriores");
        btnAnteriores.setCursor(Cursor.HAND);
        btnAnteriores.setStyle("-fx-background-color: " + AZUL_OSCURO + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 14; -fx-font-weight: bold;");
        // << CAMBIO: Funcionalidad del botón
        btnAnteriores.setOnAction(e -> CitasAnterioresScreen.show(hostContainer, matriculaSesion));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox cabecera = new HBox(titulo, spacer, btnAnteriores);
        cabecera.setAlignment(Pos.CENTER);

        VBox lista = new VBox(16);
        lista.setFillWidth(true);

        root.getChildren().addAll(bc, cabecera, lista);

        cargarCitasProximas(matriculaSesion, lista);

        ScrollPane scroller = new ScrollPane(root);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        hostContainer.getChildren().setAll(scroller);
    }

    // << CAMBIO: El método ahora filtra solo citas próximas y sin expediente
    private static void cargarCitasProximas(String matricula, VBox lista) {
        String sql = "SELECT c.ID_CITA, c.FECHA_HORA, m.NOMBRE || ' ' || m.APELLIDOS AS MEDICO, co.NOMBRE AS CONSULTORIO " +
                "FROM ADMIN.CITA c " +
                "JOIN ADMIN.MEDICOS m ON m.ID_MEDICO = c.ID_MEDICO " +
                "JOIN ADMIN.PACIENTE p ON p.ID_PACIENTE = c.ID_PACIENTE " +
                "LEFT JOIN ADMIN.CONSULTORIOS co ON co.ID_CONSULTORIO = m.ID_CONSULTORIO " +
                "WHERE p.MATRICULA = ? " +
                "AND c.FECHA_HORA >= CURRENT_TIMESTAMP " +
                "AND NOT EXISTS (SELECT 1 FROM ADMIN.EXPEDIENTE_MEDICO e WHERE e.ID_CITA = c.ID_CITA) " +
                "ORDER BY c.FECHA_HORA ASC";

        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, matricula.toUpperCase().trim());
            ResultSet rs = ps.executeQuery();

            boolean hayCitas = false;
            while (rs.next()) {
                hayCitas = true;
                int idCita = rs.getInt("ID_CITA");
                Timestamp ts = rs.getTimestamp("FECHA_HORA");
                String medico = rs.getString("MEDICO");
                String consultorio = rs.getString("CONSULTORIO");
                // La tarjeta original con botones de cancelar/reagendar se mantiene
                lista.getChildren().add(tarjetaCita(idCita, matricula, ts, medico, consultorio));
            }
            if (!hayCitas) {
                lista.getChildren().add(new Label("No tienes próximas citas agendadas."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            lista.getChildren().add(new Label("Error al cargar las próximas citas."));
        }
    }

    // El resto de los métodos (tarjetaCita, capitalize, cancelarCita, etc.)
    // se mantienen exactamente igual que en tu archivo original.
    // Solo copia y pega el método cargarCitasProximas de arriba.

    private static Node tarjetaCita(int idCita, String matricula, Timestamp ts, String medico, String consultorio) {
        Locale esMX = new Locale("es", "MX");
        DateTimeFormatter fDia = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'del' yyyy", esMX);
        DateTimeFormatter fHora = DateTimeFormatter.ofPattern("h:mm a", esMX);
        String dia = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(fDia);
        String hora = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().format(fHora).toLowerCase();

        Label ld = new Label("Día: " + capitalize(dia));
        Label lh = new Label("Hora: " + hora);
        Label lm = new Label("Doctor: " + medico);
        Label lc = new Label("Consultorio: " + (consultorio == null ? "-" : consultorio));

        for (Label l: new Label[]{ld, lh, lm, lc}) {
            l.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");
        }
        ld.setFont(Font.font("System", FontWeight.BOLD, 12));
        lh.setFont(Font.font("System", FontWeight.BOLD, 12));

        Button btnCancelar = new Button("Cancelar cita");
        btnCancelar.setStyle("-fx-background-color: " + AZUL_SUAVE + "; -fx-text-fill: " + AZUL_OSCURO + "; -fx-background-radius: 8; -fx-padding: 8 12;");
        btnCancelar.setOnAction(e -> {
            VBox lista = (VBox)((Node)e.getSource()).getParent().getParent().getParent().getParent();
            cancelarCita(idCita, lista, matricula);
        });

        Button btnReagendar = new Button("Reagendar cita");
        btnReagendar.setStyle("-fx-background-color: " + AZUL_SUAVE + "; -fx-text-fill: " + AZUL_OSCURO + "; -fx-background-radius: 8; -fx-padding: 8 12;");

        btnReagendar.setOnAction(e -> {
            Doctor doc = obtenerDoctorPorCita(idCita);
            if (doc == null) {
                System.err.println("No se encontró el doctor para la cita " + idCita);
                return;
            }
            ReagendarScreen.show(
                    (Stage)btnReagendar.getScene().getWindow(),
                    doc,
                    doc.getEspecialidad(),
                    matricula,
                    idCita
            );
        });

        HBox acciones = new HBox(10, btnCancelar, btnReagendar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        VBox detalles = new VBox(5, ld, lh, lm, lc);

        BorderPane card = new BorderPane();
        card.setPadding(new Insets(16));
        card.setLeft(detalles);
        card.setRight(acciones);
        card.setStyle("-fx-background-color: white; -fx-border-color: " + BORDE + "; -fx-border-radius: 10; -fx-background-radius: 10;");

        return card;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Doctor obtenerDoctorPorCita(int idCita) {
        Doctor doctor = null;
        String sql = "SELECT m.ID_MEDICO, m.NOMBRE, m.APELLIDOS, e.NOMBRE AS ESPECIALIDAD " +
                "FROM ADMIN.CITA c " +
                "JOIN ADMIN.MEDICOS m ON c.ID_MEDICO = m.ID_MEDICO " +
                "LEFT JOIN ADMIN.ESPECIALIDADES e ON m.ID_ESPECIALIDAD = e.ID_ESPECIALIDAD " +
                "WHERE c.ID_CITA = ?";

        try (Connection conn = OracleWalletConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idCita);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                doctor = new Doctor(
                        rs.getString("ID_MEDICO"),
                        rs.getString("NOMBRE") + " " + rs.getString("APELLIDOS"),
                        null, null, rs.getString("ESPECIALIDAD")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return doctor;
    }

    private static void cancelarCita(int idCita, VBox lista, String matricula) {
        String sqlDel = "DELETE FROM ADMIN.CITA WHERE ID_CITA = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlDel)) {
            ps.setInt(1, idCita);
            if (ps.executeUpdate() > 0) {
                if (lista.getScene() != null && lista.getScene().getRoot() instanceof Pane) {
                    Pane hostContainer = (Pane) lista.getScene().getRoot();
                    show(hostContainer, matricula);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}