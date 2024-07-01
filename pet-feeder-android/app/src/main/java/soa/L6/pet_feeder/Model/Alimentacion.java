package soa.L6.pet_feeder.Model;
public class Alimentacion implements Comparable<Alimentacion> {
    private String horario;
    private double cantComida;

    public Alimentacion(String horario, double cantComida) {
        this.horario = horario;
        this.cantComida = cantComida;
    }
    public Alimentacion() {
    }

    public String getHorario() {
        return horario;
    }

    public double getCantComida() {
        return cantComida;
    }

    @Override
    public int compareTo(Alimentacion otra) {
        // Convertimos los horarios a LocalTime para poder compararlos
        java.time.LocalTime thisTime = java.time.LocalTime.parse(this.horario);
        java.time.LocalTime otraTime = java.time.LocalTime.parse(otra.getHorario());

        return thisTime.compareTo(otraTime);
    }

    @Override
    public String toString() {
        return "Alimentacion{" +
                "horario='" + horario + '\'' +
                ", cantComida=" + cantComida +
                '}';
    }
}
