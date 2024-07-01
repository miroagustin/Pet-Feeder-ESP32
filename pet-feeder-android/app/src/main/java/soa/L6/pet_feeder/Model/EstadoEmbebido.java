package soa.L6.pet_feeder.Model;

public class EstadoEmbebido {
    private float distancia;
    private int peso;
    private String estadoAnterior;
    private String evento;
    private String estadoActual;

    public EstadoEmbebido(float distancia, int peso, String estadoAnterior, String evento, String estadoActual) {
        this.distancia = distancia;
        this.peso = peso;
        this.estadoAnterior = estadoAnterior;
        this.evento = evento;
        this.estadoActual = estadoActual;
    }

    public float getDistancia() {
        return distancia;
    }

    public void setDistancia(int distancia) {
        this.distancia = distancia;
    }

    public int getPeso() {
        return peso;
    }

    public void setPeso(int peso) {
        this.peso = peso;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEvento() {
        return evento;
    }

    public void setEvento(String evento) {
        this.evento = evento;
    }

    public String getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(String estadoActual) {
        this.estadoActual = estadoActual;
    }

    @Override
    public String toString() {
        return "EstadoEmbebido{" +
                "distancia=" + distancia +
                ", peso=" + peso +
                ", estadoAnterior='" + estadoAnterior + '\'' +
                ", evento='" + evento + '\'' +
                ", estadoActual='" + estadoActual + '\'' +
                '}';
    }

    public static EstadoEmbebido fromString(String data) {
        String[] parts = data.split(";");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid data format");
        }
        float distancia = Float.parseFloat(parts[0]);
        int peso = Integer.parseInt(parts[1]);
        String estadoAnterior = parts[2];
        String evento = parts[3];
        String estadoActual = parts[4];

        return new EstadoEmbebido(distancia, peso, estadoAnterior, evento, estadoActual);
    }
}
