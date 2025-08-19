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
    private HBox paginacionHolder;                 // NUEVO: contenedor que se regenera
    private int currentPage = 1;
    private static final int DOCTORES_POR_PAGINA = 6; // 2 cols × 3 filas

    private static final String BASE_ASSETS = "/images/mainPage/";
    private static final String DOCTORS_IMG_BASE = "/image/Doctors/";  // NUEVO

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

        HBox middle = new HBox(40, bInicio);
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

        // ===== HEADER + BODY =====
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
        VBox.setVgrow(body, Priority.ALWAYS);

        centerContainer.getChildren().setAll(renderPagina(especialidad));
        centerContainer.setMaxWidth(Double.MAX_VALUE);
        centerContainer.setPrefWidth(Double.MAX_VALUE);
        StackPane.setAlignment(centerContainer, Pos.TOP_CENTER);

        body.getChildren().add(centerContainer);
        VBox headerBody = new VBox(header, body);
        VBox.setVgrow(headerBody, Priority.ALWAYS);
        root.setCenter(headerBody);

        // ===== FOOTER (Paginación reactiva) =====
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(0, 32, 16, 32));

        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:6 14;");
        btnAtras.setOnAction(e -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));

        HBox left = new HBox(btnAtras);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);

        paginacionHolder = new HBox();                         // NUEVO
        paginacionHolder.setAlignment(Pos.CENTER);
        paginacionHolder.getChildren().setAll(crearBarraPaginacion(especialidad)); // NUEVO

        HBox right = new HBox();
        HBox.setHgrow(right, Priority.ALWAYS);

        footer.getChildren().addAll(left, paginacionHolder, right);
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

        // 2 columnas responsivas
        grid.setMaxWidth(Double.MAX_VALUE);
        grid.prefWidthProperty().bind(centerContainer.widthProperty());
        StackPane.setAlignment(grid, Pos.TOP_CENTER);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50); c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50); c2.setHgrow(Priority.ALWAYS);
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
        // FOTO: carga desde /image/Doctors/{archivo} con fallback
        ImageView photo = obtenerFotoDoctor(d);
        photo.setFitWidth(96);
        photo.setFitHeight(96);
        photo.setPreserveRatio(true);
        photo.setSmooth(true);
        photo.setClip(new Circle(48, 48, 48));

        StackPane fotoWrap = new StackPane(photo);
        fotoWrap.setMinSize(120,120);
        fotoWrap.setPrefSize(120,120);
        fotoWrap.setAlignment(Pos.CENTER);

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
        btnHorario.setOnAction(e ->
                HorarioScreen.mostrarHorario(d, esp, centerContainer, Sesion.getMatricula())
        );

        Button btnCitas = new Button("Ver Citas");
        btnCitas.setStyle("-fx-background-color:#D0E1F9; -fx-text-fill:#1F355E; -fx-background-radius:8; -fx-padding:6 10;");
        btnCitas.setOnAction(e -> CitasMed.show(centerContainer, d));   // NUEVO: abre la nueva pantalla

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
            String img = d.getImagen();
            if (img != null && !img.isBlank()) {

                // 1) Si es URL absoluta o file:, cargar directo
                String norm = img.trim();
                if (norm.startsWith("http://") || norm.startsWith("https://") || norm.startsWith("file:")) {
                    return new ImageView(new Image(norm, true));
                }

                // 2) Normalizar rutas tipo Windows/IDE y quedarnos con la parte del classpath
                norm = norm.replace("\\", "/").replaceAll("^\\./", "");
                // si viene con "src/main/resources/..." lo recortamos a classpath:
                int idx = norm.indexOf("src/main/resources/");
                if (idx >= 0) norm = norm.substring(idx + "src/main/resources/".length());

                // quitar posibles dobles slashes
                while (norm.startsWith("/")) norm = norm.substring(1);

                // 3) Si ya trae la carpeta correcta, probar tal cual (classpath)
                String try1 = "/" + norm;
                var is1 = getClass().getResourceAsStream(try1);
                if (is1 != null) return new ImageView(new Image(is1));

                // 4) Si solo vino el nombre (o ruta parcial), buscar en /image/Doctors/
                String fileName = norm.substring(norm.lastIndexOf('/') + 1);
                String base = "/images/Doctors/" + fileName;

                var is2 = getClass().getResourceAsStream(base);
                if (is2 != null) return new ImageView(new Image(is2));

                // 5) Probar sin extensión (.png/.jpg/.jpeg/.webp)
                String nameNoExt = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
                String[] exts = {".png", ".jpg", ".jpeg", ".webp", ".PNG", ".JPG", ".JPEG", ".WEBP"};
                for (String ext : exts) {
                    var is3 = getClass().getResourceAsStream("/image/Doctors/" + nameNoExt + ext);
                    if (is3 != null) return new ImageView(new Image(is3));
                }

                // 6) Log útil para depurar qué se intentó cargar
                System.err.println("⚠ No se encontró imagen para doctor. Intentos: " + try1 + " | " + base + " (+exts)");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Fallback al placeholder dentro de /images/mainPage/
        return icon("doctor_placeholder.png", 96, 96);
    }

    // ======= PAGINACIÓN =======
    private HBox crearBarraPaginacion(String especialidad){
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER);

        List<Doctor> docs = DoctorData.getDoctoresPorEspecialidad(especialidad);
        int totalPag = Math.max(1, (int)Math.ceil(docs.size() / (double) DOCTORES_POR_PAGINA));

        Button primero = miniBtn("«", () -> { currentPage = 1; refresh(especialidad); });
        Button anterior = miniBtn("‹", () -> { if (currentPage>1){ currentPage--; refresh(especialidad); } });
        HBox nums = new HBox(6);
        nums.setAlignment(Pos.CENTER);
        for (int i=1;i<=totalPag;i++) nums.getChildren().add(pill(i, especialidad));
        Button siguiente = miniBtn("›", () -> { if (currentPage<totalPag){ currentPage++; refresh(especialidad); } });
        Button ultimo   = miniBtn("»", () -> { currentPage = totalPag; refresh(especialidad); });

        bar.getChildren().addAll(primero, anterior, nums, siguiente, ultimo);
        return bar;
    }

    private void refresh(String especialidad){          // NUEVO: re-render del grid + barra
        centerContainer.getChildren().setAll(renderPagina(especialidad));
        if (paginacionHolder != null) {
            paginacionHolder.getChildren().setAll(crearBarraPaginacion(especialidad));
        }
    }

    private Button pill(int n, String esp){
        Button b = new Button(String.valueOf(n));
        b.setStyle(n==currentPage
                ? "-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:12; -fx-padding:2 8;"
                : "-fx-background-color:transparent; -fx-text-fill:#1F355E; -fx-background-radius:12; -fx-padding:2 8;");
        b.setOnAction(e -> { currentPage = n; refresh(esp); });
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
