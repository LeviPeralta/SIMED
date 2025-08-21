package app;

import org.example.OracleWalletConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorData {

    public static List<Doctor> getDoctoresPorEspecialidad(String especialidad) {
        List<Doctor> lista = new ArrayList<>();

        String sql = """
    SELECT m.ID_MEDICO, m.NOMBRE, m.APELLIDOS, e.ID_ESPECIALIDAD
    FROM MEDICOS m
    JOIN ESPECIALIDADES e ON m.ID_ESPECIALIDAD = e.ID_ESPECIALIDAD
    WHERE e.NOMBRE = ?
""";


        try (Connection conn = OracleWalletConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, especialidad);
            ResultSet rs = stmt.executeQuery();

            int baseOffset = obtenerOffsetPorEspecialidad(especialidad); // 🆕 desplazamiento base
            int index = 0;

            while (rs.next()) {
                String id = rs.getString("ID_MEDICO");
                String nombreCompleto = rs.getString("NOMBRE") + " " + rs.getString("APELLIDOS");
                String horario = "Lunes - Viernes\n09:00 - 14:00";
                String imagen = "Doctor" + (baseOffset + index) + ".jpg";

                // Agrega la especialidad al constructor
                lista.add(new Doctor(id, nombreCompleto, horario, imagen, especialidad));
                index++;
            }



        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lista;
    }

    private static int obtenerOffsetPorEspecialidad(String especialidad) {
        return switch (especialidad) {
            case "Medicina General" -> 1;
            case "Cardiología"      -> 16;
            case "Neurología"       -> 31;
            case "Ginecología"      -> 46;
            case "Urología"         -> 61;
            case "Traumatología"    -> 75;
            default -> 100; // imágenes genéricas si no se reconoce
        };
    }

}
