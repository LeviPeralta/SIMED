package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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

public class MedicosEspecialidadesScreen {

    private final StackPane centerContainer = new StackPane();
    private static final String BASE_ASSETS = "/images/mainPage/";

    private static ImageView icon(String file, double w, double h) {
        var url = MedicosEspecialidadesScreen.class.getResource(BASE_ASSETS + file);
        if (url == null) { System.err.println("⚠ Icono no encontrado: " + file); return new ImageView(); }
        ImageView iv = new ImageView(new Image(url.toExternalForm()));
        iv.setFitWidth(w); iv.setFitHeight(h);
        return iv;
    }

    public void show(Stage stage) {
        ScreenRouter.initIfNeeded(stage);

        VBox root = new VBox();
        root.setStyle("-fx-background-color:white;");

        // Top bar
        HBox top = new HBox();
        top.setStyle("-fx-background-color:#FFFFFF; -fx-border-color: transparent transparent #E9EEF5 transparent; -fx-border-width:0 0 1 0;");
        top.setPadding(new Insets(10,40,10,40));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setSpacing(12);

        ImageView simed = icon("Logo.png",120,120);

        String btn = "-fx-background-color:#D0E1F9; -fx-text-fill:#1F355E; -fx-font-weight:bold; -fx-background-radius:10; -fx-padding:10 20;";
        Button bInicio = new Button("Inicio", icon("Inicio.png",24,24));
        bInicio.setContentDisplay(ContentDisplay.LEFT);
        bInicio.setGraphicTextGap(8);
        bInicio.setStyle(btn);
        bInicio.setMinHeight(40);
        bInicio.setOnAction(e -> {
            AdminRecepcionistaScreen recepcionista = new AdminRecepcionistaScreen();
            recepcionista.show();
        });

        HBox middle = new HBox(60, bInicio);
        middle.setAlignment(Pos.CENTER);
        HBox.setHgrow(middle, Priority.ALWAYS);

        Label user = new Label(Sesion.getNombreUsuario() == null ? "Usuario" : Sesion.getNombreUsuario(), icon("User.png",24,24));
        user.setContentDisplay(ContentDisplay.LEFT);
        user.setGraphicTextGap(8);
        user.setTextFill(Color.web("#1F355E"));
        user.setFont(Font.font(14));

        Button salir = new Button("", icon("Close.png",24,24));
        salir.setStyle("-fx-background-color:#1F355E;");
        salir.setOnAction(e -> new org.example.Main().start(ScreenRouter.getStage())); // si quieres volver al login

        Button btnRegistrar = new Button("Registrar");
        btnRegistrar.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:10; -fx-padding:8 16;");
        btnRegistrar.setMinHeight(36);
        btnRegistrar.setOnAction(e -> new RegistroScreen().show(ScreenRouter.getStage()));

        VBox derecha = new VBox(6, salir, btnRegistrar);
        derecha.setAlignment(Pos.CENTER_RIGHT);

        Region spL = new Region(), spR = new Region();
        HBox.setHgrow(spL, Priority.ALWAYS);
        HBox.setHgrow(spR, Priority.ALWAYS);

        top.getChildren().addAll(simed, spL, middle, spR, user, derecha);

        // Contenido
        BorderPane content = new BorderPane();
        content.setPadding(new Insets(30,40,30,40));

        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(40);
        grid.setAlignment(Pos.CENTER);

        grid.add(servicioCard("Medicina General", icon("MedicinaGeneral.png",60,60), stage), 0, 0);
        grid.add(servicioCard("Cardiología",      icon("Cardiologia.png",60,60),      stage), 1, 0);
        grid.add(servicioCard("Neurología",       icon("Neurologia.png",60,60),       stage), 2, 0);
        grid.add(servicioCard("Ginecología",      icon("gineco.png",60,60),           stage), 0, 1);
        grid.add(servicioCard("Urología",         icon("urologia.png",60,60),         stage), 1, 1);
        grid.add(servicioCard("Traumatología",    icon("trauma.png",60,60),           stage), 2, 1);

        centerContainer.getChildren().setAll(grid);
        content.setCenter(centerContainer);

        // Atrás
        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:6 14;");
        btnAtras.setOnAction(e -> {
            AdminRecepcionistaScreen recepcionista = new AdminRecepcionistaScreen();
            recepcionista.show();
        });
        HBox bottom = new HBox(btnAtras);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10,0,0,0));
        content.setBottom(bottom);

        root.getChildren().addAll(top, content);
        ScreenRouter.setView(root); // <<< clave
    }

    /** Card sin spacing negativo ni translates agresivos. */
    private StackPane servicioCard(String titulo, Node icono, Stage stage) {
        Circle circle = new Circle(40);
        circle.setFill(Color.web("#F3F7FB"));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(7);

        StackPane iconCircle = new StackPane(circle, icono);
        // sin translateY excesivo
        iconCircle.setPadding(new Insets(10));

        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#1F355E"));
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(240);
        lblTitulo.setAlignment(Pos.CENTER);

        VBox card = new VBox(12, iconCircle, lblTitulo);
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-color:#F3F7FB; -fx-background-radius:20;");
        card.setPrefSize(310, 200);
        card.setPadding(new Insets(16));

        StackPane wrap = new StackPane(card);
        wrap.setAlignment(Pos.TOP_CENTER);
        wrap.setPadding(new Insets(10));

        wrap.setOnMouseEntered(e -> card.setScaleX(1.04));
        wrap.setOnMouseExited (e -> card.setScaleX(1.00));

        wrap.setOnMouseClicked(e -> {
            DoctoresPorEspecialidadScreen pantalla = new DoctoresPorEspecialidadScreen();
            pantalla.show(ScreenRouter.getStage(), titulo); // "titulo" es la especialidad
        });

        return wrap;
    }
}
