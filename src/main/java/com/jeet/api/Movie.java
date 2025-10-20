package com.jeet.api;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Cacheable
public class Movie implements Serializable {

	private static final long serialVersionUID = 829065582280504436L;

	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Id
	protected int movieId;

	protected String name;

	protected String detail;

	protected String thumbnail_picture;
	protected String large_picture;
	protected String iMDB_url;
	protected String category;
	/**
	 * List of screen of one movie, one movie to many screening(s)
	 */
    @OneToMany(mappedBy = "movie", fetch=FetchType.LAZY)
    protected List<Screen> screens;

    public Movie () {  	
    }
    
    public Movie(int movieId, String name, String detail, String thumbnail_picture, String large_picture, String iMDB_url) {
    	this.movieId = movieId;
    	this.name = name;
    	this.thumbnail_picture = thumbnail_picture;
    	this.large_picture = large_picture;
    	this.iMDB_url = iMDB_url;
    }
    
    /**
     * Gets the value of the movieId property.
     * 
     */
    public int getMovieId() {
        return movieId;
    }

    /**
     * Sets the value of the movieId property.
     * 
     */
    public void setMovieId(int value) {
        this.movieId = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the detail property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDetail() {
        return detail;
    }

    /**
     * Sets the value of the detail property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDetail(String value) {
        this.detail = value;
    }

	/**
	 * @return the thumbnail_picture
	 */
	public String getThumbnail_picture() {
		return thumbnail_picture;
	}

	/**
	 * @param thumbnail_picture the thumbnail_picture to set
	 */
	public void setThumbnail_picture(String thumbnail_picture) {
		this.thumbnail_picture = thumbnail_picture;
	}

	/**
	 * @return the iMDB_url
	 */
	public String getiMDB_url() {
		return iMDB_url;
	}

	/**
	 * @param iMDB_url the iMDB_url to set
	 */
	public void setiMDB_url(String iMDB_url) {
		this.iMDB_url = iMDB_url;
	}

	/**
	 * @return the large_picture
	 */
	public String getLarge_picture() {
		return large_picture;
	}

	/**
	 * @param large_picture the large_picture to set
	 */
	public void setLarge_picture(String large_picture) {
		this.large_picture = large_picture;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}
    
    
    /*
    public static void main( String[] args )
    {
        AnnotationConfiguration config = new AnnotationConfiguration();
        config.addAnnotatedClass(Movie.class );
        config.configure();
        new SchemaExport(config).create(true, true);
    }*/
}


