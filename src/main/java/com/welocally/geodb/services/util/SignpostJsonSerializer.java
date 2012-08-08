package com.welocally.geodb.services.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.app.CommandException;
import com.welocally.geodb.services.app.CommandSupport;
import com.welocally.signpost4j.client.SignpostClientException;
import com.welocally.signpost4j.client.SignpostOffersClient;
import com.welocally.signpost4j.model.Deal;

@Component
public class SignpostJsonSerializer implements CommandSupport {
    
    static Logger logger = 
        Logger.getLogger(SignpostJsonSerializer.class);
    
    @Autowired SignpostOffersClient offersClient;
    
    @Autowired JsonObjectSerializer jsonObjectSerializer;
    
    @Autowired WelocallyJSONUtils welocallyJSONUtils;
    
    @Value("${signpost.feed.path:/affiliate/1286/deals.xml}")
    private String dealsFeedEndpoint;

    
    public void doCommand(JSONObject command) throws CommandException {
        try {
            logger.debug("get deals");
            List<Deal> deals = offersClient.getDealsFromFeed(dealsFeedEndpoint);
            saveJSON(command.getString("file"), deals);
            
            logger.debug("done");
            
            System.exit(0);
            
            
        } catch (SignpostClientException e) {
           logger.error("cannot get offers", e);
        } catch (JSONException e) {
           logger.error("cannot serialize offers", e);
        }
    }
    
    public void saveJSON(String fileName, List<Deal> deals)  {
        try {
            logger.debug("starting save");

            
            FileWriter writer = 
              new FileWriter(new File(fileName));
            
            for (Deal deal : deals) {
                JSONObject dealObj = new JSONObject(jsonObjectSerializer.serialize(deal));
                welocallyJSONUtils.updateSignpostDealToWelocally(dealObj);
                writer.write(dealObj.toString()+"\n");
            }
            writer.flush();
            writer.close();
                
        } catch (UnknownHostException e) {
            logger.error("cannot get offers", e);
        } catch (FileNotFoundException e) {
            logger.error("file problem", e);
        } catch (IOException e) {
            logger.error("io", e);
        } catch (JSONException e) {
            logger.error("json parse", e);
        } 
    }
    
    

}
