package gov.usgs.wqp.ogcproxy.geo;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class JsonObjectResponseHandler implements ResponseHandler<JsonObject> {

	@Override
	public JsonObject handleResponse(HttpResponse response) throws IOException {
		StatusLine statusLine = response.getStatusLine();
		HttpEntity entity = response.getEntity();

		if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}

		if (entity == null) {
			throw new HttpResponseException(HttpStatus.SC_NOT_FOUND, "Requested Object Not Found");
		} else {
			Gson gson = new GsonBuilder().create();
			return gson.fromJson(EntityUtils.toString(entity), JsonObject.class);
		}
	}

}
