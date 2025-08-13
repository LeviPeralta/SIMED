package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.OracleWalletConnector;

import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class RegistroScreen {

    // ====== UI ======
    private TextField tfId;
    private TextField tfNombre;
    private TextField tfApellidos;
    private ComboBox<String> cbSexo;
    private TextField tfCorreo;
    private DatePicker dpNacimiento;
    private TextField tfTelefono;
    private ComboBox<String> cbEspecialidad;   // mostrará nombre -> se mapea a ID
    private ComboBox<String> cbConsultorio;    // mostrará número -> se mapea a ID

    private final Pane fotoBox = new StackPane();

    private static final String BASE_ASSETS = "/images/mainPage/";

    // Mapea nombres visibles a IDs NUMERICOS en tu BD (¡ajústalo a tus valores reales!)
    private static final Map<String, Integer> ESPECIALIDADES = new LinkedHashMap<>(){{
        put("Medicina General", 1);
        put("Cardiología", 2);
        put("Neurología", 3);
        put("Ginecología", 4);
        put("Urología", 5);
        put("Traumatología", 6);
    }};
    private static final Map<String, Integer> CONSULTORIOS = new LinkedHashMap<>(){{
        put("101", 101);
        put("102", 102);
        put("201", 201);
        put("202", 202);
    }};

    private static ImageView icon(String file, double w, double h){
        var url = RegistroScreen.class.getResource(BASE_ASSETS + file);
        if (url == null) { System.err.println("⚠ Icono no encontrado: " + file); return new ImageView(); }
        ImageView iv = new ImageView(new Image(url.toExternalForm()));
        iv.setFitWidth(w); iv.setFitHeight(h);
        return iv;
    }

    public void show(Stage stage){
        ScreenRouter.initIfNeeded(stage);

        // ===== Top bar =====
        HBox top = new HBox();
        top.setStyle("-fx-background-color:#FFFFFF; -fx-border-color: transparent transparent #E9EEF5 transparent; -fx-border-width:0 0 1 0;");
        top.setPadding(new Insets(10,40,10,40));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setSpacing(12);

        ImageView logo = icon("Logo.png",120,120);

        String btn = "-fx-background-color:#D0E1F9; -fx-text-fill:#1F355E; -fx-font-weight:bold; -fx-background-radius:10; -fx-padding:10 20;";
        String btnEm = "-fx-background-color:#B1361E; -fx-text-fill:white; -fx-font-weight:bold; -fx-background-radius:10; -fx-padding:10 20;";

        Button bInicio = new Button("Inicio", icon("Inicio.png",24,24));
        bInicio.setContentDisplay(ContentDisplay.LEFT);
        bInicio.setGraphicTextGap(8);
        bInicio.setStyle(btn);
        bInicio.setMinHeight(40);
        bInicio.setOnAction(e -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));

        Button bEm = new Button("EMERGENCIA");
        bEm.setStyle(btnEm);
        bEm.setMinHeight(40);

        HBox middle = new HBox(60, bInicio, bEm);
        middle.setAlignment(Pos.CENTER);
        HBox.setHgrow(middle, Priority.ALWAYS);

        Label user = new Label(Sesion.getNombreUsuario()==null? "Usuario" : Sesion.getNombreUsuario(), icon("User.png",24,24));
        user.setContentDisplay(ContentDisplay.LEFT);
        user.setGraphicTextGap(8);
        user.setTextFill(Color.web("#1F355E"));
        user.setFont(Font.font(14));

        Button salir = new Button("", icon("Close.png",24,24));
        salir.setStyle("-fx-background-color:#1F355E;");
        salir.setOnAction(e -> new org.example.Main().start(ScreenRouter.getStage()));

        VBox derecha = new VBox(6, salir);
        derecha.setAlignment(Pos.CENTER_RIGHT);

        Region spL = new Region(), spR = new Region();
        HBox.setHgrow(spL, Priority.ALWAYS);
        HBox.setHgrow(spR, Priority.ALWAYS);

        top.getChildren().addAll(logo, spL, middle, spR, user, derecha);

        // ===== Header (breadcrumb + título) =====
        VBox header = new VBox(8);
        header.setPadding(new Insets(16, 40, 0, 40));
        header.setAlignment(Pos.CENTER_LEFT);

        HBox bc = new HBox(6);
        bc.setAlignment(Pos.CENTER_LEFT);
        bc.getChildren().addAll(link("Inicio", () -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage())),
                dot(), new Label("Pacientes"), dot(), labelAzul("Registro"));
        header.getChildren().addAll(bc, titulo("Registro"));

        // ===== Body: Card con formulario + foto =====
        BorderPane card = new BorderPane();
        card.setPadding(new Insets(24,32,24,32));
        card.setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:12; -fx-border-color:#E9EEF5; -fx-border-radius:12;");

        GridPane form = new GridPane();
        form.setHgap(18);
        form.setVgap(14);

        int r = 0;

        tfId = readOnly(inputText("Se autogenera"));
        tfId.setText(generarId()); // lo puedes reemplazar por tu secuencia

        tfNombre = inputText("Nombre(s)");
        tfApellidos = inputText("Apellidos");
        cbSexo = new ComboBox<>();
        cbSexo.getItems().addAll("Femenino","Masculino","Otro");
        cbSexo.setPromptText("Selecciona sexo");

        tfCorreo = inputText("correo@ejemplo.com");
        dpNacimiento = new DatePicker();
        dpNacimiento.setPromptText("Fecha de nacimiento");

        tfTelefono = inputText("Teléfono");

        cbEspecialidad = new ComboBox<>();
        cbEspecialidad.getItems().addAll(ESPECIALIDADES.keySet());
        cbEspecialidad.setPromptText("Especialidad");

        cbConsultorio = new ComboBox<>();
        cbConsultorio.getItems().addAll(CONSULTORIOS.keySet());
        cbConsultorio.setPromptText("Consultorio");

        form.add(fieldWithLabel("ID Médico", tfId), 0, r++);
        form.add(fieldWithLabel("Nombre", tfNombre), 0, r++);
        form.add(fieldWithLabel("Apellidos", tfApellidos), 0, r++);
        form.add(fieldWithLabel("Sexo", cbSexo), 0, r++);
        form.add(fieldWithLabel("Correo", tfCorreo), 0, r++);
        form.add(fieldWithLabel("Fecha de nacimiento", dpNacimiento), 0, r++);
        form.add(fieldWithLabel("Teléfono", tfTelefono), 0, r++);
        form.add(fieldWithLabel("Especialidad", cbEspecialidad), 0, r++);
        form.add(fieldWithLabel("Consultorio", cbConsultorio), 0, r++);

        card.setCenter(form);

        // Foto (no se guarda en tabla; se puede persistir después)
        VBox fotoCol = new VBox(10);
        fotoCol.setAlignment(Pos.TOP_CENTER);

        fotoBox.setPrefSize(180, 180);
        fotoBox.setMinSize(180, 180);
        fotoBox.setStyle("-fx-background-color:#FFFFFF; -fx-border-color:#C9D3E3; -fx-border-radius:8; -fx-background-radius:8;");
        Label fotoIcon = new Label("?");
        fotoIcon.setTextFill(Color.web("#7B8EAA"));
        fotoIcon.setStyle("-fx-font-size:42;");
        ((StackPane)fotoBox).getChildren().add(fotoIcon);

        Label addFoto = new Label("Agregar foto");
        addFoto.setTextFill(Color.web("#1F355E"));
        addFoto.setFont(Font.font("System", FontWeight.BOLD, 14));
        addFoto.setCursor(Cursor.HAND);
        addFoto.setOnMouseClicked(e -> seleccionarFoto());

        fotoCol.getChildren().addAll(fotoBox, addFoto);
        BorderPane.setMargin(fotoCol, new Insets(0,0,0,40));
        card.setRight(fotoCol);

        // Botones inferiores
        Button btnRegistrar = new Button("Registrar");
        btnRegistrar.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:10 22;");
        btnRegistrar.setOnAction(e -> guardarMedico());

        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:6 14;");
        btnAtras.setOnAction(e -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));

        HBox acciones = new HBox(12, btnAtras, btnRegistrar);
        acciones.setAlignment(Pos.CENTER_RIGHT);

        VBox body = new VBox(20, card, acciones);
        body.setAlignment(Pos.TOP_CENTER);
        body.setPadding(new Insets(16,40,24,40));

        // Root
        VBox root = new VBox(top, header, body);
        ScreenRouter.setView(root);
    }

    // ===== Persistencia =====
    private void guardarMedico(){
        // Validaciones básicas
        if (isBlank(tfNombre) || isBlank(tfApellidos) || cbSexo.getValue()==null ||
                isBlank(tfCorreo) || dpNacimiento.getValue()==null ||
                isBlank(tfTelefono) || cbEspecialidad.getValue()==null || cbConsultorio.getValue()==null) {
            alert(Alert.AlertType.WARNING, "Por favor completa todos los campos.");
            return;
        }

        String idMedico = tfId.getText().trim();
        String nombre = tfNombre.getText().trim();
        String apellidos = tfApellidos.getText().trim();
        String sexo = cbSexo.getValue();
        String correo = tfCorreo.getText().trim();
        LocalDate fn = dpNacimiento.getValue();
        String telefono = tfTelefono.getText().trim();

        Integer idEspecialidad = ESPECIALIDADES.get(cbEspecialidad.getValue());
        Integer idConsultorio = CONSULTORIOS.get(cbConsultorio.getValue());

        String sql = "INSERT INTO ADMIN.MEDICOS " +
                "(ID_MEDICO, NOMBRE, APELLIDOS, SEXO, CORREO, FECHA_NACIMIENTO, TELEFONO, ID_ESPECIALIDAD, ID_CONSULTORIO) " +
                "VALUES (?,?,?,?,?,?,?,?,?)";

        try (Connection cn = OracleWalletConnector.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, idMedico);
            ps.setString(2, nombre);
            ps.setString(3, apellidos);
            ps.setString(4, sexo);
            ps.setString(5, correo);
            ps.setDate(6, Date.valueOf(fn));
            ps.setString(7, telefono);
            ps.setInt(8, idEspecialidad);
            ps.setInt(9, idConsultorio);

            int rows = ps.executeUpdate();
            if (rows > 0){
                alert(Alert.AlertType.INFORMATION, "Médico registrado correctamente.");
                limpiarCampos();
                // Opcional: volver a la pantalla de especialidades
                // new MedicosEspecialidadesScreen().show(ScreenRouter.getStage());
            } else {
                alert(Alert.AlertType.ERROR, "No se pudo registrar el médico.");
            }
        } catch (Exception ex){
            ex.printStackTrace();
            alert(Alert.AlertType.ERROR, "Error al guardar en Oracle: " + ex.getMessage());
        }
    }

    private void limpiarCampos(){
        tfId.setText(generarId());
        tfNombre.clear();
        tfApellidos.clear();
        cbSexo.getSelectionModel().clearSelection();
        tfCorreo.clear();
        dpNacimiento.setValue(null);
        tfTelefono.clear();
        cbEspecialidad.getSelectionModel().clearSelection();
        cbConsultorio.getSelectionModel().clearSelection();
        ((StackPane)fotoBox).getChildren().setAll(new Label("?"));
    }

    // ===== Helpers UI =====
    private static Label labelAzul(String t){ Label l=new Label(t); l.setTextFill(Color.web("#1F355E")); return l; }
    private static Label titulo(String t){ Label l=new Label(t); l.setFont(Font.font("System", FontWeight.BOLD, 28)); l.setTextFill(Color.web("#1F355E")); return l; }
    private static Label link(String text, Runnable action){ Label l=new Label(text); l.setTextFill(Color.web("#1F355E")); l.setStyle("-fx-underline:true;"); l.setOnMouseClicked(e->action.run()); return l; }
    private static Label dot(){ Label s=new Label("•"); s.setTextFill(Color.web("#1F355E")); return s; }
    private static TextField inputText(String ph){ TextField tf=new TextField(); tf.setPromptText(ph); tf.setPrefWidth(420); tf.setStyle("-fx-background-radius:8; -fx-border-radius:8;"); return tf; }
    private static TextField readOnly(TextField tf){ tf.setEditable(false); tf.setFocusTraversable(false); return tf; }
    private static VBox fieldWithLabel(String label, Control field){
        Label l = new Label(label);
        l.setTextFill(Color.web("#6B7E9F"));
        l.setFont(Font.font("System", FontWeight.NORMAL, 12));
        return new VBox(4, l, field);
    }
    private static boolean isBlank(TextField tf){ return tf.getText()==null || tf.getText().isBlank(); }

    private void seleccionarFoto(){
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File f = fc.showOpenDialog(ScreenRouter.getStage());
        if (f != null){
            ImageView iv = new ImageView(new Image(f.toURI().toString(), 180, 180, true, true));
            ((StackPane)fotoBox).getChildren().setAll(iv);
            fotoBox.setStyle("-fx-background-color:#FFFFFF; -fx-border-color:#C9D3E3; -fx-border-radius:8; -fx-background-radius:8;");
        }
    }

    // Genera un ID_Medico simple; cámbialo por tu secuencia si ya existe (ej. ADMIN.SEQ_MEDICOS.NEXTVAL)
    private String generarId(){
        return "MED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void alert(Alert.AlertType type, String msg){
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.initOwner(ScreenRouter.getStage());
        a.setHeaderText(null);
        a.showAndWait();
    }
}
