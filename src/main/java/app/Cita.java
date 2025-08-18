package app;

import java.time.LocalDateTime;

public class Cita {
    private int id;
    private int idPaciente;
    private String idDoctor;            // <-- String
    private LocalDateTime fechaHora;
    private String nombrePaciente;
    private String nombreDoctor;
    private String especialidad;
    private String consultorio;
    private String matricula;

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getIdPaciente() { return idPaciente; }
    public void setIdPaciente(int idPaciente) { this.idPaciente = idPaciente; }
    public String getIdDoctor() { return idDoctor; }             // <-- String
    public void setIdDoctor(String idDoctor) { this.idDoctor = idDoctor; } // <--
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String nombrePaciente) { this.nombrePaciente = nombrePaciente; }
    public String getNombreDoctor() { return nombreDoctor; }
    public void setNombreDoctor(String nombreDoctor) { this.nombreDoctor = nombreDoctor; }
    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    public String getConsultorio() { return consultorio; }
    public void setConsultorio(String consultorio) { this.consultorio = consultorio; }
}
