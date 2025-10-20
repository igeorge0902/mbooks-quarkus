package com.jeet.api;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Cacheable
public class Venues implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7854505308888367355L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
    protected int venuesId;
    protected String name;
    protected String address;
    protected String contact;
    protected String venues_picture;
    
    @Column(insertable=false, updatable=false)
    protected String screen_screenId;
    
    @OneToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name = "screen_screenId")
    protected Screen screen;
    
    @ManyToOne(cascade={CascadeType.PERSIST, CascadeType.REFRESH}, fetch=FetchType.EAGER)
    @JoinColumn(name = "location_locationId")
    protected Location location;
    
    @Transient
    protected int locationId;
	
    public Venues() {  	
    }
    
    public Venues(int venuesId, String name, String address, String contact, String venues_picture, String screen_screenId) { 
    	
    	this.venuesId = venuesId;
    	this.name = name;
    	this.address = address;
    	this.contact = contact;
    	this.venues_picture = venues_picture;
    	this.screen_screenId = screen_screenId;
    	
    }
    
    /**
	 * @return the venuesId
	 */
	public int getVenuesId() {
		return venuesId;
	}
	/**
	 * @param venuesId the venuesId to set
	 */
	public void setVenuesId(int venuesId) {
		this.venuesId = venuesId;
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
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * @return the contact
	 */
	public String getContact() {
		return contact;
	}
	/**
	 * @param contact the contact to set
	 */
	public void setContact(String contact) {
		this.contact = contact;
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
	
	/**
	 * @return the screen_screenId
	 */
	public String getScreen_screenId() {
		return screen_screenId;
	}

	/**
	 * @param screen_screenId the screen_screenId to set
	 */
	public void setScreen_screenId(String screen_screenId) {
		this.screen_screenId = screen_screenId;
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
     * Gets the value of the movie property.
     * 
     * @return
     *     possible object is
     *     {@link Location }
     *     
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the value of the movie property.
     * 
     * @param value
     *     allowed object is
     *     {@link Location }
     *     
     */
    public void setLocation(Location value) {
        this.location = value;
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

    
}
