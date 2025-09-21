package com.example.shoozy_shop.dto.response;

import com.example.shoozy_shop.model.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;

    private Long provinceCode;

    private String provinceName;

    private Long districtCode;

    private String districtName;

    private String wardCode;

    private String wardName;

    private String addressDetail;

    private Boolean isSelected;

    public AddressResponse(Long id,
                          String provinceName,
                          String districtName,
                          String wardName,
                          String addressDetail,
                          Boolean isSelected) {
        this.id = id;
        this.provinceName = provinceName;
        this.districtName = districtName;
        this.wardName = wardName;
        this.addressDetail = addressDetail;
        this.isSelected = isSelected;
    }






}
