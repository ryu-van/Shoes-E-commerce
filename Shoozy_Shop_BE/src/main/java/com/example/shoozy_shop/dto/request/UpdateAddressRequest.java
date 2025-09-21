package com.example.shoozy_shop.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class UpdateAddressRequest {

    @JsonProperty("province_code")
    private Long provinceCode;

    @JsonProperty("province_name")
    private String provinceName;

    @JsonProperty("district_code")
    private Long districtCode;

    @JsonProperty("district_name")
    private String districtName;

    @JsonProperty("ward_code")
    private String wardCode;

    @JsonProperty("ward_name")
    private String wardName;

    @JsonProperty("address_detail")
    private String addressDetail;

    @JsonProperty("is_selected")
    private Boolean isSelected;

}
