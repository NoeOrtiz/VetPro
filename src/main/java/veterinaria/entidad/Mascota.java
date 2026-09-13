package veterinaria.entidad;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "mascota")
public class Mascota implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idMascota")
    private Integer idMascota;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "sexo")
    private String sexo;

    @Column(name = "raza")
    private String raza;

    @Column(name = "especie")
    private String especie;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idCliente", referencedColumnName = "idCliente")
    private Cliente cliente;

    @Column(name = "castrado")
    private String castrado;

    @Column(name = "fechaNacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "tamano")
    private String tamano;

    @Column(name = "peso")
    private String peso;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    public Mascota() {
    }

    public Mascota(Integer idMascota, String nombre, String sexo, String raza, String especie, Cliente cliente, String castrado, LocalDate fechaNacimiento, String tamaño, String peso) {
        this.idMascota = idMascota;
        this.nombre = nombre;
        this.sexo = sexo;
        this.raza = raza;
        this.especie = especie;
        this.cliente = cliente;
        this.castrado = castrado;
        this.fechaNacimiento = fechaNacimiento;
        this.tamano = tamano;
        this.peso = peso;
    }

    public Integer getIdMascota() {
        return idMascota;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public String getRaza() {
        return raza;
    }

    public void setRaza(String raza) {
        this.raza = raza;
    }

    public String getEspecie() {
        return especie;
    }

    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getCastrado() {
        return castrado;
    }

    public void setCastrado(String castrado) {
        this.castrado = castrado;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public String getTamano() {
        return tamano;
    }

    public void setTamano(String tamano) {
        this.tamano = tamano;
    }

    public String getPeso() {
        return peso;
    }

    public void setPeso(String peso) {
        this.peso = peso;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

}
