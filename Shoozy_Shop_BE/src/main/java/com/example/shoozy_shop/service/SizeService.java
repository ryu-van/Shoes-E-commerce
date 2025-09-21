package com.example.shoozy_shop.service;

import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.Brand;
import com.example.shoozy_shop.model.Size;
import com.example.shoozy_shop.repository.SizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SizeService implements ISizeService {

    private final SizeRepository sizeRepository;

    @Override
    public List<Size> getAllSizes() {
        return sizeRepository.findAll();
    }

    @Override
    public List<Size> getActiveSizes() {
        return sizeRepository.findAllByStatusTrue();
    }

    @Override
    public Size getSizeById(Long id) {
        return sizeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("size", id));
    }

    @Override
    @Transactional
    public Size addSize(Size size) {
        if(sizeRepository.existsByValue(size.getValue())) {
            throw new IllegalArgumentException("Size '" + size.getValue() + "' already exists!");
        }
        size.setStatus(true);
        return sizeRepository.save(size);
    }

    @Override
    @Transactional
    public Size updateSize(Long id, Size size) {
        Size existingSize = this.getSizeById(id);
        existingSize.setValue(size.getValue());
        existingSize.setStatus(size.getStatus());
        return sizeRepository.save(existingSize);
    }

    @Override
    public void deleteSize(Long id) {
        Size existingSize = this.getSizeById(id);
        existingSize.setStatus(false);
        sizeRepository.save(existingSize);
    }

    @Override
    public Boolean restoreBrand(Long id) {
        Optional<Size> optional = sizeRepository.findById(id);
        if (optional.isPresent()) {
            Size size = optional.get();
            size.setStatus(true); // khôi phục
            sizeRepository.save(size);
            return true;
        }
        return false;
    }
}
