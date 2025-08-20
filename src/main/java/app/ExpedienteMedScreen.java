package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.example.OracleWalletConnector;

import javax.print.Doc;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class ExpedienteMedScreen {

    // ======== Estado recibido ========
    private DoctorAgendaScree.CitaInfo cita; // idCita, idPaciente, idMedico, fechaHora, nombrePaciente
    private DoctorAgendaScree agenda;        // referencia para volver a la agenda
    private LocalDate diaActual;             // día desde el que se abrió
    private Stage stage;

    // ======== Personales (read-only) ========
    private final Label lblNombre    = new Label("-");
    private final Label lblFechaNac  = new Label("-");
    private final Label lblTelefono  = new Label("-");
    private final Label lblCorreo    = new Label("-");
    private final Label lblDireccion = new Label("-");
    private final Label lblSexo      = new Label("-");
    private final Label lblCurp      = new Label("-");
    private final Label lblMatricula = new Label("-");

    // ======== Cita ========
    private final DatePicker dpFecha = new DatePicker();
    private final ComboBox<String> cbHora = new ComboBox<>();
    private final Label lblMedicoResp = new Label("-"); // SOLO LECTURA

    // ======== Expediente ========
    private final TextArea txtDiagnostico = new TextArea();
    private final TextArea txtTratamiento = new TextArea();
    private final TextArea txtObserv      = new TextArea();
    private final TextField tfPeso        = new TextField();
    private final TextField tfFR          = new TextField();
    private final TextField tfTemp        = new TextField();
    private final TextField tfIMC         = new TextField();
    private final TextArea txtExComp      = new TextArea();

    private final Button btnAceptar = new Button("Aceptar");

    // ======== DAO / control ========
    private final ExpedienteMedDao dao = new ExpedienteMedDao();
    private Long idExpedienteExistente = null;

    // ======== Constantes ========
    private static final LocalTime H_INI = LocalTime.of(9, 0);
    private static final LocalTime H_FIN = LocalTime.of(14, 0);

    public void show(Stage stage,
                     DoctorAgendaScree.CitaInfo citaInfo,
                     DoctorAgendaScree agendaRef,
                     LocalDate diaRef) {

        this.stage = stage;
        this.cita = citaInfo;
        this.agenda = agendaRef;
        this.diaActual = diaRef;

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0)); // top bar a full

        // Top: barra + breadcrumb
        VBox top = new VBox(buildTopBar(), buildBreadcrumb());
        root.setTop(top);

        // Centro: contenido dentro de ScrollPane
        VBox content = new VBox(18,
                bloqueDatosPersonales(),
                bloqueDatosCita(),
                bloqueExpediente(),
                botonesAccion()
        );
        content.setPadding(new Insets(16, 24, 24, 24));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: transparent;");
        root.setCenter(scroll);

        // Cargas iniciales
        cargarDatosPersonales(cita.idPaciente);
        cargarNombreMedico(cita.idMedico);
        initCamposCita();
        cargarExpedienteSiExiste();

        // Eventos
        btnAceptar.setOnAction(e -> {
            onGuardar();
        });

        // Show/replace scene
        Scene sc = stage.getScene();
        if (sc == null) { sc = new Scene(root, 1200, 800); stage.setScene(sc); }
        else { sc.setRoot(root); }
        stage.show();
    }

    // =================== Top Bar / Breadcrumb ===================

    private HBox buildTopBar() {
        HBox menuBar = new HBox();
        menuBar.setStyle("-fx-background-color: #FFFFFF;");
        menuBar.setPadding(new Insets(0, 40, 0, 40));
        menuBar.setSpacing(10);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        ImageView simedIcon = icon("Logo.png", 120, 120);

        String estiloBoton = "-fx-background-color: #D0E1F9; -fx-text-fill: #1F355E; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20 10 20;";

        Button btnInicio = new Button("Inicio", icon("Inicio.png", 24, 24));
        btnInicio.setContentDisplay(ContentDisplay.LEFT);
        btnInicio.setGraphicTextGap(8);
        btnInicio.setStyle(estiloBoton);
        btnInicio.setMinHeight(40);
        btnInicio.setOnAction(e -> {
            // Regresa al calendario semanal del doctor
            if (agenda != null){
                agenda.goToWeek();
            }
        });

        HBox centerButtons = new HBox(btnInicio);
        centerButtons.setSpacing(60);
        centerButtons.setAlignment(Pos.CENTER);
        centerButtons.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(centerButtons, Priority.ALWAYS);

        Label lblUsuario = new Label(app.Sesion.getNombreUsuario(), icon("User.png", 24, 24));
        lblUsuario.setContentDisplay(ContentDisplay.LEFT);
        lblUsuario.setGraphicTextGap(8);
        lblUsuario.setTextFill(Color.web("#1F355E"));

        Button btnSalir = new Button("", icon("Close.png", 24, 24));
        btnSalir.setStyle("-fx-background-color: #1F355E;");
        btnSalir.setOnAction(e -> new org.example.Main().start(ScreenRouter.getStage()));


        Region spacerL = new Region(), spacerR = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        menuBar.getChildren().addAll(simedIcon, spacerL, centerButtons, spacerR, lblUsuario, btnSalir);
        return menuBar;
    }

    private HBox buildBreadcrumb() {
        HBox bc = new HBox();
        bc.setPadding(new Insets(8, 24, 8, 24));
        bc.setSpacing(6);
        var loc = new Locale("es","MX");
        String dia = LocalDate.now().getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, loc);
        Label l1 = new Label("Inicio");
        Label sep1 = new Label(">");
        Label l2 = new Label(dia.substring(0,1).toUpperCase(loc)+dia.substring(1));
        Label sep2 = new Label(">");
        Label l3 = new Label("Historia Médico");
        for (Label l : new Label[]{l1,l2,l3}) l.setStyle("-fx-text-fill:#1F355E; -fx-font-weight:bold;");
        bc.getChildren().addAll(l1, sep1, l2, sep2, l3);
        return bc;
    }

    private ImageView icon(String fileName, double w, double h) {
        String path = "/images/mainPage/" + fileName;
        Image img = new Image(getClass().getResource(path).toExternalForm());
        ImageView iv = new ImageView(img);
        iv.setFitWidth(w); iv.setFitHeight(h);
        return iv;
    }

    // =================== Bloques UI ===================

    private VBox bloqueDatosPersonales() {
        GridPane g = new GridPane();
        g.setHgap(30); g.setVgap(12);

        g.add(new Label("Nombre completo"),    0,0); g.add(lblNombre,    0,1);
        g.add(new Label("Fecha de nacimiento"),0,2); g.add(lblFechaNac,  0,3);
        g.add(new Label("Teléfono"),           0,4); g.add(lblTelefono,  0,5);
        g.add(new Label("CURP"),               0,6); g.add(lblCurp,      0,7);

        g.add(new Label("E‑mail"),             1,0); g.add(lblCorreo,    1,1);
        g.add(new Label("Dirección"),          1,2); g.add(lblDireccion, 1,3);
        g.add(new Label("Sexo"),               1,4); g.add(lblSexo,      1,5);
        g.add(new Label("Matrícula"),          1,6); g.add(lblMatricula, 1,7);

        estilizarSoloLectura(g);
        return new VBox(6, tituloSeccion("Datos personales del paciente"), g);
    }

    private VBox bloqueDatosCita() {
        // Rellenar horas 09:00 a 14:00 cada 30'
        for (LocalTime t = H_INI; !t.isAfter(H_FIN); t = t.plusMinutes(30))
            cbHora.getItems().add(t.toString());

        GridPane g = new GridPane();
        g.setHgap(30); g.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(30);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(30);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(40);
        g.getColumnConstraints().addAll(c1,c2,c3);

        g.add(new Label("Fecha de la consulta"), 0,0); g.add(dpFecha, 0,1);
        g.add(new Label("Hora"),                 1,0); g.add(cbHora, 1,1);
        g.add(new Label("Médico responsable"),   2,0); g.add(lblMedicoResp, 2,1); // SOLO LECTURA

        return new VBox(6, tituloSeccion("Datos de la cita"), g);
    }

    private VBox bloqueExpediente() {
        txtDiagnostico.setPrefRowCount(3);
        txtTratamiento.setPrefRowCount(3);
        txtObserv.setPrefRowCount(3);
        txtExComp.setPrefRowCount(2);

        GridPane g = new GridPane();
        g.setHgap(30); g.setVgap(12);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        g.getColumnConstraints().addAll(c1,c2);

        g.add(new Label("Diagnóstico"), 0,0); g.add(txtDiagnostico, 0,1);
        g.add(new Label("Tratamiento"), 0,2); g.add(txtTratamiento, 0,3);
        g.add(new Label("Observaciones"), 0,4); g.add(txtObserv, 0,5);

        g.add(new Label("Peso (kg)"), 1,0); g.add(tfPeso, 1,1);
        g.add(new Label("Frecuencia respiratoria"), 1,2); g.add(tfFR, 1,3);
        g.add(new Label("Temperatura (°C)"), 1,4); g.add(tfTemp, 1,5);
        g.add(new Label("IMC"), 1,6); g.add(tfIMC, 1,7);
        g.add(new Label("Exámenes complementarios"), 1,8); g.add(txtExComp, 1,9);

        return new VBox(6, tituloSeccion("Expediente clínico"), g);
    }

    private HBox botonesAccion() {
        HBox h = new HBox(10, btnAceptar);
        h.setAlignment(Pos.CENTER_RIGHT);
        return h;
    }

    private Label tituloSeccion(String t){ Label l=new Label(t); l.setStyle("-fx-text-fill:#1F355E; -fx-font-weight:bold;"); return l; }

    private void estilizarSoloLectura(GridPane g){
        g.getChildren().stream().filter(n->n instanceof Label).forEach(n -> {
            Integer r = GridPane.getRowIndex(n);
            ((Label)n).setStyle((r!=null && r%2==1)?"-fx-text-fill:#444;":"-fx-text-fill:#1F355E;");
        });
    }

    // =================== Cargas ===================

    private void cargarDatosPersonales(long idPaciente) {
        final String sql = """
            SELECT NOMBRE, APELLIDOS, FECHA_NACIMIENTO, TELEFONO,
                   CORREO, DIRECCION, SEXO, CURP, MATRICULA
              FROM ADMIN.PACIENTE WHERE ID_PACIENTE = ?
            """;
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblNombre.setText(rs.getString("NOMBRE")+" "+rs.getString("APELLIDOS"));
                    Date fn = rs.getDate("FECHA_NACIMIENTO");
                    lblFechaNac.setText(fn==null?"-":
                            fn.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    lblTelefono.setText(nvl(rs.getString("TELEFONO")));
                    lblCorreo.setText(nvl(rs.getString("CORREO")));
                    lblDireccion.setText(nvl(rs.getString("DIRECCION")));
                    lblSexo.setText(nvl(rs.getString("SEXO")));
                    lblCurp.setText(nvl(rs.getString("CURP")));
                    lblMatricula.setText(nvl(rs.getString("MATRICULA")));
                }
            }
        } catch (SQLException ex) { error("Error al cargar datos del paciente:\n"+ex.getMessage()); }
    }

    private void cargarNombreMedico(String idMedico) {
        final String sql = "SELECT NOMBRE || ' ' || APELLIDOS AS N FROM ADMIN.MEDICOS WHERE ID_MEDICO = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idMedico);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) lblMedicoResp.setText(rs.getString("N"));
                else lblMedicoResp.setText(idMedico);
            }
        } catch (SQLException ex) { lblMedicoResp.setText(idMedico); }
    }

    private void initCamposCita() {
        dpFecha.setValue(cita.fechaHora.toLocalDate());
        String hh = cita.fechaHora.toLocalTime().withSecond(0).withNano(0).toString();
        if (!cbHora.getItems().contains(hh)) cbHora.getItems().add(hh);
        cbHora.getSelectionModel().select(hh);
        cbHora.setMaxWidth(Double.MAX_VALUE);
        dpFecha.setMaxWidth(Double.MAX_VALUE);
    }

    private void cargarExpedienteSiExiste() {
        try {
            // Esta línea ahora usa el DAO y el Modelo actualizados
            ExpedienteMed e = dao.getExpedienteByCitaId(cita.idCita);

            if (e != null) {
                idExpedienteExistente = e.getIdExpediente();

                // Todas estas llamadas usan los nuevos getters
                txtDiagnostico.setText(nvl(e.getDiagnostico()));
                txtTratamiento.setText(nvl(e.getTratamiento()));
                txtObserv.setText(nvl(e.getObservaciones()));
                txtExComp.setText(nvl(e.getExamenesComp()));

                tfPeso.setText(e.getPesoKg() == null ? "" : String.valueOf(e.getPesoKg()));
                tfFR.setText(e.getFrecuenciaResp() == null ? "" : String.valueOf(e.getFrecuenciaResp()));
                tfTemp.setText(e.getTemperaturaC() == null ? "" : String.valueOf(e.getTemperaturaC()));
                tfIMC.setText(e.getImc() == null ? "" : String.valueOf(e.getImc()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // =================== Guardar ===================

    private void onGuardar() {
        // 1. Crear el objeto ExpedienteMed con los datos de la pantalla
        // (Esta parte es como la tenías en tu imagen)
        ExpedienteMed e = new ExpedienteMed();
        e.setIdCita(cita.idCita);
        e.setIdPaciente(cita.idPaciente);
        e.setIdMedico(cita.idMedico);
        e.setFechaConsulta(LocalDateTime.now()); // o la fecha que corresponda

        e.setDiagnostico(emptyToNull(txtDiagnostico.getText()));
        e.setTratamiento(emptyToNull(txtTratamiento.getText()));
        e.setObservaciones(emptyToNull(txtObserv.getText()));
        e.setExamenesComp(emptyToNull(txtExComp.getText()));

        e.setPesoKg(parseD(tfPeso.getText()));
        e.setFrecuenciaResp(parseI(tfFR.getText()));
        e.setTemperaturaC(parseD(tfTemp.getText()));
        e.setImc(parseD(tfIMC.getText()));

        // 2. Decidir si ACTUALIZAR o INSERTAR (GUARDAR)
        boolean exito;
        if (idExpedienteExistente != null) {
            // Si el ID ya existe, es una ACTUALIZACIÓN
            e.setIdExpediente(idExpedienteExistente); // ¡Importante! Asignar el ID para el UPDATE
            exito = dao.update(e);
        } else {
            // Si el ID no existe, es un registro NUEVO
            exito = dao.guardar(e);
        }

        // 3. Mostrar mensaje al usuario
        if (exito) {
            new Alert(Alert.AlertType.INFORMATION, "Expediente guardado correctamente.").showAndWait();
            // Aquí puedes cerrar la ventana o volver a la agenda
        } else {
            new Alert(Alert.AlertType.ERROR, "Error al guardar el expediente.").showAndWait();
        }
    }

    // =================== Utils ===================

    private static String nvl(String s){ return (s==null||s.isBlank())?"-":s; }
    private static String emptyToNull(String s){ return (s==null||s.isBlank())?null:s.trim(); }
    private static Double parseD(String s){ try { return (s==null||s.isBlank())?null:Double.parseDouble(s.trim()); } catch(Exception e){ return null; } }
    private static Integer parseI(String s){ try { return (s==null||s.isBlank())?null:Integer.parseInt(s.trim()); } catch(Exception e){ return null; } }

    private void error(String msg){ new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
    private void info(String msg){ new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}
