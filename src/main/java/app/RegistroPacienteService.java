package app;

import org.example.OracleWalletConnector;

import java.sql.*;
import java.time.LocalDate;

public class RegistroPacienteService {

    // Normaliza SIEMPRE la matrícula antes de tocar la BD
    private static String normalizeMatricula(String raw) {
        if (raw == null) return null;
        // quita espacios al inicio/fin e intermedios, y mayúsculas
        return raw.trim().replaceAll("\\s+", "").toUpperCase();
    }

    // Crea el correo institucional desde la matrícula
    private static String buildCorreo(String matriculaNorm) {
        return matriculaNorm.toLowerCase() + "@utez.edu.mx";
    }

    /** Devuelve true si ya existe un paciente con esa matrícula (ID_PACIENTE) */
    public static boolean existeMatricula(String matriculaRaw) throws SQLException {
        String matricula = normalizeMatricula(matriculaRaw);
        final String sql = "SELECT 1 FROM PACIENTE WHERE ID_PACIENTE = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, matricula);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** (Opcional) valida correo duplicado si tu tabla tiene UNIQUE en CORREO */
    public static boolean existeCorreo(String correo) throws SQLException {
        final String sql = "SELECT 1 FROM PACIENTE WHERE LOWER(CORREO) = LOWER(?)";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Registra un paciente. Lanza IllegalArgumentException con mensaje claro si ya existe matrícula/correo.
     * Campos opcionales pueden ir null.
     */
    public static void registrarPaciente(
            String matriculaRaw, String nombre, String apellidos,
            String sexo, LocalDate fechaNacimiento,
            String telefono, String direccion, String curp, String tipoUsuario
    ) throws SQLException {

        String matricula = normalizeMatricula(matriculaRaw);
        if (matricula == null || matricula.isEmpty())
            throw new IllegalArgumentException("La matrícula es obligatoria.");

        String correo = buildCorreo(matricula);

        // 1) Duplicados explícitos (más amable que esperar al ORA-00001)
        if (existeMatricula(matricula)) {
            throw new IllegalArgumentException("La matrícula ya está registrada: " + matricula);
        }
        // Si tuvieras UNIQUE en correo, descomenta:
        // if (existeCorreo(correo)) {
        //     throw new IllegalArgumentException("El correo ya está registrado: " + correo);
        // }

        final String sql =
                "INSERT INTO PACIENTE " +
                        "(ID_PACIENTE, NOMBRE, APELLIDOS, SEXO, FECHA_NACIMIENTO, CORREO, TELEFONO, DIRECCION, CURP, TIPO_USUARIO) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, matricula);                         // ID_PACIENTE (PK, VARCHAR2)
            ps.setString(2, nombre);
            ps.setString(3, apellidos);
            ps.setString(4, sexo);
            if (fechaNacimiento != null) {
                ps.setDate(5, Date.valueOf(fechaNacimiento));   // DATE
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setString(6, correo);                            // correo institucional
            ps.setString(7, telefono);
            ps.setString(8, direccion);
            ps.setString(9, curp);
            ps.setString(10, tipoUsuario);

            ps.executeUpdate();
        } catch (SQLException ex) {
            // Si por carrera se escapó, traduce ORA-00001 de forma clara
            String msg = ex.getMessage();
            if (msg != null && msg.contains("ORA-00001")) {
                throw new IllegalArgumentException("La matrícula ya está registrada: " + matricula, ex);
            }
            throw ex;
        }
    }
}
