package com.example.shoozy_shop.service;

import com.example.shoozy_shop.model.Color;

import java.util.List;

public interface IColorService {
    List<Color> getAllColors();
    List<Color> getActiveColors();
    Color getColorById(Long id);
    Color addColor(Color color);
    Color updateColor(Long id, Color color);
    void deleteColor(Long id);
    Boolean restoreColor(Long id);
}
