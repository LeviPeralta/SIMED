package app;

import org.example.OracleWalletConnector;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CitaDAO {

    // Esquema dueño de las tablas
    private static final String SCHEMA = "ADMIN";

    // Tablas reales en tu BD
    private static final String T_CITA     = SCHEMA + ".CITA";
    private static final String T_PACIENTE = SCHEMA + ".PACIENTE";
    private static final String T_MEDICOS  = SCHEMA + ".MEDICOS";
    
    public static List<Cita> obtenerCitasPorDoctor(String idDoctor) {
        List<Cita> out = new ArrayList<>();

        String sql = """
            SELECT  c.ID_CITA,
                    c.ID_PACIENTE,
                    c.ID_MEDICO,
                    c.FECHA_HORA,
                    (p.NOMBRE || ' ' || p.APELLIDOS) AS NOMBRE_PACIENTE,
                    (m.NOMBRE || ' ' || m.APELLIDOS) AS NOMBRE_DOCTOR,
                    NULL AS ESPECIALIDAD,                  -- ajusta si unes a tabla de especialidades
                    m.ID_CONSULTORIO AS CONSULTORIO,
                    p.MATRICULA       AS MATRICULA
            FROM %s c
            JOIN %s p ON p.ID_PACIENTE = c.ID_PACIENTE
            JOIN %s m ON m.ID_MEDICO   = c.ID_MEDICO
            WHERE c.ID_MEDICO = ?
            ORDER BY c.FECHA_HORA
        """.formatted(T_CITA, T_PACIENTE, T_MEDICOS);

        try (Connection cn = OracleWalletConnector.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, idDoctor); // ID_MEDICO es VARCHAR2 en ADMIN.CITA

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cita cita = new Cita();
                    cita.setId(rs.getInt("ID_CITA"));
                    cita.setIdPaciente(rs.getInt("ID_PACIENTE"));
                    cita.setIdDoctor(rs.getString("ID_MEDICO"));

                    Timestamp ts = rs.getTimestamp("FECHA_HORA");
                    cita.setFechaHora(ts == null ? LocalDateTime.now() : ts.toLocalDateTime());

                    cita.setNombrePaciente(rs.getString("NOMBRE_PACIENTE"));
                    cita.setNombreDoctor(rs.getString("NOMBRE_DOCTOR"));
                    cita.setEspecialidad(rs.getString("ESPECIALIDAD")); // ahora será null
                    cita.setConsultorio(rs.getString("CONSULTORIO"));
                    cita.setMatricula(rs.getString("MATRICULA"));       // <-- importante para HorarioScreen

                    out.add(cita);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return out;
    }
}
