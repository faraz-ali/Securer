package com.securer.controller.webHook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securer.model.AccessToken;
import com.securer.services.AccessTokenRepository;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Created by faraz on 12/25/15.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class CallBoxAPI implements Runnable {

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CallBoxAPI.class);

    private String boxUrl = "https://app.box.com/api/oauth2/token";

    @Value("${box.clientId}")
    private String clientId;

    @Value("${box.clientSecret}")
    private String clientSecret;
    private String code;

    @Autowired
    AccessTokenRepository accessTokenRepository;

    public CallBoxAPI() {

    }

    public CallBoxAPI(String code, AccessTokenRepository accessTokenRepository) {
        this.accessTokenRepository = accessTokenRepository;
        this.code = code;
    }

    @Override
    public void run() {
        try {
            logger.info("In the Request thread calling callback url: " + "In the Request thread calling callback url: ");

            HttpClient httpClient = HttpClientConfig.getConnection();
            post(httpClient);

        } catch (IOException e) {
            logger.error(e.getStackTrace());
            e.printStackTrace();
        }
        logger.info("Call back request completed successfully.");
    }


    private void post(HttpClient httpClient) throws IOException {
        logger.debug("In Thread Call Back initiating POST call back");

        HttpPost httpPost = new HttpPost(boxUrl);

        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
        nameValuePairs.add(new BasicNameValuePair("code", code));
        nameValuePairs.add(new BasicNameValuePair("client_id", clientId));
        nameValuePairs.add(new BasicNameValuePair("client_secret", clientSecret));


        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        HttpResponse response = httpClient.execute(httpPost);

        if (response.getStatusLine().getStatusCode() != 200) {
            logger.info("Failed response from call back url: \n"
                    + "Status code : " + response.getStatusLine().getStatusCode());


        } else {
            logger.info("successful response from call back url: \n"
                    + "Status code : " + response.getStatusLine().getStatusCode());
            save(response);

        }
    }

    public void save(HttpResponse response) {
        ObjectMapper mapper = new ObjectMapper();
        AccessToken accessToken;
        try {
            accessToken = mapper.readValue(response.getEntity().getContent(), AccessToken.class);
            logger.info(accessToken.toString());
            System.out.println(accessToken.toString());
            accessToken.setCreatedDate(new Date());
            accessTokenRepository.save(accessToken);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * For future enhancements.
     *
     * @param httpClient
     * @throws IOException
     */
    private void get(HttpClient httpClient) throws IOException {

        HttpGet httpGet = new HttpGet(boxUrl);

        HttpResponse response = httpClient.execute(httpGet);
    }
}
