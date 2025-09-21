package com.example.shoozy_shop.service;

import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.Color;
import com.example.shoozy_shop.repository.ColorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorService implements IColorService {

    private final ColorRepository colorRepository;

    @Override
    public List<Color> getAllColors() {
        return colorRepository.findAll();
    }

    @Override
    public List<Color> getActiveColors() {
        return colorRepository.findAllByStatusTrue();
    }

    @Override
    public Color getColorById(Long id) {
        return colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("color", id));
    }

    @Override
    @Transactional
    public Color addColor(Color color) {
        if(colorRepository.existsByName(color.getName().toLowerCase())) {
            throw new IllegalArgumentException("Color " + color.getName() +" already exists!");
        }
        color.setStatus(true);
        return colorRepository.save(color);
    }

    @Override
    public Color updateColor(Long id, Color color) {
        Color existingColor = this.getColorById(id);
        existingColor.setName(color.getName());
        existingColor.setHexCode(color.getHexCode());
        existingColor.setStatus(color.getStatus());
        return colorRepository.save(existingColor);
    }

    @Override
    public void deleteColor(Long id) {
        Color existingColor = this.getColorById(id);
        existingColor.setStatus(false);
        colorRepository.save(existingColor);
    }

    @Override
    public Boolean restoreColor(Long id) {
        Color existingColor = this.getColorById(id);
        if(existingColor.getStatus()) {
            return false;
        }
        existingColor.setStatus(true);
        colorRepository.save(existingColor);
        return true;
    }
}
