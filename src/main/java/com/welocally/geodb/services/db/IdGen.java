package com.welocally.geodb.services.db;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Calendar;

import org.springframework.stereotype.Component;

import com.welocally.geodb.services.spatial.Point;

@Component
public class IdGen {

	private SecureRandom random = new SecureRandom();

	public String genBasic(int len) {
		return new BigInteger(130, random).toString(32).substring(0, len);
	}
	
	//WL_2uPWew1lZQQWByErSOUo95_-12.523060_131.041473@1303236331
	//WL_ouek0frrbumpraggamro8a_-12.496260_131.045792@1324448257
	public String genPoint(String prefix, Point p) {	
		String base = new BigInteger(130, random).toString(32).substring(0, "2uPWew1lZQQWByErSOUo95".length());
		DecimalFormat pFormat = new DecimalFormat("#0.000000");
		return prefix+base+"_"
			+pFormat.format(p.getLat()).toString()+"_"+
			pFormat.format(p.getLon()).toString()+"@"+
			Calendar.getInstance().getTimeInMillis()/1000;
	}
	
	
	

}
