package hack.ddakev.roadrant;
/**
 * Created by ferrerluis on 10/1/16.
 */
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Future;

public class PlateRecognizer {
    private static String alprUrl = "https://api.openalpr.com/v1/recognize?tasks=plate&country=us&secret_key=sk_cb754bf37e493856189ecd1f";

    public static void recognize(File img) {
        try {
            System.out.println("1");
            Future<HttpResponse<JsonNode>> future = Unirest.post(alprUrl)
                    .header("accept", "application/json")
                    .field("tasks", "plate")
                    .field("country", "us")
                    .field("secret_key", "sk_cb754bf37e493856189ecd1f")
                    .field("image", img)
                    .asJsonAsync(new Callback<JsonNode>() {

                        public void failed(UnirestException e) {
                            System.out.println("The request has failed");
                        }

                        public void completed(HttpResponse<JsonNode> response) {
                            int code = response.getStatus();
                            String res = jsonToString(response);
                            System.out.println(res);
                        }

                        public void cancelled() {
                            System.out.println("The request has been cancelled");
                        }

                    });
            /*HttpResponse<JsonNode> jsonResponse = Unirest.post("https://api.openalpr.com/v1/recognize?tasks=plate&country=us&secret_key=sk_cb754bf37e493856189ecd1f")
                    .queryString("tasks", "plate")
                    .queryString("country", "us")
                    .queryString("secret_key", "sk_cb754bf37e493856189ecd1f")
                    .field("image", img).asJson();*/
            System.out.println("2");
            //return jsonToString(jsonResponse);
        } catch(Exception e) {
            e.printStackTrace();
            //return null;
        }
    }

    private static String jsonToString(HttpResponse<?> jsonResponse) {
        try {
            JSONObject jsonObj = new JSONObject(jsonResponse.getBody().toString());
            String plate = jsonObj
                    .getJSONArray("array").getJSONObject(0) // Gets first item in array, which is the data (because Java is dumb)
                    .getJSONObject("plate") // Gets plate info
                    .getJSONArray("results").getJSONObject(0) // First result
                    .getString("plate"); // Plate number of first result
            return plate;
        } catch (JSONException e) {
            return null;
        }
    }
}
