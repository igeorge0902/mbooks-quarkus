package com.jeet.db;

import com.jeet.api.*;
import com.jeet.utils.CustomExceptions;
import jakarta.enterprise.context.Dependent;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.hibernate.*;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.stat.Statistics;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Dependent
public class DAO {

	private static DAO instance;
	private final SessionFactory factory;
	private static final ExecutorService executor = Executors.newSingleThreadExecutor();

	/**
	 * Instantiate DAO class that loads configured SessionFactory object.
	 */
	public DAO() throws InterruptedException {

		factory = HibernateUtil.getSessionFactory();
		System.out.println("Creating factory");

		Statistics stats = factory.getStatistics();
		stats.setStatisticsEnabled(true);

		// clear cache
		factory.getCache().evictAllRegions();
		System.out.println("Cache cleared.");
	}

	/**
	 * DAO instance — synchronized only for singleton initialisation.
	 *
	 * @return DAO instance
	 */
	public synchronized static DAO instance() throws InterruptedException {
		if (instance == null) {
			instance = new DAO();
		}
		return instance;
	}

	// ────────────────────────────────────────────────────────
	//  Read-only query methods — session-per-call, no lock
	// ────────────────────────────────────────────────────────

	/**
	 * Returns a movie object by movieId.
	 */
	public Movie getMovie(int movieId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();
			Movie movie = session.get(Movie.class, movieId);
			trans.commit();
			return movie;
		}
	}

	/**
	 * Returns all the movies as a list.
	 */
	@SuppressWarnings("unchecked")
	public List<Movie> getAllMovies(int setFirstResult, String category) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			List<Movie> movies;
			String hql;
			if (category.isBlank()) {
				hql = "from Movie order by name asc";
			} else {
				hql = "from Movie where category = :mCategory order by name asc";
			}

			if (setFirstResult == -1 && category.equals("")) {
				movies = session.createQuery(hql, Movie.class)
						.setCacheable(true)
						.setCacheRegion("movies")
						.setCacheMode(CacheMode.NORMAL)
						.getResultList();

			} else if (!category.isEmpty() && setFirstResult > -1) {
				movies = session.createQuery(hql)
						.setCacheable(true)
						.setParameter("mCategory", category)
						.setCacheRegion("movies")
						.setCacheMode(CacheMode.NORMAL)
						.setFirstResult(setFirstResult)
						.setMaxResults(30).getResultList();

			} else {
				movies = session.createQuery(hql)
						.setCacheable(true)
						.setCacheRegion("movies")
						.setCacheMode(CacheMode.NORMAL)
						.setFirstResult(setFirstResult)
						.setMaxResults(30).getResultList();
			}

			trans.commit();
			return movies;
		}
	}

	/**
	 * Returns a list of movies matching the search criteria.
	 */
	public List<Movie> searchMovies(String name, String order) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "from Movie m where name like '%" + name + "%' order by m.name " + order;
			List<Movie> list = session.createQuery(hql)
					.setCacheable(true)
					.setCacheRegion("movies").getResultList();

			trans.commit();
			return list;
		}
	}

	/**
	 * Returns all seats for a screen (movie/venue) by screeningDateId.
	 */
	public List<Seats> getSeatsForScreening(int screeningDateId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select screeningdates.venues.screen.seat from ScreeningDates as screeningdates where screeningDatesId = :mScreeningDatesId";
			List<Seats> list = session.createQuery(hql)
					.setParameter("mScreeningDatesId", screeningDateId).getResultList();

			trans.commit();
			return list;
		}
	}

	/**
	 * Returns seats state (availability).
	 */
	public synchronized Seats getSeatForAvailability(int screeningDatesId, String seatNumber) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select seats from ScreeningDates as screeningdates inner join screeningdates.venues.screen.seat as seats where screeningdates.screeningDatesId = :mScreeningDatesId and seats.seatNumber = :mSeatNumber";
			Seats seat = (Seats) session.createQuery(hql)
					.setParameter("mScreeningDatesId", screeningDatesId)
					.setParameter("mSeatNumber", seatNumber)
					.setLockMode(LockModeType.PESSIMISTIC_WRITE)
					.uniqueResult();

			trans.commit();
			return seat;
		}
	}

	/**
	 * Returns all movies for a given venue.
	 */
	public List<Movie> getMoviesForVenue(int locationId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select venues.screen.movie from Venues as venues where venues.location.locationId = :mLocationId";
			List<Movie> list = session.createQuery(hql)
					.setParameter("mLocationId", locationId)
					.getResultList();

			trans.commit();
			return list;
		}
	}

	/**
	 * Returns a list of movies by fulltext search in movie names and details.
	 *
	 * @param match
	 * @return
	 */
	/*
	public synchronized List<Movie> fullTextSearchMoviesForVenue(String match) {

        if(!session.isOpen()) {
            session = factory.openSession();
        }

        session = factory.getCurrentSession();
        trans = session.getTransaction();

        if (trans.getStatus() != TransactionStatus.ACTIVE) {
            trans.begin();
        }

        // create native Lucene query using the query DSL
        QueryBuilder qb = fullTextSession.getSearchFactory().buildQueryBuilder().forEntity(Movie.class).get();

        if (!match.contains(" ")) {
        query = qb
          .keyword()
          .onField("venues.screen.movie.name")
          .andField("venues.screen.movie.detail")
          .matching(match)
          .createQuery();
        }

        if (match.contains(" ")) {
        query = qb
                  .phrase()
                  .onField("venues.screen.movie.name")
                  .andField("venues.screen.movie.detail")
                  .sentence(match)
                  .createQuery();
        }

        // wrap Lucene query in a org.hibernate.Query
        FullTextQuery hibQuery = fullTextSession.createFullTextQuery(query, Movie.class);

        hibQuery.initializeObjectsWith(
                ObjectLookupMethod.SECOND_LEVEL_CACHE,
                DatabaseRetrievalMethod.QUERY
            );

        hibQuery
        .setCacheable(true)
        .setCacheRegion("movies")
        .setCacheMode(CacheMode.NORMAL);

        // execute search
        @SuppressWarnings("unchecked")
        List<Movie> result = hibQuery.list();

        trans.commit();

        if(session.isOpen()) {
            session.close();
        }

        return result;
    }
	*/

	/**
	 * Returns location for a venue by locationId. It will be called from individual screens.
	 *
	 * @param locationId
	 * @return
	 */
	public Location getlocationForVenue(int locationId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "from Location where locationId = :mlocationId";
			Location location = (Location) session.createQuery(hql)
					.setParameter("mlocationId", locationId)
					.setCacheable(true)
					.setCacheRegion("location").uniqueResult();

			trans.commit();
			return location;
		}
	}

	/**
	 * Returns venue for a location by locationId.
	 *
	 * @param locationId
	 * @return
	 */
	public List<Venues> getVenueByLocation(int locationId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select venues from Venues as venues inner join venues.location where venues.location.locationId = :mlocationId";
			List<Venues> venue = session.createQuery(hql)
					.setParameter("mlocationId", locationId)
					.setCacheable(true)
					.setCacheRegion("venues").getResultList();

			trans.commit();
			return venue;
		}
	}

	/**
	 * Returns all movies on venues.
	 *
	 * @return
	 */
	public List<Venues> getAllVenuesForUpdate() {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select venues from Venues as venues inner join venues.screen.movie";
			List<Venues> list = session.createQuery(hql)
					.setCacheable(true)
					.setCacheRegion("movies").getResultList();

			trans.commit();
			return list;
		}
	}

	/**
	 * Returns all location for the map.
	 *
	 * @return
	 */
	public List<Location> getAllLocations() {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select location from Location as location";
			List<Location> list = session.createQuery(hql)
					.setCacheable(true)
					.setCacheRegion("location").getResultList();

			trans.commit();
			return list;
		}
	}

	public Venues getLocationForVenue(int venuesId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select v from Venues v JOIN FETCH v.location where v.venuesId = :mVenuesId";
			Venues venue = (Venues) session.createQuery(hql)
					.setParameter("mVenuesId", venuesId)
					.setCacheable(true)
					.setCacheRegion("location")
					.uniqueResult();

			trans.commit();
			return venue;
		}
	}

	/**
	 * Select venue:location for screening movie, i.e. where the movie, on which venue its screening happens.
	 *
	 * @param movieId
	 * @return
	 */
	public List<Location> getLocationForMovie(int movieId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select distinct v.location from Venues v where v.screen.movie.movieId = :movieId";
			List<Location> location = session.createQuery(hql, Location.class)
					.setParameter("movieId", movieId)
					.setCacheable(true)
					.setCacheRegion("location").getResultList();

			trans.commit();
			return location;
		}
	}

	/**
	 * Returns venues for a movie.
	 *
	 * @param movieId
	 * @return
	 */
	public List<Venues> getVenuesForMovie(int movieId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select venues from Venues as venues inner join venues.screen.movie as movie where movie.movieId = :mMovieId";
			List<Venues> list = session.createQuery(hql)
					.setParameter("mMovieId", movieId)
					.setCacheable(true)
					.setCacheRegion("venues").getResultList();

			trans.commit();
			return list;
		}
	}

	/**
	 * Get the unique screeningDate for a movie on a venue.
	 *
	 * @param locationId
	 * @param movieId
	 * @return
	 */
	public List<ScreeningDates> getScreeningDatesForMovieOnVenue(int locationId, int movieId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select date from ScreeningDates as date inner join date.venues.location as location where date.movieId = :mMovieId and location.locationId = :mLocationId";
			List<ScreeningDates> list = session.createQuery(hql)
					.setParameter("mMovieId", movieId)
					.setParameter("mLocationId", locationId).getResultList();

			trans.commit();
			return list;
		}
	}

	/**
	 * Returns all purchases per order for the user.
	 *
	 * @return
	 */
	public List<Purchase> getPurchases(String uuid) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select purchase from Purchase as purchase where uuid = :uuid";
			List<Purchase> list = session.createQuery(hql)
					.setParameter("uuid", uuid)
					.setCacheable(true)
					.setCacheRegion("purchases").getResultList();

			trans.commit();
			return list;
		}
	}

	/**
	 * Returns tickets per purchase to the client.
	 *
	 * @param screenId
	 * @param seatId
	 * @return
	 */
	public List<Ticket> getTicketPerPurchase(int purchaseId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "SELECT t FROM Ticket t "
					+ "JOIN FETCH t.screen s "
					+ "JOIN FETCH s.movie m "
					+ "JOIN FETCH s.screeningDates sd "
					+ "JOIN FETCH sd.venues v "
					+ "WHERE t.purchase.purchaseId = :purchaseId";

			List<Ticket> list = session.createQuery(hql, Ticket.class)
					.setParameter("purchaseId", purchaseId)
					.setCacheable(true)
					.getResultList();

			trans.commit();
			return list;
		}
	}

	// ────────────────────────────────────────────────────────
	//  Write methods — synchronized to serialise mutations
	// ────────────────────────────────────────────────────────

	/**
	 * Prepares the purchaseId for the transaction.
	 */
	public synchronized Purchase setPurchaseId(String uuid, String orderId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			Purchase newPurchase = new Purchase();
			newPurchase.setUuid(uuid);
			newPurchase.setOrderId(orderId);
			newPurchase.setTime(new Date());

			session.save(newPurchase);
			session.flush();

			// Reload to ensure ID is populated
			Purchase purchase = session.get(Purchase.class, newPurchase.getPurchaseId());

			trans.commit();
			return purchase;
		}
	}

	/**
	 * Sets the BrainTree customerId.
	 */
	public synchronized Purchase getBraintreeId(String uuid) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "SELECT p FROM Purchase p WHERE p.uuid = :uuid AND p.braintree_customerId IS NOT NULL";
			List<Purchase> purchase = session.createQuery(hql)
					.setParameter("uuid", uuid).getResultList();

			trans.commit();

			if (purchase.size() > 0) {
				return purchase.get(0);
			} else {
				return null;
			}
		}
	}

	/**
	 * Sets the BrainTree customerId for the transaction.
	 */
	public synchronized Purchase setBraintreeId(String customerId, int purchaseId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			String hql = "select purchase from Purchase as purchase where purchaseId = :purchaseId";
			Purchase purchase = (Purchase) session.createQuery(hql)
					.setParameter("purchaseId", purchaseId).uniqueResult();
			purchase.setBrainTreeId(customerId);
			session.saveOrUpdate(purchase);

			trans.commit();
			return purchase;
		}
	}

	/**
	 * Reserves seats for a screening, produces tickets for the purchase.
	 */
	@Transactional
	public synchronized List<Ticket> bookTickets(int screeningDateId, List<String> seats, String uuid, String orderId) throws InterruptedException {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			// Create purchase
			Purchase purchase = new Purchase();
			purchase.setUuid(uuid);
			purchase.setOrderId(orderId);
			purchase.setTime(new Date());
			session.save(purchase);
			session.flush();

			List<Ticket> tickets = new ArrayList<>();

			try {
				// Fetch and lock all seats
				List<Seats> seatsList = new ArrayList<>();
				for (String seatNum : seats) {
					String hql = "select s from ScreeningDates as sd inner join sd.venues.screen.seat as s where sd.screeningDatesId = :mScreeningDatesId and s.seatNumber = :mSeatNumber";
					Seats seat = (Seats) session.createQuery(hql)
							.setParameter("mScreeningDatesId", screeningDateId)
							.setParameter("mSeatNumber", seatNum)
							.setLockMode(LockModeType.PESSIMISTIC_WRITE)
							.uniqueResult();
					seatsList.add(seat);
				}

				// Check if any seat is already reserved
				boolean anySeatReserved = seatsList.stream().anyMatch(seat -> !"0".equals(seat.getIsReserved()));
				if (anySeatReserved) {
					trans.rollback();
					throw new CustomExceptions("Booking Failed", "One or more seats are already reserved.");
				}

				// If all seats are available, proceed with booking
				for (Seats seat : seatsList) {
					Ticket newTicket = new Ticket();
					newTicket.setScreen(seat.getScreen());
					newTicket.setPrice(seat.getPrice());
					newTicket.setTax(seat.getTax());
					newTicket.setSeats_seatNumber(seat.getSeatNumber());
					newTicket.setSeats_seatRow(seat.getSeatRow());
					newTicket.setSeats(seat);
					newTicket.setPurchase(purchase);

					seat.setIsReserved("1");

					session.save(newTicket);
					session.update(seat);

					tickets.add(newTicket);
				}

				trans.commit();
				return tickets;

			} catch (CustomExceptions e) {
				throw e;
			} catch (Exception e) {
				if (trans.isActive()) trans.rollback();
				throw new CustomExceptions("Booking Failed", "Unable to book seats: " + e.getMessage());
			}
		}
	}

	/**
	 * Returns the seats after the booking transaction has been completed.
	 */
	public List<Seats> updatetedSeats(int screeningDateId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			ScreeningDates date = session.get(ScreeningDates.class, screeningDateId);
			List<Seats> seats = date.getVenues().getScreen().getSeatsForScreen();
			// Force initialisation while session is open
			seats.size();

			trans.commit();
			return seats;
		}
	}

	/**
	 * Deletes individual ticket and sets the seat free.
	 */
	public synchronized boolean cancelTicket(List<Integer> ticketIds, Integer purchaseId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			try {
				for (int i = 0; i < ticketIds.size(); i++) {
					Ticket tic = session.load(Ticket.class, ticketIds.get(i));
					Seats seat = session.load(Seats.class, tic.getSeats().getSeatId());
					seat.setIsReserved("0");

					session.saveOrUpdate(seat);
					session.delete(tic);
				}

				Purchase pur = session.load(Purchase.class, purchaseId);
				List<Ticket> tickets = pur.getTicketsForPurchase();

				if (tickets.size() - ticketIds.size() == 0) {
					session.delete(pur);
				}

				trans.commit();

			} catch (Exception e) {
				System.out.println(e.getLocalizedMessage());
				if (trans.isActive()) trans.rollback();
				return false;
			}
			return true;
		}
	}

	/**
	 * Deletes purchase with associated tickets.
	 */
	public synchronized boolean deletePurchase(Integer purchaseId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			Purchase pur = session.createQuery(
					"SELECT p FROM Purchase p LEFT JOIN FETCH p.ticket t LEFT JOIN FETCH t.seats WHERE p.id = :purchaseId",
					Purchase.class
			).setParameter("purchaseId", purchaseId).getSingleResult();

			List<Ticket> ticketIds = pur.getTicketsForPurchase();

			int batchSize = 20;
			for (int i = 0; i < ticketIds.size(); i++) {
				Ticket tic = ticketIds.get(i);
				Seats seat = tic.getSeats();
				seat.setIsReserved("0");

				session.saveOrUpdate(seat);

				if (i % batchSize == 0) {
					session.flush();
					session.clear();
				}
			}

			session.createQuery("DELETE FROM Purchase WHERE purchaseId = :purchaseId")
					.setParameter("purchaseId", purchaseId)
					.executeUpdate();

			trans.commit();
			return true;
		}
	}

	//TODO: add controller and admin user in iOS
	public synchronized boolean deleteScreen(int screeningDateId) {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			ScreeningDates date = session.get(ScreeningDates.class, screeningDateId);
			session.delete(date);

			try {
				trans.commit();
				return true;
			} catch (Exception e) {
				if (trans.isActive()) trans.rollback();
				return false;
			}
		}
	}

	public synchronized Screen getScreenForAvailability(String movie, String date, String venue,
														String screeningId, String category)
			throws HibernateException, ParseException {

		Transaction transaction = null;

		try (Session session = factory.openSession()) {
			transaction = session.beginTransaction();

			// Fetch Location entity
			Location location = (Location) session.createQuery("FROM Location WHERE name = :mLocationName")
					.setParameter("mLocationName", venue)
					.uniqueResult();

			if (location == null) throw new HibernateException("Location not found: " + venue);

			// Fetch or update Movie entity
			Movie movieEntity = (Movie) session.createQuery("FROM Movie WHERE name = :mMovieName")
					.setParameter("mMovieName", movie)
					.uniqueResult();

			if (movieEntity == null) throw new HibernateException("Movie not found: " + movie);

			movieEntity.setCategory(category);
			session.saveOrUpdate(movieEntity);

			// Retrieve max venue ID safely
			Integer maxVenueId = (Integer) session.createQuery("SELECT MAX(venuesId) FROM Venues").uniqueResult();
			int newVenueId = (maxVenueId == null) ? 1 : maxVenueId + 1;

			// Create and save Venue
			Venues venueEntity = new Venues();
			venueEntity.setVenuesId(newVenueId);
			venueEntity.setAddress(location.getFormatted_address());
			venueEntity.setContact("Tim Roth");
			venueEntity.setLocation(location);
			venueEntity.setName(location.getName());
			venueEntity.setVenues_picture(location.getThumbnail());
			venueEntity.setScreen_screenId(screeningId);

			session.save(venueEntity);
			session.buildLockRequest(LockOptions.UPGRADE).lock(venueEntity);

			// Retrieve max screening date ID safely
			Integer maxDateId = (Integer) session.createQuery("SELECT MAX(screeningDatesId) FROM ScreeningDates").uniqueResult();
			int newScreeningDateId = (maxDateId == null) ? 1 : maxDateId + 1;

			// Create and save ScreeningDates
			ScreeningDates screeningDate = new ScreeningDates();
			screeningDate.setScreeningDatesId(newScreeningDateId);
			screeningDate.setMovieId(movieEntity.getMovieId());
			screeningDate.setScreeningDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date));
			screeningDate.setVenues(venueEntity);

			session.save(screeningDate);

			// Create and save Screen
			Screen screen = new Screen();
			screen.setScreenId(screeningId);
			screen.setMovie(movieEntity);
			screen.setSeats(30);
			screen.setScreeningDates(screeningDate);

			session.save(screen);
			venueEntity.setScreen(screen);
			session.saveOrUpdate(venueEntity);

			// Load and return the Screen with an upgraded lock
			Screen screenResult = session.get(Screen.class, screen.getScreenId(), LockMode.UPGRADE_NOWAIT);

			transaction.commit();
			return screenResult;

		} catch (Exception e) {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
			e.printStackTrace();
			return getErrorScreen(e);
		}
	}

	public synchronized Screen insertNewScreen(String movie, String date, String venue, int nrOfRows, int nrOfSeatsInRow, String screeningId, String category) throws HibernateException, ParseException {
		Transaction transaction = null;
		Screen screen = null;

		try (Session session = factory.openSession()) {
			transaction = session.beginTransaction();

			// Step 1: Get or create the screen
			screen = DAO.instance().getScreenForAvailability(movie, date, venue, screeningId, category);

			// Flush to ensure the screen gets an ID before creating seats
			session.saveOrUpdate(screen);
			session.flush();
			session.refresh(screen);

			if (screen.getScreenId() == null) {
				throw new HibernateException("Screen ID is null after save. Something went wrong.");
			}

			// Step 2: Handle seat ID generation safely
			String hqlSeats = "select max(seatId) from Seats";
			Integer maxSeatId = (Integer) session.createQuery(hqlSeats).uniqueResult();
			int newSeatId = (maxSeatId != null) ? maxSeatId + 1 : 1;

			// Step 3: Generate and save seats
			List<Seats> seatsList = new ArrayList<>();
			String[] alphabet = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P"};

			for (int i = 0; i < nrOfRows; i++) {
				for (int j = 1; j <= nrOfSeatsInRow; j++) {
					Seats seat = new Seats();
					seat.setSeatId(newSeatId++);
					seat.setIsReserved("0");
					seat.setScreen(screen);
					seat.setSeatNumber(alphabet[i] + j);
					seat.setSeatRow(String.valueOf(i + 1));
					seat.setPrice(3);
					seat.setTax(1.33);

					session.save(seat);
					seatsList.add(seat);
				}
			}

			// Step 4: Save updated screen with seats
			screen.setSeats(seatsList);
			session.saveOrUpdate(screen);
			session.flush();

			// Step 5: Commit transaction
			transaction.commit();

		} catch (Exception e) {
			if (transaction != null && transaction.isActive()) transaction.rollback();
			e.printStackTrace();
			return null;
		}

		return screen;
	}

	// Helper method to return an error screen
	private Screen getErrorScreen(Exception e) {
		Screen errorScreen = new Screen();
		errorScreen.setScreenId("Error: " + e.getMessage());
		return errorScreen;
	}

	public synchronized Screen updateScreen(String Venue, int venuesId, int screeningDatesId, int moviesId, String screenId, String Date, String category) throws ParseException {
		try (Session session = factory.openSession()) {
			Transaction trans = session.beginTransaction();

			// change venue i.e. cinema location
			String hql = "from Location where name = :mLocationName";
			Location location = (Location) session.createQuery(hql)
					.setParameter("mLocationName", Venue).uniqueResult();

			// keep original venuesId
			Venues venue_ = session.get(Venues.class, venuesId);

			// do the change here
			venue_.setLocation(location);
			venue_.setName(location.getName());
			venue_.setAddress(location.getFormatted_address());
			venue_.setVenues_picture(location.getThumbnail());
			venue_.setContact("Quentin Tarantino");
			session.saveOrUpdate(venue_);

			// change date
			ScreeningDates dates = session.get(ScreeningDates.class, screeningDatesId);
			dates.setScreeningDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
					.parse(Date));
			session.saveOrUpdate(dates);

			// change date and movie for the venuesId
			Screen screen = session.get(Screen.class, screenId);
			Movie movie = session.get(Movie.class, moviesId);
			movie.setCategory(category);
			screen.setMovie(session.get(Movie.class, moviesId));
			session.saveOrUpdate(movie);
			session.saveOrUpdate(screen);

			try {
				trans.commit();
			} catch (Exception e) {
				if (trans.isActive()) trans.rollback();
			}

			return screen;
		}
	}
}
