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

public class AdminRecepcionistaScreen {

    // Carpeta base dentro de src/main/resources
    private static final String BASE_ASSETS = "/images/mainPage/";

    /** Carga un icono “seguro”: si el recurso no está, no lanza NPE y muestra un warning. */
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

    /** Pantalla principal para recepcionista/admin. */
    public void show(Stage stage, String nombreUsuario) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: white;");

        // ===== Barra superior (estilo alineado a MenuScreen) =====
        HBox menuBar = new HBox();
        menuBar.setStyle("-fx-background-color: #FFFFFF;");
        menuBar.setPadding(new Insets(0, 40, 0, 40));
        menuBar.setSpacing(10);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        ImageView simedIcon = safeIcon("Logo.png", 120, 120);

        String estiloBoton = "-fx-background-color: #D0E1F9; " +
                "-fx-text-fill: #1F355E; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-padding: 10 20 10 20;";
        String estiloEmergencia = "-fx-background-color: #B1361E; " +
                "-fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-padding: 10 20 10 20;";

        Button btnInicio = new Button("Inicio", safeIcon("Inicio.png", 24, 24));
        btnInicio.setContentDisplay(ContentDisplay.LEFT);
        btnInicio.setGraphicTextGap(8);
        btnInicio.setStyle(estiloBoton);
        btnInicio.setMinHeight(40);
        btnInicio.setOnAction(e -> {
            // Si quieres que Inicio te regrese a esta misma pantalla, no hagas nada.
            // Aquí, por ahora, simplemente no cambiamos de vista.
        });

        Button btnEmergencia = new Button("EMERGENCIA");
        btnEmergencia.setStyle(estiloEmergencia);
        btnEmergencia.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnEmergencia.setMinHeight(40);

        HBox centerButtons = new HBox(btnInicio, btnEmergencia);
        centerButtons.setSpacing(60);
        centerButtons.setAlignment(Pos.CENTER);
        HBox.setHgrow(centerButtons, Priority.ALWAYS);

        Label lblUsuario = new Label(
                (nombreUsuario == null || nombreUsuario.isBlank()) ? "Recepcionista" : nombreUsuario,
                safeIcon("User.png", 24, 24)
        );
        lblUsuario.setFont(Font.font("System", 14));
        lblUsuario.setTextFill(Color.web("#1F355E"));
        lblUsuario.setContentDisplay(ContentDisplay.LEFT);
        lblUsuario.setGraphicTextGap(8);

        Button btnSalir = new Button("", safeIcon("Close.png", 24, 24));
        btnSalir.setStyle("-fx-background-color: #1F355E;");
        btnSalir.setOnAction(e -> {
            // Cerrar y volver al login
            Stage current = (Stage) btnSalir.getScene().getWindow();
            current.close();
            try { new org.example.Main().start(new Stage()); } catch (Exception ex) { ex.printStackTrace(); }
        });

        Region spacerL = new Region(), spacerR = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        menuBar.getChildren().addAll(simedIcon, spacerL, centerButtons, spacerR, lblUsuario, btnSalir);

        // ===== Centro: dos tarjetas grandes (Paciente / Médico) =====
        HBox center = new HBox(80);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(60));

        // Usa nombres de íconos que EXISTEN en tu proyecto.
        // Para "Paciente" reutilizamos User.png.
        // Para "Médico" usamos MedicinaGeneral.png (ajústalo si prefieres otro).
        StackPane cardPaciente = crearCardGrande("Paciente", safeIcon("patientsss.png", 60, 60));
        StackPane cardMedico   = crearCardGrande("Médico",   safeIcon("doctorsss.png", 60, 60));

        // Acciones de navegación:
        cardPaciente.setOnMouseClicked(ev -> {
            // Reutiliza el flujo paciente (menú de especialidades)
            new MenuScreen().show(stage);
        });

        cardMedico.setOnMouseClicked(ev -> {
            // Puedes abrir MenuScreen y luego navegar a una especialidad
            new MenuScreen().show(stage);
            // Si quieres ir directo a "Medicina General", puedes exponer un método en MenuScreen para hacerlo.
        });

        center.getChildren().addAll(cardPaciente, cardMedico);

        // ===== Montaje final =====
        root.getChildren().addAll(menuBar, center);

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
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
}
