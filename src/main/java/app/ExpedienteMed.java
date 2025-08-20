package app;

import java.time.LocalDateTime;

/**
 * Clase Modelo (corregida) que representa la entidad Expediente Médico.
 * Se utiliza encapsulamiento (campos privados con getters y setters).
 */
public class ExpedienteMed {

    // --- Atributos privados ---
    private Long   idExpediente;
    private Long   idCita;
    private Long   idPaciente;
    private String idMedico;
    private LocalDateTime fechaConsulta;
    private String diagnostico;
    private String tratamiento;
    private String observaciones;
    private Double pesoKg;
    private Integer frecuenciaResp;
    private Double temperaturaC;
    private Double imc;
    private String examenesComp;

    // --- Constructores ---
    /**
     * Constructor vacío.
     */
    public ExpedienteMed() {
    }

    /**
     * Constructor con todos los parámetros para crear un objeto fácilmente desde la base de datos.
     */
    public ExpedienteMed(Long idExpediente, Long idCita, Long idPaciente, String idMedico, LocalDateTime fechaConsulta, String diagnostico, String tratamiento, String observaciones, Double pesoKg, Integer frecuenciaResp, Double temperaturaC, Double imc, String examenesComp) {
        this.idExpediente = idExpediente;
        this.idCita = idCita;
        this.idPaciente = idPaciente;
        this.idMedico = idMedico;
        this.fechaConsulta = fechaConsulta;
        this.diagnostico = diagnostico;
        this.tratamiento = tratamiento;
        this.observaciones = observaciones;
        this.pesoKg = pesoKg;
        this.frecuenciaResp = frecuenciaResp;
        this.temperaturaC = temperaturaC;
        this.imc = imc;
        this.examenesComp = examenesComp;
    }


    // --- Getters y Setters ---

    public Long getIdExpediente() {
        return idExpediente;
    }

    public void setIdExpediente(Long idExpediente) {
        this.idExpediente = idExpediente;
    }

    public Long getIdCita() {
        return idCita;
    }

    public void setIdCita(Long idCita) {
        this.idCita = idCita;
    }

    public Long getIdPaciente() {
        return idPaciente;
    }

    public void setIdPaciente(Long idPaciente) {
        this.idPaciente = idPaciente;
    }

    public String getIdMedico() {
        return idMedico;
    }

    public void setIdMedico(String idMedico) {
        this.idMedico = idMedico;
    }

    public LocalDateTime getFechaConsulta() {
        return fechaConsulta;
    }

    public void setFechaConsulta(LocalDateTime fechaConsulta) {
        this.fechaConsulta = fechaConsulta;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public Integer getFrecuenciaResp() {
        return frecuenciaResp;
    }

    public void setFrecuenciaResp(Integer frecuenciaResp) {
        this.frecuenciaResp = frecuenciaResp;
    }

    public Double getTemperaturaC() {
        return temperaturaC;
    }

    public void setTemperaturaC(Double temperaturaC) {
        this.temperaturaC = temperaturaC;
    }

    public Double getImc() {
        return imc;
    }

    public void setImc(Double imc) {
        this.imc = imc;
    }

    public String getExamenesComp() {
        return examenesComp;
    }

    public void setExamenesComp(String examenesComp) {
        this.examenesComp = examenesComp;
    }
}