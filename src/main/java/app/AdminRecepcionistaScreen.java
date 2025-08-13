package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.OracleWalletConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminRecepcionistaScreen {

    // contenedor central para overlays (mis citas, etc.)
    private final StackPane centerContainer = new StackPane();

    // Carpeta base dentro de src/main/resources
    private static final String BASE_ASSETS = "/images/mainPage/";

    private static ImageView safeIcon(String fileName, double w, double h) {
        String path = BASE_ASSETS + fileName;
        var url = AdminRecepcionistaScreen.class.getResource(path);
        if (url == null) {
            System.err.println("⚠ Icono no encontrado: " + path + "  -> usando ImageView vacío");
            return new ImageView(); // vacío (no truena)
        }
        ImageView iv = new ImageView(new Image(url.toExternalForm()));
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        return iv;
    }

    private static ImageView createIcon(String fileName, double w, double h) {
        return safeIcon(fileName, w, h);
    }

    public void show(Stage stage, String nombreUsuario) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: white;");
        stage.setTitle("SIMED - Sistema de Citas Médicas");

        // ===== Barra superior =====
        HBox menuBar = new HBox();
        menuBar.setStyle("-fx-background-color: #FFFFFF;");
        menuBar.setPadding(new Insets(0, 40, 0, 40));
        menuBar.setSpacing(10);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        ImageView simedIcon = createIcon("Logo.png", 120, 120);

        String estiloBoton = "-fx-background-color: #D0E1F9; " +
                "-fx-text-fill: #1F355E; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-padding: 10 20 10 20;";

        String estiloEmergencia = "-fx-background-color: #B1361E; " +
                "-fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-padding: 10 20 10 20;";

        Button btnInicio = new Button("Inicio", createIcon("Inicio.png", 24, 24));
        btnInicio.setContentDisplay(ContentDisplay.LEFT);
        btnInicio.setGraphicTextGap(8);
        btnInicio.setStyle(estiloBoton);
        btnInicio.setMinHeight(40);
        // si Inicio debe recargar esta misma pantalla, puedes dejarlo sin acción

        Button btnEmergencia = new Button("EMERGENCIA");
        btnEmergencia.setStyle(estiloEmergencia);
        btnEmergencia.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnEmergencia.setMinHeight(40);

        HBox centerButtons = new HBox(btnInicio, btnEmergencia);
        centerButtons.setSpacing(60);
        centerButtons.setAlignment(Pos.CENTER);
        centerButtons.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(centerButtons, Priority.ALWAYS);

        // Nombre de usuario (desde BD por matrícula)
        String m = Sesion.getMatricula();
        String nombrePaciente = obtenerNombrePacientePorMatricula(m);
        if (nombrePaciente == null || nombrePaciente.isBlank()) {
            System.out.println("⚠ No se encontró paciente para matrícula: " + m);
            nombrePaciente = (nombreUsuario == null || nombreUsuario.isBlank()) ? "Paciente" : nombreUsuario;
        }

        Label lblUsuario = new Label(nombrePaciente, createIcon("User.png", 24, 24));
        lblUsuario.setFont(Font.font("System", 14));
        lblUsuario.setTextFill(Color.web("#1F355E"));
        lblUsuario.setContentDisplay(ContentDisplay.LEFT);
        lblUsuario.setGraphicTextGap(8);

        Button btnSalir = new Button("", createIcon("Close.png", 24, 24));
        btnSalir.setStyle("-fx-background-color: #1F355E;");
        btnSalir.setOnAction(e -> {
            Stage currentStage = (Stage) btnSalir.getScene().getWindow();
            currentStage.close();
            Stage loginStage = new Stage();
            try { new org.example.Main().start(loginStage); } catch (Exception ex) { ex.printStackTrace(); }
        });

        Region spacerL = new Region();
        Region spacerR = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        menuBar.getChildren().addAll(simedIcon, spacerL, centerButtons, spacerR, lblUsuario, btnSalir);

        // ===== Centro: dos tarjetas grandes (Paciente / Médico) =====
        HBox cardsRow = new HBox(80);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setPadding(new Insets(60));

        StackPane cardPaciente = crearCardGrande("Paciente", safeIcon("patientsss.png", 60, 60));
        StackPane cardMedico   = crearCardGrande("Médico",   safeIcon("doctorsss.png", 60, 60));

        // Navegación
        cardPaciente.setOnMouseClicked(ev -> new MenuScreen().show(stage));
        cardMedico.setOnMouseClicked(ev -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));

        cardsRow.getChildren().addAll(cardPaciente, cardMedico);

        // Montaje final: el centro se mete dentro de centerContainer
        centerContainer.getChildren().setAll(cardsRow);
        root.getChildren().addAll(menuBar, centerContainer);

        Scene scene = new Scene(root, 1000, 700);
        ScreenRouter.setView(root);
        stage.setMaximized(true);
        stage.show();
    }

    /** Card grande con burbuja superior e icono centrado. */
    private StackPane crearCardGrande(String titulo, Node icono) {
        // Burbuja superior
        Circle circle = new Circle(50);
        circle.setFill(Color.web("#F3F7FB"));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(8);
        StackPane iconCircle = new StackPane(circle, icono);
        iconCircle.setPrefSize(100, 100);
        iconCircle.setTranslateY(5);

        // Panel rectangular
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setTranslateY(90);
        panel.setPrefSize(370, 200);
        panel.setStyle("-fx-background-color: #F3F7FB; -fx-background-radius: 20;");

        Label lbl = new Label(titulo);
        lbl.setFont(Font.font("System", FontWeight.BOLD, 28));
        lbl.setTextFill(Color.web("#1F355E"));

        VBox content = new VBox(iconCircle, lbl);
        lbl.setTranslateY(30);
        content.setAlignment(Pos.CENTER);

        StackPane card = new StackPane(panel, content);
        card.setPadding(new Insets(10));

        // Hover
        card.setOnMouseEntered(e -> {
            panel.setScaleX(1.05); panel.setScaleY(1.05);
            iconCircle.setTranslateY(-25);
        });
        card.setOnMouseExited(e -> {
            panel.setScaleX(1.0); panel.setScaleY(1.0);
            iconCircle.setTranslateY(5);
        });

        return card;
    }

    private String obtenerNombrePacientePorMatricula(String matriculaSesion) {
        if (matriculaSesion == null || matriculaSesion.isBlank()) return null;

        String mat = matriculaSesion.trim().replaceAll("\\s+", "").toUpperCase();
        String correoInstitucional = mat.toLowerCase() + "@utez.edu.mx";

        final String sql =
                "SELECT NOMBRE, APELLIDOS " +
                        "FROM ADMIN.PACIENTE " +
                        "WHERE UPPER(MATRICULA) = ? OR LOWER(CORREO) = ?";

        try (Connection conn = OracleWalletConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, mat);
            ps.setString(2, correoInstitucional);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String nombre = rs.getString("NOMBRE");
                    String apellidos = rs.getString("APELLIDOS");
                    String completo = ((nombre == null ? "" : nombre.trim()) + " " +
                            (apellidos == null ? "" : apellidos.trim())).trim();
                    return completo.isEmpty() ? null : completo;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
