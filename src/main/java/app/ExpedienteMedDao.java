package app;

import org.example.OracleWalletConnector;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * Data Access Object (DAO) para la entidad Expediente.
 * VERSIÓN CORREGIDA: Utiliza el nombre de tabla 'EXPEDIENTE_MEDICO' y maneja la columna UPDATED_AT.
 */
public class ExpedienteMedDao {

    /**
     * Verifica si ya existe un expediente asociado a un ID de cita.
     */
    public boolean existsByCita(long idCita) {
        // CORREGIDO: Nombre de la tabla
        String sql = "SELECT COUNT(*) FROM ADMIN.EXPEDIENTE_MEDICO WHERE ID_CITA = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public ExpedienteMed getExpedienteByCitaId(long idCita) {
        String sql = "SELECT * FROM ADMIN.EXPEDIENTE_MEDICO WHERE ID_CITA = ?";
        try (Connection conn = OracleWalletConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, idCita);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // -- INICIO DE LA CORRECCIÓN A PRUEBA DE BALAS --

                // Conversión segura para Long (el nuevo sospechoso)
                BigDecimal idCitaBigDecimal = rs.getBigDecimal("ID_CITA");
                Long idCitaConvertido = (idCitaBigDecimal == null) ? null : idCitaBigDecimal.longValue();

                // Conversión segura para Integer
                BigDecimal frBigDecimal = rs.getBigDecimal("FRECUENCIA_RESP");
                Integer frecuenciaResp = (frBigDecimal == null) ? null : frBigDecimal.intValue();

                // Conversiones seguras para Doubles
                BigDecimal pesoBigDecimal = rs.getBigDecimal("PESO_KG");
                Double pesoKg = (pesoBigDecimal == null) ? null : pesoBigDecimal.doubleValue();

                BigDecimal tempBigDecimal = rs.getBigDecimal("TEMPERATURA_C");
                Double temperaturaC = (tempBigDecimal == null) ? null : tempBigDecimal.doubleValue();

                BigDecimal imcBigDecimal = rs.getBigDecimal("IMC");
                Double imc = (imcBigDecimal == null) ? null : imcBigDecimal.doubleValue();

                // Usamos todas las variables convertidas de forma segura
                return new ExpedienteMed(
                        rs.getLong("ID_EXPEDIENTE"),
                        idCitaConvertido, // Variable corregida
                        rs.getLong("ID_PACIENTE"),
                        rs.getString("ID_MEDICO"),
                        rs.getTimestamp("FECHA_CONSULTA") != null ? rs.getTimestamp("FECHA_CONSULTA").toLocalDateTime() : null,
                        rs.getString("DIAGNOSTICO"),
                        rs.getString("TRATAMIENTO"),
                        rs.getString("OBSERVACIONES"),
                        pesoKg,
                        frecuenciaResp,
                        temperaturaC,
                        imc,
                        rs.getString("EXAMENES_COMP")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean guardar(ExpedienteMed exp) {
        // CORREGIDO: Nombre de la tabla. No incluimos CREATED_AT ni UPDATED_AT, la BD se encarga.
        String sql = "INSERT INTO ADMIN.EXPEDIENTE_MEDICO (ID_CITA, ID_PACIENTE, ID_MEDICO, FECHA_CONSULTA, DIAGNOSTICO, TRATAMIENTO, OBSERVACIONES, PESO_KG, FRECUENCIA_RESP, TEMPERATURA_C, IMC, EXAMENES_COMP) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setObject(1, exp.getIdCita());
            ps.setObject(2, exp.getIdPaciente());
            ps.setString(3, exp.getIdMedico());
            ps.setTimestamp(4, exp.getFechaConsulta() != null ? Timestamp.valueOf(exp.getFechaConsulta()) : null);
            ps.setString(5, exp.getDiagnostico());
            ps.setString(6, exp.getTratamiento());
            ps.setString(7, exp.getObservaciones());
            ps.setObject(8, exp.getPesoKg());
            ps.setObject(9, exp.getFrecuenciaResp());
            ps.setObject(10, exp.getTemperaturaC());
            ps.setObject(11, exp.getImc());
            ps.setString(12, exp.getExamenesComp());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Actualiza un expediente existente.
     * Modifica la columna UPDATED_AT a la fecha y hora actual del servidor de base de datos.
     */
    public boolean update(ExpedienteMed exp) {
        // CORREGIDO: Nombre de tabla y se añade la actualización de UPDATED_AT
        String sql = "UPDATE ADMIN.EXPEDIENTE_MEDICO SET " +
                "DIAGNOSTICO = ?, TRATAMIENTO = ?, OBSERVACIONES = ?, PESO_KG = ?, " +
                "FRECUENCIA_RESP = ?, TEMPERATURA_C = ?, IMC = ?, EXAMENES_COMP = ?, " +
                "UPDATED_AT = CURRENT_TIMESTAMP " + // Actualiza la fecha de modificación
                "WHERE ID_EXPEDIENTE = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, exp.getDiagnostico());
            ps.setString(2, exp.getTratamiento());
            ps.setString(3, exp.getObservaciones());
            ps.setObject(4, exp.getPesoKg());
            ps.setObject(5, exp.getFrecuenciaResp());
            ps.setObject(6, exp.getTemperaturaC());
            ps.setObject(7, exp.getImc());
            ps.setString(8, exp.getExamenesComp());
            ps.setLong(9, exp.getIdExpediente()); // El ID para el WHERE

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}