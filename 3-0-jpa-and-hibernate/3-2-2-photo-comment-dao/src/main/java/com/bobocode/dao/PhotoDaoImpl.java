package com.bobocode.dao;

import com.bobocode.model.Photo;
import com.bobocode.model.PhotoComment;
import com.bobocode.util.ExerciseNotCompletedException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

/**
 * Please note that you should not use auto-commit mode for your implementation.
 */
public class PhotoDaoImpl implements PhotoDao {
    private final EntityManagerFactory entityManagerFactory;

    public PhotoDaoImpl(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void save(Photo photo) {
        doInTransaction(em -> {
            em.persist(photo);
            return null;
        });
    }

    @Override
    public Photo findById(long id) {
        return doInTransaction(em -> em.find(Photo.class, id)
        );
    }

    @Override
    public List<Photo> findAll() {
        return doInTransaction(em ->
                em.createQuery("select p from Photo p", Photo.class)
                        .setHint("org.hibernate.readOnly", true)
                        .getResultList()
        );
    }

    @Override
    public void remove(Photo photo) {
        doInTransaction(em -> {
            Photo managed = em.merge(photo);
            em.remove(managed);
            return null;
        });
    }

    @Override
    public void addComment(long photoId, String comment) {
        doInTransaction(em -> {
            Photo reference = em.getReference(Photo.class, photoId);
            PhotoComment photoComment = new PhotoComment();
            photoComment.setText(comment);
            photoComment.setPhoto(reference);
            photoComment.setCreatedOn(LocalDateTime.now());
            em.persist(photoComment);
            return null;
        });
    }

    private <T> T doInTransaction(Function<EntityManager, T> function) {
        EntityManager em = null;
        try {
            em = entityManagerFactory.createEntityManager();
            em.getTransaction().begin();
            T res = function.apply(em);
            em.getTransaction().commit();
            return res;
        } catch (Exception e) {
            if (em != null) em.getTransaction().rollback();
            throw new RuntimeException(e);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}
