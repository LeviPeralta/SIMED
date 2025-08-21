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
import java.sql.*;
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
    private PasswordField pfContrasena;
    private PasswordField pfConfirmarContrasena;
    private DatePicker dpNacimiento;
    private TextField tfTelefono;
    private ComboBox<String> cbEspecialidad;   // mostrará nombre -> se mapea a ID
    private ComboBox<String> cbConsultorio;    // mostrará número -> se mapea a ID

    private final Pane fotoBox = new StackPane();

    private static final String BASE_ASSETS = "/images/mainPage/";

    // Mapea nombres visibles a IDs NUMERICOS en tu BD (¡ajústalo a tus valores reales!)
    private static final Map<String, Integer> ESPECIALIDADES = new LinkedHashMap<>(){{
        put("Neurología", 1);
        put("Medicina General", 2);
        put("Traumatología", 3);
        put("Ginecología", 4);
        put("Cardiología", 5);
        put("Urología", 6);
    }};

    private static final Map<String, Integer> CONSULTORIOS = new LinkedHashMap<>(){{
        put("Consultorio A", 1);
        put("Consultorio B", 2);
        put("Consultorio C", 3);
        put("Consultorio D", 4);
        put("Consultorio E", 5);
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

        Button bInicio = new Button("Inicio", icon("Inicio.png",24,24));
        bInicio.setContentDisplay(ContentDisplay.LEFT);
        bInicio.setGraphicTextGap(8);
        bInicio.setStyle(btn);
        bInicio.setMinHeight(40);
        bInicio.setOnAction(e -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));

        HBox middle = new HBox(60, bInicio);
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
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:12; -fx-border-color:#E9EEF5; -fx-border-radius:12;");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);

        int r = 0;

        tfId = readOnly(inputText("Se autogenera"));
        tfId.setText(generarId());

        tfNombre = inputText("Nombre(s)");
        tfApellidos = inputText("Apellidos");
        cbSexo = new ComboBox<>();
        cbSexo.getItems().addAll("Femenino","Masculino","Otro");
        cbSexo.setPromptText("Selecciona sexo");

        tfCorreo = inputText("correo@ejemplo.com");

        pfContrasena = new PasswordField();
        pfContrasena.setPromptText("Contraseña");
        pfContrasena.setPrefWidth(420);
        pfContrasena.setStyle("-fx-background-radius:8; -fx-border-radius:8;");

        pfConfirmarContrasena = new PasswordField();
        pfConfirmarContrasena.setPromptText("Confirmar contraseña");
        pfConfirmarContrasena.setPrefWidth(420);
        pfConfirmarContrasena.setStyle("-fx-background-radius:8; -fx-border-radius:8;");

        dpNacimiento = new DatePicker();
        dpNacimiento.setPromptText("Fecha de nacimiento");

        LocalDate hoy = LocalDate.now();
        LocalDate hace18 = hoy.minusYears(18);
        LocalDate limiteAntiguo = hoy.minusYears(100);

        // Se abre en la fecha mínima válida
        dpNacimiento.setValue(hace18);
        dpNacimiento.setShowWeekNumbers(false);

        dpNacimiento.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (date.isAfter(hace18)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #ffc0cb;");
                }
                if (date.isBefore(limiteAntiguo)) {
                    setDisable(true);
                    setStyle("-fx-background-color: #d3d3d3;");
                }
            }
        });

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
        form.add(fieldWithLabel("Contraseña", pfContrasena), 0, r++);
        form.add(fieldWithLabel("Confirmar Contraseña", pfConfirmarContrasena), 0, r++);
        form.add(fieldWithLabel("Fecha de nacimiento", dpNacimiento), 0, r++);
        form.add(fieldWithLabel("Teléfono", tfTelefono), 0, r++);
        form.add(fieldWithLabel("Especialidad", cbEspecialidad), 0, r++);
        form.add(fieldWithLabel("Consultorio", cbConsultorio), 0, r++);

        card.setCenter(form);

        // Foto
        VBox fotoCol = new VBox(10);
        fotoCol.setAlignment(Pos.TOP_CENTER);

        fotoBox.setPrefSize(160, 160);
        fotoBox.setMinSize(160, 160);
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
        BorderPane.setMargin(fotoCol, new Insets(0,0,0,20));
        card.setRight(fotoCol);

        // === Scroll solo para el formulario ===
        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color:transparent;");

        // Botones inferiores
        Button btnRegistrar = new Button("Registrar");
        btnRegistrar.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:10 22;");
        btnRegistrar.setOnAction(e -> guardarMedico());

        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:6 14;");
        btnAtras.setOnAction(e -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));

        HBox acciones = new HBox(12, btnAtras, btnRegistrar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        acciones.setPadding(new Insets(10, 20, 10, 20));

        BorderPane root = new BorderPane();
        root.setTop(new VBox(top, header));   // barra + título
        root.setCenter(scroll);               // formulario con scroll
        root.setBottom(acciones);             // botones fijos
        BorderPane.setMargin(acciones, new Insets(0, 40, 20, 40));

        ScreenRouter.setView(root);
    }

    // ===== Persistencia =====
    private void guardarMedico() {
        // Validaciones básicas
        if (isBlank(tfNombre) || isBlank(tfApellidos) || isBlank(tfCorreo) || isBlank(pfContrasena)) {
            alert(Alert.AlertType.WARNING, "Por favor llena todos los campos obligatorios.");
            return;
        }
        if (!pfContrasena.getText().equals(pfConfirmarContrasena.getText())) {
            alert(Alert.AlertType.WARNING, "Las contraseñas no coinciden.");
            return;
        }

        String correo = tfCorreo.getText().trim();
        String password = pfContrasena.getText().trim();
        String nombres = tfNombre.getText().trim();
        String apellidos = tfApellidos.getText().trim();
        String telefono = tfTelefono.getText().trim();
        String genero = cbSexo.getValue();
        LocalDate fechaNacimiento = dpNacimiento.getValue();

        // Obtener IDs
        Integer idEspecialidad = ESPECIALIDADES.get(cbEspecialidad.getValue());
        Integer idConsultorio = CONSULTORIOS.get(cbConsultorio.getValue());

        if (idEspecialidad == null || idConsultorio == null) {
            alert(Alert.AlertType.WARNING, "Selecciona especialidad y consultorio.");
            return;
        }

        boolean exito = crearMedicoYUsuarioTransaccional(
                correo, password, nombres, apellidos, genero, fechaNacimiento, telefono, idEspecialidad, idConsultorio);

        if (exito) {
            alert(Alert.AlertType.INFORMATION, "Médico y usuario guardados correctamente.");
            limpiarCampos();
        }
    }

    private void limpiarCampos(){
        tfId.setText(generarId());
        tfNombre.clear();
        tfApellidos.clear();
        cbSexo.getSelectionModel().clearSelection();
        tfCorreo.clear();
        pfContrasena.clear();
        pfConfirmarContrasena.clear();
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
            ImageView iv = new ImageView(new Image(f.toURI().toString(), 160, 160, true, true));
            ((StackPane)fotoBox).getChildren().setAll(iv);
            fotoBox.setStyle("-fx-background-color:#FFFFFF; -fx-border-color:#C9D3E3; -fx-border-radius:8; -fx-background-radius:8;");
        }
    }

    private String generarId(){
        return "MED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void alert(Alert.AlertType type, String msg){
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.initOwner(ScreenRouter.getStage());
        a.setHeaderText(null);
        a.showAndWait();
    }

    private boolean crearMedicoYUsuarioTransaccional(
            String correo, String password,
            String nombres, String apellidos, String genero, LocalDate fechaNacimiento, String telefono,
            Integer idEspecialidad,
            Integer idConsultorio
    ) {
        String idMedico = correo.replace("@utez.edu.mx", "");

        String insertMedico =
                "INSERT INTO ADMIN.MEDICOS " +
                        "(ID_MEDICO, NOMBRE, APELLIDOS, SEXO, CORREO,  FECHA_NACIMIENTO, TELEFONO, ID_ESPECIALIDAD, ID_CONSULTORIO) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String insertUsuario =
                "INSERT INTO ADMIN.USUARIO " +
                        "(ID_USUARIO, CORREO, CONTRASENA, ROL, ID_REFERENCIA, TIPO_USUARIO) " +
                        "VALUES (ADMIN.USUARIO_SEQ.NEXTVAL, ?, ?, ?, ADMIN.REFERENCIA_SEQ.NEXTVAL, ?)";

        Connection conn = null;

        try {
            conn = OracleWalletConnector.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psM = conn.prepareStatement(insertMedico)) {
                psM.setString(1, idMedico);
                psM.setString(2, nombres);
                psM.setString(3, apellidos);
                psM.setString(4, genero);
                psM.setString(5, correo);

                if (fechaNacimiento != null) {
                    psM.setDate(6, java.sql.Date.valueOf(fechaNacimiento));
                } else {
                    psM.setNull(6, Types.DATE);
                }

                psM.setString(7, telefono);
                psM.setInt(8, idEspecialidad);
                psM.setInt(9, idConsultorio);
                psM.executeUpdate();
            }

            try (PreparedStatement psU = conn.prepareStatement(insertUsuario)) {
                psU.setString(1, correo);
                psU.setString(2, password);
                psU.setString(3, "medico");
                psU.setString(4, "medico");
                psU.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignore) {}
            if (e.getErrorCode() == 1) {
                showAlert("Duplicado", "El correo o el ID de médico ya están registrados.");
            } else {
                showAlert("Error de BD", e.getMessage());
            }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignore) {}
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
