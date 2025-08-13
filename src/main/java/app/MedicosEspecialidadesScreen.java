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

public class MedicosEspecialidadesScreen {

    // Contenedor donde podremos mostrar overlays (p.ej. "Mis citas")
    private final StackPane centerContainer = new StackPane();

    private static final String BASE_ASSETS = "/images/mainPage/";

    // ---------- Helpers de iconos ----------
    private static ImageView safeIcon(String fileName, double w, double h) {
        String path = BASE_ASSETS + fileName;
        var url = MedicosEspecialidadesScreen.class.getResource(path);
        if (url == null) {
            System.err.println("⚠ Icono no encontrado: " + path);
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

    // ---------- Pantalla ----------
    public void show(Stage stage) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color:white;");
        stage.setTitle("SIMED - Sistema de Citas Médicas");

        // TOP BAR
        HBox top = new HBox();
        top.setStyle("-fx-background-color:#FFFFFF; -fx-border-color: transparent transparent #E9EEF5 transparent; -fx-border-width:0 0 1 0;");
        top.setPadding(new Insets(10,32,10,32));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setSpacing(12);

        ImageView simedIcon = createIcon("Logo.png", 120, 120);

        String estiloBoton =
                "-fx-background-color: #D0E1F9; -fx-text-fill: #1F355E; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 10 20 10 20;";
        String estiloEmergencia =
                "-fx-background-color: #B1361E; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 10 20 10 20;";

        Button btnInicio = new Button("Inicio", createIcon("Inicio.png", 24, 24));
        btnInicio.setContentDisplay(ContentDisplay.LEFT);
        btnInicio.setGraphicTextGap(8);
        btnInicio.setStyle(estiloBoton);
        btnInicio.setMinHeight(40);
        // Si quieres que regrese a recepción:
        btnInicio.setOnAction(e -> new AdminRecepcionistaScreen().show(stage, "Recepcionista"));


        Button btnEmergencia = new Button("EMERGENCIA");
        btnEmergencia.setStyle(estiloEmergencia);
        btnEmergencia.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnEmergencia.setMinHeight(40);

        HBox centerButtons = new HBox(btnInicio, btnEmergencia);
        centerButtons.setSpacing(60);
        centerButtons.setAlignment(Pos.CENTER);
        centerButtons.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(centerButtons, Priority.ALWAYS);

        // Nombre de usuario (desde BD por matrícula de sesión)
        String m = Sesion.getMatricula();
        String nombrePaciente = obtenerNombrePacientePorMatricula(m);
        if (nombrePaciente == null || nombrePaciente.isBlank()) {
            System.out.println("⚠ No se encontró paciente para matrícula: " + m);
            nombrePaciente = "Recepcionista";
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

        Region spacerL = new Region(), spacerR = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        top.getChildren().addAll(simedIcon, spacerL, centerButtons, spacerR, lblUsuario, btnSalir);

        // CONTENIDO
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(30,40,30,40));

        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(40);
        grid.setAlignment(Pos.CENTER);

        grid.add(servicioCard("Medicina General", createIcon("MedicinaGeneral.png",60,60), stage), 0, 0);
        grid.add(servicioCard("Cardiología",      createIcon("Cardiologia.png",60,60),      stage), 1, 0);
        grid.add(servicioCard("Neurología",       createIcon("Neurologia.png",60,60),       stage), 2, 0);
        grid.add(servicioCard("Ginecología",      createIcon("gineco.png",60,60),           stage), 0, 1);
        grid.add(servicioCard("Urología",         createIcon("urologia.png",60,60),         stage), 1, 1);
        grid.add(servicioCard("Traumatología",    createIcon("trauma.png",60,60),           stage), 2, 1);

        centerContainer.getChildren().setAll(grid);
        content.setCenter(centerContainer);

        // Botón Atrás
        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:6 14;");
        btnAtras.setOnAction(e -> new AdminRecepcionistaScreen().show(stage, "Recepcionista"));
        HBox bottom = new HBox(btnAtras);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10,0,0,0));
        content.setBottom(bottom);

        // Montaje
        root.getChildren().addAll(top, content);

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    // ---------- Card de especialidad ----------
    /** Card con icono y título grande; al clic abre MenuScreen y muestra doctores de esa especialidad */
    private static StackPane servicioCard(String titulo, Node icono, Stage stage){
        Circle circle = new Circle(40);
        circle.setFill(Color.web("#F3F7FB"));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(7);

        StackPane iconCircle = new StackPane(circle, icono);
        iconCircle.setTranslateY(-40);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 20));
        lblTitulo.setTextFill(Color.web("#1F355E"));
        lblTitulo.setWrapText(true);
        lblTitulo.setAlignment(Pos.CENTER);

        VBox card = new VBox(iconCircle, lblTitulo);
        card.setAlignment(Pos.TOP_CENTER);
        card.setSpacing(-10);
        card.setStyle("-fx-background-color:#F3F7FB; -fx-background-radius:20;");
        card.setPrefSize(310, 200);

        StackPane wrap = new StackPane(card);
        wrap.setAlignment(Pos.TOP_CENTER);
        wrap.setPadding(new Insets(10));

        // Hover
        wrap.setOnMouseEntered(e -> { card.setScaleX(1.07); card.setScaleY(1.07); iconCircle.setTranslateY(-60); });
        wrap.setOnMouseExited (e -> { card.setScaleX(1.0);  card.setScaleY(1.0);  iconCircle.setTranslateY(-40); });

        // CLICK -> abrir lista de doctores
        wrap.setOnMouseClicked(e -> {
            MenuScreen ms = new MenuScreen();
            ms.show(stage);            // 1) inicializa su escena y contenedores
            ms.mostrarDoctores(titulo);// 2) ya puedes navegar a la lista
        });

        return wrap;
    }

    // ---------- Consulta de nombre por matrícula ----------
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
