package com.welocally.geodb.services.jmx;

import java.util.Calendar;
import java.util.Date;

import org.springframework.stereotype.Component;

@Component
public class IndexMonitor  {

	private Date start;
	
	private Long count=new Long(0);
	
	public Long getElapsed(){
		Date now = Calendar.getInstance().getTime();
		return new Long((now.getTime()-start.getTime())/1000);
	}
	
	public Integer getRate(){
		return Math.round(count.floatValue()/getElapsed().floatValue());
	}

	public void increment(){
		count++;
	}
	
	public Long getCount() {
		return count;
	}

	
	public void reset(){
		start = Calendar.getInstance().getTime();
		count = new Long(0);
	}


}
