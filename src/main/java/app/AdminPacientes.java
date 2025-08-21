package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.OracleWalletConnector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdminPacientes {

    // La clase interna Paciente no necesita cambios
    public static class Paciente {
        private final String matricula;
        private final String nombre;
        private final String apellidos;
        private final String telefono;
        private final String correo;

        public Paciente(String matricula, String nombre, String apellidos, String telefono, String correo) {
            this.matricula = matricula;
            this.nombre = nombre;
            this.apellidos = apellidos;
            this.telefono = telefono;
            this.correo = correo;
        }

        public String getMatricula() { return matricula; }
        public String getNombreCompleto() { return nombre + " " + apellidos; }
        public String getTelefono() { return telefono; }
        public String getCorreo() { return correo; }
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");

        // ===== Barra superior =====
        HBox menuBar = new HBox();
        menuBar.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 5; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 1 0;");
        menuBar.setPadding(new Insets(5, 40, 5, 40));
        menuBar.setSpacing(10);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        ImageView simedIcon = createIcon("Logo.png", 100, 100);

        String estiloBoton = "-fx-background-color: #D0E1F9; " +
                "-fx-text-fill: #1F355E; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-padding: 10 20 10 20;";

        Button btnInicio = new Button("Inicio", createIcon("Inicio.png", 24, 24));
        btnInicio.setContentDisplay(ContentDisplay.LEFT);
        btnInicio.setGraphicTextGap(8);
        btnInicio.setStyle(estiloBoton);
        btnInicio.setMinHeight(40);
        btnInicio.setOnAction(e -> {
            AdminRecepcionistaScreen recepcionista = new AdminRecepcionistaScreen();
            recepcionista.show();
        });

        HBox centerButtons = new HBox(btnInicio);
        centerButtons.setSpacing(60);
        centerButtons.setAlignment(Pos.CENTER);

        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        // ===== INICIO DE LA CORRECCIÓN =====
        // Se obtiene el nombre del usuario directamente de la sesión.
        // Esto elimina la necesidad de la consulta a la base de datos que causaba el error.
        String nombreUsuario = Sesion.getNombreCompleto();

        // Si por alguna razón el nombre en la sesión es nulo, se pone un valor por defecto.
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            nombreUsuario = "Usuario";
        }
        // ===== FIN DE LA CORRECCIÓN =====

        Label lblUsuario = new Label(nombreUsuario, createIcon("User.png", 24, 24));
        lblUsuario.setFont(Font.font("System", 14));
        lblUsuario.setTextFill(Color.web("#1F355E"));
        lblUsuario.setContentDisplay(ContentDisplay.LEFT);
        lblUsuario.setGraphicTextGap(8);

        Button btnSalir = new Button("", createIcon("Close.png", 24, 24));
        btnSalir.setStyle("-fx-background-color: transparent;");
        btnSalir.setOnAction(e -> new org.example.Main().start(ScreenRouter.getStage()));

        menuBar.getChildren().addAll(simedIcon, spacerLeft, centerButtons, spacerRight, lblUsuario, btnSalir);
        root.setTop(menuBar);

        // ===== Grid de tarjetas de pacientes (sin cambios) =====
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(50);
        grid.setVgap(30);
        grid.setAlignment(Pos.TOP_CENTER);

        List<Paciente> pacientes = obtenerPacientes();
        int col = 0, row = 0;
        for (Paciente p : pacientes) {
            HBox card = crearCardPaciente(p);
            grid.add(card, col, row);

            if (++col == 2) {
                col = 0;
                row++;
            }
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        root.setCenter(scroll);

        // ===== Barra inferior con botón para regresar (sin cambios) =====
        HBox bottomBar = new HBox();
        bottomBar.setPadding(new Insets(20, 40, 20, 40));
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setStyle("-fx-background-color: white; -fx-border-color: #E0E0E0; -fx-border-width: 1 0 0 0;");

        Button btnRegresar = new Button("Regresar");
        btnRegresar.setStyle(estiloBoton);
        btnRegresar.setOnAction(e -> {
            AdminRecepcionistaScreen recepcionistaScreen = new AdminRecepcionistaScreen();
            recepcionistaScreen.show();
        });

        bottomBar.getChildren().add(btnRegresar);
        root.setBottom(bottomBar);

        return root;
    }

    public void show() {
        ScreenRouter.setView(getView());
    }

    // El resto de los métodos no necesitan cambios...
    private HBox crearCardPaciente(Paciente p) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #F8FAFF; -fx-background-radius: 15;");
        card.setMinWidth(300);

        ImageView imgPaciente = loadPacienteImage(p);
        Circle circle = new Circle(40);
        if (imgPaciente.getImage() != null) {
            circle.setFill(new ImagePattern(imgPaciente.getImage()));
        } else {
            Label iniciales = new Label(p.getNombreCompleto().substring(0, 1));
            iniciales.setFont(Font.font("System", FontWeight.BOLD, 30));
            iniciales.setTextFill(Color.WHITE);
            StackPane placeholder = new StackPane(circle, iniciales);
            circle.setFill(Color.web("#1F355E"));
            card.getChildren().add(placeholder);
        }

        VBox info = new VBox(4);
        Label lblNombre = new Label(p.getNombreCompleto());
        lblNombre.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblNombre.setStyle("-fx-text-fill: #1F355E;");

        Label lblTel = new Label("📞 " + (p.getTelefono() == null ? "No disponible" : p.getTelefono()));
        Label lblCorreo = new Label("✉️ " + (p.getCorreo() == null ? "No disponible" : p.getCorreo()));

        info.getChildren().addAll(lblNombre, lblTel, lblCorreo);

        if (imgPaciente.getImage() != null) {
            card.getChildren().add(circle);
        }
        card.getChildren().add(info);

        card.setOnMouseClicked(e -> {
            Stage stage = ScreenRouter.getStage();
            StackPane newRoot = new StackPane();
            CitasProximasScreen.show(newRoot, p.getMatricula());

            if (stage != null && stage.getScene() != null) {
                stage.getScene().setRoot(newRoot);
            }
        });

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #E0E7FF; -fx-background-radius: 15; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, #A6B4CC, 8, 0.5, 0, 0);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #F8FAFF; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, transparent, 0, 0, 0, 0);"));

        return card;
    }

    private ImageView loadPacienteImage(Paciente p) {
        String fileName = p.getMatricula() + ".jpg";
        try {
            Image img = new Image(getClass().getResourceAsStream("/images/pacientes/" + fileName));
            return new ImageView(img);
        } catch (Exception e) {
            return new ImageView();
        }
    }

    private List<Paciente> obtenerPacientes() {
        List<Paciente> lista = new ArrayList<>();
        String sql = "SELECT MATRICULA, NOMBRE, APELLIDOS, TELEFONO, CORREO FROM ADMIN.PACIENTE ORDER BY NOMBRE";

        try (Connection conn = OracleWalletConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String matricula = rs.getString("MATRICULA");
                String nombre = rs.getString("NOMBRE");
                String apellidos = rs.getString("APELLIDOS");
                String tel = rs.getString("TELEFONO");
                String correo = rs.getString("CORREO");

                lista.add(new Paciente(matricula, nombre, apellidos, tel, correo));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    private ImageView createIcon(String fileName, int w, int h) {
        try {
            Image img = new Image(getClass().getResourceAsStream("/images/mainPage/" + fileName));
            ImageView iv = new ImageView(img);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            return iv;
        } catch (Exception e) {
            System.err.println("No se pudo cargar el ícono: " + fileName);
            return new ImageView();
        }
    }
}
