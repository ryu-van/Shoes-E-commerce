package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.NewAddressRequest;
import com.example.shoozy_shop.dto.request.UpdateAddressRequest;
import com.example.shoozy_shop.dto.response.AddressResponse;
import com.example.shoozy_shop.model.Address;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public interface IAddressService {

    AddressResponse createAddress(NewAddressRequest addressRequest);
    AddressResponse updateAddress(UpdateAddressRequest updateAddressRequest, Long addressId);
    void deleteAddress(Long addressId);
    AddressResponse getAddressById(Long addressId);
    List<AddressResponse> getAllAddresses(Long userId);

    void setSelected(Long addressId,Long userId);

    @Transactional(readOnly = true)
    Optional<AddressResponse> getSelectedAddress(Long userId);
}
