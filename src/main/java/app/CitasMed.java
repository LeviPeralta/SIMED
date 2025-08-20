package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CitasMed {

    private static final String TITLE_COLOR = "#1F355E";
    private static final String CARD_BORDER_COLOR = "#D0D5DD";
    private static final String BASE_ASSETS = "/images/mainPage/";

    private static final DateTimeFormatter DIA_FMT = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'del' uuuu", new Locale("es", "MX"));
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("h:mm a", new Locale("es", "MX"));

    public void show(Stage stage, Doctor doctor) {
        ScreenRouter.initIfNeeded(stage);

        BorderPane screenRoot = new BorderPane();
        screenRoot.setStyle("-fx-background-color:white;");

        screenRoot.setTop(crearBarraSuperior());

        VBox content = new VBox();
        content.setSpacing(10);

        HBox breadcrumb = new HBox(6);
        breadcrumb.setAlignment(Pos.CENTER_LEFT);
        Label lInicio = link("Inicio", () -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));
        Label dot1 = dot();
        Label lDocs = link("Médico", () -> new DoctoresPorEspecialidadScreen().show(ScreenRouter.getStage(), doctor.getEspecialidad()));
        Label dot2 = dot();
        Label lAct = new Label("Mis Citas");
        lAct.setTextFill(Color.web(TITLE_COLOR));
        breadcrumb.getChildren().addAll(lInicio, dot1, lDocs, dot2, lAct);
        breadcrumb.setPadding(new Insets(12, 32, 0, 32));

        Label titulo = new Label("Citas agendadas");
        titulo.setTextFill(Color.web(TITLE_COLOR));
        titulo.setFont(Font.font("System", FontWeight.BOLD, 16));
        titulo.setPadding(new Insets(10, 32, 0, 32));

        VBox lista = new VBox(16);
        lista.setPadding(new Insets(10, 32, 20, 32));
        ScrollPane scrollPane = new ScrollPane(lista);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: white; -fx-border-color: white;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        List<Cita> citas = CitaDAO.obtenerCitasPorDoctor(doctor.getId());
        if (citas.isEmpty()) {
            Label vacio = new Label("Este médico no tiene citas agendadas.");
            vacio.setTextFill(Color.web(TITLE_COLOR));
            vacio.setPadding(new Insets(20, 32, 20, 32));
            lista.getChildren().add(vacio);
        } else {
            for (Cita c : citas) {
                lista.getChildren().add(cardCita(c, screenRoot));
            }
        }

        content.getChildren().addAll(breadcrumb, titulo, scrollPane);
        screenRoot.setCenter(content);

        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:6 14;");
        btnAtras.setOnAction(e -> new DoctoresPorEspecialidadScreen().show(ScreenRouter.getStage(), doctor.getEspecialidad()));
        HBox footerLocal = new HBox(btnAtras);
        footerLocal.setAlignment(Pos.CENTER_LEFT);
        footerLocal.setPadding(new Insets(0, 32, 20, 32));
        screenRoot.setBottom(footerLocal);

        ScreenRouter.setView(screenRoot);
    }

    private HBox crearBarraSuperior() {
        HBox top = new HBox();
        top.setStyle("-fx-background-color:#FFFFFF; -fx-border-color: transparent transparent #E9EEF5 transparent; -fx-border-width:0 0 1 0;");
        top.setPadding(new Insets(8, 32, 8, 32));
        top.setAlignment(Pos.CENTER_LEFT);
        top.setSpacing(12);

        ImageView simed = icon("Logo.png", 100, 100);

        Button bInicio = new Button("Inicio", icon("Inicio.png", 22, 22));
        bInicio.setContentDisplay(ContentDisplay.LEFT);
        bInicio.setGraphicTextGap(8);
        bInicio.setStyle("-fx-background-color:#D0E1F9; -fx-text-fill:#1F355E; -fx-font-weight:bold; -fx-background-radius:10; -fx-padding:8 16;");
        bInicio.setMinHeight(36);
        bInicio.setOnAction(e -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));

        Region spL = new Region(); HBox.setHgrow(spL, Priority.ALWAYS);
        HBox middle = new HBox(40, bInicio);
        middle.setAlignment(Pos.CENTER);
        Region spR = new Region(); HBox.setHgrow(spR, Priority.ALWAYS);

        Label user = new Label(Sesion.getNombreUsuario() == null ? "Usuario" : Sesion.getNombreUsuario(), icon("User.png", 22, 22));
        user.setContentDisplay(ContentDisplay.LEFT);
        user.setGraphicTextGap(8);
        user.setTextFill(Color.web(TITLE_COLOR));
        user.setFont(Font.font(13));

        Button salir = new Button("", icon("Close.png", 22, 22));
        salir.setStyle("-fx-background-color:#1F355E;");
        salir.setOnAction(e -> new org.example.Main().start(ScreenRouter.getStage()));

        top.getChildren().addAll(simed, spL, middle, spR, user, salir);
        return top;
    }

    private Pane cardCita(Cita c, BorderPane mainContainer) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color:" + CARD_BORDER_COLOR + "; -fx-border-radius:12; -fx-background-radius:12;");
        card.setSpacing(10);

        HBox fila1 = new HBox(40);
        fila1.setAlignment(Pos.CENTER_LEFT);
        Label dia = bold("Día: " + capitalize(c.getFechaHora().toLocalDate().format(DIA_FMT)));
        Label hora = bold("Hora: " + c.getFechaHora().toLocalTime().format(HORA_FMT).toLowerCase());
        fila1.getChildren().addAll(dia, hora);

        HBox fila2 = new HBox(40);
        fila2.setAlignment(Pos.CENTER_LEFT);
        Label doctorLbl = new Label("Doctor: " + c.getNombreDoctor());
        doctorLbl.setTextFill(Color.web(TITLE_COLOR));
        Label pac = new Label("Paciente: " + c.getNombrePaciente());
        pac.setTextFill(Color.web(TITLE_COLOR));
        fila2.getChildren().addAll(doctorLbl, pac);

        Label consult = new Label("Consultorio: " + (c.getConsultorio() == null ? "-" : c.getConsultorio()));
        consult.setTextFill(Color.web(TITLE_COLOR));

        Button reagendar = new Button("Reagendar cita");
        reagendar.setStyle("-fx-background-color:#D0E1F9; -fx-text-fill:#1F355E; -fx-font-weight:bold; -fx-background-radius:8; -fx-padding:6 10;");

        // =======================================================
        // LÍNEA CORREGIDA
        // =======================================================
        reagendar.setOnAction(e -> {
            String esp = (c.getEspecialidad() == null) ? "Medicina General" : c.getEspecialidad();
            Doctor d = new Doctor(c.getIdDoctor(), c.getNombreDoctor(), null, null, esp);

            ReagendarAdmin.show(
                    ScreenRouter.getStage(),
                    d,
                    esp,
                    c.getMatricula(),
                    c.getId()
            );
        });
        // =======================================================

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox filaBtn = new HBox(spacer, reagendar);

        card.getChildren().addAll(fila1, fila2, consult, filaBtn);
        return card;
    }

    // --- Helpers ---
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Label bold(String t) {
        Label l = new Label(t);
        l.setTextFill(Color.web(TITLE_COLOR));
        l.setFont(Font.font("System", FontWeight.BOLD, 13));
        return l;
    }

    private static Label link(String text, Runnable action) {
        Label l = new Label(text);
        l.setTextFill(Color.web(TITLE_COLOR));
        l.setStyle("-fx-underline:true; -fx-cursor: hand;");
        l.setOnMouseClicked(e -> action.run());
        return l;
    }

    private static Label dot() {
        Label s = new Label("•");
        s.setTextFill(Color.web(TITLE_COLOR));
        return s;
    }

    private static ImageView icon(String file, double w, double h) {
        try {
            var url = CitasMed.class.getResource(BASE_ASSETS + file);
            if (url == null) {
                System.err.println("⚠ Icono no encontrado: " + file);
                return new ImageView();
            }
            return new ImageView(new Image(url.toExternalForm(), w, h, true, true));
        } catch (Exception e) {
            System.err.println("Error cargando icono: " + file);
            return new ImageView();
        }
    }
}