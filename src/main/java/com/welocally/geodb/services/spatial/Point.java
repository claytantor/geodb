package com.welocally.geodb.services.spatial;

public class Point {
	 private final double lat;
	    private final double lon;
	    private String name;
	    public Point(final double lat, final double lon) {
	        this.lat = lat;
	        this.lon = lon;
	    }
	 
	    public Point(final double lat, final double lon, final String name) {
	        this(lat, lon);
	        this.name = name;
	    }
	 
	    public double getLat() {
	        return lat;
	    }
	 
	    public double getLon() {
	        return lon;
	    }
	 
	    public String getName() {
	        return name;
	    }
}
