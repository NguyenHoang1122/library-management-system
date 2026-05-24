package com.librarymanagementsystem.service.shipping;

public interface ShippingService {
    double calculateDistance(String destinationAddress);
    double calculateShippingFee(double distanceInKm, int totalBooks);
}
