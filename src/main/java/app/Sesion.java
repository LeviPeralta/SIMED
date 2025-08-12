package app;

public class Sesion {

    private static String correo;
    private static String matricula;        // paciente
    private static String nombreCompleto;   // paciente o médico
    private static String tipoUsuario;      // "paciente" | "medico"
    private static String doctorId;         // médico (VARCHAR2, mismo que MEDICOS.ID_MEDICO)

    // --- Compat alias: mantener código existente que usa "Usuario" ---
    public static String getNombreUsuario() {
        return nombreCompleto;
    }

    public static void setNombreUsuario(String nombre) {
        nombreCompleto = nombre;
    }

    public static void setCorreo(String c) { correo = c; }
    public static String getCorreo() { return correo; }

    public static void setMatricula(String m) { matricula = m; }
    public static String getMatricula() { return matricula; }

    public static void setNombreCompleto(String nombre) { nombreCompleto = nombre; }
    public static String getNombreCompleto() { return nombreCompleto; }

    public static void setTipoUsuario(String tipo) { tipoUsuario = tipo; }
    public static String getTipoUsuario() { return tipoUsuario; }

    // <<< NUEVO: ID del médico para agenda de doctor >>>
    public static void setDoctorId(String id) { doctorId = id; }
    public static String getDoctorId() { return doctorId; }

    public static void cerrarSesion() {
        correo = null;
        matricula = null;
        nombreCompleto = null;
        tipoUsuario = null;
        doctorId = null;   // <<< limpiar también al salir >>>
    }
}
