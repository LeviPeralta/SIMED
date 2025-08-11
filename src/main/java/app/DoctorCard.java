package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.InputStream;

public class DoctorCard extends StackPane {

    public static HBox crearTarjetaDoctor(Doctor doc, String especialidad, MenuScreen menuScreen) {
        // ---------------------
        // CARGA DE IMAGEN
        // ---------------------
        String path = "/images/Doctors/" + doc.getImagen();
        InputStream is = MenuScreen.class.getResourceAsStream(path);

        if (is == null) {
            System.out.println("⚠️ Imagen no encontrada, usando default.jpg para: " + doc.getNombre());
            is = MenuScreen.class.getResourceAsStream("/images/Doctors/default.jpg"); // Imagen genérica
        }

        Image img = new Image(is);
        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(90);
        imageView.setFitHeight(90);
        imageView.setClip(new Circle(45, 45, 45)); // máscara circular

        // Borde blanco
        Circle bordeBlanco = new Circle(50);
        bordeBlanco.setFill(Color.WHITE);
        StackPane imagenConBorde = new StackPane(bordeBlanco, imageView);
        imagenConBorde.setPrefSize(96, 96);
        imagenConBorde.setAlignment(Pos.CENTER);

        // ---------------------
        // NOMBRE Y HORARIO
        // ---------------------
        Label lblNombre = new Label(doc.getNombre());
        lblNombre.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblNombre.setTextFill(Color.web("#1F355E"));

        String[] partesHorario = doc.getHorario().split("\n");
        Label lblDias = new Label(partesHorario.length > 0 ? partesHorario[0] : "");
        Label lblHoras = new Label(partesHorario.length > 1 ? partesHorario[1] : "");

        lblDias.setFont(Font.font("System", 13));
        lblHoras.setFont(Font.font("System", 13));
        lblDias.setTextFill(Color.web("#7A8CA6"));
        lblHoras.setTextFill(Color.web("#7A8CA6"));

        VBox horarios = new VBox(lblDias, lblHoras);
        horarios.setAlignment(Pos.CENTER_LEFT);
        horarios.setSpacing(2);

        VBox infoBox = new VBox(lblNombre, horarios);
        infoBox.setSpacing(8);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        // Mueve la imagen hacia la izquierda para sobresalir del rectángulo
        imagenConBorde.setTranslateX(-70);

        String estiloNormal = "-fx-background-color: #F6F9FF; -fx-background-radius: 20;";
        String estiloHover  = "-fx-background-color: #E0ECFF; -fx-background-radius: 20;";

        // Crea la tarjeta
        HBox tarjeta = new HBox(30, imagenConBorde, infoBox);
        tarjeta.setPadding(new Insets(20, 40, 20, 40)); // más espacio a la izquierda para compensar
        tarjeta.setStyle("-fx-background-color: #F6F9FF; -fx-background-radius: 20;");
        tarjeta.setAlignment(Pos.CENTER_LEFT);
        tarjeta.setPrefWidth(450);
        tarjeta.setMinHeight(120);

        // Cambiar estilo al pasar el mouse
        tarjeta.setOnMouseEntered(e -> tarjeta.setStyle(estiloHover));
        tarjeta.setOnMouseExited(e -> tarjeta.setStyle(estiloNormal));

        tarjeta.setOnMouseClicked(e -> {
            menuScreen.mostrarHorarioDoctor(doc, especialidad);
        });


        return tarjeta;
    }
}
