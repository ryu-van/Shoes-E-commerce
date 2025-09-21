package com.example.shoozy_shop.service;

import com.example.shoozy_shop.model.Size;

import java.util.List;

public interface ISizeService {
    List<Size> getAllSizes();
    List<Size> getActiveSizes();
    Size getSizeById(Long id);
    Size addSize(Size size);
    Size updateSize(Long id, Size size);
    void deleteSize(Long id);
    Boolean restoreBrand(Long id);
}
