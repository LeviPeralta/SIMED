package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
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

import static app.DoctorCard.crearTarjetaDoctor;

public class MenuScreen {
    private static VBox root;
    private StackPane centerContainer = new StackPane();
    private int currentPage = 1;
    private final int DOCTORES_POR_PAGINA = 6;
    private List<Doctor> doctoresEspecialidadActual;
    private String idPaciente; // String (VARCHAR2)

    public void show(Stage stage) {
        root = new VBox();
        centerContainer = new StackPane();
        stage.setTitle("SIMED - Sistema de Citas Médicas");

        HBox menuBar = new HBox();
        menuBar.setStyle("-fx-background-color: #FFFFFF;");
        menuBar.setPadding(new Insets(0, 40, 0, 40));
        menuBar.setSpacing(10);
        menuBar.setAlignment(Pos.CENTER_LEFT);

        ImageView simedIcon = createIcon("Logo.png", 120, 120); // Tamaño más grande

        String estiloBoton = "-fx-background-color: #D0E1F9; " +
                "-fx-text-fill: #1F355E; " +
                "-fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-padding: 10 20 10 20;";

        String estiloEmergencia = "-fx-background-color: #B1361E; " +
                "-fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; " +
                "-fx-padding: 10 20 10 20;";

        Button btnInicio = new Button("Inicio", createIcon("Inicio.png", 24, 24));
        btnInicio.setContentDisplay(ContentDisplay.LEFT);
        btnInicio.setGraphicTextGap(8);
        btnInicio.setStyle(estiloBoton);
        btnInicio.setMinHeight(40);
        btnInicio.setOnAction(e -> volverAMenu());

        Button btnCitas = new Button("Mis citas", createIcon("miCitas.png", 24, 24));
        btnCitas.setContentDisplay(ContentDisplay.LEFT);
        btnCitas.setGraphicTextGap(8);
        btnCitas.setStyle(estiloBoton);
        btnCitas.setMinHeight(40);

        Button btnEmergencia = new Button("EMERGENCIA");
        btnEmergencia.setStyle(estiloEmergencia);
        btnEmergencia.setFont(Font.font("System", FontWeight.BOLD, 14));
        btnEmergencia.setMinHeight(40);

        HBox centerButtons = new HBox(btnInicio, btnCitas, btnEmergencia);
        centerButtons.setSpacing(60);
        centerButtons.setAlignment(Pos.CENTER);
        centerButtons.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(centerButtons, Priority.ALWAYS);

        Label lblUsuario = new Label("Nombre usuario", createIcon("User.png", 24, 24));
        lblUsuario.setFont(Font.font("System", 14));
        lblUsuario.setTextFill(Color.web("#1F355E"));
        lblUsuario.setContentDisplay(ContentDisplay.LEFT);
        lblUsuario.setGraphicTextGap(8);

        Button btnSalir = new Button("", createIcon("Close.png", 24, 24));
        btnSalir.setStyle("-fx-background-color: #1F355E;");
        btnSalir.setOnAction(e -> {
            // Cerrar la ventana actual
            Stage currentStage = (Stage) btnSalir.getScene().getWindow();
            currentStage.close();

            // Volver a la pantalla de login (Main)
            Stage loginStage = new Stage();
            try {
                new org.example.Main().start(loginStage); // ← asegúrate del paquete correcto
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


        Region spacerL = new Region();
        Region spacerR = new Region();
        HBox.setHgrow(spacerL, Priority.ALWAYS);
        HBox.setHgrow(spacerR, Priority.ALWAYS);

        menuBar.getChildren().addAll(simedIcon, spacerL, centerButtons, spacerR, lblUsuario, btnSalir);

        GridPane grid = new GridPane();
        centerContainer.getChildren().add(grid);
        grid.setHgap(40);
        grid.setVgap(40);
        grid.setPadding(new Insets(60));
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: white;");

        ServicioCard cardMedicina = new ServicioCard(
                "Medicina General",
                "Es la atención médica primaria, encargada de diagnosticar, prevenir y tratar enfermedades comunes.",
                "Procedimientos comunes:\n• Consulta médica general\n• Toma de signos vitales\n• Prescripción de medicamentos\n• Certificados médicos\n• Control de enfermedades crónicas\n• Análisis clínicos básicos",
                createIcon("MedicinaGeneral.png", 60, 60)
        );
        cardMedicina.setOnMouseClicked(e -> mostrarDoctores("Medicina General"));
        grid.add(cardMedicina, 0, 0);

// CARDIOLOGÍA
        ServicioCard cardCardio = new ServicioCard(
                "Cardiología",
                "Especialidad enfocada en el diagnóstico y tratamiento de enfermedades del corazón y del sistema circulatorio",
                "Procedimientos comunes:\n• Electrocardiograma (ECG)\n• Ecocardiograma\n• Pruebas de esfuerzo\n• Consulta especializada",
                createIcon("Cardiologia.png", 60, 60)
        );
        cardCardio.setOnMouseClicked(e -> mostrarDoctores("Cardiología"));
        grid.add(cardCardio, 1, 0);

// NEUROLOGÍA
        ServicioCard cardNeuro = new ServicioCard(
                "Neurología",
                "Se encarga del diagnóstico y tratamiento de enfermedades del sistema nervioso central y periférico.",
                "Procedimientos comunes:\n• Evaluación neurológica\n• Resonancia magnética\n• Tratamiento de epilepsia y migraña",
                createIcon("Neurologia.png", 60, 60)
        );
        cardNeuro.setOnMouseClicked(e -> mostrarDoctores("Neurología"));
        grid.add(cardNeuro, 2, 0);

// GINECOLOGÍA
        ServicioCard cardGine = new ServicioCard(
                "Ginecología",
                "Atiende la salud del aparato reproductor femenino y el seguimiento del embarazo.",
                "Procedimientos comunes:\n• Papanicolaou\n• Ultrasonido pélvico\n• Control prenatal",
                createIcon("gineco.png", 60, 60)
        );
        cardGine.setOnMouseClicked(e -> mostrarDoctores("Ginecología"));
        grid.add(cardGine, 0, 1);

// UROLOGÍA
        ServicioCard cardUro = new ServicioCard(
                "Urología",
                "Trata enfermedades del aparato urinario en hombres y mujeres, y del aparato reproductor masculino.",
                "Procedimientos comunes:\n• Consulta urológica\n• Estudios de próstata\n• Ecografía renal",
                createIcon("urologia.png", 60, 60)
        );
        cardUro.setOnMouseClicked(e -> mostrarDoctores("Urología"));
        grid.add(cardUro, 1, 1);

// TRAUMATOLOGÍA
        ServicioCard cardTrauma = new ServicioCard(
                "Traumatología",
                "Especialidad que atiende lesiones del sistema músculo-esquelético (huesos, músculos, articulaciones).",
                "Procedimientos comunes:\n• Radiografías\n• Inmovilización de fracturas\n• Terapia física",
                createIcon("trauma.png", 60, 60)
        );
        cardTrauma.setOnMouseClicked(e -> mostrarDoctores("Traumatología"));
        grid.add(cardTrauma, 2, 1);


        root.setStyle("-fx-background-color: white;");
        root.setSpacing(0);
        root.getChildren().addAll(menuBar, centerContainer); // ya los creaste antes correctamente

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    private Node crearGridEspecialidades(String especialidad) {
        List<Doctor> doctores = DoctorData.getDoctoresPorEspecialidad(especialidad);
        GridPane grid = new GridPane();
        grid.setHgap(40);
        grid.setVgap(20);
        grid.setPadding(new Insets(40));
        int col = 0;
        int row = 0;

        for (Doctor doctor : doctores) {
            VBox card = crearDoctorCard(doctor);
            grid.add(card, col, row);
            col++;
            if (col == 3) {
                col = 0;
                row++;
            }
        }

        return grid;
    }

    private VBox crearDoctorCard(Doctor doctor) {
        VBox card = new VBox();
        card.setPadding(new Insets(15));
        card.setSpacing(10);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(200);
        card.setStyle("-fx-background-color: #D0E1F9; -fx-background-radius: 15;");

        ImageView imageView = new ImageView(new Image(getClass().getResource("/images/mainPage/" + doctor.getImagen()).toExternalForm()));
        imageView.setFitWidth(120);
        imageView.setFitHeight(120);

        Label nombre = new Label(doctor.getNombre());
        nombre.setFont(new Font(14));
        nombre.setTextFill(Color.web("#1F355E"));
        nombre.setWrapText(true);
        nombre.setAlignment(Pos.CENTER);

        Label horario = new Label(doctor.getHorario());
        horario.setFont(new Font(12));
        horario.setTextFill(Color.web("#3A3A3A"));
        horario.setWrapText(true);
        horario.setAlignment(Pos.CENTER);

        card.getChildren().addAll(imageView, nombre, horario);
        return card;
    }

    private static ImageView createIcon(String fileName, double width, double height) {
        String path = "/images/mainPage/" + fileName;
        Image img = new Image(MenuScreen.class.getResource(path).toExternalForm());
        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        return imageView;
    }

    public static class ServicioCard extends StackPane {
        private final String descripcionCorta;
        private final String descripcionExtendida;
        public ServicioCard(String titulo, String descripcionCorta, String descripcionExtendida, Node icono) {
            this.descripcionCorta = descripcionCorta;
            this.descripcionExtendida = descripcionExtendida;
            Circle circle = new Circle(40);
            circle.setFill(Color.web("#F3F7FB"));
            circle.setStroke(Color.WHITE);            // Borde blanco
            circle.setStrokeWidth(7);

            StackPane iconCircle = new StackPane(circle, icono);
            iconCircle.setPrefSize(80, 80);
            iconCircle.setTranslateY(-40);

            Label lblTitulo = new Label(titulo);
            lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 16));
            lblTitulo.setTextFill(Color.web("#1F355E"));
            lblTitulo.setAlignment(Pos.CENTER);
            lblTitulo.setWrapText(true);

            Label lblDescripcion = new Label(descripcionCorta);
            lblDescripcion.setWrapText(true);
            lblDescripcion.setFont(Font.font(14));
            lblDescripcion.setTextFill(Color.web("#555555"));
            lblDescripcion.setMaxWidth(220);
            lblDescripcion.setMaxWidth(240);

            VBox content = new VBox(lblTitulo, lblDescripcion);
            content.setAlignment(Pos.TOP_CENTER);
            content.setSpacing(10);
            content.setPadding(new Insets(0, 10, 0, 10));

            VBox card = new VBox(iconCircle, content);
            card.setAlignment(Pos.TOP_CENTER);
            card.setSpacing(-20);
            card.setStyle("-fx-background-color: #F3F7FB; -fx-background-radius: 20;");
            card.setPrefSize(310, 260);

            this.getChildren().add(card);
            this.setAlignment(Pos.TOP_CENTER);
            this.setPadding(new Insets(10));

            setOnMouseEntered(e -> {
                card.setScaleX(1.07);
                card.setScaleY(1.07);
                iconCircle.setTranslateY(-60);
                lblDescripcion.setText(descripcionExtendida);
            });

            setOnMouseExited(e -> {
                card.setScaleX(1.0);
                card.setScaleY(1.0);
                iconCircle.setTranslateY(-40);
                lblDescripcion.setText(descripcionCorta);
            });

        }
    }

    public void volverAMenu() {
        centerContainer.getChildren().clear();
        GridPane grid = new GridPane();
        centerContainer.getChildren().add(grid);
        grid.setHgap(40);
        grid.setVgap(40);
        grid.setPadding(new Insets(60));
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: white;");

        ServicioCard cardMedicina = new ServicioCard(
                "Medicina General",
                "Es la atención médica primaria, encargada de diagnosticar, prevenir y tratar enfermedades comunes.",
                "Procedimientos comunes:\n• Consulta médica general\n• Toma de signos vitales\n• Prescripción de medicamentos\n• Certificados médicos\n• Control de enfermedades crónicas\n• Análisis clínicos básicos",
                createIcon("MedicinaGeneral.png", 60, 60)
        );
        cardMedicina.setOnMouseClicked(e -> mostrarDoctores("Medicina General"));
        grid.add(cardMedicina, 0, 0);

        ServicioCard cardCardio = new ServicioCard(
                "Cardiología",
                "Especialidad enfocada en el diagnóstico y tratamiento de enfermedades del corazón y del sistema circulatorio",
                "Procedimientos comunes:\n• Electrocardiograma (ECG)\n• Ecocardiograma\n• Pruebas de esfuerzo\n• Consulta especializada",
                createIcon("Cardiologia.png", 60, 60)
        );
        cardCardio.setOnMouseClicked(e -> mostrarDoctores("Cardiología"));
        grid.add(cardCardio, 1, 0);

        ServicioCard cardNeuro = new ServicioCard(
                "Neurología",
                "Se encarga del diagnóstico y tratamiento de enfermedades del sistema nervioso central y periférico.",
                "Procedimientos comunes:\n• Evaluación neurológica\n• Resonancia magnética\n• Tratamiento de epilepsia y migraña",
                createIcon("Neurologia.png", 60, 60)
        );
        cardNeuro.setOnMouseClicked(e -> mostrarDoctores("Neurología"));
        grid.add(cardNeuro, 2, 0);

        ServicioCard cardGine = new ServicioCard(
                "Ginecología",
                "Atiende la salud del aparato reproductor femenino y el seguimiento del embarazo.",
                "Procedimientos comunes:\n• Papanicolaou\n• Ultrasonido pélvico\n• Control prenatal",
                createIcon("gineco.png", 60, 60)
        );
        cardGine.setOnMouseClicked(e -> mostrarDoctores("Ginecología"));
        grid.add(cardGine, 0, 1);

        ServicioCard cardUro = new ServicioCard(
                "Urología",
                "Trata enfermedades del aparato urinario en hombres y mujeres, y del aparato reproductor masculino.",
                "Procedimientos comunes:\n• Consulta urológica\n• Estudios de próstata\n• Ecografía renal",
                createIcon("urologia.png", 60, 60)
        );
        cardUro.setOnMouseClicked(e -> mostrarDoctores("Urología"));
        grid.add(cardUro, 1, 1);

        ServicioCard cardTrauma = new ServicioCard(
                "Traumatología",
                "Especialidad que atiende lesiones del sistema músculo-esquelético (huesos, músculos, articulaciones).",
                "Procedimientos comunes:\n• Radiografías\n• Inmovilización de fracturas\n• Terapia física",
                createIcon("trauma.png", 60, 60)
        );
        cardTrauma.setOnMouseClicked(e -> mostrarDoctores("Traumatología"));
        grid.add(cardTrauma, 2, 1);

        centerContainer.getChildren().setAll(grid);
    }

    public void mostrarDoctores(String especialidad) {
        this.currentPage = 1;
        this.doctoresEspecialidadActual = DoctorData.getDoctoresPorEspecialidad(especialidad);

        renderPaginaDoctores(especialidad);
    }

    private void renderPaginaDoctores(String especialidad) {
        VBox contenedor = new VBox(20);
        contenedor.setPadding(new Insets(20));
        contenedor.setStyle("-fx-background-color: white;");

        HBox breadcrumb = new HBox();
        breadcrumb.setSpacing(5);
        breadcrumb.setPadding(new Insets(0, 0, 0, 20));
        breadcrumb.setAlignment(Pos.CENTER_LEFT);

        // "Inicio" clickable
        Label linkInicio = new Label("Inicio");
        linkInicio.setTextFill(Color.web("#1F355E"));
        linkInicio.setStyle("-fx-underline: true;");
        linkInicio.setOnMouseClicked(e -> volverAMenu());

        // Separador
        Label sep1 = new Label("•");
        sep1.setTextFill(Color.web("#1F355E"));

        // "Doctores" clickable
        Label linkDoctores = new Label("Doctores");
        linkDoctores.setTextFill(Color.web("#1F355E"));
        linkDoctores.setStyle("-fx-underline: true;");
        linkDoctores.setOnMouseClicked(e -> mostrarDoctores("Medicina General")); // Puedes modificar si quieres otra lógica

        // Separador
        Label sep2 = new Label("•");
        sep2.setTextFill(Color.web("#1F355E"));

        // Texto actual (no clickable)
        Label especialidadActual = new Label(especialidad);
        especialidadActual.setTextFill(Color.web("#1F355E"));
        especialidadActual.setFont(Font.font("System", FontWeight.BOLD, 14));

        breadcrumb.getChildren().addAll(linkInicio, sep1, linkDoctores, sep2, especialidadActual);


        Label titulo = new Label(especialidad);
        titulo.setFont(Font.font("System", FontWeight.BOLD, 28));
        titulo.setTextFill(Color.web("#1F355E"));
        titulo.setAlignment(Pos.CENTER);
        titulo.setMaxWidth(Double.MAX_VALUE);

        // Calcular rangos
        int totalDoctores = doctoresEspecialidadActual.size();
        int totalPaginas = (int) Math.ceil((double) totalDoctores / DOCTORES_POR_PAGINA);
        int desde = (currentPage - 1) * DOCTORES_POR_PAGINA;
        int hasta = Math.min(desde + DOCTORES_POR_PAGINA, totalDoctores);

        List<Doctor> paginaDoctores = doctoresEspecialidadActual.subList(desde, hasta);

        // Mostrar doctores en Grid
        GridPane gridDoctores = new GridPane();
        gridDoctores.setHgap(70);
        gridDoctores.setVgap(40);
        gridDoctores.setAlignment(Pos.CENTER);

        int col = 0;
        int row = 0;
        for (Doctor doc : paginaDoctores) {
            Node tarjeta = crearTarjetaDoctor(doc, especialidad, this);

            gridDoctores.add(tarjeta, col, row);
            col++;
            if (col > 1) {
                col = 0;
                row++;
            }
        }


        // Botón Atrás
        Button btnAtras = new Button("← Atrás");
        btnAtras.setStyle("-fx-background-color: #1F355E; -fx-text-fill: white; -fx-background-radius: 8;");
        btnAtras.setOnAction(e -> volverAMenu());

        // Barra de paginación real
        HBox paginacion = crearBarraPaginacion(totalPaginas, especialidad);
        paginacion.setAlignment(Pos.CENTER);
        VBox.setMargin(paginacion, new Insets(10, 0, 0, 0));

        contenedor.getChildren().addAll(breadcrumb, titulo, gridDoctores, paginacion, crearBotonAtras());
        centerContainer.getChildren().setAll(contenedor);

    }

    private HBox crearBotonAtras() {
        Button btnAtras = new Button("← Atrás");
        btnAtras.setStyle("-fx-background-color: #1F355E; -fx-text-fill: white; -fx-background-radius: 8;");
        btnAtras.setOnAction(e -> volverAMenu());

        HBox contenedor = new HBox(btnAtras);
        contenedor.setAlignment(Pos.CENTER_LEFT); // Alineado a la izquierda
        contenedor.setPadding(new Insets(10, 0, 0, 20)); // Separación interna

        return contenedor;
    }

    private HBox crearBarraPaginacion(int totalPaginas, String especialidad) {
        HBox barra = new HBox(10);

        Button primero = new Button("«");
        Button anterior = new Button("‹");
        Button siguiente = new Button("›");
        Button ultimo = new Button("»");

        primero.setOnAction(e -> {
            currentPage = 1;
            renderPaginaDoctores(especialidad);
        });

        anterior.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderPaginaDoctores(especialidad);
            }
        });

        siguiente.setOnAction(e -> {
            if (currentPage < totalPaginas) {
                currentPage++;
                renderPaginaDoctores(especialidad);
            }
        });

        ultimo.setOnAction(e -> {
            currentPage = totalPaginas;
            renderPaginaDoctores(especialidad);
        });

        // Página actual
        Label lblPagina = new Label("Página " + currentPage + " de " + totalPaginas);
        lblPagina.setFont(Font.font("System", FontWeight.NORMAL, 14));
        lblPagina.setTextFill(Color.web("#1F355E"));

        barra.getChildren().addAll(primero, anterior, lblPagina, siguiente, ultimo);
        barra.setAlignment(Pos.CENTER);
        return barra;
    }

    public void mostrarHorarioDoctor(Doctor doctor, String especialidad) {
        HorarioScreen.mostrarHorario(doctor, especialidad, centerContainer, Sesion.getMatricula());
    }

}
