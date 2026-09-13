
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

@Entity
@Table(name = "proveedor")
public class Proveedor implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idProveedor")
    private Integer idProveedor;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idPersona", referencedColumnName = "idPersona")    
    private Persona persona;
    
    @Column(name = "rubro")
    private String rubro;
    
    @Column(name = "email")
    private String email;  
    
    @Column(name = "estado")
    private String estado;

    @Column(name = "razonSocial")
    private String razonSocial;
    
    @Column(name = "cuit")
    private String cuit;
    
    public Proveedor() {
    }

    public Proveedor(Integer idProveedor, Persona persona, String rubro, String email, String estado, String razonSocial, String cuit) {
        this.idProveedor = idProveedor;
        this.persona = persona;
        this.rubro = rubro;
        this.email = email;
        this.estado = estado;
        this.razonSocial = razonSocial;
        this.cuit = cuit;
    }

    public Integer getIdProveedor() {
        return idProveedor;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }

    public String getRubro() {
        return rubro;
    }

    public void setRubro(String rubro) {
        this.rubro = rubro;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
    
    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }
}
