package app;

public class Sesion {

    private static String correo;
    private static String matricula;
    private static String nombreCompleto;
    private static String tipoUsuario;

    public static void setCorreo(String correoRecuperacion) {
        correo = correoRecuperacion;
    }

    public static String getCorreo() {
        return correo;
    }

    public static void setMatricula(String mat) {
        matricula = mat;
    }

    public static String getMatricula() {
        return matricula;
    }

    public static void setNombreCompleto(String nombre) {
        nombreCompleto = nombre;
    }

    public static String getNombreCompleto() {
        return nombreCompleto;
    }

    public static void setTipoUsuario(String tipo) {
        tipoUsuario = tipo;
    }

    public static String getTipoUsuario() {
        return tipoUsuario;
    }

    public static void cerrarSesion() {
        correo = null;
        matricula = null;
        nombreCompleto = null;
        tipoUsuario = null;
    }
}
