package com.example.shoozy_shop.service;

import com.example.shoozy_shop.model.Brand;

import java.util.List;

public interface IBrandService {
    List<Brand> getAllBrands();
    List<Brand> getActiveBrands();
    Brand getBrandById(Long id);
    Brand addBrand(Brand brand);
    Brand updateBrand(Long id, Brand brand);
    void deleteBrand(Long id);
    Boolean restoreBrand(Long id);
}
