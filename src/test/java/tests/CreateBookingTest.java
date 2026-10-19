package tests;

import org.testng.annotations.Test;
import api.BookingAPI;
import builders.BookingBuilder;
import dataprovider.BookingDataProvider;
import io.restassured.response.Response;
import models.Booking;
import models.BookingResponse;
import validators.ResponseValidator;



import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

import endpoints.Routes;
import specifications.RequestSpecificationFactory;
import org.testng.Assert;




public class CreateBookingTest {

	@Test(dataProvider="bookingData",dataProviderClass=BookingDataProvider.class)
	public void createBooking( String firstName,String lastName,int price,String checkIn,String checkOut,boolean depositPaid,
	        String additionalNeeds) {
			
	
	Booking booking= BookingBuilder.createBooking(firstName,lastName,price,checkIn,checkOut,depositPaid,additionalNeeds);
	Response response =BookingAPI.createBooking(booking);
	BookingResponse bookingResponse =response.as(BookingResponse.class);
	ResponseValidator.validateBookingResponse(booking,bookingResponse);
	ResponseValidator.validateStatusCode(response,200);
	ResponseValidator.validateContentType(response);
	ResponseValidator.validateNotNullField(response, "bookingid");
	ResponseValidator.validateResponseTime(response);
	ResponseValidator.validateBookingId(response);
	ResponseValidator.validateSchema(response,"schema/create-booking-response-schema.json");
	
	}






	@Test
    public void createBookingWithoutFirstName() {
        Map<String, Object> booking = new HashMap<>();

         booking.put("lastname", "TestUser");
         booking.put("totalprice", 100);
         booking.put("depositpaid", true);

        Map<String, String> dates = new HashMap<>();
        dates.put("checkin", "2026-10-20");
        dates.put("checkout", "2026-10-25");

        booking.put("bookingdates", dates);
        booking.put("additionalneeds", "Breakfast");

        assertInvalidBooking(booking);
    }




	
    @Test
    public void createBookingWithInvalidTotalPrice() {
        Map<String, Object> booking = new HashMap<>();

        booking.put("firstname", "Tom");
        booking.put("lastname", "Lee");
        booking.put("totalprice", "abc");
        booking.put("depositpaid", true);

        Map<String, String> dates = new HashMap<>();
        dates.put("checkin", "2026-10-20");
        dates.put("checkout", "2026-10-25");
        booking.put("bookingdates", dates);

        booking.put("additionalneeds", "Breakfast");

        assertInvalidBooking(booking);
    }



	
    @Test
    public void createBookingWithoutCheckin() {
        Map<String, Object> booking = new HashMap<>();

        booking.put("firstname", "Tom");
        booking.put("lastname", "Lee");
        booking.put("totalprice", 100);
        booking.put("depositpaid", true);

        Map<String, String> dates = new HashMap<>();
        dates.put("checkout", "2026-10-25");
        booking.put("bookingdates", dates);

        booking.put("additionalneeds", "Breakfast");

        assertInvalidBooking(booking);
    }



	
    private void assertInvalidBooking(Map<String, Object> booking) {
        Response response = given()
                .spec(RequestSpecificationFactory.requestSpec())
                .body(booking)
                .when()
                .post(Routes.CREATE_BOOKING);

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(
            response.jsonPath().getString("error"),
            "Invalid booking payload"
        );
    }



    @Test
    public void createBookingWithNegativePrice() {
        Map<String, Object> booking = new HashMap<>();

        booking.put("firstname", "Tom");
        booking.put("lastname", "Lee");
        booking.put("totalprice", -100);
        booking.put("depositpaid", true);
        booking.put("bookingdates", Map.of(
                "checkin", "2026-10-20",
                "checkout", "2026-10-25"
        ));
        booking.put("additionalneeds", "Breakfast");

        Response response = given()
                .spec(RequestSpecificationFactory.requestSpec())
                .body(booking)
                .when()
                .post(Routes.CREATE_BOOKING);

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(
                response.jsonPath().getString("error"),
                "Invalid booking payload"
        );
    }


    @Test
    public void createBookingWithZeroPrice() {
        Map<String, Object> booking = new HashMap<>();

        booking.put("firstname", "Tom");
        booking.put("lastname", "Lee");
        booking.put("totalprice", 0);
        booking.put("depositpaid", true);
        booking.put("bookingdates", Map.of(
                "checkin", "2026-10-20",
                "checkout", "2026-10-25"
        ));
        booking.put("additionalneeds", "Breakfast");

        Response response = given()
                .spec(RequestSpecificationFactory.requestSpec())
                .body(booking)
                .when()
                .post(Routes.CREATE_BOOKING);

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(
                response.jsonPath().getInt("booking.totalprice"),
                0
        );
    }



}


