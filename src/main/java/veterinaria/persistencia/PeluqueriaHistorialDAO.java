package veterinaria.persistencia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import veterinaria.entidad.PeluqueriaHistorial;

public class PeluqueriaHistorialDAO {

    public Map<Integer, PeluqueriaHistorial> obtenerUltimoHistorialPorTurnos(List<Integer> idsTurno) {
        Map<Integer, PeluqueriaHistorial> out = new HashMap<>();
        if (idsTurno == null || idsTurno.isEmpty()) {
            return out;
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<PeluqueriaHistorial> q = em.createQuery(
                    "SELECT h FROM PeluqueriaHistorial h "
                    + " JOIN FETCH h.turno t "
                    + " LEFT JOIN FETCH h.usuario u "
                    + " LEFT JOIN FETCH u.persona up "
                    + " WHERE t.idTurno IN :ids "
                    + " ORDER BY t.idTurno ASC, h.fechaEvento DESC, h.idHistorial DESC",
                    PeluqueriaHistorial.class
            );
            q.setParameter("ids", idsTurno);
            List<PeluqueriaHistorial> all = q.getResultList();

            for (PeluqueriaHistorial h : all) {
                if (h == null || h.getTurno() == null || h.getTurno().getIdTurno() == null) {
                    continue;
                }
                Integer idTurno = h.getTurno().getIdTurno();
                out.putIfAbsent(idTurno, h);
            }
            return out;
        } finally {
            em.close();
        }
    }
}
