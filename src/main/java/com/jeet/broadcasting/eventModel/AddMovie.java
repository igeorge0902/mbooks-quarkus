package com.jeet.broadcasting.eventModel;

import java.io.Serializable;

public class AddMovie implements Serializable {
	private static final long serialVersionUID = 1L;

	public String movie;
	public String date;
	public String venue;
	public String screeningId;
	public String screeningDatesId;
}