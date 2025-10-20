package com.jeet.api;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Seats implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2370784476844286877L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
    protected int seatId;
    protected String seatNumber;
    protected String seatRow;
    protected String isReserved;
    protected int price;
    protected double tax;
    @ManyToOne (fetch=FetchType.EAGER)
    @JoinColumn(name="screen_screenId")
    protected Screen screen;

    public Seats() {
    }
    
    public Seats(int seatId, String seatNumber, String seatRow, String isReserved, int price, double tax) {
    	this.seatId = seatId;
    	this.seatNumber = seatNumber;
    	this.seatRow = seatRow;
    	this.isReserved = isReserved;
    	this.price = price;
    	this.tax = tax;
    }

	/**
	 * @return the seatId
	 */
	public int getSeatId() {
		return seatId;
	}

	/**
	 * @param seatId the seatId to set
	 */
	public void setSeatId(int seatId) {
		this.seatId = seatId;
	}

	/**
	 * @return the seatNumber
	 */
	public String getSeatNumber() {
		return seatNumber;
	}

	/**
	 * @param seatNumber the seatNumber to set
	 */
	public void setSeatNumber(String seatNumber) {
		this.seatNumber = seatNumber;
	}

	/**
	 * @return the seatRow
	 */
	public String getSeatRow() {
		return seatRow;
	}

	/**
	 * @param seatRow the seatRow to set
	 */
	public void setSeatRow(String seatRow) {
		this.seatRow = seatRow;
	}

	/**
	 * @return the isReserved
	 */
	public String getIsReserved() {
		return isReserved;
	}

	/**
	 * @param isReserved the isReserved to set
	 */
	public void setIsReserved(String isReserved) {
		this.isReserved = isReserved;
	}

	/**
	 * @return the screen
	 */
	public Screen getScreen() {
		return screen;
	}

	/**
	 * @param screen the screen to set
	 */
	public void setScreen(Screen screen) {
		this.screen = screen;
	}

	/**
	 * @return the price
	 */
	public int getPrice() {
		return price;
	}

	/**
	 * @param price the price to set
	 */
	public void setPrice(int price) {
		this.price = price;
	}

	/**
	 * @return the tax
	 */
	public double getTax() {
		return tax;
	}

	/**
	 * @param tax the tax to set
	 */
	public void setTax(double tax) {
		this.tax = tax;
	}
    
    
}
