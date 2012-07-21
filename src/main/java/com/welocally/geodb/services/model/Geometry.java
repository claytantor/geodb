package com.welocally.geodb.services.model;

//{"type": "Point", "coordinates": [131.041488647, -12.5230064392]}
public class Geometry {
	private String type;
		
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	//0=lng 1=lat
	private Double[] coordinates;

	public Double[] getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(Double[] coordinates) {
		this.coordinates = coordinates;
	}
	
}
