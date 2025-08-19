package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import org.example.OracleWalletConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminRecepcionistaScreen {

    private final StackPane centerContainer = new StackPane();
    private static final String BASE_ASSETS = "/images/mainPage/";

    private static ImageView safeIcon(String fileName, double w, double h) {
        String path = BASE_ASSETS + fileName;
        var url = AdminRecepcionistaScreen.class.getResource(path);
        if (url == null) {
            System.err.println("⚠ Icono no encontrado: " + path + "  -> usando ImageView vacío");
            return new ImageView();
        }
        ImageView iv = new ImageView(new Image(url.toExternalForm()));
        iv.setFitWidth(w);
        iv.setFitHeight(h);
        return iv;
    }

    private static ImageView createIcon(String fileName, double w, double h) {
        return safeIcon(fileName, w, h);
    }

    // Nuevo método: devuelve la vista de la pantalla
    public Parent getView() {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: white;");

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
        // Si inicio recarga la misma pantalla
        btnInicio.setOnAction(e -> this.show());

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
            nombrePaciente = "Paciente";
        }

        Label lblUsuario = new Label(nombrePaciente, createIcon("User.png", 24, 24));
        lblUsuario.setFont(Font.font("System", 14));
        lblUsuario.setTextFill(Color.web("#1F355E"));
        lblUsuario.setContentDisplay(ContentDisplay.LEFT);
        lblUsuario.setGraphicTextGap(8);

        Button btnSalir = new Button("", createIcon("Close.png", 24, 24));
        btnSalir.setStyle("-fx-background-color: #1F355E;");
        btnSalir.setOnAction(e -> ScreenRouter.getStage().close());

        Region spacerL = new Region();
        Region spacerR = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        menuBar.getChildren().addAll(simedIcon, spacerL, centerButtons, spacerR, lblUsuario, btnSalir);

        // ===== Centro: tarjetas Paciente / Médico =====
        HBox cardsRow = new HBox(80);
        cardsRow.setAlignment(Pos.CENTER);
        cardsRow.setPadding(new Insets(60));

        StackPane cardPaciente = crearCardGrande("Paciente", safeIcon("patientsss.png", 60, 60));
        StackPane cardMedico   = crearCardGrande("Médico",   safeIcon("doctorsss.png", 60, 60));

        // Navegación
        cardPaciente.setOnMouseClicked(ev -> new AdminPacientes().show());
        cardMedico.setOnMouseClicked(ev -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));

        cardsRow.getChildren().addAll(cardPaciente, cardMedico);

        centerContainer.getChildren().setAll(cardsRow);
        root.getChildren().addAll(menuBar, centerContainer);

        return root;
    }

    // Nuevo método show() usando ScreenRouter
    public void show() {
        ScreenRouter.setView(getView());
    }

    /** Card grande con burbuja superior e icono centrado. */
    private StackPane crearCardGrande(String titulo, Node icono) {
        Circle circle = new Circle(50);
        circle.setFill(Color.web("#F3F7FB"));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(8);
        StackPane iconCircle = new StackPane(circle, icono);
        iconCircle.setPrefSize(100, 100);
        iconCircle.setTranslateY(5);

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
