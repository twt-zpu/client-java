/*
 * This work is part of the Productive 4.0 innovation project, which receives grants from the
 * European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 * (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 * national funding authorities from involved countries.
 */

package eu.arrowhead.common.misc;

import eu.arrowhead.common.exception.ArrowheadRuntimeException;
import eu.arrowhead.common.exception.DuplicateEntryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.Set;
import javax.persistence.PersistenceException;
import javax.ws.rs.core.Response.Status;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.Query;

//NOTE should move to EntityManager from Sessions, using the JPA criteria API (Hibernate criteria will be removed in
// Hibernate 6)
public class DatabaseManager {
  private SessionFactory sessionFactory;
  private String dbAddress;
  private String dbUser;
  private String dbPassword;

  public static DatabaseManager createFromProperties() {
    return createFromProperties(ArrowheadProperties.loadDefault());
  }

  public static DatabaseManager createFromProperties(ArrowheadProperties props) {
    return create(props.getProperty("db_address"),
                  props.getProperty("db_user"),
                  props.getProperty("db_password"));
  }

  public static DatabaseManager create(String address, String user, String password) {
    return new DatabaseManager(address, user, password);
  }

  private DatabaseManager(String address, String user, String password) {
    dbAddress = address;
    dbUser = user;
    dbPassword = password;

    try {
      Configuration configuration = new Configuration().configure("hibernate.cfg.xml")
                                                       .setProperty("hibernate.connection.url", dbAddress)
                                                       .setProperty("hibernate.connection.username", dbUser)
                                                       .setProperty("hibernate.connection.password", dbPassword);
      sessionFactory = configuration.buildSessionFactory();
    } catch (Exception e) {
      throw new ServiceConfigurationError(
          "Database connection could not be established, check default.conf/app.conf files!", e);
    }
  }

  private synchronized SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      Configuration configuration = new Configuration().configure("hibernate.cfg.xml")
                                                       .setProperty("hibernate.connection.url", dbAddress)
                                                       .setProperty("hibernate.connection.username", dbUser)
                                                       .setProperty("hibernate.connection.password", dbPassword);
      sessionFactory = configuration.buildSessionFactory();
    }
    return sessionFactory;
  }

  public synchronized void closeSessionFactory() {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
  }

  public <T> Optional<T> get(Class<T> queryClass, long id) {
    T object;
    Transaction transaction = null;

    try (Session session = getSessionFactory().openSession()) {
      transaction = session.beginTransaction();
      object = session.get(queryClass, id);
      transaction.commit();
    } catch (Exception e) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw e;
    }

    return Optional.ofNullable(object);
  }

  public <T> List<T> get(Class<T> queryClass, Set<Long> ids) {
    List<T> retrievedList = new ArrayList<>();
    Transaction transaction = null;

    try (Session session = getSessionFactory().openSession()) {
      transaction = session.beginTransaction();
      for (Long id : ids) {
        retrievedList.add(session.get(queryClass, id));
      }
      transaction.commit();
    } catch (Exception e) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw e;
    }

    return retrievedList;
  }

  @SuppressWarnings("unchecked")
  public <T> T get(Class<T> queryClass, Map<String, Object> restrictionMap) {
    T object;
    Transaction transaction = null;

    try (Session session = getSessionFactory().openSession()) {
      transaction = session.beginTransaction();
      //NOTE session.createCriteria will be removed in Hibernate 6
      //noinspection deprecation
      Criteria criteria = session.createCriteria(queryClass);
      if (restrictionMap != null && !restrictionMap.isEmpty()) {
        for (Entry<String, Object> entry : restrictionMap.entrySet()) {
          criteria.add(Restrictions.eq(entry.getKey(), entry.getValue()));
        }
      }
      object = (T) criteria.uniqueResult();
      transaction.commit();
    } catch (Exception e) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw e;
    }

    return object;
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> getAll(Class<T> queryClass, Map<String, Object> restrictionMap) {
    List<T> retrievedList;
    Transaction transaction = null;

    try (Session session = getSessionFactory().openSession()) {
      transaction = session.beginTransaction();
      //NOTE session.createCriteria will be removed in Hibernate 6
      //noinspection deprecation
      Criteria criteria = session.createCriteria(queryClass);
      if (restrictionMap != null && !restrictionMap.isEmpty()) {
        for (Entry<String, Object> entry : restrictionMap.entrySet()) {
          criteria.add(Restrictions.eq(entry.getKey(), entry.getValue()));
        }
      }
      retrievedList = (List<T>) criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    } catch (Exception e) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw e;
    }

    return retrievedList;
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> getAllOfEither(Class<T> queryClass, Map<String, Object> restrictionMap) {
    List<T> retrievedList;
    Transaction transaction = null;

    try (Session session = getSessionFactory().openSession()) {
      transaction = session.beginTransaction();
      //NOTE session.createCriteria will be removed in Hibernate 6
      //noinspection deprecation
      Criteria criteria = session.createCriteria(queryClass);
      if (restrictionMap != null && !restrictionMap.isEmpty()) {
        Disjunction disjunction = Restrictions.disjunction();
        for (Entry<String, Object> entry : restrictionMap.entrySet()) {
          disjunction.add(Restrictions.eq(entry.getKey(), entry.getValue()));
        }
        criteria.add(disjunction);
      }
      retrievedList = (List<T>) criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
      transaction.commit();
    } catch (Exception e) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw e;
    }

    return retrievedList;
  }


  @SafeVarargs
  public final <T> T save(T... objects) {
    Transaction transaction = null;

    try (Session session = getSessionFactory().openSession()) {
      transaction = session.beginTransaction();
      for (T object : objects) {
        session.save(object);
      }
      transaction.commit();
    } catch (PersistenceException e) {
      if (transaction != null) {
        transaction.rollback();
      }

      Throwable cause = e.getCause();
      if (cause instanceof ConstraintViolationException && cause.getMessage().equals("could not execute statement")) {
        throw new DuplicateEntryException(
            "There is already an entry in the database with these parameters. Please check the unique fields of the "
                + objects.getClass(), Status.BAD_REQUEST.getStatusCode(), e);
      } else {
        Throwable rootCause = getExceptionRootCause(e);
        throw new ArrowheadRuntimeException(
            "Unknown exception during database save: " + rootCause.getClass() + " - " + rootCause.getMessage(), e);
      }
    } catch (Exception e) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw e;
    }

    return objects[0];
  }


  @SafeVarargs
  public final <T> T merge(T... objects) {
    Transaction transaction = null;

    try (Session session = getSessionFactory().openSession()) {
      transaction = session.beginTransaction();
      for (T object : objects) {
        session.merge(object);
      }
      transaction.commit();
    } catch (PersistenceException e) {
      if (transaction != null) {
        transaction.rollback();
      }
      Throwable cause = e.getCause();
      if (cause instanceof ConstraintViolationException && cause.getMessage().equals("could not execute statement")) {
        throw new DuplicateEntryException(
            "There is already an entry in the database with these parameters. Please check the unique fields of the "
                + objects.getClass(), Status.BAD_REQUEST.getStatusCode(), e);
      } else {
        Throwable rootCause = getExceptionRootCause(e);
        throw new ArrowheadRuntimeException(
            "Unknown exception during database merge: " + rootCause.getClass() + " - " + rootCause.getMessage(), e);
      }
    } catch (Exception e) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw e;
    }

    return objects[0];
  }

  @SafeVarargs
  public final <T> void delete(T... objects) {
    Transaction transaction = null;

    try (Session session = getSessionFactory().openSession()) {
      transaction = session.beginTransaction();
      for (T object : objects) {
        session.delete(object);
      }
      transaction.commit();
    } catch (PersistenceException e) {
      if (transaction != null) {
        transaction.rollback();
      }

      Throwable cause = e.getCause();
      if (cause instanceof ConstraintViolationException && cause.getMessage().equals("could not execute statement")) {
        throw new ArrowheadRuntimeException(
            "There is a reference to this object in another table, which prevents the delete operation. (" + objects
                .getClass() + ")", Status.BAD_REQUEST.getStatusCode(), e);
      } else {
        Throwable rootCause = getExceptionRootCause(e);
        throw new ArrowheadRuntimeException(
            "Unknown exception during database delete: " + rootCause.getClass() + " - " + rootCause.getMessage(), e);
      }
    } catch (Exception e) {
      if (transaction != null) {
        transaction.rollback();
      }
      throw e;
    }
  }

  // NOTE this only works well on tables which dont have any connection to any other tables (HQL does not do cascading)
  @SuppressWarnings("unused")
  public void deleteAll(String tableName) {
    Session session = getSessionFactory().openSession();
    String stringQuery = "DELETE * FROM " + tableName;
    Query query = session.createQuery(stringQuery);
    query.executeUpdate();
  }

  @SuppressWarnings("WeakerAccess")
  private Throwable getExceptionRootCause(Throwable e) {
    Throwable cause = null;
    Throwable result = e;

    while (null != (cause = result.getCause()) && (result != cause)) {
      result = cause;
    }
    return result;
  }


}
