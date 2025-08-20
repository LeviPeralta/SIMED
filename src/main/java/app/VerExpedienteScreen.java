// Archivo: VerExpedienteScreen.java
package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class VerExpedienteScreen {

    public void show(Stage owner, long idCita) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Visualizar Expediente Médico");

        ExpedienteMedDao dao = new ExpedienteMedDao();
        // Usamos el tipo de dato correcto: ExpedienteMed
        ExpedienteMed expediente = dao.getExpedienteByCitaId(idCita);

        if (expediente == null) {
            new Alert(Alert.AlertType.ERROR, "Error: No se pudo encontrar el expediente.", ButtonType.OK).showAndWait();
            return;
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F7F9FC;");

        // Encabezado
        Label lblTitulo = new Label("Detalles del Expediente");
        lblTitulo.setFont(Font.font("System", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#1F355E"));
        HBox headerBox = new HBox(lblTitulo);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(20));
        headerBox.setStyle("-fx-background-color: #FFFFFF;");
        root.setTop(headerBox);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));

        // Campos del formulario (usando los getters de ExpedienteMed)
        grid.add(createLabel("Diagnóstico:"), 0, 0);
        grid.add(createReadOnlyTextArea(expediente.getDiagnostico()), 1, 0);

        grid.add(createLabel("Tratamiento:"), 0, 1);
        grid.add(createReadOnlyTextArea(expediente.getTratamiento()), 1, 1);

        grid.add(createLabel("Observaciones:"), 0, 2);
        grid.add(createReadOnlyTextArea(expediente.getObservaciones()), 1, 2);

        grid.add(createLabel("Exámenes Comp.:"), 0, 3);
        grid.add(createReadOnlyTextArea(expediente.getExamenesComp()), 1, 3);

        grid.add(createLabel("Signos Vitales:"), 0, 4);
        TextField pesoField = createReadOnlyTextField(expediente.getPesoKg() == null ? "N/A" : expediente.getPesoKg() + " kg");
        TextField tempField = createReadOnlyTextField(expediente.getTemperaturaC() == null ? "N/A" : expediente.getTemperaturaC() + " °C");
        TextField frField = createReadOnlyTextField(expediente.getFrecuenciaResp() == null ? "N/A" : expediente.getFrecuenciaResp() + " rpm");
        TextField imcField = createReadOnlyTextField(expediente.getImc() == null ? "N/A" : String.format("%.2f", expediente.getImc()));
        HBox signosVitalesBox = new HBox(10, pesoField, tempField, frField, imcField);
        grid.add(signosVitalesBox, 1, 4);

        root.setCenter(grid);

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color:#1F355E; -fx-text-fill:white; -fx-background-radius:8; -fx-padding:8 18;");
        HBox footerBox = new HBox(btnCerrar);
        footerBox.setAlignment(Pos.CENTER_RIGHT);
        footerBox.setPadding(new Insets(20));
        root.setBottom(footerBox);
        btnCerrar.setOnAction(e -> stage.close());

        Scene scene = new Scene(root, 750, 700);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
        return label;
    }

    private TextField createReadOnlyTextField(String text) {
        TextField tf = new TextField(text);
        tf.setEditable(false);
        tf.setStyle("-fx-background-color: #EFF4FA;");
        return tf;
    }

    private TextArea createReadOnlyTextArea(String text) {
        TextArea ta = new TextArea(text);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefRowCount(4);
        ta.setStyle("-fx-background-color: #EFF4FA;");
        return ta;
    }
}