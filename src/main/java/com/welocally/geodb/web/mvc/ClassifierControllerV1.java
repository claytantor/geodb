package com.welocally.geodb.web.mvc;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.db.DynamoJsonDatabase;
import com.welocally.geodb.services.db.IdGen;


@Controller
@RequestMapping("/classifier/1_0")
public class ClassifierControllerV1 extends AbstractJsonController {
	
	static Logger logger = 
		Logger.getLogger(PlaceControllerV1.class);
	
	@Autowired 
	@Qualifier("dynamoJsonDatabase")
	DynamoJsonDatabase jsonDatabase;
	
	@Autowired IdGen idGen; 
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ModelAndView get(@PathVariable String id, @RequestParam(required=false) String callback, Model m){
        ModelAndView mav = null;
        if(StringUtils.isEmpty(callback))
            mav = new ModelAndView("mapper-result");
        else {
            mav = new ModelAndView("jsonp-mapper-result");
            mav.addObject(
                    "callback", 
                    callback);
        }
	        
		try {
			JSONObject classifier = jsonDatabase.findById("classifiers", id);
			mav.addObject("mapperResult", classifier.toString());
			
		} catch (DbException e) {
			logger.error("could not get results");
			mav.addObject("mapperResult", makeErrorsJson(e));
		} 
		return mav;
	}
	
	@RequestMapping(value = "/types", method = RequestMethod.GET)
	public ModelAndView getTypes(@RequestParam(required=false) String callback, Model m){
        
	    ModelAndView mav = null;
        if(StringUtils.isEmpty(callback))
            mav = new ModelAndView("mapper-result");
        else {
            mav = new ModelAndView("jsonp-mapper-result");
            mav.addObject(
                    "callback", 
                    callback);
        }
        
		try {
			JSONObject example = new JSONObject();
			example.put("type", "Type");
			
			JSONArray types = jsonDatabase.findTypes();

			mav.addObject("mapperResult", types.toString());
			
		} catch (DbException e) {
			logger.error("could not get results");
			mav.addObject("mapperResult", makeErrorsJson(e));
		} catch (JSONException e) {
			logger.error("could not get results");
			mav.addObject("mapperResult", makeErrorsJson(e));
		} 
		return mav;
	}
	
	@RequestMapping(value = "/categories", method = RequestMethod.GET)
	public ModelAndView getCategories(@RequestParam String type, @RequestParam(required=false) String callback, Model m){
	    ModelAndView mav = null;
        if(StringUtils.isEmpty(callback))
            mav = new ModelAndView("mapper-result");
        else {
            mav = new ModelAndView("jsonp-mapper-result");
            mav.addObject(
                    "callback", 
                    callback);
        }
        
		try {
			
			JSONArray types = jsonDatabase.findCatgories(type);

			mav.addObject("mapperResult", types.toString());
			
		} catch (DbException e) {
			logger.error("could not get results");
			mav.addObject("mapperResult", makeErrorsJson(e));
		} 
		return mav;
	}
	
	@RequestMapping(value = "/subcategories", method = RequestMethod.GET)
	public ModelAndView getSubCategories(@RequestParam String type, @RequestParam String category, @RequestParam(required=false) String callback, Model m){
	    ModelAndView mav = null;
        if(StringUtils.isEmpty(callback))
            mav = new ModelAndView("mapper-result");
        else {
            mav = new ModelAndView("jsonp-mapper-result");
            mav.addObject(
                    "callback", 
                    callback);
        }
		try {			
			JSONArray types = jsonDatabase.findSubcatgories(type, category);
			mav.addObject("mapperResult", types.toString());
		} catch (DbException e) {
			logger.error("could not get results");
			mav.addObject("mapperResult", makeErrorsJson(e));
		} 
		return mav;
	}
	
	

}
