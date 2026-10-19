
package tests;

import static org.testng.Assert.assertEquals;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import api.AuthenticationAPI;
import api.BookingAPI;
import builders.BookingBuilder;
import builders.PatchBuilder;
import dataprovider.BookingDataProvider;
import io.restassured.response.Response;
import models.Booking;
import validators.ResponseValidator;

public class PartialUpdateBookingTest {

    @Test(
            dataProvider = "partialUpdateBookingData",
            dataProviderClass = BookingDataProvider.class
    )
    public void verifyPartialUpdateBookingRequest(
            String firstName,
            String lastName,
            int price,
            String checkIn,
            String checkOut,
            boolean depositPaid,
            String additionalNeeds,
            String updatedFirstName) {

        int bookingId = 0;
        String token = null;

        try {
            // 1. 创建原始预约
            Booking originalBooking =
                    BookingBuilder.createBooking(
                            firstName,
                            lastName,
                            price,
                            checkIn,
                            checkOut,
                            depositPaid,
                            additionalNeeds
                    );

            bookingId =
                    BookingAPI.createBookingAndReturnId(originalBooking);

            // 2. 获取认证 Token
            token = AuthenticationAPI.generateToken();

            // 3. 构造 PATCH 请求，只修改 firstname
            Map<String, Object> patchRequest =
                    new PatchBuilder()
                            .addField("firstname", updatedFirstName)
                            .build();

            // 4. 发送局部更新请求
            Response response =
                    BookingAPI.patchBooking(
                            bookingId,
                            token,
                            patchRequest
                    );

            // 5. 校验响应
            ResponseValidator.validateStatusCode(response, 200);
            ResponseValidator.validateContentType(response);
            ResponseValidator.validateResponseTime(response);

            // 6. 反序列化响应数据
            Booking actualBooking = response.as(Booking.class);

            // 7. 验证 firstname 已更新
            assertEquals(
                    actualBooking.getFirstname(),
                    updatedFirstName,
                    "firstname 应更新为指定值"
            );

            // 8. 验证其他字段保持不变
            assertEquals(
                    actualBooking.getLastname(),
                    originalBooking.getLastname(),
                    "lastname 不应发生变化"
            );

            assertEquals(
                    actualBooking.getTotalprice(),
                    originalBooking.getTotalprice(),
                    "totalprice 不应发生变化"
            );

            assertEquals(
                    actualBooking.isDepositpaid(),
                    originalBooking.isDepositpaid(),
                    "depositpaid 不应发生变化"
            );

            assertEquals(
                    actualBooking.getAdditionalneeds(),
                    originalBooking.getAdditionalneeds(),
                    "additionalneeds 不应发生变化"
            );

            assertEquals(
                    actualBooking.getBookingdates().getCheckin(),
                    originalBooking.getBookingdates().getCheckin(),
                    "checkin 不应发生变化"
            );

            assertEquals(
                    actualBooking.getBookingdates().getCheckout(),
                    originalBooking.getBookingdates().getCheckout(),
                    "checkout 不应发生变化"
            );

            // 9. 验证响应 JSON Schema
            ResponseValidator.validateSchema(
                    response,
                    "schema/booking-schema.json"
            );

        } finally {
            // 10. 清理测试创建的预约
            if (bookingId > 0) {
                try {
                    if (token == null) {
                        token = AuthenticationAPI.generateToken();
                    }

                    Response deleteResponse =
                            BookingAPI.deleteBooking(bookingId, token);

                    Assert.assertEquals(
                            deleteResponse.getStatusCode(),
                            201,
                            "清理测试预约失败，bookingId=" + bookingId
                    );

                } catch (Exception e) {
                    throw new AssertionError(
                            "清理测试预约时发生异常，bookingId=" + bookingId,
                            e
                    );
                }
            }
        }
    }
}
