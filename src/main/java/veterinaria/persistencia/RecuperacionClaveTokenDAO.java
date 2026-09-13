package veterinaria.persistencia;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import veterinaria.entidad.Usuario;
import veterinaria.util.PasswordSecurityUtil;

public class RecuperacionClaveTokenDAO {

    public String generarCodigoRecuperacion(String nombreUsuario, String email, String dni, int minutosVigencia) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            ensureSchema(em);
            Usuario usuario = buscarUsuarioValido(em, nombreUsuario, email, dni);
            if (usuario == null) {
                return null;
            }

            String codigo = PasswordSecurityUtil.generateRecoveryCode(6);
            String codigoHash = PasswordSecurityUtil.hashPassword(codigo);
            Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());
            Timestamp expira = Timestamp.valueOf(LocalDateTime.now().plusMinutes(minutosVigencia));

            em.getTransaction().begin();
            em.createNativeQuery("UPDATE usuario_recuperacion_clave SET usado = 1 WHERE idUsuario = ? AND usado = 0")
                    .setParameter(1, usuario.getIdUsuario())
                    .executeUpdate();

            em.createNativeQuery("INSERT INTO usuario_recuperacion_clave (idUsuario, codigo_hash, email_destino, fecha_solicitud, fecha_expiracion, usado) VALUES (?, ?, ?, ?, ?, 0)")
                    .setParameter(1, usuario.getIdUsuario())
                    .setParameter(2, codigoHash)
                    .setParameter(3, usuario.getEmail())
                    .setParameter(4, ahora)
                    .setParameter(5, expira)
                    .executeUpdate();
            em.getTransaction().commit();
            return codigo;
        } catch (Exception ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ex.printStackTrace();
            return null;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public boolean restablecerConCodigo(String nombreUsuario, String email, String dni, String codigo, String nuevaContrasena) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            ensureSchema(em);
            Usuario usuario = buscarUsuarioValido(em, nombreUsuario, email, dni);
            if (usuario == null) {
                return false;
            }

            List<Object[]> rows = em.createNativeQuery(
                    "SELECT id, codigo_hash FROM usuario_recuperacion_clave WHERE idUsuario = ? AND usado = 0 AND fecha_expiracion >= NOW() ORDER BY id DESC")
                    .setParameter(1, usuario.getIdUsuario())
                    .setMaxResults(1)
                    .getResultList();

            if (rows == null || rows.isEmpty()) {
                return false;
            }

            Object[] row = rows.get(0);
            Number tokenId = (Number) row[0];
            String codigoHash = row[1] != null ? row[1].toString() : null;
            if (!PasswordSecurityUtil.matches(codigo, codigoHash)) {
                return false;
            }

            em.getTransaction().begin();
            Usuario usuarioManaged = em.find(Usuario.class, usuario.getIdUsuario());
            usuarioManaged.setContrasena(PasswordSecurityUtil.normalizeForPersist(nuevaContrasena));
            em.merge(usuarioManaged);
            em.createNativeQuery("UPDATE usuario_recuperacion_clave SET usado = 1 WHERE id = ?")
                    .setParameter(1, tokenId.longValue())
                    .executeUpdate();
            em.getTransaction().commit();
            return true;
        } catch (Exception ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            ex.printStackTrace();
            return false;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    private Usuario buscarUsuarioValido(EntityManager em, String nombreUsuario, String email, String dni) {
        try {
            return em.createQuery(
                    "SELECT u FROM Usuario u JOIN u.persona p WHERE u.nombreUsuario = :nombreUsuario AND LOWER(u.email) = :email AND p.dni = :dni",
                    Usuario.class)
                    .setParameter("nombreUsuario", nombreUsuario)
                    .setParameter("email", email == null ? null : email.trim().toLowerCase())
                    .setParameter("dni", dni)
                    .getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    private void ensureSchema(EntityManager em) {
        boolean ownTx = !em.getTransaction().isActive();
        try {
            if (ownTx) {
                em.getTransaction().begin();
            }
            em.createNativeQuery(
                    "CREATE TABLE IF NOT EXISTS usuario_recuperacion_clave ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT,"
                    + "idUsuario INT NOT NULL,"
                    + "codigo_hash VARCHAR(255) NOT NULL,"
                    + "email_destino VARCHAR(255) NULL,"
                    + "fecha_solicitud DATETIME NOT NULL,"
                    + "fecha_expiracion DATETIME NOT NULL,"
                    + "usado TINYINT(1) NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY (id),"
                    + "INDEX idx_usuario_recuperacion_usuario (idUsuario),"
                    + "CONSTRAINT fk_usuario_recuperacion_usuario FOREIGN KEY (idUsuario) REFERENCES usuario (idUsuario) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
                    .executeUpdate();
            if (ownTx) {
                em.getTransaction().commit();
            }
        } catch (Exception ex) {
            if (ownTx && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        }
    }
}
