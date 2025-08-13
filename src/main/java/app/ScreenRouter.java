package app;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class ScreenRouter {
    private static Stage stage;
    private static Scene scene;
    private static StackPane rootContainer;

    private ScreenRouter() {}

    // IMPORTANTE: llamar esto al entrar en cualquier pantalla
    public static void initIfNeeded(Stage s) {
        if (stage != null) return;
        stage = s;
        rootContainer = new StackPane();
        scene = new Scene(rootContainer, 1000, 700);
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }

    // Cambia la vista actual por la nueva
    public static void setView(Parent view) {
        if (rootContainer == null) rootContainer = new StackPane();
        rootContainer.getChildren().setAll(view);
    }

    public static Stage getStage() { return stage; }
}
