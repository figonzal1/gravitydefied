package com.figonzal.gravitydefied.API;

public interface ResponseHandler {

	public void onResponse(Response response);

	public void onError(APIException error);

}
