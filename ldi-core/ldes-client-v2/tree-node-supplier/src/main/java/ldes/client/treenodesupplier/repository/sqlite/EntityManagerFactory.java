package ldes.client.treenodesupplier.repository.sqlite;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class EntityManagerFactory {

	private static EntityManagerFactory instance = null;
	private EntityManager em;
	private javax.persistence.EntityManagerFactory emf;

	private EntityManagerFactory() {
		emf = Persistence.createEntityManagerFactory("pu-sqlite-jpa");
		em = emf.createEntityManager();
	}

	public static synchronized EntityManagerFactory getInstance() {
		if (instance == null) {
			instance = new EntityManagerFactory();
		}

		return instance;
	}

	public EntityManager getEntityManager() {
		return em;
	}

	public void destroyState() {
		try {
			em.close();
			emf.close();
			Files.delete(Path.of("database.db"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
