package com.jeet.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.json.JSONArray;
import org.json.JSONObject;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.CreditCardVerification;
import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Environment;
import com.braintreegateway.PaymentMethod;
import com.braintreegateway.PaymentMethodRequest;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import com.jeet.api.Location;
import com.jeet.api.Movie;
import com.jeet.api.Purchase;
import com.jeet.api.Screen;
import com.jeet.api.ScreeningDates;
import com.jeet.api.Seats;
import com.jeet.api.Ticket;
import com.jeet.api.Venues;
import com.jeet.db.DAO;
import com.jeet.db.HibernateUtil;
import com.jeet.service.BookingHandlerImpl;
import org.hibernate.cache.spi.Region.*;

public class HibernateTest {
	
	private static volatile int seatId;
	private static volatile String seatNumber;
	private static volatile String seatRow;
	private static volatile String isReserved;
	
    private static BraintreeGateway gateway = new BraintreeGateway(
  		  Environment.SANDBOX,
    		  "j3ndqpzrhy4gp2p7",
    		  "rzmyrsbswb3hwsmk",
    		  "37113dbf6dc015806f510e7e630755fb"
  		);

	public static void main(String[] args) throws Exception {
		
		/*
		double sum_ = 330.6600000000001;
		String s = String.format("%.2f", sum_);

		BigDecimal bg = new BigDecimal(s);
		System.out.println(bg);

    	String clientToken = gateway.clientToken().generate();
		
    	JSONObject json = new JSONObject();
		json.put("clientToken", clientToken);
		*/
		
		BookingHandlerImpl bh = new BookingHandlerImpl();
		//List<Location> venues = DAO.instance().getLocationForMovie(1075);

	//	Purchase purchase_ = bh.getBrainTreeCustomerId("65f63602-6ddf-11e5-8441-71caa0c5f788");
	//	Purchase purchase = bh.setBrainTreeCustomerId("11111", 238);
		//Screen screen = DAO.instance().insertNewScreen("Dangerous Minds", "2021-02-10 22:00:00", "Uránia Filmszínház", 6, 6, "T60", "Romance");
		//ScreeningDates date = DAO.instance().insertNewScreen();
	//	List<Movie> movies0 = bh.getAllMovies(0, "");
	//	List<Movie> movies1 = bh.getAllMovies(0, "");
	//	List<Movie> movies2 = bh.getAllMovies(0, "");
		//System.out.print(movies.size());
		//DAO.instance().updateScreen();
		//DAO.instance().insertNewScreen();
		
		//for (int i = 246; i < 248; i++) {
		//	DAO.instance().deleteScreen(i);

		//}
		//DAO.instance().deleteScreen(208);

	//	JSONObject json = new JSONObject();
	//	json.put("movies", movies);
	//	System.out.println(json.toString());
		
	//	List<Movie> moviesForVenue = bh.getMoviesForVenue(10);
	//    List<Venues> venue = bh.getVenueByLocation(10);
	//	System.out.println(venue.toString());

		
	//	List<Location> location = bh.getAllLocations();
	  
    //  List<Venues> venue = bh.getVenueByLocation(10);
	//	List<Movie> movies = bh.fullTextSearchMovies("Angry","", 0);

	//	List<Integer> ticketIds = new ArrayList<Integer>();
	//	ticketIds.add(78);
	//	ticketIds.add(82);
		
		bh.deletePurchase(176);
		
	/*
      	List<Purchase> purchases = bh.getAllPurchases("65f63602-6ddf-11e5-8441-71caa0c5f788");
      	
		JSONObject json = new JSONObject();
		
		for (int i = 0; i < purchases.size(); i++) {
			
			JSONObject responseObj = new JSONObject();
			
			responseObj.put("purchaseId", String.valueOf(purchases.get(i).getPurchaseId()));
			responseObj.put("orderId", purchases.get(i).getOrderId());
			responseObj.put("movieName", purchases.get(i).getTicketsForPurchase().get(0).getScreen().getMovie().getName());
			responseObj.put("movieName", purchases.get(i).getTicketsForPurchase().get(0).getScreen().getMovie().getLarge_picture());
			responseObj.put("time", purchases.get(i).getTime());
			
			responseObj.put("screeningDate", purchases.get(i).getTicketsForPurchase().get(0).getScreen().getScreeningDates().getScreeningDate());
			
			json.append("purchases", responseObj);
		
		}
		
		System.out.println(json);
	*/
		
//		List<String> list = new ArrayList<>();
//		list.add("B4");
//		list.add("A2");

	//	List<Ticket> allTickets = bh.returnTickets(3, list, "65f63602-6ddf-11e5-8441-71caa0c5f788", "1507984914547");		
/*
		List<Integer> ticketIds1 = new ArrayList<Integer>();	
		List<Integer> ticketIds_ = allTickets.stream().map(e -> e.getTicketId()).collect(Collectors.toList());			
		ticketIds1.addAll(ticketIds_);
		
		bh.deleteTicket(ticketIds1, allTickets.get(0).getPurchase().getPurchaseId());
*/
//		bh.deleteTicket(ticketIds, 67);

		
	//	System.out.println(ticket.get(0).getMovie_name());
	//	List<ScreeningDates> dates = bh.getScreeningDatesForMovieOnVenue(2, 1002);
		
//		Venues ven = dates.get(0).getVenues();
//		String pic = ven.getVenues_picture();
		//Movie movies8 = bh.getMovie(1002);
		//List<Seats> seats = bh.returnUpdatedseats(3);
		//List<Seats> seats = bh.getSeatsForScreenForMovieOnVenue(2);
		//bh.deleteTicket(37);
		//bh.deleteTicket(17);
		//List<Movie> movies = bh.getAllMovies();

		//JSONObject json = new JSONObject();
		//HashMap<String,String> responseObjPayment = null;

		//for (int i = 0; i < movies.size(); i++) {
			
			
		//	responseObjPayment = new HashMap<String,String>();
			
		//	responseObjPayment.put("AuthCode", movies.get(i).getiMDB_url());
		//	responseObjPayment.put("ResponseCode", movies.get(i).getName());
			//responseObjPayment.put("ResponseText", ResponseText);
			//responseObjPayment.put("Status",Status);
		//	json.append("Receipt", responseObjPayment);
			
	//	}
		
		
		/*
		List<String> list = new ArrayList<>();
		list.add("B4");
		
		List<Ticket> tickets = bh.returnTickets(2, list);
		*/
		JSONObject json = new JSONObject();
	//	json.put("fullTextMovies", movies);
	//	json.put("movies", movies0);

		
		//System.out.println(json.toString());
         
		/*
		String newScreen = "{\"movie\":\"3\",\"venue\":\"Snatch.\",\"date\":\"2020-10-01 12:00:00\",\"ScreeningId\":\"T13\",\"nrOfSeatsInRow\":\"Puskin Mozi\",\"nrOfRows\":\"10\"}";
		StringBuilder sb = new StringBuilder();
		InputStream ps = IOUtils.toInputStream(newScreen, "UTF-8");
	    BufferedReader br = new BufferedReader(new InputStreamReader(ps));
	    
	    String str;
	    while( (str = br.readLine()) != null ){
	        sb.append(str);
	    }    
	    
		 JSONObject jObj = new JSONObject(sb.toString());			    		        

	     String movie = jObj.getString("movie");
	     String date = jObj.getString("date");
	     */
		
	    // SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    // Date date_ =  formattedDate.parse("2020-10-01 12:00:00");
	    // System.out.println(new Date());

	    // System.out.println(date_);
		System.out.print(true);

	}
	
	private void brainTree () {
		
		String AuthCode = "";
		String ResponseCode = "";
		String ResponseText = "";
		String Status = "";
		BigDecimal Amount = null;
		BigDecimal TaxAmount = null;
		String CVS = "";
		
		CustomerRequest requestC = new CustomerRequest().customerId("65f63602-6ddf-11e5-8441-71caa0c5f788");
			Result<Customer> resultR = gateway.customer().create(requestC);

			resultR.isSuccess();
			// true

		String R = resultR.getTarget().getId();
		
		PaymentMethodRequest request_ = new PaymentMethodRequest()
				  .customerId(R)
				  .paymentMethodNonce("fake-valid-visa-nonce")
				  .options()
				    .verifyCard(true)
				    .verificationAmount("5101.89")
				    .done();
		
		Result<? extends PaymentMethod> result_ = gateway.paymentMethod().create(request_);

		if (!result_.isSuccess()) {
		// false
		
		System.out.println(Boolean.valueOf(result_.isSuccess()));


		CreditCardVerification verification = result_.getCreditCardVerification();
		//com.braintreegateway.CreditCardVerification.Status VerificationStatus = verification.getStatus();
		//String VS = VerificationStatus.name();
		System.out.println(verification.getStatus());
		// "PROCESSOR_DECLINED"
		
		verification.getGatewayRejectionReason();
		// GatewayRejectionReason.CVV
		
		verification.getProcessorResponseCode();
		// 2000
		
		verification.getProcessorResponseText();
		// Do Not Honor
		
		} else {
		
		
		
		TransactionRequest request = new TransactionRequest()
				/*
				.creditCard()
				    .number("4111111111111111")
				    .expirationDate("05/2020")
				    .cvv("123")
				    .cardholderName("Card Holder")
				    .done()
				 */
				.merchantAccountId("testcompany")
    		    .amount(new BigDecimal(Double.toString(5101.89)))
    		    .taxAmount(new BigDecimal(Double.toString(100.00)))
    		    .paymentMethodNonce("fake-valid-visa-nonce")
    		    //.purchaseOrderNumber("1507984914546")
    		    .options()
    		      .submitForSettlement(true)
    		      .done();
    	
    	request.getKind();
    	
    	System.out.print(request.getKind());
    	
    	Result<Transaction> result = gateway.transaction().sale(request);
		Transaction transaction = result.getTarget();		
		
			// TODO: add error cases
			AuthCode = transaction.getProcessorAuthorizationCode().trim();
			ResponseCode = transaction.getProcessorResponseCode().trim();
			ResponseText = transaction.getProcessorResponseText().trim();
			Amount = transaction.getAmount();
			TaxAmount = transaction.getTaxAmount();
			Status = gateway.transaction().find(transaction.getId()).getStatus().toString();
			CVS = transaction.getCvvResponseCode().trim();

		
		System.out.println(ResponseText);
		System.out.println(ResponseCode);
		System.out.println(Status);
		System.out.println(CVS);
		
		}
		
	}
	
}

/*
Object [] obj = movies3.toArray();
Object [] obs = null;
Object obb = null;

for (int i = 0; i < obj.length; i++) {
	obs = (Object[]) obj[i];

for (int j = 0; j < obs.length; j++) {
	
	Movie m = (Movie) obs[0];
	obb = obs[j]; 
System.out.println(obb);
	}

}
*/