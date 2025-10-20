package com.jeet.api;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Date;


@Entity
public class ScreeningDates implements Serializable {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 7748466763090880917L;
		
		@GeneratedValue(strategy=GenerationType.IDENTITY)
		@Id
	    protected int screeningDatesId;
		@Temporal(TemporalType.TIMESTAMP)
	    protected Date screeningDate;
	    /**
	     * about the OneToOne relationship:
	     * We don't want to create the list of venues as a list at an index, 
	     * but we create a json array (basically a list) where each index represents a venue, 
	     * that will make up our list by screeningDate, and so on...
	     */
	    @OneToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
	    @JoinColumn(name = "venues_venuesId", nullable = false, updatable = true, insertable = true)
	    protected Venues venues;
	    
	    @Transient
	    protected String venues_picture;
	    protected int movieId;
	    
	    public ScreeningDates() {
	    }
	    
	    public ScreeningDates(int screeningDatesId, Date screeningDate, int movieId, String venues_picture) {
	    	this.screeningDatesId = screeningDatesId;
	    	this.screeningDate = screeningDate;
	    	this.movieId = movieId;
	    }

		/**
		 * @return the screeningDatesId
		 */
		public int getScreeningDatesId() {
			return screeningDatesId;
		}

		/**
		 * @param screeningDatesId the screeningDatesId to set
		 */
		public void setScreeningDatesId(int screeningDatesId) {
			this.screeningDatesId = screeningDatesId;
		}

		/**
		 * @return the screeningDate
		 */
		public Date getScreeningDate() {
			return screeningDate;
		}

		/**
		 * @param screeningDate the screeningDate to set
		 */
		public void setScreeningDate(Date screeningDate) {
			this.screeningDate = screeningDate;
		}

		/**
		 * @return the venues
		 */
		public Venues getVenues() {
			return venues;
		}

		/**
		 * @param venues the venues to set
		 */
		public void setVenues(Venues venues) {
			this.venues = venues;
		}

		/**
		 * @return the movieId
		 */
		public int getMovieId() {
			return movieId;
		}

		/**
		 * @param movieId the movieId to set
		 */
		public void setMovieId(int movieId) {
			this.movieId = movieId;
		}

		/**
		 * @return the venues_picture
		 */
		public String getVenues_picture() {
			return venues_picture;
		}

		/**
		 * @param venues_picture the venues_picture to set
		 */
		public void setVenues_picture(String venues_picture) {
			this.venues_picture = venues_picture;
		}
}
