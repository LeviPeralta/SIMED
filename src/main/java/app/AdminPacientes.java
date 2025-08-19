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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javafx.stage.Stage;
import org.example.OracleWalletConnector;

public class AdminPacientes {

    public static class Paciente {
        private final String nombre;
        private final String apellidos;
        private final String telefono;
        private final String correo;

        public Paciente(String nombre, String apellidos, String telefono, String correo) {
            this.nombre    = nombre;
            this.apellidos = apellidos;
            this.telefono  = telefono;
            this.correo    = correo;
        }

        public String getNombreCompleto() { return nombre + apellidos; } // sin espacios
        public String getTelefono()       { return telefono; }
        public String getCorreo()         { return correo; }
    }

    // Devuelve la vista para ScreenRouter
    public Parent getView() {
        BorderPane root = new BorderPane();
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

        Button btnInicio = new Button("Inicio", createIcon("Inicio.png", 24, 24));
        btnInicio.setContentDisplay(ContentDisplay.LEFT);
        btnInicio.setGraphicTextGap(8);
        btnInicio.setStyle(estiloBoton);
        btnInicio.setMinHeight(40);
        // Regresa a la pantalla de Recepcionista usando ScreenRouter
        btnInicio.setOnAction(e -> {
            AdminRecepcionistaScreen recepcionista = new AdminRecepcionistaScreen();
            recepcionista.show();
        });

        HBox centerButtons = new HBox(btnInicio);
        centerButtons.setSpacing(60);
        centerButtons.setAlignment(Pos.CENTER);
        centerButtons.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(centerButtons, Priority.ALWAYS);

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
        btnSalir.setOnAction(e -> {
            Stage currentStage = (Stage) btnSalir.getScene().getWindow();
            currentStage.close();

            Stage primaryStage = new Stage();
            try {
                new org.example.Main().start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Region spacerL = new Region();
        Region spacerR = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        menuBar.getChildren().addAll(simedIcon, spacerL, centerButtons, spacerR, lblUsuario, btnSalir);
        root.setTop(menuBar);

        // ===== Grid de tarjetas de pacientes =====
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(50);
        grid.setVgap(30);
        grid.setAlignment(Pos.TOP_CENTER);

        List<Paciente> pacientes = obtenerPacientes();
        int col = 0, row = 0;
        for (Paciente p : pacientes) {
            grid.add(crearCardPaciente(p), col, row);
            if (++col == 2) { // 2 columnas
                col = 0;
                row++;
            }
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        root.setCenter(scroll);

        return root;
    }

    public void show() {
        ScreenRouter.setView(getView());
    }

    private HBox crearCardPaciente(Paciente p) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #F8FAFF; -fx-background-radius: 15;");

        // Intentar cargar imagen del paciente
        ImageView imgPaciente = loadPacienteImage(p);
        Circle circle = new Circle(40);
        if (imgPaciente.getImage() != null) {
            circle.setFill(new ImagePattern(imgPaciente.getImage()));
        } else {
            circle.setFill(Color.LIGHTGRAY);
        }

        VBox info = new VBox(4);
        Label lblNombre = new Label(p.getNombreCompleto());
        lblNombre.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblNombre.setStyle("-fx-text-fill: #1F355E;");

        Label lblTel   = new Label("📞 " + (p.getTelefono() == null ? "-" : p.getTelefono()));
        Label lblCorreo= new Label("✉️ " + (p.getCorreo() == null ? "-" : p.getCorreo()));

        info.getChildren().addAll(lblNombre, lblTel, lblCorreo);
        card.getChildren().addAll(circle, info);

        return card;
    }

    private ImageView loadPacienteImage(Paciente p) {
        String fileName = p.getNombreCompleto() + ".jpg"; // nombre exacto
        try {
            Image img = new Image(getClass().getResourceAsStream("/images/pacientes/" + fileName));
            return new ImageView(img);
        } catch (Exception e) {
            return new ImageView(); // no existe imagen
        }
    }

    private List<Paciente> obtenerPacientes() {
        List<Paciente> lista = new ArrayList<>();
        String sql = "SELECT NOMBRE, APELLIDOS, TELEFONO, CORREO FROM ADMIN.PACIENTE";

        try (Connection conn = OracleWalletConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String nombre    = rs.getString("NOMBRE");
                String apellidos = rs.getString("APELLIDOS");
                String tel       = rs.getString("TELEFONO");
                String correo    = rs.getString("CORREO");

                lista.add(new Paciente(nombre, apellidos, tel, correo));
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
            return new ImageView();
        }
    }

    private String obtenerNombrePacientePorMatricula(String matricula) {
        String sql = "SELECT NOMBRE || APELLIDOS AS NOMBRE_COMPLETO " +
                "FROM ADMIN.PACIENTE WHERE MATRICULA = '" + matricula + "'";
        try (Connection conn = OracleWalletConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString("NOMBRE_COMPLETO");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
