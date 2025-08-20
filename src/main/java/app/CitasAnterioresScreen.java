package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.example.OracleWalletConnector;

public class CitasAnterioresScreen {

    private static final String AZUL_OSCURO = "#1F355E";
    private static final String AZUL_SUAVE = "#E9EEF5";
    private static final String BORDE = "#0F274A";

    public static void show(Pane hostContainer, String matriculaSesion) {
        VBox root = new VBox(18);
        root.setPadding(new Insets(16, 24, 24, 24));
        root.setStyle("-fx-background-color: white;");
        root.setAlignment(Pos.TOP_LEFT);

        // --- Breadcrumbs (Migas de pan) ---
        HBox breadcrumbs = new HBox(6);
        Label t1 = new Label("Inicio");
        Label sep1 = new Label("•");
        Label t2 = new Label("Mis Citas");
        Label sep2 = new Label("•");
        Label t3 = new Label("Citas Anteriores");

        t1.setStyle("-fx-text-fill: " + AZUL_OSCURO + "; -fx-underline: true;");
        t2.setStyle("-fx-text-fill: " + AZUL_OSCURO + "; -fx-underline: true;");
        t3.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");
        sep1.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");
        sep2.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");

        t1.setCursor(Cursor.HAND);
        t1.setOnMouseClicked(e -> new MenuScreen().show((Stage) hostContainer.getScene().getWindow()));

        t2.setCursor(Cursor.HAND);
        t2.setOnMouseClicked(e -> CitasProximasScreen.show(hostContainer, matriculaSesion));

        breadcrumbs.getChildren().addAll(t1, sep1, t2, sep2, t3);

        // --- Cabecera con Título y Botón Agendar ---
        Label titulo = new Label("Citas anteriores");
        titulo.setFont(Font.font("System", FontWeight.BOLD, 18));
        titulo.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox cabecera = new HBox(titulo, spacer);
        cabecera.setAlignment(Pos.CENTER);

        VBox listaCitas = new VBox(16);
        listaCitas.setFillWidth(true);

        root.getChildren().addAll(breadcrumbs, cabecera, listaCitas);

        cargarCitasAnteriores(matriculaSesion, listaCitas);

        // --- ScrollPane para el contenido ---
        ScrollPane scroller = new ScrollPane(root);
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        hostContainer.getChildren().setAll(scroller);
    }

    private static void cargarCitasAnteriores(String matricula, VBox lista) {
        // SQL que obtiene citas si la fecha ya pasó O si ya existe un expediente para esa cita
        String sql = "SELECT c.ID_CITA, c.FECHA_HORA, p.NOMBRE || ' ' || p.APELLIDOS AS PACIENTE, " +
                "m.NOMBRE || ' ' || m.APELLIDOS AS MEDICO, co.NOMBRE AS CONSULTORIO " +
                "FROM ADMIN.CITA c " +
                "JOIN ADMIN.PACIENTE p ON c.ID_PACIENTE = p.ID_PACIENTE " +
                "JOIN ADMIN.MEDICOS m ON c.ID_MEDICO = m.ID_MEDICO " +
                "LEFT JOIN ADMIN.CONSULTORIOS co ON m.ID_CONSULTORIO = co.ID_CONSULTORIO " +
                "WHERE p.MATRICULA = ? " +
                "AND (c.FECHA_HORA < CURRENT_TIMESTAMP OR EXISTS (SELECT 1 FROM ADMIN.EXPEDIENTE_MEDICO e WHERE e.ID_CITA = c.ID_CITA)) " +
                "ORDER BY c.FECHA_HORA DESC";

        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, matricula.toUpperCase().trim());
            ResultSet rs = ps.executeQuery();

            boolean hayCitas = false;
            while (rs.next()) {
                hayCitas = true;
                long idCita = rs.getLong("ID_CITA");
                Timestamp ts = rs.getTimestamp("FECHA_HORA");
                String paciente = rs.getString("PACIENTE");
                String medico = rs.getString("MEDICO");
                String consultorio = rs.getString("CONSULTORIO");
                lista.getChildren().add(tarjetaCitaAnterior(idCita, ts, paciente, medico, consultorio));
            }
            if (!hayCitas) {
                lista.getChildren().add(new Label("No tienes citas en tu historial."));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            lista.getChildren().add(new Label("Error al cargar el historial de citas."));
        }
    }

    private static Node tarjetaCitaAnterior(long idCita, Timestamp ts, String paciente, String medico, String consultorio) {
        Locale esMX = new Locale("es", "MX");
        DateTimeFormatter fDia = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'del' yyyy", esMX);
        DateTimeFormatter fHora = DateTimeFormatter.ofPattern("h:mm a", esMX);
        String dia = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(fDia);
        String hora = ts.toInstant().atZone(ZoneId.systemDefault()).toLocalTime().format(fHora).toLowerCase();

        Label lblPaciente = new Label("Paciente: " + paciente);
        lblPaciente.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label lblDia = new Label("Día: " + capitalize(dia));
        Label lblHora = new Label("Hora: " + hora);
        Label lblMedico = new Label("Doctor: " + medico);
        Label lblConsultorio = new Label("Consultorio: " + (consultorio == null ? "N/A" : consultorio));

        for (Label l : new Label[]{lblPaciente, lblDia, lblHora, lblMedico, lblConsultorio}) {
            l.setStyle("-fx-text-fill: " + AZUL_OSCURO + ";");
        }

        Button btnVerHistorial = new Button("Ver historial médico");
        btnVerHistorial.setCursor(Cursor.HAND);
        btnVerHistorial.setStyle("-fx-background-color: " + AZUL_SUAVE + "; -fx-text-fill: " + AZUL_OSCURO + "; -fx-background-radius: 8; -fx-padding: 8 12;");
        btnVerHistorial.setOnAction(e -> {
            Stage stage = (Stage) btnVerHistorial.getScene().getWindow();
            new VerExpedienteScreen().show(stage, idCita);
        });

        VBox detalles = new VBox(5, lblDia, lblHora, lblMedico, lblConsultorio);

        BorderPane cardLayout = new BorderPane();
        cardLayout.setLeft(detalles);
        cardLayout.setRight(btnVerHistorial);
        BorderPane.setAlignment(btnVerHistorial, Pos.CENTER);

        VBox card = new VBox(10, lblPaciente, cardLayout);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color: #DDE3EA; -fx-border-radius: 10; -fx-background-radius: 10;");

        return card;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}