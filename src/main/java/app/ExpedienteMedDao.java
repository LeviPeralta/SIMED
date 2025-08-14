package app;

import org.example.OracleWalletConnector;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExpedienteMedDao {

    // ============ INSERT ============
    public long insert(ExpedienteMed e) throws SQLException {
        final String sql = """
            INSERT INTO ADMIN.EXPEDIENTE_MEDICO
            (ID_CITA, ID_PACIENTE, ID_MEDICO, FECHA_CONSULTA,
             DIAGNOSTICO, TRATAMIENTO, OBSERVACIONES,
             PESO_KG, FRECUENCIA_RESP, TEMPERATURA_C, IMC, EXAMENES_COMP)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, new String[]{"ID_EXPEDIENTE"})) {

            if (e.idCita == null) ps.setNull(1, Types.NUMERIC); else ps.setLong(1, e.idCita);
            ps.setLong(2, e.idPaciente);
            ps.setString(3, e.idMedico);
            ps.setTimestamp(4, Timestamp.valueOf(e.fechaConsulta));

            ps.setString(5,  e.diagnostico);
            ps.setString(6,  e.tratamiento);
            ps.setString(7,  e.observaciones);

            if (e.pesoKg == null)        ps.setNull(8,  Types.NUMERIC); else ps.setDouble(8,  e.pesoKg);
            if (e.frecuenciaResp == null)ps.setNull(9,  Types.NUMERIC); else ps.setInt(9,    e.frecuenciaResp);
            if (e.temperaturaC == null)  ps.setNull(10, Types.NUMERIC); else ps.setDouble(10, e.temperaturaC);
            if (e.imc == null)           ps.setNull(11, Types.NUMERIC); else ps.setDouble(11, e.imc);
            ps.setString(12, e.examenesComp);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("No se obtuvo ID_EXPEDIENTE generado");
    }

    // ============ UPDATE ============
    public int update(ExpedienteMed e) throws SQLException {
        if (e.idExpediente == null) throw new IllegalArgumentException("Se requiere idExpediente");
        final String sql = """
            UPDATE ADMIN.EXPEDIENTE_MEDICO
               SET ID_CITA = ?, ID_PACIENTE = ?, ID_MEDICO = ?, FECHA_CONSULTA = ?,
                   DIAGNOSTICO = ?, TRATAMIENTO = ?, OBSERVACIONES = ?,
                   PESO_KG = ?, FRECUENCIA_RESP = ?, TEMPERATURA_C = ?, IMC = ?, EXAMENES_COMP = ?
             WHERE ID_EXPEDIENTE = ?
            """;
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            if (e.idCita == null) ps.setNull(1, Types.NUMERIC); else ps.setLong(1, e.idCita);
            ps.setLong(2, e.idPaciente);
            ps.setString(3, e.idMedico);
            ps.setTimestamp(4, Timestamp.valueOf(e.fechaConsulta));

            ps.setString(5,  e.diagnostico);
            ps.setString(6,  e.tratamiento);
            ps.setString(7,  e.observaciones);

            if (e.pesoKg == null)        ps.setNull(8,  Types.NUMERIC); else ps.setDouble(8,  e.pesoKg);
            if (e.frecuenciaResp == null)ps.setNull(9,  Types.NUMERIC); else ps.setInt(9,    e.frecuenciaResp);
            if (e.temperaturaC == null)  ps.setNull(10, Types.NUMERIC); else ps.setDouble(10, e.temperaturaC);
            if (e.imc == null)           ps.setNull(11, Types.NUMERIC); else ps.setDouble(11, e.imc);
            ps.setString(12, e.examenesComp);

            ps.setLong(13, e.idExpediente);
            return ps.executeUpdate();
        }
    }

    // ============ GETTERS ============
    public Optional<ExpedienteMed> getById(long idExpediente) throws SQLException {
        final String sql = baseSelect() + " WHERE ID_EXPEDIENTE = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idExpediente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<ExpedienteMed> getByCita(long idCita) throws SQLException {
        final String sql = baseSelect() + " WHERE ID_CITA = ?";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<ExpedienteMed> listByPaciente(long idPaciente) throws SQLException {
        final String sql = baseSelect() + " WHERE ID_PACIENTE = ? ORDER BY FECHA_CONSULTA DESC";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idPaciente);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExpedienteMed> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public List<ExpedienteMed> listByPacienteBetween(long idPaciente, LocalDate desde, LocalDate hasta) throws SQLException {
        final String sql = baseSelect() +
                " WHERE ID_PACIENTE = ? AND TRUNC(CAST(FECHA_CONSULTA AS DATE)) BETWEEN ? AND ? " +
                " ORDER BY FECHA_CONSULTA DESC";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idPaciente);
            ps.setDate(2, Date.valueOf(desde));
            ps.setDate(3, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                List<ExpedienteMed> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    // ============ DELETE opcional ============
    public int delete(long idExpediente) throws SQLException {
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ADMIN.EXPEDIENTE_MEDICO WHERE ID_EXPEDIENTE = ?")) {
            ps.setLong(1, idExpediente);
            return ps.executeUpdate();
        }
    }

    // Helpers
    private String baseSelect() {
        return """
            SELECT ID_EXPEDIENTE, ID_CITA, ID_PACIENTE, ID_MEDICO, FECHA_CONSULTA,
                   DIAGNOSTICO, TRATAMIENTO, OBSERVACIONES,
                   PESO_KG, FRECUENCIA_RESP, TEMPERATURA_C, IMC, EXAMENES_COMP
              FROM ADMIN.EXPEDIENTE_MEDICO
            """;
    }

    private ExpedienteMed map(ResultSet rs) throws SQLException {
        ExpedienteMed e = new ExpedienteMed();
        e.idExpediente   = rs.getLong("ID_EXPEDIENTE");
        e.idCita         = rs.getObject("ID_CITA") == null ? null : rs.getLong("ID_CITA");
        e.idPaciente     = rs.getLong("ID_PACIENTE");
        e.idMedico       = rs.getString("ID_MEDICO");
        Timestamp ts     = rs.getTimestamp("FECHA_CONSULTA");
        e.fechaConsulta  = (ts == null) ? null : ts.toLocalDateTime();

        e.diagnostico    = rs.getString("DIAGNOSTICO");
        e.tratamiento    = rs.getString("TRATAMIENTO");
        e.observaciones  = rs.getString("OBSERVACIONES");

        e.pesoKg         = rs.getObject("PESO_KG") == null ? null : rs.getDouble("PESO_KG");
        e.frecuenciaResp = rs.getObject("FRECUENCIA_RESP") == null ? null : rs.getInt("FRECUENCIA_RESP");
        e.temperaturaC   = rs.getObject("TEMPERATURA_C") == null ? null : rs.getDouble("TEMPERATURA_C");
        e.imc            = rs.getObject("IMC") == null ? null : rs.getDouble("IMC");
        e.examenesComp   = rs.getString("EXAMENES_COMP");
        return e;
    }

    public boolean existsByCita(long idCita) throws SQLException {
        final String sql = "SELECT 1 FROM ADMIN.EXPEDIENTE_MEDICO WHERE ID_CITA = ? FETCH FIRST 1 ROWS ONLY";
        try (Connection con = OracleWalletConnector.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
