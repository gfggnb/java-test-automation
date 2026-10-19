
package tests;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import api.AuthenticationAPI;
import api.BookingAPI;
import builders.BookingBuilder;
import builders.PatchBuilder;
import io.restassured.response.Response;
import models.Booking;
import models.BookingResponse;
import validators.ResponseValidator;

public class BookingWorkflowTest {

    private final String firstName = "Dishi";
    private final String lastName = "Gogia";
    private final int totalPrice = 3500;
    private final String checkIn = "2026-07-01";
    private final String checkOut = "2026-07-10";
    private final boolean depositPaid = true;
    private final String additionalNeeds = "Breakfast";

    @Test(groups = "workflow")
    public void verifyBookingWorkflow() {

        int bookingId = 0;
        String token = null;
        boolean deleteSucceeded = false;

        try {
            // 1. CREATE BOOKING
            Booking booking = BookingBuilder.createBooking(
                    firstName,
                    lastName,
                    totalPrice,
                    checkIn,
                    checkOut,
                    depositPaid,
                    additionalNeeds
            );

            Response createResponse = BookingAPI.createBooking(booking);

            ResponseValidator.validateStatusCode(createResponse, 200);
            ResponseValidator.validateContentType(createResponse);
            ResponseValidator.validateResponseTime(createResponse);
            ResponseValidator.validateSchema(
                    createResponse,
                    "schema/create-booking-response-schema.json"
            );

            BookingResponse bookingResponse =
                    createResponse.as(BookingResponse.class);

            ResponseValidator.validateBookingResponse(
                    booking,
                    bookingResponse
            );

            bookingId = bookingResponse.getBookingid();

            Assert.assertTrue(
                    bookingId > 0,
                    "创建预约后应返回有效的 bookingId"
            );

            // 2. GENERATE TOKEN
            token = AuthenticationAPI.generateToken();

            // 3. GET BOOKING
            Response getResponse = BookingAPI.getBooking(bookingId);

            ResponseValidator.validateStatusCode(getResponse, 200);
            ResponseValidator.validateContentType(getResponse);
            ResponseValidator.validateResponseTime(getResponse);
            ResponseValidator.validateSchema(
                    getResponse,
                    "schema/booking-schema.json"
            );

            Booking actualBooking = getResponse.as(Booking.class);

            ResponseValidator.validateBooking(booking, actualBooking);

            // 4. UPDATE BOOKING
            Booking updatedBooking = BookingBuilder.createBooking(
                    firstName + " Updated",
                    lastName,
                    totalPrice + 1000,
                    checkIn,
                    checkOut,
                    depositPaid,
                    "Dinner"
            );

            Response updateResponse = BookingAPI.updateBooking(
                    bookingId,
                    token,
                    updatedBooking
            );

            ResponseValidator.validateStatusCode(updateResponse, 200);
            ResponseValidator.validateContentType(updateResponse);
            ResponseValidator.validateResponseTime(updateResponse);
            ResponseValidator.validateSchema(
                    updateResponse,
                    "schema/booking-schema.json"
            );

            Booking updatedResponse = updateResponse.as(Booking.class);

            ResponseValidator.validateBooking(
                    updatedBooking,
                    updatedResponse
            );

            // 5. PATCH BOOKING
            Map<String, Object> patchRequest = new PatchBuilder()
                    .addField("firstname", "Patched Name")
                    .build();

            Response patchResponse = BookingAPI.patchBooking(
                    bookingId,
                    token,
                    patchRequest
            );

            ResponseValidator.validateStatusCode(patchResponse, 200);
            ResponseValidator.validateContentType(patchResponse);
            ResponseValidator.validateResponseTime(patchResponse);
            ResponseValidator.validateSchema(
                    patchResponse,
                    "schema/booking-schema.json"
            );

            Booking patchedBooking = patchResponse.as(Booking.class);

            Assert.assertEquals(
                    patchedBooking.getFirstname(),
                    "Patched Name"
            );

            Assert.assertEquals(
                    patchedBooking.getLastname(),
                    updatedBooking.getLastname()
            );

            Assert.assertEquals(
                    patchedBooking.getTotalprice(),
                    updatedBooking.getTotalprice()
            );

            Assert.assertEquals(
                    patchedBooking.getAdditionalneeds(),
                    updatedBooking.getAdditionalneeds()
            );

            // 6. DELETE BOOKING
            Response deleteResponse = BookingAPI.deleteBooking(
                    bookingId,
                    token
            );

            ResponseValidator.validateStatusCode(deleteResponse, 201);
            deleteSucceeded = true;

            // 7. VERIFY DELETION
            Response deletedResponse = BookingAPI.getBooking(bookingId);

            ResponseValidator.validateStatusCode(deletedResponse, 404);

        } finally {
            // 8. 异常时尝试清理尚未成功删除的预约
            if (bookingId > 0 && !deleteSucceeded) {
                try {
                    if (token == null) {
                        token = AuthenticationAPI.generateToken();
                    }

                    Response cleanupResponse =
                            BookingAPI.deleteBooking(bookingId, token);

                    int statusCode = cleanupResponse.getStatusCode();

                    Assert.assertTrue(
                            statusCode == 201 || statusCode == 404,
                            "清理预约失败，bookingId=" + bookingId
                                    + "，HTTP 状态码=" + statusCode
                    );

                } catch (Exception e) {
                    throw new AssertionError(
                            "工作流测试清理预约时发生异常，bookingId="
                                    + bookingId,
                            e
                    );
                }
            }
        }
    }
}
