package com.jeet.api;

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
@Cacheable
public class Location implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 829065582280504436L;
	
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Id
    protected int locationId;
    protected String formatted_address;

    protected String name;
    protected double latitude;
    protected double longitude;
    protected String thumbnail;
    
    @OneToMany(mappedBy = "location", fetch=FetchType.LAZY)
    protected List<Venues> venues;

    public Location () {  	
    }
    
    public Location (int locationId, String formatted_address, String name, double latitude, double longitude) { 
    	this.locationId = locationId;
    	this.formatted_address = formatted_address;
    	this.name = name;
    	this.latitude = latitude;
    	this.longitude = longitude;
    }

	/**
	 * @return the locationId
	 */
	public int getLocationId() {
		return locationId;
	}

	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(int locationId) {
		this.locationId = locationId;
	}

	/**
	 * @return the formatted_address
	 */
	public String getFormatted_address() {
		return formatted_address;
	}

	/**
	 * @param formatted_address the formatted_address to set
	 */
	public void setFormatted_address(String formatted_address) {
		this.formatted_address = formatted_address;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the thumnbnail
	 */
	public String getThumbnail() {
		return thumbnail;
	}

	/**
	 * @param thumnbnail the thumnbnail to set
	 */
	public void setThumnbnail(String thumnbnail) {
		this.thumbnail = thumnbnail;
	}
    
}