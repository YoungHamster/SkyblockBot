package com.viktorx.skyblockbot.tgBot;


import com.viktorx.skyblockbot.SkyblockBot;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

public class ResponseHandler implements HttpClientResponseHandler<String> {

    public static final ResponseHandler INSTANCE = new ResponseHandler();

    @Override
    public String handleResponse(ClassicHttpResponse response) throws HttpException {
        //Get the status of the response
        int status = response.getCode();

        SkyblockBot.LOGGER.info("Got response from tgbot. Code: " + status);

        if (status == 200) {
            if(response.containsHeader("value")) {
                return response.getHeader("value").getValue();
            } else {
                return Integer.toString(response.getCode());
            }
        } else {
            return null;
        }
    }
}
