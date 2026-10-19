#!/usr/bin/env python3
"""
Local mock API for the REST Assured booking test framework.
This is a test double, not the real Restful-Booker service.
Uses only Python's standard library.
"""
import json
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Lock
from urllib.parse import urlparse

HOST = "127.0.0.1"
PORT = 8081
TOKEN = "local-test-token"
_bookings = {}
_next_id = 1
_lock = Lock()


class BookingHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args))

    def _send_json(self, status, payload):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Connection", "close")
        self.end_headers()
        self.wfile.write(body)
        self.close_connection = True

    def _read_json(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
            raw = self.rfile.read(length) if length else b"{}"
            print("DEBUG raw bytes:", repr(raw))
            print("DEBUG content length:", length)
            return json.loads(raw.decode("utf-8-sig"))
        except Exception as exc:
            print("DEBUG JSON parse error:", repr(exc))
            return None
    def _has_token(self):
        cookie = self.headers.get("Cookie", "")
        print("DEBUG Cookie received:", repr(cookie))
        print("DEBUG Expected token:", repr(TOKEN))
        result = any(
            part.strip() == "token=" + TOKEN
            for part in cookie.split(";")
       )
        print("DEBUG Token validation:", result)
        return result

    def do_POST(self):
        global _next_id
        path = urlparse(self.path).path

        if path == "/auth":
            body = self._read_json()
            if not isinstance(body, dict):
                return self._send_json(400, {"reason": "Invalid JSON"})
            if body.get("username") == "admin" and body.get("password") == "password123":
                return self._send_json(200, {"token": TOKEN})
            return self._send_json(200, {"reason": "Bad credentials"})

        if path == "/booking":
            booking = self._read_json()
            print("DEBUG booking:", repr(booking))
            print("DEBUG types:", {k: type(v).__name__ for k, v in booking.items()} if isinstance(booking, dict) else type(booking).__name__)
            if not self._valid_booking(booking):
                return self._send_json(400, {"error": "Invalid booking payload"})
            with _lock:
                booking_id = _next_id
                _next_id += 1
                _bookings[booking_id] = booking
            return self._send_json(200, {"bookingid": booking_id, "booking": booking})

        return self._send_json(404, {"error": "Not found"})

    def do_GET(self):
        path = urlparse(self.path).path
        match = re.fullmatch(r"/booking/(\d+)", path)
        if not match:
            return self._send_json(404, {"error": "Not found"})
        booking_id = int(match.group(1))
        with _lock:
            booking = _bookings.get(booking_id)
        if booking is None:
            return self._send_json(404, {"error": "Booking not found"})
        return self._send_json(200, booking)

    def do_PUT(self):
        path = urlparse(self.path).path
        match = re.fullmatch(r"/booking/(\d+)", path)
        if not match:
            return self._send_json(404, {"error": "Not found"})
        if not self._has_token():
            return self._send_json(403, {"error": "Forbidden"})
        booking = self._read_json()
        if not self._valid_booking(booking):
            return self._send_json(400, {"error": "Invalid booking payload"})
        booking_id = int(match.group(1))
        with _lock:
            if booking_id not in _bookings:
                return self._send_json(404, {"error": "Booking not found"})
            _bookings[booking_id] = booking
        return self._send_json(200, booking)

    def do_PATCH(self):
        path = urlparse(self.path).path
        match = re.fullmatch(r"/booking/(\d+)", path)
        if not match:
            return self._send_json(404, {"error": "Not found"})
        if not self._has_token():
            return self._send_json(403, {"error": "Forbidden"})
        patch = self._read_json()
        if not isinstance(patch, dict):
            return self._send_json(400, {"error": "Invalid JSON"})
        booking_id = int(match.group(1))
        with _lock:
            existing = _bookings.get(booking_id)
            if existing is None:
                return self._send_json(404, {"error": "Booking not found"})
            updated = dict(existing)
            for key, value in patch.items():
                if key == "bookingdates" and isinstance(value, dict):
                    dates = dict(updated.get("bookingdates", {}))
                    dates.update(value)
                    updated["bookingdates"] = dates
                else:
                    updated[key] = value
            if not self._valid_booking(updated):
                return self._send_json(400, {"error": "Invalid booking payload"})
            _bookings[booking_id] = updated
        return self._send_json(200, updated)

    def do_DELETE(self):
        path = urlparse(self.path).path
        match = re.fullmatch(r"/booking/(\d+)", path)
        if not match:
            return self._send_json(404, {"error": "Not found"})
        if not self._has_token():
            return self._send_json(403, {"error": "Forbidden"})
        booking_id = int(match.group(1))
        with _lock:
            if booking_id not in _bookings:
                return self._send_json(404, {"error": "Booking not found"})
            del _bookings[booking_id]
        self.send_response(201)
        self.send_header("Content-Length", "0")
        self.send_header("Connection", "close")
        self.end_headers()
        self.close_connection = True

    @staticmethod
    def _valid_booking(booking):
        if not isinstance(booking, dict):
            return False
        required = ("firstname", "lastname", "totalprice", "depositpaid", "bookingdates")
        if not all(key in booking for key in required):
            return False
        dates = booking.get("bookingdates")
        return (
            isinstance(booking.get("firstname"), str)
            and isinstance(booking.get("lastname"), str)
            and isinstance(booking.get("totalprice"), int)
            and booking.get("totalprice") >= 0
            and isinstance(booking.get("depositpaid"), bool)
            and isinstance(dates, dict)
            and isinstance(dates.get("checkin"), str)
            and isinstance(dates.get("checkout"), str)
            and ("additionalneeds" not in booking or isinstance(booking["additionalneeds"], str))
        )


if __name__ == "__main__":
    server = ThreadingHTTPServer((HOST, PORT), BookingHandler)
    print(f"Local mock booking API listening on http://{HOST}:{PORT}")
    print("Press Ctrl+C to stop.")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping local mock API...")
    finally:
        server.server_close()
