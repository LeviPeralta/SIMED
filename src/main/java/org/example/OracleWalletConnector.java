package org.example;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class OracleWalletConnector {

    private static final String PROPERTIES_PATH = "/wallet/ojdbc.properties";

    public static Connection getConnection() throws SQLException {
        try {
            // Establece la ruta del wallet desde el classpath
            Path walletPath = Paths.get(OracleWalletConnector.class.getClassLoader().getResource("wallet").toURI());

            // Configura propiedades requeridas por Oracle
            System.setProperty("oracle.net.tns_admin", walletPath.toString());
            System.setProperty("oracle.net.ssl_server_dn_match", "true");
            System.setProperty("oracle.net.ssl_version", "1.2");

            System.out.println("📂 Wallet ubicado en: " + walletPath);

            // URL basada en alias definido en tnsnames.ora
            String jdbcUrl = "jdbc:oracle:thin:@w3yxl6sur0noq280_high";
            String user = "ADMIN"; // Asegúrate de usar mayúsculas
            String pass = "levi@Peralta09"; // o el que tengas configurado

            // Carga explícita del driver (opcional si ya lo haces por ojdbc8.jar)
            Class.forName("oracle.jdbc.OracleDriver");

            // Retorna la conexión
            return DriverManager.getConnection(jdbcUrl, user, pass);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Error al establecer la conexión a Oracle: " + e.getMessage(), e);
        }
    }
}

