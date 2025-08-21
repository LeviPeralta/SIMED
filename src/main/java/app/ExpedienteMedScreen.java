package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.OracleWalletConnector;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ExpedienteMedScreen {

    // --- Estado ---
    private DoctorAgendaScree.CitaInfo cita;
    private DoctorAgendaScree agenda;
    private Stage stage;
    private final ExpedienteMedDao dao = new ExpedienteMedDao();
    private Long idExpedienteExistente = null;

    // --- Constantes de Estilo ---
    private static final String COLOR_AZUL_OSCURO = "#1F355E";
    private static final String COLOR_GRIS_TEXTO = "#555555";
    private static final String COLOR_FONDO_INPUT = "#F3F7FB";
    private static final String ESTILO_BOTON_PRINCIPAL = "-fx-background-color: " + COLOR_AZUL_OSCURO + "; -fx-text-fill: white; -fx-background-radius: 8; -fx-padding: 8 18; -fx-font-weight: bold;";
    private static final String ESTILO_BOTON_SECUNDARIO = "-fx-background-color: #E9EEF5; -fx-text-fill: " + COLOR_AZUL_OSCURO + "; -fx-background-radius: 8; -fx-padding: 8 18; -fx-font-weight: bold;";

    // --- Componentes UI ---
    private final Label lblNombreCompleto = new Label("-");
    private final Label lblFechaNacimiento = new Label("-");
    private final Label lblTelefono = new Label("-");
    private final Label lblAlergias = new Label("Ninguna registrada");
    private final Label lblEmail = new Label("-");
    private final Label lblDireccion = new Label("-");
    private final Label lblSexo = new Label("-");
    private final Label lblFechaConsulta = new Label("-");
    private final Label lblMedicoResponsable = new Label("-");
    private final TextArea txtDiagnostico = new TextArea();
    private final TextArea txtTratamiento = new TextArea();
    private final TextArea txtObservaciones = new TextArea();
    private final TextField tfPeso = new TextField();
    private final TextField tfFrecuenciaResp = new TextField();
    private final TextField tfExamenesComp = new TextField();
    private final TextField tfIMC = new TextField();
    private final TextField tfTemperatura = new TextField();


    public void show(Stage stage, DoctorAgendaScree.CitaInfo citaInfo, DoctorAgendaScree agendaRef, LocalDate diaRef) {
        this.stage = stage;
        this.cita = citaInfo;
        this.agenda = agendaRef;

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");

        root.setTop(new VBox(buildTopBar(), buildBreadcrumb()));

        VBox content = new VBox(30);
        content.setPadding(new Insets(20, 60, 40, 60));
        content.setAlignment(Pos.TOP_CENTER);
        content.getChildren().addAll(
                bloqueDatosPersonales(),
                new Separator(),
                bloqueDatosCitaYExpediente(),
                new Separator(),
                bloqueSignosVitales(),
                botonesAccion()
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        root.setCenter(scroll);

        cargarDatosPersonales(cita.idPaciente);
        cargarDatosCita();
        cargarExpedienteSiExiste();

        // --- INICIO DE LA CORRECCIÓN ---
        ScreenRouter.setView(root);
        // --- FIN DE LA CORRECCIÓN ---
    }

    private HBox buildTopBar() {
        HBox menuBar = new HBox();
        menuBar.setStyle("-fx-background-color: #FFFFFF;");
        menuBar.setPadding(new Insets(0, 40, 0, 40));
        menuBar.setSpacing(10);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        ImageView simedIcon = icon("Logo.png", 120, 120);

        String estiloBotonNav = "-fx-background-color: #D0E1F9; -fx-text-fill: #1F355E; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20;";
        Button btnInicio = new Button("Inicio", icon("Inicio.png", 24, 24));
        btnInicio.setContentDisplay(ContentDisplay.LEFT);
        btnInicio.setGraphicTextGap(8);
        btnInicio.setStyle(estiloBotonNav);
        btnInicio.setMinHeight(40);
        // --- INICIO DE LA CORRECCIÓN ---
        btnInicio.setOnAction(e -> {
            if (agenda != null) {
                agenda.goToWeek();
                ScreenRouter.setView(agenda.getRoot());
            }
        });
        // --- FIN DE LA CORRECCIÓN ---

        HBox centerButtons = new HBox(btnInicio);
        centerButtons.setAlignment(Pos.CENTER);

        Label lblUsuario = new Label(Sesion.getNombreUsuario(), icon("User.png", 24, 24));
        lblUsuario.setContentDisplay(ContentDisplay.LEFT);
        lblUsuario.setGraphicTextGap(8);
        lblUsuario.setTextFill(Color.web("#1F355E"));

        Button btnSalir = new Button("", icon("Close.png", 24, 24));
        btnSalir.setStyle("-fx-background-color: #1F355E;");

        // --- INICIO DE LA CORRECCIÓN ---
        btnSalir.setOnAction(e -> new org.example.Main().start(ScreenRouter.getStage()));
        // --- FIN DE LA CORRECCIÓN ---

        Region spacerL = new Region(); HBox.setHgrow(spacerL, Priority.ALWAYS);
        Region spacerR = new Region(); HBox.setHgrow(spacerR, Priority.ALWAYS);

        menuBar.getChildren().addAll(simedIcon, spacerL, centerButtons, spacerR, lblUsuario, btnSalir);
        return menuBar;
    }

    private HBox buildBreadcrumb() {
        HBox bc = new HBox(5);
        bc.setPadding(new Insets(10, 60, 10, 60));
        bc.setAlignment(Pos.CENTER_LEFT);

        Label l1 = createBreadcrumbLink("Inicio", () -> agenda.goToWeek());
        Label l2 = createBreadcrumbLink("Mis Citas", () -> agenda.goToWeek());
        Label l3 = createBreadcrumbLink("Citas Anteriores", () -> agenda.goToDay(cita.fechaHora.toLocalDate()));
        Label l4 = new Label("Historial médico");
        l4.setStyle("-fx-text-fill: " + COLOR_GRIS_TEXTO + "; -fx-font-weight: bold;");

        bc.getChildren().addAll(l1, new Label(">"), l2, new Label(">"), l3, new Label(">"), l4);
        return bc;
    }

    private HBox botonesAccion() {
        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle(ESTILO_BOTON_SECUNDARIO);
        // --- INICIO DE LA CORRECCIÓN ---
        btnAtras.setOnAction(e -> {
            if (agenda != null) {
                agenda.goToDay(cita.fechaHora.toLocalDate());
                ScreenRouter.setView(agenda.getRoot());
            }
        });
        // --- FIN DE LA CORRECCIÓN ---

        Button btnAceptar = new Button("Aceptar");
        btnAceptar.setStyle(ESTILO_BOTON_PRINCIPAL);
        btnAceptar.setOnAction(e -> onGuardar());

        HBox box = new HBox(15, btnAtras, btnAceptar);
        box.setAlignment(Pos.CENTER_RIGHT);
        box.setPadding(new Insets(20, 0, 0, 0));
        return box;
    }

    // --- (El resto de los métodos se mantienen igual, no es necesario copiarlos de nuevo si no han cambiado) ---

    private Label createBreadcrumbLink(String text, Runnable action) {
        Label link = new Label(text);
        link.setStyle("-fx-text-fill: " + COLOR_AZUL_OSCURO + "; -fx-underline: true; -fx-cursor: hand;");
        link.setOnMouseClicked(e -> action.run());
        return link;
    }

    private VBox bloqueDatosPersonales() {
        GridPane grid = new GridPane();
        grid.setHgap(80);
        grid.setVgap(15);
        grid.add(createFieldGroup("Nombre completo", lblNombreCompleto), 0, 0);
        grid.add(createFieldGroup("Fecha de nacimiento", lblFechaNacimiento), 0, 1);
        grid.add(createFieldGroup("Teléfono", lblTelefono), 0, 2);
        grid.add(createFieldGroup("Alergias", lblAlergias), 0, 3);
        grid.add(createFieldGroup("E-mail", lblEmail), 1, 0);
        grid.add(createFieldGroup("Dirección", lblDireccion), 1, 1);
        grid.add(createFieldGroup("Sexo", lblSexo), 1, 2);
        return new VBox(15, createSectionTitle("Datos personales del paciente"), grid);
    }

    private VBox bloqueDatosCitaYExpediente() {
        GridPane grid = new GridPane();
        grid.setHgap(80);
        grid.setVgap(15);
        styleTextArea(txtDiagnostico, 4);
        styleTextArea(txtTratamiento, 4);
        styleTextArea(txtObservaciones, 4);
        grid.add(createFieldGroup("Diagnóstico", txtDiagnostico), 0, 0);
        grid.add(createFieldGroup("Tratamiento", txtTratamiento), 0, 1);
        grid.add(createFieldGroup("Observaciones", txtObservaciones), 0, 2);
        grid.add(createFieldGroup("Fecha de la consulta", lblFechaConsulta), 1, 0);
        grid.add(createFieldGroup("Médico Responsable", lblMedicoResponsable), 1, 1);
        return new VBox(15, createSectionTitle("Datos de la cita"), grid);
    }

    private VBox bloqueSignosVitales() {
        GridPane grid = new GridPane();
        grid.setHgap(80);
        grid.setVgap(15);
        styleTextField(tfPeso);
        styleTextField(tfFrecuenciaResp);
        styleTextField(tfExamenesComp);
        styleTextField(tfIMC);
        styleTextField(tfTemperatura);
        grid.add(createFieldGroup("Peso", tfPeso), 0, 0);
        grid.add(createFieldGroup("Exámenes complementarios", tfExamenesComp), 0, 1);
        grid.add(createFieldGroup("Temperatura", tfTemperatura), 0, 2);
        grid.add(createFieldGroup("Frecuencia respiratoria", tfFrecuenciaResp), 1, 0);
        grid.add(createFieldGroup("IMC", tfIMC), 1, 1);
        return new VBox(15, createSectionTitle("Signos vitales"), grid);
    }

    private void cargarDatosPersonales(long idPaciente) {
        String sql = "SELECT * FROM ADMIN.PACIENTE WHERE ID_PACIENTE = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idPaciente);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                lblNombreCompleto.setText(nvl(rs.getString("NOMBRE") + " " + rs.getString("APELLIDOS")));
                Date fn = rs.getDate("FECHA_NACIMIENTO");
                lblFechaNacimiento.setText(fn == null ? "-" : fn.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                lblTelefono.setText(nvl(rs.getString("TELEFONO")));
                lblEmail.setText(nvl(rs.getString("CORREO")));
                lblDireccion.setText(nvl(rs.getString("DIRECCION")));
                lblSexo.setText(nvl(rs.getString("SEXO")));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void cargarDatosCita() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("es", "MX"));
        lblFechaConsulta.setText(capitalize(cita.fechaHora.format(formatter)));
        cargarNombreMedico(cita.idMedico);
    }

    private void cargarNombreMedico(String idMedico) {
        String sql = "SELECT NOMBRE || ' ' || APELLIDOS AS N FROM ADMIN.MEDICOS WHERE ID_MEDICO = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idMedico);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) lblMedicoResponsable.setText(rs.getString("N"));
        } catch (SQLException ex) {
            lblMedicoResponsable.setText("No disponible");
        }
    }

    private void cargarExpedienteSiExiste() {
        ExpedienteMed exp = dao.getExpedienteByCitaId(cita.idCita);
        if (exp != null) {
            idExpedienteExistente = exp.getIdExpediente();
            txtDiagnostico.setText(exp.getDiagnostico());
            txtTratamiento.setText(exp.getTratamiento());
            txtObservaciones.setText(exp.getObservaciones());
            tfExamenesComp.setText(exp.getExamenesComp());
            tfPeso.setText(exp.getPesoKg() == null ? "" : String.valueOf(exp.getPesoKg()));
            tfFrecuenciaResp.setText(exp.getFrecuenciaResp() == null ? "" : String.valueOf(exp.getFrecuenciaResp()));
            tfTemperatura.setText(exp.getTemperaturaC() == null ? "" : String.valueOf(exp.getTemperaturaC()));
            tfIMC.setText(exp.getImc() == null ? "" : String.valueOf(exp.getImc()));
        }
    }

    private void onGuardar() {
        ExpedienteMed exp = new ExpedienteMed();
        exp.setIdCita(cita.idCita);
        exp.setIdPaciente(cita.idPaciente);
        exp.setIdMedico(cita.idMedico);
        exp.setFechaConsulta(cita.fechaHora);
        exp.setDiagnostico(emptyToNull(txtDiagnostico.getText()));
        exp.setTratamiento(emptyToNull(txtTratamiento.getText()));
        exp.setObservaciones(emptyToNull(txtObservaciones.getText()));
        exp.setExamenesComp(emptyToNull(tfExamenesComp.getText()));
        exp.setPesoKg(parseD(tfPeso.getText()));
        exp.setFrecuenciaResp(parseI(tfFrecuenciaResp.getText()));
        exp.setTemperaturaC(parseD(tfTemperatura.getText()));
        exp.setImc(parseD(tfIMC.getText()));

        boolean exito;
        if (idExpedienteExistente != null) {
            exp.setIdExpediente(idExpedienteExistente);
            exito = dao.update(exp);
        } else {
            exito = dao.guardar(exp);
        }

        if (exito) {
            new Alert(Alert.AlertType.INFORMATION, "Expediente guardado correctamente.").showAndWait();
            // --- INICIO DE LA CORRECCIÓN ---
            if (agenda != null) {
                agenda.goToDay(cita.fechaHora.toLocalDate());
                ScreenRouter.setView(agenda.getRoot());
            }
            // --- FIN DE LA CORRECCIÓN ---
        } else {
            new Alert(Alert.AlertType.ERROR, "Error al guardar el expediente.").showAndWait();
        }
    }

    private Label createSectionTitle(String title) {
        Label label = new Label(title);
        label.setFont(Font.font("System", FontWeight.BOLD, 16));
        label.setTextFill(Color.web(COLOR_AZUL_OSCURO));
        return label;
    }

    private VBox createFieldGroup(String title, Node content) {
        Label label = new Label(title);
        label.setFont(Font.font("System", FontWeight.NORMAL, 12));
        label.setTextFill(Color.web(COLOR_GRIS_TEXTO));
        if (content instanceof Label) {
            content.setStyle("-fx-font-size: 14px; -fx-text-fill: " + COLOR_AZUL_OSCURO + ";");
        }
        VBox group = new VBox(5, label, content);
        group.setMinWidth(250);
        return group;
    }

    private void styleTextArea(TextArea textArea, int rows) {
        textArea.setPrefRowCount(rows);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-background-color: " + COLOR_FONDO_INPUT + "; -fx-border-color: #DDE3EA; -fx-background-radius: 5; -fx-border-radius: 5;");
    }

    private void styleTextField(TextField textField) {
        textField.setStyle("-fx-background-color: " + COLOR_FONDO_INPUT + "; -fx-border-color: #DDE3EA; -fx-background-radius: 5; -fx-border-radius: 5;");
    }

    private ImageView icon(String fileName, double w, double h) {
        try {
            String path = "/images/mainPage/" + fileName;
            return new ImageView(new Image(getClass().getResource(path).toExternalForm(), w, h, true, true));
        } catch (Exception e) {
            return new ImageView();
        }
    }

    private String nvl(String s) { return (s == null || s.isBlank()) ? "-" : s.trim(); }
    private String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private Double parseD(String s) { try { return Double.parseDouble(s.trim()); } catch (Exception e) { return null; } }
    private Integer parseI(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; } }
    private String capitalize(String s) { return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
}