package com.jeet.rest;

import com.braintreegateway.*;
import com.jeet.api.*;
import com.jeet.booking.PaymentService;
import com.jeet.booking.PurchaseDAO;
import com.jeet.booking.TicketService;
import com.jeet.broadcasting.KafkaMessageProducer;
import com.jeet.broadcasting.eventModel.AddMovie;
import com.jeet.broadcasting.serialization.Serialization;
import com.jeet.service.BookingHandlerImpl;
import com.jeet.utils.AesUtil;
import com.jeet.utils.CustomExceptions;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.core.Response.Status;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

//TODO: add APIKEY container request filter
/*
 * https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/filters-and-interceptors.html#d0e9368
 */
//TODO: error handling
/*
 * https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/representations.html#d0e6367
 */
@Path("/rest")
@RequestScoped
public class BookController implements Serializable {

	@Inject
	TicketService ticketService;

	@Inject
	PaymentService paymentService;

	@Inject
	PurchaseDAO purchaseDAO;

	@Inject
	private BookingHandlerImpl bookingHandler;

	@Context
	private HttpServletRequest request_;

	private static final String SALT = "3FF2EC019C627B945225DEBAD71A01B6985FE84C95A70EB132882F88C0A59A55";
    private static final String IV = "F27D5C9927726BCEFE7510B1BDD3D137";
    private static volatile String ciphertext;
    private static volatile String uuid;
	private static volatile String braintreecustomerid;
	private static volatile String orderId;
    private static volatile String xsrfToken;
    private static volatile Boolean xsrf;
    private static volatile Cookie[] cookies;
    private static volatile String plaintext;
    private static final String ORIGINPLAINTEXT = "G";
    private static final String PASSPHRASE = "SecretPassphrase";
    private static final int KEYSIZE = 128;
    private static final int ITERATIONCOUNT = 1000;   
    private static AesUtil aesUtil = new AesUtil(KEYSIZE, ITERATIONCOUNT);
    private static volatile String APIKEY = "19891213";
    private static volatile String c;
    private static volatile String seatsToBeReserved;
    private static volatile String ticketsToBeCancelled;
    private static volatile String screeningDateId;
    private static volatile String nonce;
    private static volatile String customerId;
    private static volatile String clientToken;
        
    private static BraintreeGateway gateway = new BraintreeGateway(
    		  Environment.SANDBOX,
      		  "j3ndqpzrhy4gp2p7",
      		  "rzmyrsbswb3hwsmk",
      		  "37113dbf6dc015806f510e7e630755fb"
    		);

	@GET
	@Path("/book/hello")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getGreeting() {
		JSONObject json = new JSONObject();
		json.put("greeting", "hello");
		return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).build();
	}
    
    /**
     * The user completes the checkout: the user needs to have a customer, therefore create a new CustomerRequest, if necessary.
     * Unless the card is not valid, the booking takes places, and the order will be recorded with purchase and payment details, that 
     * defines an order: the Order table will hold the payment details, 
     * linked with the order_orderId to the Purchase table, which holds all the purchased items for a given order, 
     * so the "order" is an abstraction of the purchase(s) with payment.
     * 
     * For reservation only, the scenario is the same, but without payment transactions.
     * 
     * @param headers
     * @param request_
     * @return
     * @throws IOException
     */
	@POST
	@Path("/book/payment/fullcheckout2")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response fullcheckout2(@Context HttpHeaders headers, @Context HttpServletRequest request_)
			throws IOException, InterruptedException {

		String uuid = request_.getHeader("uuid");
		String orderId = request_.getParameter("orderId");
		String seatsToBeReserved = request_.getParameter("seatsToBeReserved").trim();
		String nonce = request_.getParameter("payment_method_nonce").trim();

		if (uuid == null || orderId == null || seatsToBeReserved.isEmpty() || nonce.isEmpty()) {
			return Response.status(Status.BAD_REQUEST)
					.entity("{\"error\": \"Missing required parameters\"}")
					.type(MediaType.APPLICATION_JSON)
					.build();
		}

		JSONObject jsonObj = new JSONObject(seatsToBeReserved);
		JSONArray seatArray = jsonObj.getJSONArray("seatsToBeReserved");

		//List<Ticket> allTickets = new ArrayList<>();
		List<Ticket> tickets = new ArrayList<>();
		List<Ticket> failedTickets = new ArrayList<>();
		List<Integer> ticketIds_ = new ArrayList<>();
		List<Seats> allSeats = new ArrayList<>();
		BigDecimal totalAmount = BigDecimal.ZERO;
		BigDecimal totalTax = BigDecimal.ZERO;

		int purchaseId = 0;

		for (int i = 0; i < seatArray.length(); i++) {
			JSONObject seatObj = seatArray.getJSONObject(i);
			int screeningDateId = seatObj.getInt("screeningDateId");
			String[] seatNumbers = seatObj.getString("seat").split("-");

			tickets = ticketService.reserveTickets(screeningDateId, seatNumbers, uuid, orderId);
			ticketIds_ = tickets.stream().map(e -> e.getTicketId()).collect(Collectors.toList());
			//allTickets.addAll(tickets);

			if (tickets.size() != seatNumbers.length) {
				failedTickets.addAll(tickets);
			} else {
				purchaseId = tickets.get(0).getPurchase().getPurchaseId();
				allSeats.addAll(ticketService.getUpdatedSeats(screeningDateId));
				for (Ticket ticket : tickets) {
					BigDecimal price = BigDecimal.valueOf(ticket.getPrice());
					BigDecimal tax = price.multiply(BigDecimal.valueOf(ticket.getTax()));
					totalAmount = totalAmount.add(price).add(tax);
					totalTax = totalTax.add(tax);
				}
			}
		}

		if (tickets.isEmpty() || !failedTickets.isEmpty()) {
			ticketService.rollbackTickets(ticketIds_, purchaseId);
			JSONObject json = new JSONObject();
			json.put("Error","Tickets are already sold!");
			json.put("Success", "true");
			json.put("failedTickets", failedTickets);
			return Response.ok()
					.entity(json)
					.type(MediaType.APPLICATION_JSON)
					.build();
		}

		Purchase brainTreeCustomer = purchaseDAO.getBrainTreeCustomer(uuid);
		String customerId = paymentService.getOrCreateCustomerId(uuid, brainTreeCustomer);
		bookingHandler.setBrainTreeCustomerId(customerId, purchaseId);

		TransactionRequest request = new TransactionRequest()
				.merchantAccountId("testcompany")
				.customerId(customerId)
				.amount(totalAmount.setScale(2, RoundingMode.HALF_UP))
				.taxAmount(totalTax.setScale(2, RoundingMode.HALF_UP))
				.paymentMethodNonce(nonce)
				.options().submitForSettlement(true).done();

		Result<Transaction> result = paymentService.processTransaction(request);

		if (!result.isSuccess()) {
			ticketService.rollbackTickets(ticketIds_, purchaseId);
			JSONObject json = new JSONObject();
			json.put("Error with Transaction", result.getMessage());
			return Response.ok()
					.entity(json)
					.type(MediaType.APPLICATION_JSON)
					.build();
		}

		Transaction transaction = result.getTarget();

		JSONObject responseJson = new JSONObject();
		responseJson.put("ResponseText", "hello");
		responseJson.put("AuthCode", transaction.getProcessorAuthorizationCode().trim());
		responseJson.put("ResponseCode", transaction.getProcessorResponseCode().trim());
		responseJson.put("Status", transaction.getStatus().toString());
		responseJson.put("Amount", totalAmount);
		responseJson.put("TaxAmount", String.format("%.2f", totalTax));
		responseJson.put("Success", "true");
		responseJson.put("tickets", tickets);
		responseJson.put("seatsforscreen", allSeats);

		return Response.ok().entity(responseJson.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
	}
    
    @GET
	@Path("/book/purchases")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getAllPurchases() throws IOException, InterruptedException {

		uuid = request_.getHeader("uuid").toString();
		//
      	List<Purchase> purchases = bookingHandler.getAllPurchases(uuid);
      	
		JSONObject json = new JSONObject();
		HashMap<String, String> tickets = new HashMap<>();

		for (int i = 0; i < purchases.size(); i++) {
			
			JSONObject responseObj = new JSONObject();
			
			responseObj.put("purchaseId", String.valueOf(purchases.get(i).getPurchaseId()));
			responseObj.put("orderId", purchases.get(i).getOrderId());
			responseObj.put("movie_name", purchases.get(i).getTicketsForPurchase().get(0).getScreen().getMovie().getName());
			responseObj.put("movie_picture", purchases.get(i).getTicketsForPurchase().get(0).getScreen().getMovie().getLarge_picture());
			responseObj.put("screeningDate", purchases.get(i).getTicketsForPurchase().get(0).getScreen().getScreeningDates().getScreeningDate());
			responseObj.put("venue_name", purchases.get(i).getTicketsForPurchase().get(0).getScreen().getScreeningDates().getVenues().getName());
			responseObj.put("purchaseDate", purchases.get(i).getTime());
						
			json.append("purchases", responseObj);
		
		}
		
		return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();	

	}

	@GET
	@Path("/book/purchases/tickets")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTicketsPerPurchase(
			@Context HttpHeaders headers,
			@Context HttpServletRequest request_,
			@QueryParam(value = "purchaseId") String purchaseId) throws IOException, InterruptedException {

		List<Ticket> tickets = new BookingHandlerImpl().getTicketPerPurchase(Integer.valueOf(purchaseId));

		if (tickets.isEmpty()) {
			throw new CustomExceptions("Error!", "No tickets available.");
		}

		JSONArray ticketsArray = new JSONArray();

		for (Ticket ticket : tickets) {
			JSONObject responseObj = new JSONObject();

			responseObj.put("ticketId", ticket.getTicketId());
			responseObj.put("price", ticket.getPrice());
			responseObj.put("tax", ticket.getTax());
			responseObj.put("seats_seatNumber", ticket.getSeats_seatNumber());
			responseObj.put("seats_seatRow", ticket.getSeats_seatRow());
			responseObj.put("movie_name", ticket.getScreen().getMovie().getName());
			responseObj.put("venue_name", ticket.getScreen().getScreeningDates().getVenues().getName());
			responseObj.put("screening_date", ticket.getScreen().getScreeningDates().getScreeningDate().toString());
			responseObj.put("screen_screenId", ticket.getScreen().getScreenId());
			responseObj.put("movie_picture", ticket.getScreen().getMovie().getLarge_picture());
			responseObj.put("iMDB_url", ticket.getScreen().getMovie().getiMDB_url());

			ticketsArray.put(responseObj);
		}

		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("tickets", ticketsArray);

		return Response.ok().entity(jsonResponse.toString()).type(MediaType.APPLICATION_JSON).build();
	}
 
    //INFO: for web, and for iOS if BTAPIClient produces constant memory leak
    @GET
    @Path("/book/payment/clientToken")
	@Produces(MediaType.APPLICATION_JSON)
    public Response clientToken(
    		@Context HttpHeaders headers,
    		@Context HttpServletRequest request_) throws IOException {
	
    	clientToken = gateway.clientToken().generate();
    	JSONObject json = new JSONObject();

		if(clientToken != null) {
		json.put("clientToken", clientToken);
    	return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
   	 
		} else {
		return Response.ok().status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).build();
		}
	}
    
    @POST
    @Path("/book/payment/webcheckout")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response webcheckout(
    		@Context HttpHeaders headers,
    		@Context HttpServletRequest request_) throws IOException {
    	
    	System.out.println("Request recieved");
    	
    	nonce = request_.getParameter("payment_method_nonce_web");
    	for (Enumeration<?> e = request_.getParameterNames(); e.hasMoreElements();)

			while (e.hasMoreElements()) {
				String propertyKey = (String) e.nextElement();
				System.out.println("Request parameters: " + propertyKey);

			}
    	
   		//String clientToken = gateway.clientToken().generate();    		
    	//System.out.println(clientToken);

    	// Use payment method nonce here
    	TransactionRequest request = new TransactionRequest()
    		    .amount(new BigDecimal("10.00"))
    		    .paymentMethodNonce(nonce)
    		    .options()
    		      .submitForSettlement(true)
    		      .done();
    	
    	request.getKind();
    	
    	Result<Transaction> result = gateway.transaction().sale(request);
		Transaction transaction = result.getTarget();
		
		String AuthCode = "";
		String ResponseCode = "";
		String ResponseText = "";
		String Status = "";
		
		try {
		
			AuthCode = transaction.getProcessorAuthorizationCode();
			ResponseCode = transaction.getProcessorResponseCode();
			ResponseText = transaction.getProcessorResponseText();
			Status = gateway.transaction().find(transaction.getId()).getStatus().toString();
		
			} catch (Exception e) {
			
		}
		JSONObject json = new JSONObject();
		json.put("AuthCode", AuthCode);
		json.put("ResponseCode", ResponseCode);
		json.put("ResponseText", ResponseText);
		json.put("Status",Status);  
		json.put("Success", "true");
    	
    	return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON_TYPE).build();
   	  }

    /*
    @POST
	@Path("/book/{screeningDateId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getMovieTicket(
			@PathParam(value = "screeningDateId") int screeningDateId,
			String seatsToBeReserved) throws IOException {
		
		StringBuilder sb = new StringBuilder();
		InputStream ps = IOUtils.toInputStream(seatsToBeReserved, "UTF-8");
	    BufferedReader br = new BufferedReader(new InputStreamReader(ps));
	    
	    String str;
	    while( (str = br.readLine()) != null ){
	        sb.append(str);
	    }    
	    
		 JSONObject jObj = new JSONObject(sb.toString());
		    
	     String seats = jObj.getString("seatsToBeReserved");
	        
	     List<String> seatList = new ArrayList<>();
		 for (String seat: seats.split("-")){
			 	
			 if (!seat.isEmpty()) {
				 
				 seatList.add(seat);
			 
			 }
			 
		 }
		 
		
		
		List<Ticket> ticket =bookingHandler.returnTickets(screeningDateId, seatList, uuid);
		List<Seats> seats_ =bookingHandler.returnUpdatedseats(screeningDateId);
		//List<Seats> seats_ =bookingHandler.getSeatsForScreenForMovieOnVenue(screeningDateId);
		
		if (ticket != null && seats != null) {
			
			JSONObject json = new JSONObject();
			json.put("tickets", ticket);
			json.put("seatsforscreen", seats_);

		return	Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).build();	
		
		} else {
		
			return Response.status(404).build();
		}
	}*/
	
	@GET
	@Path("/book/movies/{name}/{order}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response searchMovies(
			@PathParam(value = "name") String name,
			@PathParam(value = "order") String order) throws InterruptedException {
		
		
		List<Movie> movies = new BookingHandlerImpl().searchMovies(name, order);
		
		if (movies != null) {
			
			if (movies.size() > 0) {
				
				JSONObject json = new JSONObject();

				for (int i = 0; i < movies.size(); i++) {
					
					HashMap<String,String> responseObj = new HashMap<String,String>();
					
					responseObj.put("movieId", String.valueOf(movies.get(i).getMovieId()));
					responseObj.put("name", movies.get(i).getName());
					responseObj.put("detail", movies.get(i).getDetail());
					responseObj.put("large_picture",movies.get(i).getLarge_picture());
					responseObj.put("thumbnail_picture",movies.get(i).getThumbnail_picture());
					responseObj.put("iMDB_url", movies.get(i).getiMDB_url());
					json.append("searchedMovies", responseObj);
					
				}
				
				return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).build();	
				
		} else {
			
			throw new CustomExceptions("Error!", "No results for the search.");
			
			}
		
		} 
		
		else  throw new CustomExceptions("Error!", "Wrong sorting argument");
	}
	

	@GET
	@Path("/book/movies/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fullTextSearchMovies(
			@QueryParam(value = "match") String match,
			@QueryParam(value = "setFirstResult") String setFirstResult,
    		@QueryParam(value = "category") String category) throws InterruptedException {
		
		if (match.length() < 3) {
			
			throw new CustomExceptions("Error!", "Search query has to be at least 3 characters long.");	
		
		}
		

		if(category == null) {
			category = "";
		}
		
		List<Movie> movies = bookingHandler.fullTextSearchMovies(match, category, Integer.parseInt(setFirstResult));
		
		if (!movies.isEmpty()) {
						
			JSONObject json = new JSONObject();

			for (int i = 0; i < movies.size(); i++) {
				
				HashMap<String,String> responseObj = new HashMap<String,String>();
				
				responseObj.put("movieId", String.valueOf(movies.get(i).getMovieId()));
				responseObj.put("name", movies.get(i).getName());
				responseObj.put("detail", movies.get(i).getDetail());
				responseObj.put("large_picture",movies.get(i).getLarge_picture());
				responseObj.put("thumbnail_picture",movies.get(i).getThumbnail_picture());
				responseObj.put("iMDB_url", movies.get(i).getiMDB_url());
				json.append("searchedMovies", responseObj);
				
			}
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).build();		
		
		} else {
			
			HashMap<String,String> responseObj = new HashMap<String,String>();
			JSONObject json = new JSONObject();
			json.append("NotFoundMovies", "No such movie(s)");
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).build();
			
			}
	}


	@GET
	@Path("/book/movies")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getAllMovies(
    		@Context HttpHeaders headers,
    		@Context HttpServletResponse response,
    		@Context HttpServletRequest request,
    		@Context ServletContext contex) throws InterruptedException {
		
		//TODO PBI: use the variable X-Token and deviceId.
		List<Movie> movies = new BookingHandlerImpl().getAllMovies(-1, "");

		if (movies.size() > 0) {
			
			JSONObject json = new JSONObject();

			for (int i = 0; i < movies.size(); i++) {
				
				HashMap<String,String> responseObj = new HashMap<String,String>();
				
				responseObj.put("movieId", String.valueOf(movies.get(i).getMovieId()));
				responseObj.put("name", movies.get(i).getName());
				responseObj.put("detail", movies.get(i).getDetail());
				responseObj.put("large_picture",movies.get(i).getLarge_picture());
				responseObj.put("thumbnail_picture",movies.get(i).getThumbnail_picture());
				responseObj.put("iMDB_url", movies.get(i).getiMDB_url());
				json.append("movies", responseObj);
				
			}
			
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			JSONObject json = new JSONObject();
			json.put("NotFoundMovies", "EndOfFile:)");
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		}
	}
	
	@GET
	@Path("/book/movies/paging")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getAllMoviesWithPaging (
    		@Context HttpHeaders headers,
    		@Context HttpServletResponse response,
    		@Context HttpServletRequest request,
    		@Context ServletContext contex,
    		@QueryParam(value = "setFirstResult") String setFirstResult,
    		@QueryParam(value = "category") String category) throws InterruptedException {
		
		ciphertext = headers.getRequestHeader("X-Device").get(0);
		c = aesUtil.encrypt(SALT, IV, APIKEY, ciphertext);
		NewCookie nc = new NewCookie("ApiKey", c.trim(), request.getContextPath(), null, null, 1800, true);
		
		if(setFirstResult == null) {
			setFirstResult = "-1";
		}
		if(category == null) {
			category = "";
		}
		
		List<Movie> movies = new BookingHandlerImpl().getAllMovies(Integer.parseInt(setFirstResult), category);
		if (movies.size() > 0) {
			
			JSONObject json = new JSONObject();

			for (int i = 0; i < movies.size(); i++) {
				
				HashMap<String,String> responseObj = new HashMap<String,String>();
				
				responseObj.put("movieId", String.valueOf(movies.get(i).getMovieId()));
				responseObj.put("name", movies.get(i).getName());
				responseObj.put("detail", movies.get(i).getDetail());
				responseObj.put("large_picture",movies.get(i).getLarge_picture());
				responseObj.put("thumbnail_picture",movies.get(i).getThumbnail_picture());
				responseObj.put("iMDB_url", movies.get(i).getiMDB_url());
				json.append("movies", responseObj);
				
			}
			
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).header("APIKEY", c).lastModified(Date.from(Instant.now())).cookie(nc).build();
		
		} else {
			JSONObject json = new JSONObject();
			json.put("NotFoundMovies", "EndOfFile:)");
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).header("APIKEY", c).lastModified(Date.from(Instant.now())).cookie(nc).build();			
		}
	}
	
	@GET
	@Path("/book/venue/{movieId}")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getVenuesForMovie(
			@PathParam(value = "movieId") int movieId) throws InterruptedException {
				
		List<Venues> venue = new BookingHandlerImpl().getVenues(movieId);
		
		if (venue != null) {
			
			JSONObject json = new JSONObject();  
			json.put("venues", venue);
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			
			throw new CustomExceptions("Error!", "No venues available.");
			
		}
	}
	
	@GET
	@Path("/book/venue/v2/{movieId}")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getLocationForMovie(
			@PathParam(value = "movieId") int movieId) throws InterruptedException {
				
		List<Location> locations = new BookingHandlerImpl().getLocationForMovie(movieId);
		
		if (!locations.isEmpty()) {
			
			JSONObject json = new JSONObject();  
			json.put("locations", locations);
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			
			throw new CustomExceptions("Error!", "No locations available.");
			
		}
	}
	
	@GET
	@Path("/book/venue/movies")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getMoviesForVenue(
			@QueryParam(value = "locationId") int locationId) throws InterruptedException {
				
		

		List<Movie> movies =bookingHandler.getMoviesForVenue(locationId);
	    List<Venues> venue =bookingHandler.getVenueByLocation(locationId);
		
		if (movies != null && venue != null) {
			
			JSONObject json = new JSONObject();  
			
			for (int i = 0; i < movies.size(); i++) {
				
				HashMap<String,String> responseObj = new HashMap<String,String>();
				
				responseObj.put("movieId", String.valueOf(movies.get(i).getMovieId()));
				responseObj.put("name", movies.get(i).getName());
				responseObj.put("detail", movies.get(i).getDetail());
				responseObj.put("large_picture",movies.get(i).getLarge_picture());
				responseObj.put("thumbnail_picture",movies.get(i).getThumbnail_picture());
				responseObj.put("iMDB_url", movies.get(i).getiMDB_url());
				json.append("movies", responseObj);
				
			}
			
			json.put("venue", venue);
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			
			throw new CustomExceptions("Error!", "No movies or venue available.");
			
		}
	}
	

	@GET
	@Path("/book/locations/venue")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getLocationForVenue(
			@QueryParam(value = "venuesId") int venuesId) throws InterruptedException {
				
		Location location = new BookingHandlerImpl().getLocationForVenue(venuesId);
		
		if (location != null) {
						
			return Response.ok().entity(location).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			
			throw new CustomExceptions("Error!", "There is no location or venue data available.");
			
		}
	}
	
	
	@GET
	@Path("book/locations")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getAllLocation() throws InterruptedException {
				
		List<Location> locations = bookingHandler.getAllLocations();
		
		if (locations != null) {
			
			JSONObject json = new JSONObject();  
			json.put("locations", locations);
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			
			throw new CustomExceptions("Error", "No locations available.");
			
		}
	}
	
	@GET
	@Path("/book/admin/moviesonvenues")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getAllMoviesOnAllVenues() throws InterruptedException {
				
		List<Venues> venues = new BookingHandlerImpl().getAllVenuesForUpdate();
		
		if (venues != null) {
			JSONObject json = new JSONObject();  

			for (int i = 0; i < venues.size(); i++) {
				HashMap<String,String> responseObj = new HashMap<String,String>();

				responseObj.put("movie", venues.get(i).getScreen().getMovie().getName());
				responseObj.put("movieId", String.valueOf(venues.get(i).getScreen().getMovie().getMovieId()));
				responseObj.put("category", venues.get(i).getScreen().getMovie().getCategory());
				responseObj.put("large_picture", venues.get(i).getScreen().getMovie().getLarge_picture());

				responseObj.put("ScreeningId", venues.get(i).getScreen_screenId());
				responseObj.put("venueId", String.valueOf(venues.get(i).getVenuesId()));
				responseObj.put("venue", String.valueOf(venues.get(i).getName()));
				responseObj.put("screeningDatesId", String.valueOf(venues.get(i).getScreen().getScreeningDates().getScreeningDatesId()));
				responseObj.put("date", String.valueOf(venues.get(i).getScreen().getScreeningDates().getScreeningDate()));
				json.append("venues", responseObj);

			}
			
			
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			
			throw new CustomExceptions("Error", "No locations available.");
			
		}
	}
	
	@GET
	@Path("/book/admin/moviesonvenuescategorized")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getAllMoviesOnAllVenuesWithCategory(
					@QueryParam(value = "category") String category) throws InterruptedException {
				
		List<Venues> venues = new BookingHandlerImpl().getAllVenuesForUpdate();
		
		if (venues != null) {
			JSONObject json = new JSONObject();  

			for (int i = 0; i < venues.size(); i++) {
				HashMap<String,String> responseObj = new HashMap<String,String>();

				if (venues.get(i).getScreen().getMovie().getCategory().equalsIgnoreCase(category)) {
				
				responseObj.put("movie", venues.get(i).getScreen().getMovie().getName());
				responseObj.put("movieId", String.valueOf(venues.get(i).getScreen().getMovie().getMovieId()));
				responseObj.put("category", venues.get(i).getScreen().getMovie().getCategory());
				responseObj.put("large_picture", venues.get(i).getScreen().getMovie().getLarge_picture());

				responseObj.put("ScreeningId", venues.get(i).getScreen_screenId());
				responseObj.put("venueId", String.valueOf(venues.get(i).getVenuesId()));
				responseObj.put("venue", String.valueOf(venues.get(i).getName()));
				responseObj.put("screeningDatesId", String.valueOf(venues.get(i).getScreen().getScreeningDates().getScreeningDatesId()));
				responseObj.put("date", String.valueOf(venues.get(i).getScreen().getScreeningDates().getScreeningDate()));
				json.append("venues", responseObj);
				}
			}
			
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			
			throw new CustomExceptions("Error", "No locations available.");
			
		}
	}
	
	@GET
	@Path("/book/admin/moviesonvenuessearch")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getAllMoviesOnAllVenuesForSearch(
					@QueryParam(value = "match") String match) throws InterruptedException {
				
		List<Venues> venues = new BookingHandlerImpl().getAllVenuesForUpdate();
		
		if (venues != null) {
			JSONObject json = new JSONObject();  

			for (int i = 0; i < venues.size(); i++) {
				HashMap<String,String> responseObj = new HashMap<String,String>();

				if (venues.get(i).getScreen().getMovie().getName().toLowerCase().contains(match.toLowerCase())) {
				
				responseObj.put("movie", venues.get(i).getScreen().getMovie().getName());
				responseObj.put("movieId", String.valueOf(venues.get(i).getScreen().getMovie().getMovieId()));
				responseObj.put("category", venues.get(i).getScreen().getMovie().getCategory());
				responseObj.put("large_picture", venues.get(i).getScreen().getMovie().getLarge_picture());

				responseObj.put("ScreeningId", venues.get(i).getScreen_screenId());
				responseObj.put("venueId", String.valueOf(venues.get(i).getVenuesId()));
				responseObj.put("venue", String.valueOf(venues.get(i).getName()));
				responseObj.put("screeningDatesId", String.valueOf(venues.get(i).getScreen().getScreeningDates().getScreeningDatesId()));
				responseObj.put("date", String.valueOf(venues.get(i).getScreen().getScreeningDates().getScreeningDate()));
				json.append("venues", responseObj);
				}
			}
			
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			
			throw new CustomExceptions("Error", "No locations available.");
			
		}
	}
	
	
	@GET
	@Path("/book/dates/{locationId}/{movieId}")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getDatesForMovieOnVenue (
			@PathParam(value = "locationId") int locationId,
			@PathParam(value = "movieId") int movieId) throws InterruptedException {
				
		List<ScreeningDates> dates = new BookingHandlerImpl().getScreeningDatesForMovieOnVenue(locationId, movieId);

		if (dates != null) {
			
			JSONObject json = new JSONObject();  
			json.put("dates", dates);
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			
			throw new CustomExceptions("Error!", "No screening dates available for the movie");
			
		}
	}
	
	@GET
	@Path("/book/seats/{screeningDateId}")
	@Produces(MediaType.APPLICATION_JSON)	
	public Response getSeatsForScreenForMovieOnVenue (
			@PathParam(value = "screeningDateId") int screeningDateId) throws InterruptedException {
				
		List<Seats> seats = new BookingHandlerImpl().getSeatsForScreenForMovieOnVenue(screeningDateId);
		
		if (seats != null) {
			
			JSONObject json = new JSONObject();  
			json.put("seatsforscreen", seats);
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).lastModified(Date.from(Instant.now())).build();
		
		} else {
			
			throw new CustomExceptions("Error!", "No seats available for the screening.");
			
		}
	}
	
	@POST
	@Path("/book/managepurchases")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cancelTicket(
			@Context HttpServletRequest request,
			@Context HttpHeaders headers) throws InterruptedException {
		
		ciphertext = headers.getRequestHeader("Ciphertext").get(0);
		
		if (!ciphertext.equals(request.getHeader("token2").toString())) {
			JSONObject json = new JSONObject();
			json.put("Error", "User not authorized!)");
			
			return Response.ok().entity(json.toString()).type(MediaType.APPLICATION_JSON).build();
		} 
		
		String purchaseId = request.getParameter("purchaseId").trim();
		String ticketsToBeCancelled = request.getParameter("ticketsToBeCancelled").trim();
	    JSONObject jsonObj = new JSONObject(ticketsToBeCancelled);
	    JSONArray ticketsList = (JSONArray) jsonObj.get("ticketIds");
	    
        List<Integer> ticketIds = new ArrayList<>();
		for (int i = 0; i < ticketsList.length(); i++) { 	    	
		    
	    	int ticketId = Integer.valueOf(ticketsList.get(i).toString());
	    	ticketIds.add(ticketId);
				 
			 }
	    
		
		boolean deleteTickets =bookingHandler.deleteTicket(ticketIds, Integer.valueOf(purchaseId));
		
		if(deleteTickets) {
			
			JSONObject json = new JSONObject();  
			json.put("Success", "true");
			
		return Response.ok().entity(json.toString()).build(); 

	} else {
	
		throw new CustomExceptions("Error!", "Something went wrong.");

			}
		
	}
	
	@POST
	@Path("/book/deletepurchases")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })	
	public Response deletePurchase(
			@Context HttpServletRequest request) throws InterruptedException {
		
		String purchaseId = request.getParameter("purchaseId").trim();
		    
		
		boolean deletePurchase =bookingHandler.deletePurchase(Integer.valueOf(purchaseId));
		
		if(deletePurchase) {
			
			JSONObject json = new JSONObject();  
			json.put("Success", "true");
			
		return Response.ok().entity(json.toString()).build(); 

	} else {
	
		throw new CustomExceptions("Error!", "Something went wrong.");

		}
	}
		
		@POST
		@Path("/book/admin/addscreen")
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })	
		public Response addScreen(
				@Context HttpServletRequest request, String newScreen) throws ParseException, IOException, InterruptedException {
			
			//String newScreen = request.getParameter("newScreen").trim();
		    //System.out.print(newScreen);

			StringBuilder sb = new StringBuilder();
			InputStream ps = IOUtils.toInputStream(newScreen);
		    BufferedReader br = new BufferedReader(new InputStreamReader(ps, StandardCharsets.UTF_8));
		    
		    String str;
		    while( (str = br.readLine()) != null ){
		        sb.append(str);
		    }    
		    
			 JSONObject jObj = new JSONObject(sb.toString());			    		        

		     String movie = jObj.getString("movie");
		     String date = jObj.getString("date");
		     String venue = jObj.getString("venue");
		     String nrOfRows_ = jObj.getString("nrOfRows");
		     String nrOfSeatsInRow_ = jObj.getString("nrOfSeatsInRow");
		     String ScreeningId = jObj.getString("ScreeningId");
		     String category = jObj.getString("category");

		     

		     int nrOfRows = Integer.parseInt(nrOfRows_);
		     int nrOfSeatsInRow = Integer.parseInt(nrOfSeatsInRow_);

			
			Screen newScreen_ =bookingHandler.addScreen(movie, date, venue, nrOfRows, nrOfSeatsInRow, ScreeningId, category);
			
			//String movieName = newScreen_.getMovie().getName();
			//String dateTime = newScreen_.getScreeningDates().getScreeningDate().toString();
			//String venueName = newScreen_.getScreeningDates().getVenues().getName();
			String screeningId = newScreen_.getScreenId();
			if(screeningId.contains("Error:")) {
				
				 JSONObject json = new JSONObject();			    		        
				 json.put("movie", movie);
				 json.put("date", date);
				 json.put("venue", venue);
				 json.put("ScreeningId", screeningId);

				return Response.ok().status(Status.CONFLICT).entity(json.toString()).build(); 
				
			} else {
			 
			 String ScreeningDatesId = Integer.toString(newScreen_.getScreeningDates().getScreeningDatesId());
			 JSONObject json = new JSONObject();			    		        
			 json.put("movie", movie);
			 json.put("date", date);
			 json.put("venue", venue);
			 json.put("ScreeningId", screeningId);
			 json.put("ScreeningDateId", ScreeningDatesId);

			 AddMovie event = new AddMovie();
			 event.movie = movie;
			 event.date = date;
			 event.venue = venue;
			 event.screeningId = screeningId;
			 event.screeningDatesId = ScreeningDatesId;

			 Serialization serialization = new Serialization();
			 String message = serialization.serializeAddMovie(event);
			 ExecutorService executor = Executors.newSingleThreadExecutor();
				executor.submit(() -> {
					KafkaMessageProducer producer = new KafkaMessageProducer();
					producer.sendMessage(message);
					}
				);

			return Response.ok().entity(json.toString()).build(); 
			}
		}

		@POST
		@Path("/book/admin/updatescreen")				
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })	
		public Response updateScreen(
				@Context HttpServletRequest request, String updateScreen) throws ParseException, IOException, InterruptedException {
			
			//String newScreen = request.getParameter("newScreen").trim();
		    //System.out.print(newScreen);

			StringBuilder sb = new StringBuilder();
			InputStream ps = IOUtils.toInputStream(updateScreen);
		    BufferedReader br = new BufferedReader(new InputStreamReader(ps, StandardCharsets.UTF_8));
		    
		    String str;
		    while( (str = br.readLine()) != null ){
		        sb.append(str);
		    }    
		    
			 JSONObject jObj = new JSONObject(sb.toString());			    		        

		     String moviesId = jObj.getString("movieId");
		     String Date = jObj.getString("date");
		     String Venue = jObj.getString("venue");
		     String venuesId = jObj.getString("venueId");
		     String screenId = jObj.getString("screenId");
		     String category = jObj.getString("category"); 
		     String screeningDatesId = jObj.getString("ScreeningDatesId");
		     

			
			Screen newScreen_ =bookingHandler.updateScreen(Venue,Integer.parseInt(venuesId), Integer.parseInt(screeningDatesId), Integer.parseInt(moviesId), screenId, Date, category);
			
			String screeningId = newScreen_.getScreenId();
			if(screeningId.contains("Error:")) {
				
				 JSONObject json = new JSONObject();			    		        
				 json.put("movie", moviesId);
				 json.put("date", Date);
				 json.put("venue", Venue);
				 json.put("ScreeningId", screeningId);

				return Response.ok().status(Status.CONFLICT).entity(json.toString()).build(); 
				
			} else {
			 
			 JSONObject json = new JSONObject();			    		        
			 json.put("movie", moviesId);
			 json.put("date", Date);
			 json.put("venue", Venue);
			 json.put("ScreeningId", screeningId);

			return Response.ok().entity(json.toString()).build(); 
			}
		}
		
		@DELETE
		@Path("/book/admin/deletescreen")				
		@Consumes(MediaType.APPLICATION_JSON)
		@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })	
		public Response deleteScreen(
				@Context HttpServletRequest request, String deleteScreen) throws ParseException, IOException, InterruptedException {
			
			//String newScreen = request.getParameter("newScreen").trim();
		    //System.out.print(newScreen);

			StringBuilder sb = new StringBuilder();
			InputStream ps = IOUtils.toInputStream(deleteScreen);
		    BufferedReader br = new BufferedReader(new InputStreamReader(ps, StandardCharsets.UTF_8));
		    
		    String str;
		    while( (str = br.readLine()) != null ){
		        sb.append(str);
		    }    
		    
			 JSONObject jObj = new JSONObject(sb.toString());			    		         
		     String screeningDatesId = jObj.getString("ScreeningDatesId");
		     

			
			boolean deleteScreen_ =bookingHandler.deleteScreen(Integer.parseInt(screeningDatesId));
			
			if(!deleteScreen_) {
				
				 JSONObject json = new JSONObject();			    		        
				 json.put("screeningDatesId", screeningDatesId);

				return Response.ok().status(Status.CONFLICT).entity(json.toString()).build(); 
				
			} else {
			 
			 JSONObject json = new JSONObject();			    		        
			 json.put("screeningDatesId", screeningDatesId);


			return Response.ok().entity(json.toString()).build(); 
			}
		}
		
	
}
