package app;

public class Doctor {
    private String id;
    private String nombre;
    private String horario;
    private String imagen;
    private String especialidad;

    public Doctor(String id, String nombre, String horario, String imagen, String especialidad) {
        this.id = id;
        this.nombre = nombre;
        this.horario = horario;
        this.imagen = imagen;
        this.especialidad = especialidad;
    }

    // Getters
    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getHorario() { return horario; }
    public String getImagen() { return imagen; }
    public String getEspecialidad() { return especialidad; }
}


