package com.jeet.api;

/**
 * Projection DTO for the trending-movies endpoint.
 * Holds aggregated booking data for a single movie over a time window.
 */
public class TrendingMovieRow {

    private final int movieId;
    private final String name;
    private final String thumbnail_picture;
    private final String large_picture;
    private final long bookedTickets;
    private final String lastBookingTime; // ISO-8601 or null

    public TrendingMovieRow(
            int movieId,
            String name,
            String thumbnail_picture,
            String large_picture,
            long bookedTickets,
            String lastBookingTime) {
        this.movieId = movieId;
        this.name = name;
        this.thumbnail_picture = thumbnail_picture != null ? thumbnail_picture : "";
        this.large_picture = large_picture != null ? large_picture : "";
        this.bookedTickets = bookedTickets;
        this.lastBookingTime = lastBookingTime;
    }

    public int getMovieId() { return movieId; }
    public String getName() { return name; }
    public String getThumbnail_picture() { return thumbnail_picture; }
    public String getLarge_picture() { return large_picture; }
    public long getBookedTickets() { return bookedTickets; }
    public String getLastBookingTime() { return lastBookingTime; }
}

