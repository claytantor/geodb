package com.welocally.geodb.web.mvc;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.db.JsonDatabase;
import com.welocally.geodb.services.db.JsonDatabase.StatusType;

@Controller
@RequestMapping("/dynamo/1_0")
public class DynamoController extends AbstractJsonController {
	static Logger logger = Logger.getLogger(DynamoController.class);

	@Autowired
	@Qualifier("dynamoJsonDatabase")
	JsonDatabase jsonDatabase;

    @RequestMapping(value = "/{collection}/{id}", method = RequestMethod.PUT)
    public ModelAndView savePlacePublicPut(
    		@PathVariable String collection,
			@PathVariable String id,
			@RequestBody String requestJson, HttpServletRequest req){  
        
        ModelAndView  mav = new ModelAndView("mapper-result");
              
        try {
                JSONObject doc = 
                    new JSONObject(requestJson);
                doc.put("id", id);
                //put it in the public store
                jsonDatabase.put(doc, null, collection, id, JsonDatabase.EntityType.DOCUMENT, StatusType.PUBLISHED);
        } catch (JSONException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (DbException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        }  catch (RuntimeException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } 
        
        return mav;
    }
	@RequestMapping(value = "/{collection}/{id}", method = RequestMethod.GET)
	public ModelAndView get(@PathVariable String collection,
			@PathVariable String id,
			@RequestParam(required = false) String callback, Model m) {
		ModelAndView mav = null;
		if (StringUtils.isEmpty(callback))
			mav = new ModelAndView("mapper-result");
		else {
			mav = new ModelAndView("jsonp-mapper-result");
			mav.addObject("callback", callback);
		}

		try {
			JSONArray places = new JSONArray();
			JSONObject place = jsonDatabase.findById(collection, id);
			places.put(place);
			mav.addObject("mapperResult", places.toString());
		} catch (DbException e) {
			logger.error("could not get results");
			if (e.getExceptionType() == DbException.Type.OBJECT_NOT_FOUND) {
				mav.addObject("mapperResult", new JSONArray().toString());
			} else {
				mav.addObject("mapperResult", makeErrorsJson(e));
			}
		}
		return mav;
	}
}
