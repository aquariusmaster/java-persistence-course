package com.bobocode.dao;

import com.bobocode.model.Company;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class CompanyDaoImpl implements CompanyDao {
    private EntityManagerFactory entityManagerFactory;

    public CompanyDaoImpl(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public Company findByIdFetchProducts(Long id) {
        EntityManager em = null;
        try {
            em = entityManagerFactory.createEntityManager();
            return em.createQuery(
                    "select c from Company c " +
                            "join fetch c.products " +
                            "where c.id = :company_id", Company.class)
                    .setParameter("company_id", id)
                    .setHint("org.hibernate.readOnly", true)
                    .getSingleResult();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }
}
