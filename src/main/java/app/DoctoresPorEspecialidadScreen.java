package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

import java.util.List;

public class DoctoresPorEspecialidadScreen {

    private final StackPane centerContainer = new StackPane();
    private int currentPage = 1;
    private static final int DOCTORES_POR_PAGINA = 6; // 2 cols × 3 filas

    private static final String BASE_ASSETS = "/images/mainPage/";

    private static ImageView icon(String file, double w, double h){
        var url = DoctoresPorEspecialidadScreen.class.getResource(BASE_ASSETS + file);
        if (url == null) { System.err.println("⚠ Icono no encontrado: " + file); return new ImageView(); }
        ImageView iv = new ImageView(new Image(url.toExternalForm()));
        iv.setFitWidth(w); iv.setFitHeight(h);
        return iv;
    }

    public void show(Stage stage, String especialidad){
        ScreenRouter.initIfNeeded(stage);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:white;");

        // ===== TOP =====
        HBox top = new HBox();
        top.setStyle("-fx-background-color:#FFFFFF; -fx-border-color: transparent transparent #E9EEF5 transparent; -fx-border-width:0 0 1 0;");
        top.setPadding(new Insets(8,32,8,32));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setSpacing(12);

        ImageView simed = icon("Logo.png",100,100);

        String btn = "-fx-background-color:#D0E1F9; -fx-text-fill:#1F355E; -fx-font-weight:bold; -fx-background-radius:10; -fx-padding:8 16;";
        String btnEm = "-fx-background-color:#B1361E; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:10; -fx-padding:8 16;";

        Button bInicio = new Button("Inicio", icon("Inicio.png",22,22));
        bInicio.setContentDisplay(ContentDisplay.LEFT);
        bInicio.setGraphicTextGap(8);
        bInicio.setStyle(btn);
        bInicio.setMinHeight(36);
        bInicio.setOnAction(e -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));

        Button bCitas = new Button("Mis citas", icon("miCitas.png",22,22));
        bCitas.setContentDisplay(ContentDisplay.LEFT);
        bCitas.setGraphicTextGap(8);
        bCitas.setStyle(btn);
        bCitas.setMinHeight(36);
        bCitas.setOnAction(e -> CitasAgendadasScreen.show(centerContainer, Sesion.getMatricula()));

        Button bEm = new Button("EMERGENCIA");
        bEm.setStyle(btnEm);
        bEm.setMinHeight(36);

        HBox middle = new HBox(40, bInicio, bCitas, bEm);
        middle.setAlignment(Pos.CENTER);
        HBox.setHgrow(middle, Priority.ALWAYS);

        Label user = new Label(Sesion.getNombreUsuario() == null ? "Usuario" : Sesion.getNombreUsuario(), icon("User.png",22,22));
        user.setContentDisplay(ContentDisplay.LEFT);
        user.setGraphicTextGap(8);
        user.setTextFill(Color.web("#1F355E"));
        user.setFont(Font.font(13));

        Button salir = new Button("", icon("Close.png",22,22));
        salir.setStyle("-fx-background-color:#1F355E;");
        salir.setOnAction(e -> new org.example.Main().start(ScreenRouter.getStage()));

        Region spL = new Region(), spR = new Region();
        HBox.setHgrow(spL, Priority.ALWAYS);
        HBox.setHgrow(spR, Priority.ALWAYS);
        top.getChildren().addAll(simed, spL, middle, spR, user, salir);
        root.setTop(top);

        // ===== HEADER + BODY (sin scroll) =====
        VBox header = new VBox(6);
        header.setPadding(new Insets(12, 32, 0, 32));
        header.setAlignment(Pos.CENTER_LEFT);

        HBox breadcrumb = new HBox(6);
        breadcrumb.setAlignment(Pos.CENTER_LEFT);
        Label lInicio = link("Inicio", () -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));
        Label dot1 = dot();
        Label lDocs = link("Doctores", () -> show(ScreenRouter.getStage(), especialidad));
        Label dot2 = dot();
        Label lAct = new Label(especialidad);
        lAct.setTextFill(Color.web("#1F355E"));
        breadcrumb.getChildren().addAll(lInicio, dot1, lDocs, dot2, lAct);

        Label titulo = new Label(especialidad);
        titulo.setFont(Font.font("System", FontWeight.BOLD, 24));
        titulo.setTextFill(Color.web("#1F355E"));

        header.getChildren().addAll(breadcrumb, titulo);

        VBox body = new VBox(16);
        body.setAlignment(Pos.TOP_CENTER);
        body.setPadding(new Insets(8, 32, 12, 32));
        body.setFillWidth(true);
        VBox.setVgrow(body, Priority.ALWAYS); // ocupa el espacio disponible

        centerContainer.getChildren().setAll(renderPagina(especialidad));
        centerContainer.setMaxWidth(Double.MAX_VALUE);
        centerContainer.setPrefWidth(Double.MAX_VALUE);
        StackPane.setAlignment(centerContainer, Pos.TOP_CENTER);

        body.getChildren().add(centerContainer);
        VBox headerBody = new VBox(header, body);
        VBox.setVgrow(headerBody, Priority.ALWAYS);
        root.setCenter(headerBody);

        // ===== FOOTER =====
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(0, 32, 16, 32));

        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:6 14;");
        btnAtras.setOnAction(e -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));

        HBox left = new HBox(btnAtras);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox paginacion = crearBarraPaginacion(especialidad);

        HBox right = new HBox();
        HBox.setHgrow(right, Priority.ALWAYS);

        footer.getChildren().addAll(left, paginacion, right);
        root.setBottom(footer);

        ScreenRouter.setView(root);
    }

    // ======= RENDER =======
    private Pane renderPagina(String especialidad){
        List<Doctor> doctores = DoctorData.getDoctoresPorEspecialidad(especialidad);

        int total = doctores.size();
        int desde = Math.max(0,(currentPage - 1) * DOCTORES_POR_PAGINA);
        int hasta = Math.min(desde + DOCTORES_POR_PAGINA, total);
        List<Doctor> pagina = doctores.subList(desde, hasta);

        GridPane grid = new GridPane();
        grid.setHgap(32);
        grid.setVgap(16);
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setPadding(new Insets(4, 0, 0, 0));

        // Ancho responsivo a 2 columnas
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.prefWidthProperty().bind(centerContainer.widthProperty());
        StackPane.setAlignment(grid, Pos.TOP_CENTER);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().setAll(c1, c2);

        int col = 0, row = 0;
        for (Doctor d : pagina) {
            HBox card = tarjetaDoctor(d, especialidad);
            GridPane.setFillWidth(card, true);
            GridPane.setHgrow(card, Priority.ALWAYS);
            grid.add(card, col, row);
            col = (col == 0) ? 1 : 0;
            if (col == 0) row++;
        }
        return grid;
    }

    private HBox tarjetaDoctor(Doctor d, String especialidadCtx){
        // Foto circular más compacta
        ImageView photo = obtenerFotoDoctor(d);
        photo.setFitWidth(96); photo.setFitHeight(96);
        photo.setClip(new Circle(48));

        StackPane fotoWrap = new StackPane(photo);
        fotoWrap.setMinSize(120,120);
        fotoWrap.setPrefSize(120,120);
        fotoWrap.setAlignment(Pos.CENTER_LEFT);

        String nombre = (d.getNombre()!=null && !d.getNombre().isBlank()) ? d.getNombre() : "Médico";
        String esp    = (d.getEspecialidad()!=null && !d.getEspecialidad().isBlank()) ? d.getEspecialidad() : especialidadCtx;

        Label lblNombre = new Label(nombre);
        lblNombre.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblNombre.setTextFill(Color.web("#1F355E"));
        lblNombre.setWrapText(true);

        Label lblEsp = new Label(esp);
        lblEsp.setFont(Font.font(13));
        lblEsp.setTextFill(Color.web("#566B8E"));
        lblEsp.setWrapText(true);

        Button btnHorario = new Button("Cambiar horario");
        btnHorario.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:6 10;");
        btnHorario.setOnAction(e -> HorarioScreen.mostrarHorario(d, esp, centerContainer, Sesion.getMatricula()));

        Button btnCitas = new Button("Ver Citas");
        btnCitas.setStyle("-fx-background-color:#D0E1F9; -fx-text-fill:#1F355E; -fx-background-radius:8; -fx-padding:6 10;");
        btnCitas.setOnAction(e -> CitasAgendadasScreen.show(centerContainer, Sesion.getMatricula()));

        HBox actions = new HBox(10, btnHorario, btnCitas);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox data = new VBox(6, lblNombre, lblEsp, actions);
        data.setAlignment(Pos.CENTER_LEFT);
        data.setMinWidth(240);
        data.setMaxWidth(Double.MAX_VALUE);

        HBox tile = new HBox(16, fotoWrap, data);
        tile.setAlignment(Pos.CENTER_LEFT);
        tile.setPadding(new Insets(12));
        tile.setStyle("-fx-background-color:#EFF4FA; -fx-background-radius:16;");
        tile.setMinHeight(120);
        tile.setMinWidth(460);
        tile.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(tile, Priority.ALWAYS);
        HBox.setHgrow(data, Priority.ALWAYS);
        GridPane.setHgrow(tile, Priority.ALWAYS);

        return tile;
    }

    private ImageView obtenerFotoDoctor(Doctor d){
        try {
            if (d.getImagen() != null && !d.getImagen().isBlank()) {
                Image image = new Image(d.getImagen(), true);
                return new ImageView(image);
            }
        } catch (Exception ignored) {}
        return icon("doctor_placeholder.png", 96, 96);
    }

    // ======= PAGINACIÓN =======
    private HBox crearBarraPaginacion(String especialidad){
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER);

        List<Doctor> docs = DoctorData.getDoctoresPorEspecialidad(especialidad);
        int totalPag = Math.max(1, (int)Math.ceil(docs.size() / (double) DOCTORES_POR_PAGINA));

        Button primero = miniBtn("«", () -> { currentPage = 1; centerContainer.getChildren().setAll(renderPagina(especialidad)); });
        Button anterior = miniBtn("‹", () -> { if (currentPage>1){ currentPage--; centerContainer.getChildren().setAll(renderPagina(especialidad)); } });
        Button siguiente = miniBtn("›", () -> { if (currentPage<totalPag){ currentPage++; centerContainer.getChildren().setAll(renderPagina(especialidad)); } });
        Button ultimo   = miniBtn("»", () -> { currentPage = totalPag; centerContainer.getChildren().setAll(renderPagina(especialidad)); });

        HBox nums = new HBox(6);
        nums.setAlignment(Pos.CENTER);
        for (int i=1;i<=totalPag;i++) nums.getChildren().add(pill(i, especialidad));

        bar.getChildren().addAll(primero, anterior, nums, siguiente, ultimo);
        return bar;
    }

    private Button pill(int n, String esp){
        Button b = new Button(String.valueOf(n));
        b.setStyle(n==currentPage
                ? "-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:12; -fx-padding:2 8;"
                : "-fx-background-color:transparent; -fx-text-fill:#1F355E; -fx-background-radius:12; -fx-padding:2 8;");
        b.setOnAction(e -> { currentPage = n; centerContainer.getChildren().setAll(renderPagina(esp)); });
        return b;
    }

    private Button miniBtn(String txt, Runnable run){
        Button b = new Button(txt);
        b.setStyle("-fx-background-color:transparent; -fx-text-fill:#1F355E;");
        b.setOnAction(e -> run.run());
        return b;
    }

    // ======= UI helpers =======
    private Label link(String text, Runnable action){
        Label l = new Label(text);
        l.setTextFill(Color.web("#1F355E"));
        l.setStyle("-fx-underline:true;");
        l.setOnMouseClicked(e -> action.run());
        return l;
    }
    private Label dot(){
        Label s = new Label("•");
        s.setTextFill(Color.web("#1F355E"));
        return s;
    }
}
