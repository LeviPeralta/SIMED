//PROYECTO INTEGRADOR - SISTEMAS DE GESTIÓN DE CITAS MÉDICAS

package org.example;

import app.MenuScreen;
import app.Sesion;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.util.Duration;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Pattern;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.sql.SQLException;

import static java.awt.SystemColor.window;


public class Main extends Application {

    //CONTENEDORES
    private VBox formContainer; // PARA EL FORMULARIO LOGIN/REGISTRO
    private VBox welcomeContainer; //PARA EL PANEL DE BIENVENIDA AZUL
    private boolean isSignIn = true; // VARIABLE QUE ALTERA ENTRE EL LOGIN Y EL REGISTRO
    private Font customFont; // FUENTE PERSONALIZADA
    private String verificationCode; //CODIGO GENERADO PARA LA RECUPERACIÓN DE CONTRASEÑA

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        // Probar la conexión de base de datos
        try (Connection conn = OracleWalletConnector.getConnection()) {
            System.out.println("✅ Conexión exitosa a Oracle");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error de conexión", "No se pudo conectar: " + e.getMessage());
        }
        loadCustomFont();

        primaryStage.setTitle("Login / Registro"); //Título de la ventana

        // Contenedor horizontal principal
        HBox mainContainer = new HBox();
        mainContainer.setPrefSize(800, 500);
        mainContainer.setMaxSize(800, 500);
        mainContainer.setStyle("-fx-background-color: white; -fx-border-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        // Aplicar esquinas redondeadas
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(800, 500);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        mainContainer.setClip(clip);

        // panel del formulario (login o registro)
        formContainer = new VBox();
        formContainer.setAlignment(Pos.CENTER);
        formContainer.setSpacing(15);
        formContainer.setPadding(new Insets(30));
        formContainer.setPrefWidth(400);

        // Panel azul de bbienvenida con animaciones
        welcomeContainer = new VBox();
        welcomeContainer.setAlignment(Pos.CENTER);
        welcomeContainer.setSpacing(20);
        welcomeContainer.setPadding(new Insets(40, 50, 40, 50));
        welcomeContainer.setPrefWidth(400);
        welcomeContainer.setStyle("-fx-background-color: #3e7bb2; -fx-border-radius: 0 15 15 0;");

        formContainer.setTranslateX(0);
        welcomeContainer.setTranslateX(0);

        // Inicializa el contenido dependiendo del modo (login/registro)
        updateForm();
        updateWelcome();

        mainContainer.getChildren().addAll(formContainer, welcomeContainer);

        //Fondo gris claro general
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: #F0F4F3;");
        root.getChildren().add(mainContainer);
        root.setPrefSize(800, 500);

        Scene scene = new Scene(root, 800, 500);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Método para cargar una fuente
    private void loadCustomFont() {
        InputStream fontStream = getClass().getResourceAsStream("/resources/fonts/InstrumentSans-VariableFont_wdth,wght.ttf");
        if (fontStream != null) {
            customFont = Font.loadFont(fontStream, 14);
        } else {
            System.out.println("No se pudo cargar la fuente, se usará la predeterminada.");
            customFont = Font.getDefault();
        }
    }

    // Normaliza SIEMPRE la matrícula: sin espacios y en MAYÚSCULAS
    private static String normalizeMatricula(String raw) {
        if (raw == null) return null;
        return raw.trim().replaceAll("\\s+", "").toUpperCase();
    }

    private static String buildCorreo(String matriculaNorm) {
        return matriculaNorm.toLowerCase() + "@utez.edu.mx";
    }

    private void updateForm() {
        formContainer.getChildren().clear();

        // Parte de Inicio de sesión
        if (isSignIn) {
            Label title = new Label("Iniciar Sesión");
            title.setFont(Font.font(customFont.getFamily(), 24));

            TextField emailField = new TextField();
            emailField.setPromptText("ejemplo@correo.com");
            styleTextField(emailField);

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("••••••••");
            styleTextField(passwordField);

            VBox emailBox = createLabeledField("Correo electrónico", emailField);
            VBox passwordBox = createLabeledField("Contraseña", passwordField);

            Hyperlink forgotPassword = new Hyperlink("¿Olvidaste tu contraseña?");
            forgotPassword.setFont(customFont);
            forgotPassword.setStyle("" +
                    "-fx-text-fill: #134074; " +
                    "-fx-underline: true;");
            forgotPassword.setOnAction(e -> showPasswordResetWindow());

            // Acción del botón
            Button signInButton = new Button("Iniciar sesión");
            styleButton(signInButton);
            signInButton.setOnAction(e -> {
                String correo = emailField.getText().trim();
                String contrasena = passwordField.getText().trim();

                //Validación de campos
                if (correo.isEmpty() || contrasena.isEmpty()) {
                    showAlert("Campos vacíos", "Por favor ingrese el correo y la contraseña.");
                    return;
                }

                // Paso 1: verificar si el correo está registrado
                if (!correoExiste(correo)) {
                    showAlert("Correo no encontrado", "Este correo no está registrado. Por favor regístrese primero.");
                    return;
                }

                // Paso 2: validar las credenciales si el correo existe
                if (validarCredenciales(correo, contrasena)) {

                    // Suponiendo que txtCorreo contiene el correo ingresado
                    String correoRecuperacion = emailField.getText().trim();
                    System.out.println(correoRecuperacion);

                    // Extraer la matrícula (todo antes de la @)
                    String matricula = correoRecuperacion.split("@")[0];

                    // Guardar en la sesión global
                    Sesion.setMatricula(matricula);  // matricula es el valor que recuperaste del login o base de dato
                    System.out.println(matricula);
                    pantallaDeCarga();
                } else {
                    showAlert("Error", "Contraseña incorrecta.");
                }

            });


            formContainer.getChildren().addAll(title, emailBox, passwordBox, forgotPassword, signInButton);
        } else {
            Label title = new Label("Registrarse");
            title.setFont(Font.font(customFont.getFamily(), 24));

            TextField matriculaField = new TextField();
            matriculaField.setPromptText("Ej. 20243ds145");
            styleTextField(matriculaField);

            PasswordField passwordField2 = new PasswordField();
            passwordField2.setPromptText("••••••••");
            styleTextField(passwordField2);

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("••••••••");
            styleTextField(passwordField);

            VBox nameBox = createLabeledField("Matrícula", matriculaField);
            VBox emailBox = createLabeledField("Contraseña", passwordField2);
            VBox passwordBox = createLabeledField("Confirme su contraseña", passwordField);

            Button signUpButton = new Button("Registrarse");
            styleButton(signUpButton);

            signUpButton.setOnAction(e -> {
                // 1) Normaliza matrícula y arma correo institucional
                String matriculaNorm = normalizeMatricula(matriculaField.getText());
                String password = passwordField2.getText().trim();
                String confirmPassword = passwordField.getText().trim();

                if (matriculaNorm == null || matriculaNorm.isEmpty() || password.isEmpty()) {
                    showAlert("Campos incompletos", "Por favor complete la matrícula y la contraseña.");
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    showAlert("Error", "Las contraseñas no coinciden.");
                    return;
                }

                String correo = buildCorreo(matriculaNorm);

                // 2) ¿Ya existe el correo?
                if (correoYaRegistrado(correo)) {
                    showAlert("Usuario ya registrado", "Ya existe una cuenta con esa matrícula (" + correo + ").");
                    return;
                }

                // 3) NO insertamos nada aún. Abrimos el formulario de datos personales.
                showPatientForm(correo, password, matriculaNorm);
            });




            formContainer.getChildren().addAll(title, nameBox, emailBox, passwordBox, signUpButton);

        }
    }

    private boolean insertarNuevoUsuario(String correo, String contrasena) {
        String sql = "INSERT INTO Usuario (CORREO, CONTRASENA, ROL) VALUES (?, ?, ?)";
        try (Connection conn = OracleWalletConnector.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            System.out.println(sql);
            stmt.setString(1, correo);
            System.out.println("BD correo = " + correo);
            stmt.setString(2, contrasena);
            stmt.setString(3, "paciente"); // asignar rol paciente por defecto

            int filas = stmt.executeUpdate();
            return filas > 0;

        } catch (SQLException e) {
            if (e.getErrorCode() == 1) { // ORA-00001: violación de clave única
                showAlert("Usuario duplicado", "Ya existe una cuenta con esa matrícula.");
            } else {
                showAlert("Error de BD", "No se pudo insertar el usuario: " + e.getMessage());
            }
            return false;
        }
    }


    private boolean insertarNuevoUsuarioConPaciente(
            String correo,
            String password,
            String nombres,
            String apellidos,
            LocalDate fecha,
            String genero,
            String curp,
            String telefono,
            String tipoUsuario
    ) {
        Connection conn = null;

        try {
            conn = OracleWalletConnector.getConnection();
            conn.setAutoCommit(false); // Iniciar transacción

            // 1. Verificar si el usuario ya existe
            String checkUsuario = "SELECT 1 FROM Usuario WHERE correo = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkUsuario)) {
                checkStmt.setString(1, correo);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        // No existe, insertar nuevo usuario
                        String insertUsuario = "INSERT INTO Usuario (correo, contrasena, rol, tipo_usuario) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertUsuario)) {
                            insertStmt.setString(1, correo);
                            insertStmt.setString(2, password);
                            insertStmt.setString(3, "paciente");
                            insertStmt.setString(4, tipoUsuario);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            }

            // 2. Verificar si el paciente ya existe
            String checkPaciente = "SELECT 1 FROM Paciente WHERE correo = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkPaciente)) {
                checkStmt.setString(1, correo);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (!rs.next()) {
                        // No existe, insertar en Paciente
                        String insertPaciente = "INSERT INTO Paciente (correo, nombre, apellidos, fecha_nacimiento, sexo, curp, telefono, tipo_usuario) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement stmtPaciente = conn.prepareStatement(insertPaciente)) {
                            stmtPaciente.setString(1, correo);
                            stmtPaciente.setString(2, nombres);
                            stmtPaciente.setString(3, apellidos);
                            stmtPaciente.setDate(4, java.sql.Date.valueOf(fecha));
                            stmtPaciente.setString(5, genero);
                            stmtPaciente.setString(6, curp);
                            stmtPaciente.setString(7, telefono);
                            stmtPaciente.setString(8, tipoUsuario);
                            stmtPaciente.executeUpdate();
                        }
                    }
                }
            }

            conn.commit(); // todo exitoso
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }

            if (e.getErrorCode() == 1) {
                showAlert("Error", "Ya existe un usuario o paciente con ese correo.");
            } else {
                showAlert("Error de BD", "No se pudo registrar el usuario y paciente: " + e.getMessage());
            }
            return false;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void updateWelcome() {
        welcomeContainer.getChildren().clear();

        Label title = new Label(isSignIn ? "¡Bienvenido!" : "¡Hola!");
        title.setFont(Font.font(customFont.getFamily(), 30));
        title.setTextFill(Color.WHITE);

        Label message = new Label(isSignIn ?
                "Ingresa tus datos personales para usar todas las funciones del sitio" :
                "Regístrese con sus datos personales para usar todas las funciones del sitio");
        message.setWrapText(true);
        message.setTextFill(Color.WHITE);
        message.setFont(Font.font(customFont.getFamily(), 14));
        message.setAlignment(Pos.CENTER);
        message.setMaxWidth(300);
        message.setStyle("-fx-text-alignment: center;");

        Button toggleButton = new Button(isSignIn ? "REGISTRARSE" : "INICIAR SESIÓN");
        toggleButton.setFont(Font.font(customFont.getFamily(), 14));
        toggleButton.setStyle("-fx-background-color: transparent; -fx-border-color: white; -fx-border-width: 2; -fx-text-fill: white;");
        toggleButton.setOnAction(e -> {
            isSignIn = !isSignIn;
            updateForm();
            updateWelcome();
        });

        animatePanelColor(isSignIn);
        animatePanelSlide(isSignIn);

        welcomeContainer.setStyle(isSignIn
                ? "-fx-border-radius: 250 0 0 250;"
                : "-fx-border-radius: 0 250 250 0;");

        welcomeContainer.getChildren().addAll(title, message, toggleButton);
    }

    private VBox createLabeledField(String labelText, TextField inputField) {
        Label label = new Label(labelText);
        label.setFont(Font.font(customFont.getFamily(), 12));
        label.setTextFill(Color.web("#333"));

        VBox fieldBox = new VBox(5);
        fieldBox.getChildren().addAll(label, inputField);
        return fieldBox;
    }

    private void styleTextField(TextField tf) {
        tf.setFont(Font.font(customFont.getFamily(), 13));
        tf.setPrefWidth(250);
        tf.setPrefHeight(40);
        tf.setStyle("-fx-background-color: #EEEEEE; -fx-background-radius: 5; -fx-border-radius: 5;");
    }

    private void styleTextField(ComboBox<?> cb) {
        cb.setPrefWidth(250);
        cb.setPrefHeight(40);
        cb.setStyle("-fx-background-color: #EEEEEE; -fx-background-radius: 5; -fx-border-radius: 5;");
    }

    private void styleTextField(DatePicker dp) {
        dp.setPrefWidth(250);
        dp.setPrefHeight(40);
        dp.setStyle("-fx-background-color: #EEEEEE; -fx-background-radius: 5; -fx-border-radius: 5;");
    }


    private void styleButton(Button btn) {
        btn.setFont(Font.font(customFont.getFamily(), 15));
        btn.setPrefWidth(200);
        btn.setPrefHeight(35);
        String color = isSignIn ? "#134074" : "#13315C";
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 5;");

        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.05);
            btn.setScaleY(1.05);
            btn.setStyle("-fx-background-color: " + darken(color) + "; -fx-text-fill: white; -fx-background-radius: 5;");
        });

        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
            btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-background-radius: 5;");
        });
    }

    private String darken(String hexColor) {
        Color color = Color.web(hexColor);
        color = color.deriveColor(0, 1, 0.85, 1);
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    private void animatePanelColor(boolean toSignIn) {
        Color fromColor = toSignIn ? Color.web("#13315C") : Color.web("#134074");
        Color toColor = toSignIn ? Color.web("#134074") : Color.web("#13315C");

        CornerRadii corner = toSignIn
                ? new CornerRadii(250, 0, 0, 250, false)
                : new CornerRadii(0, 250, 250, 0, false);

        Background fromBg = new Background(new BackgroundFill(fromColor, corner, Insets.EMPTY));
        Background toBg = new Background(new BackgroundFill(toColor, corner, Insets.EMPTY));

        Timeline colorTransition = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(welcomeContainer.backgroundProperty(), fromBg)),
                new KeyFrame(Duration.seconds(0.5), new KeyValue(welcomeContainer.backgroundProperty(), toBg))
        );
        colorTransition.play();
    }

    private void animatePanelSlide(boolean toSignIn) {
        double offset = 400;

        TranslateTransition welcomeSlide = new TranslateTransition(Duration.seconds(0.5), welcomeContainer);
        TranslateTransition formSlide = new TranslateTransition(Duration.seconds(0.5), formContainer);

        welcomeSlide.setFromX(toSignIn ? -offset : 0);
        welcomeSlide.setToX(toSignIn ? 0 : -offset);

        formSlide.setFromX(toSignIn ? offset : 0);
        formSlide.setToX(toSignIn ? 0 : offset);

        welcomeSlide.play();
        formSlide.play();
    }

    // Funciones de recuperación de contraseña
    private boolean isValidEmail(String email) {
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return Pattern.matches(emailRegex, email);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showPasswordResetWindow() {
        Stage window = new Stage();
        window.setTitle("Cambio de contraseña");
        window.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        layout.setStyle("-fx-background-color: white;");

        Label title = new Label("Cambio de contraseña");
        title.setFont(Font.font(customFont.getFamily(), 20));
        title.setTextFill(Color.web("#13315C"));

        Label message = new Label("Ingrese el correo del cual desea cambiar la contraseña, le enviaremos un código para reestablecerla");
        message.setFont(Font.font(customFont.getFamily(), 14));
        message.setTextFill(Color.web("#13315C"));
        message.setWrapText(true);
        message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        styleTextField(emailField);

        Button sendButton = new Button("Enviar");
        styleButton(sendButton);

        sendButton.setOnAction(e -> {
            String email = emailField.getText().trim();
            if (!isValidEmail(email)) {
                showAlert("Correo inválido", "Por favor ingrese un correo electrónico válido.");
                return;
            }
            sendEmailWithCode(email);
            showCodeVerificationWindow();
            window.close();
        });

        layout.getChildren().addAll(title, message, emailField, sendButton);

        Scene scene = new Scene(layout, 500, 300);
        window.setScene(scene);
        window.showAndWait();
    }

    private void sendEmailWithCode(String toEmail) {
        verificationCode = String.valueOf(new Random().nextInt(900000) + 100000);

        final String username = "leviytp@gmail.com";
        final String password = "xccq ujfn cvgo oyqs";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Código de recuperación");
            message.setText("Tu código de recuperación es: " + verificationCode);
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo enviar el correo: " + e.getMessage());
        }
    }

    private void showCodeVerificationWindow() {
        Stage window = new Stage();
        window.setTitle("Cambio de contraseña");
        window.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        layout.setStyle("-fx-background-color: white; -fx-border-radius: 15;");

        Label title = new Label("Cambio de contraseña");
        title.setFont(Font.font(customFont.getFamily(), 20));
        title.setTextFill(Color.web("#13315C"));

        Label instructions = new Label("Ingrese el código enviado a su correo, para cambiar su contraseña");
        instructions.setWrapText(true);
        instructions.setTextFill(Color.web("#13315C"));
        instructions.setFont(Font.font(customFont.getFamily(), 14));
        instructions.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        TextField codeField = new TextField();
        codeField.setPromptText("Código");
        styleTextField(codeField);

        Button sendButton = new Button("Enviar");
        styleButton(sendButton);

        sendButton.setOnAction(e -> {
            String enteredCode = codeField.getText().trim();
            if (enteredCode.equals(verificationCode)) {
                showAlert("Éxito", "Código verificado correctamente. Ahora puedes cambiar tu contraseña.");
                window.close();
                showNewPasswordWindow();
            } else {
                showAlert("Error", "El código ingresado no es correcto.");
            }
        });

        layout.getChildren().addAll(title, instructions, codeField, sendButton);

        Scene scene = new Scene(layout, 500, 300);
        window.setScene(scene);
        window.showAndWait();
    }

    private void showNewPasswordWindow() {
        Stage window = new Stage();
        window.setTitle("Cambio de contraseña");
        window.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        layout.setStyle("-fx-background-color: white; -fx-border-radius: 15;");

        Label title = new Label("Cambio de contraseña");
        title.setFont(Font.font(customFont.getFamily(), 20));
        title.setTextFill(Color.web("#13315C"));

        VBox passBox1 = new VBox(5);
        Label passLabel1 = new Label("Ingresa tu contraseña");
        passLabel1.setFont(Font.font(customFont.getFamily(), 13));
        passLabel1.setTextFill(Color.web("#13315C"));
        PasswordField pass1 = new PasswordField();
        pass1.setPromptText("Password");
        styleTextField(pass1);
        passBox1.getChildren().addAll(passLabel1, pass1);

        VBox passBox2 = new VBox(5);
        Label passLabel2 = new Label("Ingresa tu contraseña nuevamente");
        passLabel2.setFont(Font.font(customFont.getFamily(), 13));
        passLabel2.setTextFill(Color.web("#13315C"));
        PasswordField pass2 = new PasswordField();
        pass2.setPromptText("Password");
        styleTextField(pass2);
        passBox2.getChildren().addAll(passLabel2, pass2);

        Button submitButton = new Button("Enviar");
        styleButton(submitButton);

        submitButton.setOnAction(e -> {
            String p1 = pass1.getText().trim();
            String p2 = pass2.getText().trim();

            if (p1.isEmpty() || p2.isEmpty()) {
                showAlert("Error", "Por favor completa ambos campos de contraseña.");
                return;
            }

            if (!p1.equals(p2)) {
                showAlert("Error", "Las contraseñas no coinciden.");
                return;
            }

            window.close();
            showPasswordChangedSuccess();

        });

        layout.getChildren().addAll(title, passBox1, passBox2, submitButton);

        Scene scene = new Scene(layout, 500, 300);
        window.setScene(scene);
        window.showAndWait();
    }

    private void showPasswordChangedSuccess() {
        Stage window = new Stage();
        window.setTitle("Contraseña restablecida");
        window.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(40));
        layout.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 10;");

        Label successLabel1 = new Label("La contraseña se ha reestablecido correctamente");
        successLabel1.setFont(Font.font(customFont.getFamily(), 16));
        successLabel1.setTextFill(Color.web("#13315C"));
        successLabel1.setWrapText(true);
        successLabel1.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Label successLabel2 = new Label("Vuelve a iniciar sesión");
        successLabel2.setFont(Font.font(customFont.getFamily(), 16));
        successLabel2.setTextFill(Color.web("#13315C"));

        Button backButton = new Button("Regresar");
        styleButton(backButton);

        backButton.setOnAction(e -> {
            isSignIn = true;
            updateForm();
            updateWelcome();
            window.close();
        });

        layout.getChildren().addAll(successLabel1, successLabel2, backButton);

        Scene scene = new Scene(layout, 500, 250);
        window.setScene(scene);
        window.showAndWait();
    }

    private void showNextScreen() {
        Stage currentStage = (Stage) formContainer.getScene().getWindow(); // obtener el stage actual
        MenuScreen menuScreen = new MenuScreen(); // crear instancia
        menuScreen.show(currentStage); // pasar el stage como lo espera tu método
    }


    private void showPatientForm(String correo, String password, String matriculaNorm) {
        ImageView photoView = new ImageView();
        Stage window = new Stage();
        window.setTitle("Datos personales del paciente");
        window.initModality(Modality.APPLICATION_MODAL);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(15);
        formGrid.setPadding(new Insets(30));

        Label sectionTitle = new Label("Datos personales del paciente");
        sectionTitle.setFont(Font.font(customFont.getFamily(), 16));
        sectionTitle.setTextFill(Color.web("#13315C"));
        GridPane.setColumnSpan(sectionTitle, 2);

        TextField nombresField = new TextField();
        nombresField.setPromptText("Ej. Juan Carlos");

        TextField apellidosField = new TextField();
        apellidosField.setPromptText("Ej. Pérez Gómez");

        DatePicker fechaNacimiento = new DatePicker();
        fechaNacimiento.setPromptText("Ej. 1995-06-12");

        ComboBox<String> generoCombo = new ComboBox<>();
        generoCombo.getItems().addAll("Masculino", "Femenino", "Otro");
        generoCombo.setPromptText("Seleccione una opción");

        TextField curpField = new TextField();
        curpField.setPromptText("Ej. PEGR950612HMCLNS08");

        TextField telefonoField = new TextField();
        telefonoField.setPromptText("Ej. 5512345678");

        ComboBox<String> tipoUsuarioCombo = new ComboBox<>();
        tipoUsuarioCombo.getItems().addAll("Docente", "Estudiante", "Administrativo");
        tipoUsuarioCombo.setPromptText("Seleccione el tipo de usuario");
        styleTextField(tipoUsuarioCombo);


        // Aplicar estilos
        for (TextField field : new TextField[]{nombresField,apellidosField, curpField, telefonoField}) {
            styleTextField(field);
        }
        styleTextField(generoCombo);
        styleTextField(fechaNacimiento);

        // Imagen y botón
        photoView.setFitWidth(140);
        photoView.setFitHeight(160);
        photoView.setPreserveRatio(true);
        photoView.setStyle("-fx-border-color: #999; -fx-border-radius: 10;");

        VBox photoBox = new VBox(10);
        photoBox.setAlignment(Pos.CENTER);
        photoBox.setPrefWidth(200);

        Label agregarFoto = new Label("Agregar foto");
        agregarFoto.setFont(Font.font(customFont.getFamily(), 14));
        agregarFoto.setTextFill(Color.web("#13315C"));
        agregarFoto.setStyle("-fx-cursor: hand;");

        photoBox.getChildren().addAll(photoView, agregarFoto);

        agregarFoto.setOnMouseClicked(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Seleccionar imagen");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File selectedFile = fileChooser.showOpenDialog(window);
            if (selectedFile != null) {
                Image image = new Image(selectedFile.toURI().toString());
                photoView.setImage(image); // Ahora sí debe reconocer photoView
            }
        });

        Button aceptarBtn = new Button("Aceptar");
        styleButton(aceptarBtn);
        aceptarBtn.setOnAction(ev -> {
            String nombres = nombresField.getText().trim();
            String apellidos = apellidosField.getText().trim();
            LocalDate fecha = fechaNacimiento.getValue();
            String genero = generoCombo.getValue();
            String curp = curpField.getText().trim();
            String telefono = telefonoField.getText().trim();
            String tipoUsuario = tipoUsuarioCombo.getValue();

            if (tipoUsuario == null || nombres.isEmpty() || apellidos.isEmpty() ||
                    fecha == null || genero == null || curp.isEmpty() || telefono.isEmpty()) {
                showAlert("Campos vacíos", "Por favor complete todos los campos.");
                return;
            }

            // Transacción: primero PACIENTE (con MATRICULA), luego USUARIO enlazado
            boolean ok = crearPacienteYUsuarioTransaccional(
                    matriculaNorm, correo, password,
                    nombres, apellidos, fecha, genero,
                    curp, telefono, tipoUsuario
            );

            if (ok) {
                window.close();
                Stage currentStage = (Stage) formContainer.getScene().getWindow();
                new MenuScreen().show(currentStage);
            } else {
                showAlert("Error", "No se pudo completar el registro.");
            }
        });

        formGrid.add(sectionTitle, 0, 0, 2, 1); // Título ocupa 2 columnas

        formGrid.add(new Label("Nombre(s)"), 0, 1);
        formGrid.add(nombresField, 0, 2);

        formGrid.add(new Label("Apellidos"), 0, 3);
        formGrid.add(apellidosField, 0, 4);

        formGrid.add(new Label("Fecha de nacimiento"), 0, 5);
        formGrid.add(fechaNacimiento, 0, 6);

        formGrid.add(new Label("Género"), 0, 7);
        formGrid.add(generoCombo, 0, 8);

        formGrid.add(new Label("CURP"), 0, 9);
        formGrid.add(curpField, 0, 10);

        formGrid.add(new Label("Teléfono"), 0, 11);
        formGrid.add(telefonoField, 0, 12);

        formGrid.add(new Label("Tipo de usuario"), 0, 13);
        formGrid.add(tipoUsuarioCombo, 0, 14);

        VBox leftForm = new VBox(30, formGrid, aceptarBtn);
        leftForm.setAlignment(Pos.TOP_CENTER);
        leftForm.setPrefWidth(400);

        HBox mainContent = new HBox(40, leftForm, photoBox);
        mainContent.setPadding(new Insets(40));
        mainContent.setAlignment(Pos.CENTER);

        BorderPane root = new BorderPane();
        root.setCenter(mainContent);
        root.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(root, 800, 650);
        window.setScene(scene);
        window.setMinWidth(800);
        window.setMinHeight(650);
        window.centerOnScreen();       // Centra inicialmente
        window.setResizable(true);     // Permitimos redimensionar

        // Centramos de nuevo al maximizar
        window.maximizedProperty().addListener((obs, wasMaximized, isNowMaximized) -> {
            if (isNowMaximized) {
                window.centerOnScreen();
            }
        });

        window.showAndWait();
    }

    private boolean validarCredenciales(String correo, String contrasena) {
        String sql = "SELECT * FROM Usuario WHERE correo = ? AND contrasena = ?";
        try (Connection conn = OracleWalletConnector.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, correo);
            stmt.setString(2, contrasena);

            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // Si encuentra una fila, el usuario es válido
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error de BD", "No se pudo verificar las credenciales: " + e.getMessage());
            return false;
        }
    }

    private boolean correoYaRegistrado(String correo) {
        String sql = "SELECT 1 FROM Usuario WHERE correo = ?";
        try (Connection conn = OracleWalletConnector.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, correo);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // retorna true si existe
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo verificar el correo: " + e.getMessage());
            return true; // prevenir inserciones si hay error
        }

    }


    private boolean correoExiste(String correo) {
        String sql = "SELECT 1 FROM Usuario WHERE correo = ?";
        try (Connection conn = OracleWalletConnector.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, correo);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // existe si devuelve alguna fila
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Error al verificar el correo: " + e.getMessage());
            return false;
        }
    }

    private void pantallaDeCarga() {
        Stage stage = new Stage();

        // Mensaje de bienvenida
        Label mensaje = new Label("¡Bienvenido al sistema!\nEn un momento te redirigiremos");
        mensaje.setFont(Font.font("System", FontWeight.BOLD, 20));
        mensaje.setTextAlignment(TextAlignment.CENTER);
        mensaje.setStyle("-fx-text-fill: #0C3C78;"); // Azul oscuro

        // Barra de progreso personalizada
        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(250);
        progressBar.setStyle("-fx-accent: #0C3C78;"); // Azul oscuro

        VBox contenedorInterno = new VBox(20, mensaje, progressBar);
        contenedorInterno.setPadding(new Insets(40));
        contenedorInterno.setAlignment(Pos.CENTER);
        contenedorInterno.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #B0B0B0;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 0);"
        );

        StackPane root = new StackPane(contenedorInterno);
        root.setStyle("-fx-background-color: #FBFCFA;"); // Fondo blanco grisáceo

        Scene escenaTemporal = new Scene(root, 800, 500);
        stage.setScene(escenaTemporal);
        stage.setTitle("Cargando...");
        stage.show();

        // Espera antes de continuar
        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(event -> {
            stage.close();
            showNextScreen(); // Lógica tuya de navegación
        });
        delay.play();
    }

    private boolean actualizarDatosPersonales(String correo, String nombres, String apellidos, LocalDate fecha, String genero, String curp, String telefono, String tipoUsuario){
        String sql = "UPDATE Usuario SET nombres = ?, apellidos = ?, fecha_nacimiento = ?, genero = ?, curp = ?, telefono = ?, tipo_usuario = ? WHERE correo = ?";
        try (Connection conn = OracleWalletConnector.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombres);
            stmt.setString(2, apellidos);
            stmt.setDate(3, java.sql.Date.valueOf(fecha));
            stmt.setString(4, genero);
            stmt.setString(5, curp);
            stmt.setString(6, telefono);
            stmt.setString(7, tipoUsuario);
            stmt.setString(8, correo);


            int filas = stmt.executeUpdate();
            return filas > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error de BD", "No se pudieron guardar los datos personales: " + e.getMessage());
            return false;
        }
    }


    private boolean existePacientePorMatricula(String matriculaNorm) {
        final String sql = "SELECT 1 FROM PACIENTE WHERE ID_PACIENTE = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, matriculaNorm);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            showAlert("Error de BD", "No se pudo verificar la matrícula: " + e.getMessage());
            return true; // bloquea si hay error
        }
    }

    // Inserta PACIENTE con ID_PACIENTE = matrícula (PK). CORREO es el institucional.
    private boolean insertarPacienteSiNoExiste(
            String matriculaNorm, String nombres, String apellidos,
            LocalDate fecha, String genero, String curp, String telefono,
            String correo, String tipoUsuario
    ) {
        if (existePacientePorMatricula(matriculaNorm)) {
            showAlert("Matrícula duplicada", "La matrícula ya está registrada: " + matriculaNorm);
            return false;
        }

        final String sql = "INSERT INTO PACIENTE " +
                "(ID_PACIENTE, NOMBRE, APELLIDOS, SEXO, FECHA_NACIMIENTO, CORREO, TELEFONO, DIRECCION, CURP, TIPO_USUARIO) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, matriculaNorm); // PK
            ps.setString(2, nombres);
            ps.setString(3, apellidos);
            ps.setString(4, genero);
            if (fecha != null) ps.setDate(5, java.sql.Date.valueOf(fecha));
            else ps.setNull(5, Types.DATE);
            ps.setString(6, correo);
            ps.setString(7, telefono);
            ps.setString(8, null);  // DIRECCION (si no la pides aún)
            ps.setString(9, curp);
            ps.setString(10, tipoUsuario);

            ps.executeUpdate();
            return true;

        } catch (SQLException ex) {
            // ORA-00001 unique constraint (por carrera o porque ya existía)
            if (ex.getErrorCode() == 1) {
                showAlert("Matrícula duplicada", "La matrícula ya está registrada: " + matriculaNorm);
                return false;
            }
            showAlert("Error de BD", "No se pudo insertar el paciente: " + ex.getMessage());
            return false;
        }
    }

    private boolean crearPacienteYUsuarioTransaccional(
            String matriculaNorm, String correo, String password,
            String nombres, String apellidos, LocalDate fecha,
            String genero, String curp, String telefono, String tipoUsuario
    ) {
        String insertPaciente =
                "INSERT INTO ADMIN.PACIENTE " +
                        "(ID_PACIENTE, NOMBRE, APELLIDOS, SEXO, FECHA_NACIMIENTO, CORREO, TELEFONO, DIRECCION, CURP, TIPO_USUARIO, MATRICULA) " +
                        "VALUES (ADMIN.PACIENTE_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String selectPacienteId = "SELECT ADMIN.PACIENTE_SEQ.CURRVAL AS ID_PACIENTE FROM dual";

        String correoInstitucional = matriculaNorm + "@utez.edu.mx";
        System.out.println(correoInstitucional);

        String insertUsuario =
                "INSERT INTO ADMIN.USUARIO " +
                        "(ID_USUARIO, CORREO, CONTRASENA, ROL, ID_REFERENCIA, TIPO_USUARIO) " +
                        "VALUES (ADMIN.USUARIO_SEQ.NEXTVAL, ?, ?, ?, ?, ?)";


        Connection conn = null;

        try {
            conn = OracleWalletConnector.getConnection();
            conn.setAutoCommit(false);

            // 1) Insert PACIENTE
            try (PreparedStatement psP = conn.prepareStatement(insertPaciente)) {
                psP.setString(1, nombres);
                psP.setString(2, apellidos);
                psP.setString(3, genero);
                if (fecha != null) psP.setDate(4, java.sql.Date.valueOf(fecha));
                else psP.setNull(4, Types.DATE);
                psP.setString(5, correo);
                psP.setString(6, telefono);
                psP.setString(7, null);       // DIRECCION (si aún no la manejas)
                psP.setString(8, curp);
                psP.setString(9, tipoUsuario);
                psP.setString(10, matriculaNorm); // <-- MATRÍCULA AQUÍ
                psP.executeUpdate();
            }

            // 2) Obtener ID_PACIENTE recién insertado
            long idPaciente;
            try (PreparedStatement psId = conn.prepareStatement(selectPacienteId);
                 ResultSet rs = psId.executeQuery()) {
                if (!rs.next()) throw new SQLException("No se pudo obtener ID_PACIENTE.");
                idPaciente = rs.getLong("ID_PACIENTE");
            }

            // 3) Insert USUARIO enlazado
            try (PreparedStatement psU = conn.prepareStatement(insertUsuario)) {
                psU.setString(1, correoInstitucional);
                psU.setString(2, password);
                psU.setString(3, "paciente");
                psU.setLong(4, idPaciente);      // ID_REFERENCIA -> PACIENTE
                psU.setString(5, tipoUsuario);
                psU.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignore) {}
            // Mensajes amigables por errores típicos
            if (e.getErrorCode() == 1) { // ORA-00001 unique constraint
                showAlert("Duplicado", "El correo o la matrícula ya están registrados.");
            } else {
                showAlert("Error de BD", e.getMessage());
            }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignore) {}
        }
    }


}