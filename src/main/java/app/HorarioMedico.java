package app;

public class HorarioMedico {
    private String diaSemana;
    private String horaInicio;

    public HorarioMedico(String diaSemana, String horaInicio) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public String getHoraInicio() {
        return horaInicio;
    }
}


