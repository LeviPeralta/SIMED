package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class CitasMed {

    private static final String TITLE_COLOR = "#1F355E";
    private static final String CARD_BORDER = "#1F355E";
    private static final DateTimeFormatter DIA_FMT  = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'del' uuuu");
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("h:mm a");

    /** Muestra la pantalla dentro de un BorderPane existente (centerContainer). */
    public static void show(BorderPane container, Doctor doctor){
        VBox root = new VBox();
        root.setStyle("-fx-background-color:white;");
        root.setSpacing(10);

        // Breadcrumb
        HBox breadcrumb = new HBox(6);
        breadcrumb.setAlignment(Pos.CENTER_LEFT);
        Label lInicio = link("Inicio", () -> new MedicosEspecialidadesScreen().show(ScreenRouter.getStage()));
        Label dot1 = dot();
        Label lDocs = link("Doctores", () -> new DoctoresPorEspecialidadScreen().show(ScreenRouter.getStage(), doctor.getEspecialidad()));
        Label dot2 = dot();
        Label lAct = new Label("Mis Citas");
        lAct.setTextFill(Color.web(TITLE_COLOR));
        breadcrumb.getChildren().addAll(lInicio, dot1, lDocs, dot2, lAct);
        breadcrumb.setPadding(new Insets(10, 40, 0, 40));

        // Título
        Label titulo = new Label("Citas agendadas");
        titulo.setTextFill(Color.web(TITLE_COLOR));
        titulo.setFont(Font.font("System", FontWeight.BOLD, 16));
        titulo.setPadding(new Insets(10, 40, 0, 40));

        // Lista
        VBox lista = new VBox(16);
        lista.setPadding(new Insets(10, 40, 20, 40));

        List<Cita> citas = CitaDAO.obtenerCitasPorDoctor(doctor.getId());
        if (citas.isEmpty()){
            Label vacio = new Label("Este médico no tiene citas agendadas.");
            vacio.setTextFill(Color.web(TITLE_COLOR));
            vacio.setPadding(new Insets(20, 40, 20, 40));
            lista.getChildren().add(vacio);
        } else {
            for (Cita c : citas){
                lista.getChildren().add(cardCita(c, container));
            }
        }

        // Botón Atrás
        Button btnAtras = new Button("Atrás");
        btnAtras.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:6 14;");
        btnAtras.setOnAction(e -> new DoctoresPorEspecialidadScreen().show(ScreenRouter.getStage(), doctor.getEspecialidad()));
        HBox footerLocal = new HBox(btnAtras);
        footerLocal.setAlignment(Pos.CENTER_LEFT);
        footerLocal.setPadding(new Insets(0, 40, 20, 40));

        root.getChildren().addAll(breadcrumb, titulo, lista, footerLocal);

        // Pinta el contenido en el centro del BorderPane
        container.setCenter(root);
    }

    private static Pane cardCita(Cita c, BorderPane container){
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color:" + CARD_BORDER + "; -fx-border-radius:12; -fx-background-radius:12;");
        card.setSpacing(10);

        HBox fila1 = new HBox(40);
        fila1.setAlignment(Pos.CENTER_LEFT);
        Label dia   = bold("Día: " + c.getFechaHora().toLocalDate().format(DIA_FMT));
        Label hora  = bold("Hora: " + c.getFechaHora().toLocalTime().format(HORA_FMT).toLowerCase());
        fila1.getChildren().addAll(dia, hora);

        HBox fila2 = new HBox(40);
        fila2.setAlignment(Pos.CENTER_LEFT);
        Label doctor = new Label("Doctor: " + c.getNombreDoctor());
        doctor.setTextFill(Color.web(TITLE_COLOR));
        Label pac    = new Label("Paciente: " + c.getNombrePaciente());
        pac.setTextFill(Color.web(TITLE_COLOR));
        fila2.getChildren().addAll(doctor, pac);

        Label consult = new Label("Consultorio: " + (c.getConsultorio() == null ? "-" : c.getConsultorio()));
        consult.setTextFill(Color.web(TITLE_COLOR));

        Button reagendar = new Button("Reagendar cita");
        reagendar.setOnAction(e -> {
            String esp = (c.getEspecialidad() == null) ? "Medicina General" : c.getEspecialidad();
            Doctor d = new Doctor(c.getIdDoctor(), c.getNombreDoctor(), null, null, esp);
            HorarioScreen.mostrarHorario(d, esp, container, c.getMatricula(), c.getId());
        });

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox filaBtn = new HBox(spacer, reagendar);

        card.getChildren().addAll(fila1, fila2, consult, filaBtn);
        return card;
    }

    private static Label bold(String t){
        Label l = new Label(t);
        l.setTextFill(Color.web(TITLE_COLOR));
        l.setFont(Font.font("System", FontWeight.BOLD, 13));
        return l;
    }
    private static Label link(String text, Runnable action){
        Label l = new Label(text);
        l.setTextFill(Color.web(TITLE_COLOR));
        l.setStyle("-fx-underline:true;");
        l.setOnMouseClicked(e -> action.run());
        return l;
    }
    private static Label dot(){
        Label s = new Label("•");
        s.setTextFill(Color.web(TITLE_COLOR));
        return s;
    }
}