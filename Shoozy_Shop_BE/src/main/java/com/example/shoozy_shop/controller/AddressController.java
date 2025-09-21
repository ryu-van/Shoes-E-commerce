package com.example.shoozy_shop.controller;

import com.example.shoozy_shop.dto.request.NewAddressRequest;
import com.example.shoozy_shop.dto.request.UpdateAddressRequest;
import com.example.shoozy_shop.dto.response.AddressResponse;
import com.example.shoozy_shop.exception.ApiResponse;
import com.example.shoozy_shop.model.Address;
import com.example.shoozy_shop.service.IAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/addresses")
public class AddressController {

    private final IAddressService addressService;

    // POST /api/v1/addresses
    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(@Valid @RequestBody NewAddressRequest newAddressRequest) {
        AddressResponse address = addressService.createAddress(newAddressRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(address.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success("Address created successfully",address));
    }

    // PUT /api/v1/addresses/{address_id}
    @PutMapping("/{address_id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable("address_id") Long addressId,
            @Valid @RequestBody UpdateAddressRequest updateAddressRequest) {
        AddressResponse address = addressService.updateAddress(updateAddressRequest, addressId);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", address));
    }

    // GET /api/v1/addresses?user_id=123
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAllAddresses(@RequestParam("user_id") Long userId) {
        List<AddressResponse> addresses = addressService.getAllAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success("List of addresses retrieved successfully", addresses));
    }

    // GET /api/v1/addresses/{address_id}
    @GetMapping("/{address_id}")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(@PathVariable("address_id") Long addressId) {
        AddressResponse address = addressService.getAddressById(addressId);
        return ResponseEntity.ok(ApiResponse.success("Address retrieved successfully", address));
    }

    // DELETE /api/v1/addresses/{address_id}
    @DeleteMapping("/{address_id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable("address_id") Long addressId) {
        addressService.deleteAddress(addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    // PUT /api/v1/addresses/{address_id}/select?user_id=123
    @PutMapping("/{address_id}/select")
    public ResponseEntity<ApiResponse<Void>> setAddressSelected(
            @PathVariable("address_id") Long addressId,
            @RequestParam("user_id") Long userId) {
        addressService.setSelected(addressId, userId);
        return ResponseEntity.ok(ApiResponse.success("Address set selected successfully", null));
    }

    // GET /api/v1/addresses/selected?user_id=123
    @GetMapping("/selected")
    public ResponseEntity<ApiResponse<?>> getSelectedAddress(@RequestParam("user_id") Long userId) {
        return addressService.getSelectedAddress(userId)
                .<ResponseEntity<ApiResponse<?>>>map(ar ->
                        ResponseEntity.ok(ApiResponse.success("Selected address retrieved successfully", ar))
                )
                .orElseGet(() ->
                        ResponseEntity.ok(ApiResponse.success("No selected address for this user", null))
                );
    }
}
