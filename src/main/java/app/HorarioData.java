package app;

import org.example.OracleWalletConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HorarioData {
    public static List<HorarioMedico> getHorariosOcupados(String idMedico) {
        List<HorarioMedico> horarios = new ArrayList<>();

        String sql = "SELECT DIA_SEMANA, TO_CHAR(HORA_INICIO, 'HH24:MI') AS HORA_INICIO " +
                "FROM HORARIO_MEDICO WHERE ID_MEDICO = ?";

        try (Connection conn = OracleWalletConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idMedico);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String dia = rs.getString("DIA_SEMANA");
                String hora = rs.getString("HORA_INICIO");
                horarios.add(new HorarioMedico(dia, hora));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return horarios;
    }
}
