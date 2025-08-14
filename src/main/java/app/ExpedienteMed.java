package app;

import java.time.LocalDateTime;

public class ExpedienteMed {
    public Long   idExpediente;
    public Long   idCita;          // puede ser null
    public Long   idPaciente;
    public String idMedico;
    public LocalDateTime fechaConsulta;

    public String diagnostico;
    public String tratamiento;
    public String observaciones;

    public Double pesoKg;
    public Integer frecuenciaResp;
    public Double temperaturaC;
    public Double imc;
    public String examenesComp;
}
