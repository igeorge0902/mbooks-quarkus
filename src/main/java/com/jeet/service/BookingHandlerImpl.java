package com.jeet.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.jeet.api.TrendingMovieRow;
import com.jeet.api.Location;
import com.jeet.api.Movie;
import com.jeet.api.Purchase;
import com.jeet.api.Screen;
import com.jeet.api.ScreeningDates;
import com.jeet.api.Seats;
import com.jeet.api.Ticket;
import com.jeet.api.Venues;
import com.jeet.db.DAO;
import com.jeet.db.Enums;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class BookingHandlerImpl {

	@Inject
	DAO dao;

	/**
	 * Returns trending movies ranked by booked ticket count for the given time window.
	 *
	 * @param days  look-back window in days (>= 1)
	 * @param limit max rows (1..50)
	 * @return ordered list of TrendingMovieRow
	 */
	public List<TrendingMovieRow> getTrendingMovies(int days, int limit) throws InterruptedException {
		return dao.getTrendingMovies(days, limit);
	}

	public List<Ticket> returnTickets(int screeningDateId, List<String> seats, String uuid, String orderId) throws InterruptedException {
		
		List<Ticket> tickets = dao.bookTickets(screeningDateId, seats, uuid, orderId);

		List<Ticket> bookedTickets = new ArrayList<Ticket>();
		
		for (int i = 0; i < tickets.size(); i++) {
			
			Ticket ticket = new Ticket();
			
			ticket.setTicketId(tickets.get(i).getTicketId());
			ticket.setPrice(tickets.get(i).getPrice());
			ticket.setTax(tickets.get(i).getTax());
			ticket.setSeats_seatNumber(tickets.get(i).getSeats_seatNumber());
			ticket.setSeats_seatRow(tickets.get(i).getSeats_seatRow());
			ticket.setScreen_screenId(tickets.get(i).getScreen().getScreenId());
			ticket.setMovie_name(tickets.get(i).getScreen().getMovie().getName());
			ticket.setPurchase(tickets.get(i).getPurchase());
			
			bookedTickets.add(ticket);
		}
		
		return bookedTickets;
	}
	
	public List<Seats> returnUpdatedseats(int screeningDateId) throws InterruptedException {
		
		List<Seats> seats = dao.updatetedSeats(screeningDateId);

		List<Seats> seats_ = new ArrayList<Seats>();

		for (int i = 0; i < seats.size(); i++) {
			
			Seats seat_ = new Seats();
			
			seat_.setSeatId(seats.get(i).getSeatId());
			seat_.setSeatNumber(seats.get(i).getSeatNumber());
			seat_.setSeatRow(seats.get(i).getSeatRow());
			seat_.setIsReserved(seats.get(i).getIsReserved());
			seat_.setPrice(seats.get(i).getPrice());
			seat_.setTax(seats.get(i).getTax());

			seats_.add(seat_);
		}
		
		return seats_;
	}
	
	public List<Movie> getAllMovies(int setFirstResult, String category) throws InterruptedException {
		
		List<Movie> movies = dao.getAllMovies(setFirstResult, category);

		return movies;	
	}
	
	public Movie getMovie(int movieId) throws InterruptedException {
		
		Movie movie = dao.getMovie(movieId);

		return movie;	
	}
	
	public List<Movie> getMoviesForVenue(int locationId) throws InterruptedException {
		
		List<Movie> movies = dao.getMoviesForVenue(locationId);

		return movies;	
	}
	
	public List<Location> getAllLocations() throws InterruptedException {
		
		List<Location> rawLocations = dao.getAllLocations();
		List<Location> locations = new ArrayList<Location>();

		for (int i = 0; i < rawLocations.size(); i++) {
			Location loc = new Location();
			loc.setLocationId(rawLocations.get(i).getLocationId());
			loc.setName(rawLocations.get(i).getName());
			loc.setFormatted_address(rawLocations.get(i).getFormatted_address());
			loc.setLatitude(rawLocations.get(i).getLatitude());
			loc.setLongitude(rawLocations.get(i).getLongitude());
			loc.setThumnbnail(rawLocations.get(i).getThumbnail());
			locations.add(loc);
		}

		return locations;	
	}
	
	public List<Venues> getAllVenuesForUpdate() throws InterruptedException {
		
		List<Venues> venues = dao.getAllVenuesForUpdate();

		return venues;	
	}
	
	public Location getLocationForVenue(int venuesId) throws InterruptedException {
		
		Venues venue = dao.getLocationForVenue(venuesId);
		Location location = venue.getLocation();
				
		return location;	
	}
	
	public List<Venues> getVenueByLocation(int locationId) throws InterruptedException {
		
		List<Venues> venue = dao.getVenueByLocation(locationId);
		List<Venues> venues = new ArrayList<Venues>();
		
		for (int i = 0; i < venue.size(); i++) {
		Venues venue_ = new Venues();
		
		venue_.setVenuesId(venue.get(i).getVenuesId());
		venue_.setName(venue.get(i).getName());
		venue_.setAddress(venue.get(i).getAddress());
		venue_.setContact(venue.get(i).getContact());
		venue_.setVenues_picture(venue.get(i).getVenues_picture());
		venue_.setScreen_screenId(venue.get(i).getScreen_screenId());
		
		venues.add(venue_);
		
		}
		
		return venues;	
	}

	
	public List<Location> getLocationForMovie(int movieId) throws InterruptedException {
		
		List<Location> rawLocations = dao.getLocationForMovie(movieId);
		List<Location> locations = new ArrayList<Location>();

		for (int i = 0; i < rawLocations.size(); i++) {

			Location loc = new Location();
			loc.setLocationId(rawLocations.get(i).getLocationId());
			loc.setName(rawLocations.get(i).getName());
			loc.setFormatted_address(rawLocations.get(i).getFormatted_address());
			loc.setLatitude(rawLocations.get(i).getLatitude());
			loc.setLongitude(rawLocations.get(i).getLongitude());
			loc.setThumnbnail(rawLocations.get(i).getThumbnail());

			locations.add(loc);
		}

		return locations;	
	}
	
	public List<Venues> getVenues(int movieId) throws InterruptedException {
		
		List<Venues> venue = dao.getVenuesForMovie(movieId);
		List<Venues> venues = new ArrayList<Venues>();
		
		for (int i = 0; i < venue.size(); i++) {
			
			Venues venue_ = new Venues();
			
			venue_.setVenuesId(venue.get(i).getVenuesId());
			venue_.setName(venue.get(i).getName());
			venue_.setAddress(venue.get(i).getAddress());
			venue_.setContact(venue.get(i).getContact());
			venue_.setVenues_picture(venue.get(i).getVenues_picture());
			venue_.setScreen_screenId(venue.get(i).getScreen_screenId());
			venue_.setLocationId(venue.get(i).getLocation().getLocationId());
			
			venues.add(venue_);
		}

		return venues;	
	}
	
	public List<ScreeningDates> getScreeningDatesForMovieOnVenue(int locationId, int movieId) throws InterruptedException {
		
		List<ScreeningDates> date = dao.getScreeningDatesForMovieOnVenue(locationId, movieId);
		List<ScreeningDates> dates = new ArrayList<ScreeningDates>();
		
		for (int i = 0; i < date.size(); i++) {
			
			ScreeningDates date_ = new ScreeningDates();
			
			date_.setScreeningDatesId(date.get(i).getScreeningDatesId());
			date_.setScreeningDate(date.get(i).getScreeningDate());
			date_.setMovieId(date.get(i).getMovieId());
			date_.setVenues_picture(date.get(i).getVenues().getVenues_picture());
			
			dates.add(date_);
		}

		return dates;	
	}
	
	public List<Seats> getSeatsForScreenForMovieOnVenue(int screeningDateId) throws InterruptedException {
		
		List<Seats> seat = dao.getSeatsForScreening(screeningDateId);
		List<Seats> seats = new ArrayList<Seats>();
		
		for (int i = 0; i < seat.size(); i++) {
			
			Seats seat_ = new Seats();
			
			seat_.setSeatId(seat.get(i).getSeatId());
			seat_.setSeatNumber(seat.get(i).getSeatNumber());
			seat_.setSeatRow(seat.get(i).getSeatRow());
			seat_.setIsReserved(seat.get(i).getIsReserved());
			seat_.setPrice(seat.get(i).getPrice());
			seat_.setTax(seat.get(i).getTax());

			seats.add(seat_);
		}

		return seats;	
	}
	

	public List<Movie> fullTextSearchMovies(String match, String category, int setFirstResult) throws InterruptedException {
		return dao.fullTextSearchMovies(match, category, setFirstResult);
	}

	
	public List<Movie> searchMovies(String name, String order) throws InterruptedException {
		
		if(order.equalsIgnoreCase(Enums.SortOrder.ASC.get()) || order.equalsIgnoreCase(Enums.SortOrder.DESC.get())) {

			List<Movie> movie = dao.searchMovies(name, order);

		return movie;
		
		}
		
		else 
		
		return null;
	}

    /*
	public boolean saveTransactionId(String transactionId) {
		
		dao.saveTransactionId(transactionId);

		return true;
	}
	*/
	
	public boolean deleteTicket(List<Integer> ticketIds, Integer purchaseId) throws InterruptedException {
		
		dao.cancelTicket(ticketIds, purchaseId);

		return true;
	}

	public Purchase setBrainTreeCustomerId(String customerId, int purchaseId) throws InterruptedException {

		Purchase purchase = dao.setBraintreeId(customerId, purchaseId);

		return purchase;
	}

	public Purchase getBrainTreeCustomerId(String uuid) throws InterruptedException {

		Purchase purchase = dao.getBraintreeId(uuid);

		return purchase;
	}
	
	public boolean deletePurchase(Integer purchaseId) throws InterruptedException {
		
		dao.deletePurchase(purchaseId);

		return true;
	}

	public Screen addScreen(String movie, String Date, String Venue, int nrOfRows, int nrOfSeatsInRow, String ScreeningId, String category) throws ParseException, InterruptedException {
		
		Screen screen = dao.insertNewScreen(movie, Date, Venue, nrOfRows, nrOfSeatsInRow, ScreeningId, category);

		return screen;
	}
	
	public Screen updateScreen(String Venue, int venuesId, int screeningDatesId, int moviesId, String screenId, String Date, String category) throws ParseException, InterruptedException {
		
		Screen screen = dao.updateScreen(Venue, venuesId, screeningDatesId, moviesId, screenId, Date, category);

		return screen;
	}
	
	public boolean deleteScreen(int screeningDatesId) throws ParseException, InterruptedException {
		 
		boolean delete = dao.deleteScreen(screeningDatesId);

		return delete;
	}
	
	public List<Purchase> getAllPurchases(String uuid) throws InterruptedException {
		
		List<Purchase> movies = dao.getPurchases(uuid);

		return movies;	
	}
	
	public List<Ticket> getTicketPerPurchase(int purchaseId) throws InterruptedException {
		
		List<Ticket> tickets = dao.getTicketPerPurchase(purchaseId);

		return tickets;
	}
}
