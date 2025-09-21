package com.example.shoozy_shop.service;

import com.example.shoozy_shop.dto.request.NewAddressRequest;
import com.example.shoozy_shop.dto.request.UpdateAddressRequest;
import com.example.shoozy_shop.dto.response.AddressResponse;
import com.example.shoozy_shop.model.Address;
import com.example.shoozy_shop.model.User;
import com.example.shoozy_shop.repository.AddressRepository;
import com.example.shoozy_shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Transactional
public class AddressService implements IAddressService {
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public AddressResponse createAddress(NewAddressRequest req) {
        if (req == null) throw new IllegalArgumentException("Request is null");

        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + req.getUserId()));
            addressRepository.unselectAllByUser(user.getId()); // chỉ unselect khi cần set mặc định mới
        Address address = Address.builder()
                .provinceCode(req.getProvinceCode())
                .provinceName(req.getProvinceName())
                .districtCode(req.getDistrictCode())
                .districtName(req.getDistrictName())
                .wardCode(req.getWardCode())
                .wardName(req.getWardName())
                .addressDetail(req.getAddressDetail())
                .isSelected(true)
                .user(user)
                .build();

        addressRepository.save(address);

        return AddressResponse.builder()
                .id(address.getId())
                .provinceCode(address.getProvinceCode())
                .provinceName(address.getProvinceName())
                .districtCode(address.getDistrictCode())
                .districtName(address.getDistrictName())
                .wardCode(address.getWardCode())
                .wardName(address.getWardName())
                .addressDetail(address.getAddressDetail())
                .isSelected(address.getIsSelected())
                .build();
    }

    @Override
    public AddressResponse updateAddress(UpdateAddressRequest req, Long addressId) {
        if (req == null) throw new IllegalArgumentException("Request is null");

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));

        if (Boolean.TRUE.equals(req.getIsSelected())) {
            addressRepository.unselectOthers(address.getUser().getId(), address.getId());
            address.setIsSelected(true);
        } else if (req.getIsSelected() != null) {
            address.setIsSelected(false);
        }

        address.setProvinceCode(req.getProvinceCode());
        address.setProvinceName(req.getProvinceName());
        address.setDistrictCode(req.getDistrictCode());
        address.setDistrictName(req.getDistrictName());
        address.setWardCode(req.getWardCode());
        address.setWardName(req.getWardName());
        address.setAddressDetail(req.getAddressDetail());
        addressRepository.save(address);

        return AddressResponse.builder()
                .id(address.getId())
                .provinceCode(address.getProvinceCode())
                .provinceName(address.getProvinceName())
                .districtCode(address.getDistrictCode())
                .districtName(address.getDistrictName())
                .wardCode(address.getWardCode())
                .wardName(address.getWardName())
                .addressDetail(address.getAddressDetail())
                .isSelected(address.getIsSelected())
                .build();
    }

    @Override
    public void deleteAddress(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));
        addressRepository.delete(address);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with id: " + addressId));
        return AddressResponse.builder().id(address.getId())
                .provinceCode(address.getProvinceCode())
                .provinceName(address.getProvinceName())
                .districtCode(address.getDistrictCode())
                .districtName(address.getDistrictName())
                .wardCode(address.getWardCode())
                .wardName(address.getWardName())
                .addressDetail(address.getAddressDetail())
                .isSelected(address.getIsSelected())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAllAddresses(Long userId) {
        List<Address> addresses = addressRepository.findAllByUser_Id(userId);
        if (addresses == null || addresses.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return addresses.stream()
                .map(a -> AddressResponse.builder()
                        .id(a.getId())
                        .provinceCode(a.getProvinceCode())
                        .provinceName(a.getProvinceName())
                        .districtCode(a.getDistrictCode())
                        .districtName(a.getDistrictName())
                        .wardCode(a.getWardCode())
                        .wardName(a.getWardName())
                        .addressDetail(a.getAddressDetail())
                        .isSelected(Boolean.TRUE.equals(a.getIsSelected()))
                        .build()
                )
                .toList();
    }


    @Override
    public void setSelected(Long addressId, Long userId) {
        if (!addressRepository.existsByIdAndUser_Id(addressId, userId)) {
            throw new RuntimeException("Address does not belong to user");
        }
        addressRepository.unselectAllByUser(userId);
        int updated = addressRepository.markSelected(addressId, userId);
        if (updated == 0) {
            throw new RuntimeException("Failed to mark selected");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<AddressResponse> getSelectedAddress(Long userId) {
        return addressRepository.findSelectedProjectionByUserId(userId, PageRequest.of(0, 1))
                .stream().findFirst();
    }
}
