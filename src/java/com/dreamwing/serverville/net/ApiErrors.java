package com.dreamwing.serverville.net;

import io.netty.handler.codec.http.HttpResponseStatus;

public enum ApiErrors {

	UNKNOWN_API(1, "Unknown api call"),
	BAD_AUTH(2, "Invalid authentication", HttpResponseStatus.FORBIDDEN),
	NOT_AUTHED(3, "Authentication required", HttpResponseStatus.FORBIDDEN),
	BAD_HTTP_METHOD(4, "HTTP method not allowed", HttpResponseStatus.METHOD_NOT_ALLOWED),
	NOT_FOUND(5, "Not found", HttpResponseStatus.NOT_FOUND),
	FORBIDDEN(6, "Forbidden", HttpResponseStatus.FORBIDDEN),
	HTTP_DECODE_ERROR(7, "Unable to decode HTTP request"),
	INTERNAL_SERVER_ERROR(8, "Internal server error, sorry. :(", HttpResponseStatus.INTERNAL_SERVER_ERROR),
	JSON_ERROR(9, "Error decoding JSON"),
	DB_ERROR(10, "There was a (hopefully) temporary database error", HttpResponseStatus.SERVICE_UNAVAILABLE),
	MISSING_INPUT(11, "Missing required request parameters"),
	INVALID_INPUT(12, "Supplied paramter is not a valid value"),
	CONCURRENT_MODIFICATION(13, "Update failed due to another update happening at the same time"),
	INVALID_CONTENT(14, "Invalid content in HTTP body"),
	JAVASCRIPT_ERROR(15, "Error parsing or executing javascript"),
	DATA_CONVERSION(16, "Can't convert the supplied value to the correct type"),
	JSON_ENCODE_ERROR(17, "Error encoding JSON. This shouldn't happen."),
	INVALID_IP_RANGE(18, "Couldn't parse the supplied IP range"),
	SESSION_EXPIRED(19, "Your session has expired, call ValidateSession to renew it", HttpResponseStatus.FORBIDDEN),
	INVITE_REQUIRED(20, "An invite code is required to create a new account"),
	INVALID_INVITE_CODE(21, "This invite code is not valid"),
	INVALID_COUNTRY_CODE(22, "The country code was not in the ISO 3166 recognized list"),
	INVALID_URL(23, "The supplied url could not be parsed"),
	RESIDENT_ID_TAKEN(24, "This ID is already in use by a different resident"),
	INTERRUPTED(25, "Operation interrupted by network issue or server crash"),
	NO_INDEXES(26, "No Lucene index appender as been added to Log4j"),
	
	INVALID_QUERY(100, "Invalid lucene query"),
	INVALID_KEY_NAME(101, "Invalid key name"),
	ALREADY_REGISTERED(102, "Account already registered"),
	NOT_IN_CHANNEL(103, "User has not joined that channel"),
	PRIVATE_DATA(104, "This data cannot be read by the current user"),
	CHANNEL_ID_TAKEN(105, "This channel ID is already taken by something other than a channel"),
	USER_NOT_PRESENT(106, "The user does not have a two-way presence connection to the server"),
	ALREADY_JOINED(107, "The user has already joined this channel"),
	CURRENCY_LIMIT(108, "The requested currency operation would put the user under the minimum or over the maximum"),
	CURRENCY_OVERFLOW(109, "The requested currency operation would cause the user's currency balance to wrap around"),
	ANON_NOT_ALLOWED(110, "The request is not allowed for an anonymous user"),
	
	CHARGE_ERROR(200, "Could not create a charge to purchase a product"),
	
	UNKNOWN(Integer.MAX_VALUE, "Unknown error");
	
	private final int Code;
	private final String Message;
	private final HttpResponseStatus HttpStatus;
	
	ApiErrors(int c, String m)
	{
		Code = c;
		Message = m;
		HttpStatus = HttpResponseStatus.BAD_REQUEST;
	}
	
	ApiErrors(int c, String m, HttpResponseStatus status)
	{
		Code = c;
		Message = m;
		HttpStatus = status;
	}
	
	public int getCode() { return Code; }
	public String getMessage() { return Message; }
	public HttpResponseStatus getHttpStatus() { return HttpStatus; }
}
