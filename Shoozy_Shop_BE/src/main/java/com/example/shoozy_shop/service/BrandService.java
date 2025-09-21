package com.example.shoozy_shop.service;

import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.Brand;
import com.example.shoozy_shop.model.Product;
import com.example.shoozy_shop.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BrandService implements IBrandService {

    private final BrandRepository brandRepository;

    @Override
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Override
    public List<Brand> getActiveBrands() {
        return brandRepository.findAllByStatusTrue();
    }

    @Override
    public Brand getBrandById(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("brand", id));
    }

    @Override
    @Transactional
    public Brand addBrand(Brand brand) {
        if(brandRepository.existsByName(brand.getName().toLowerCase())) {
            throw new IllegalArgumentException("Brand " + brand.getName() + " already exists!");
        }
        return brandRepository.save(brand);
    }

    @Override
    @Transactional
    public Brand updateBrand(Long id, Brand brand) {
        Brand existingBrand = this.getBrandById(id);
        existingBrand.setName(brand.getName());
        existingBrand.setDescription(brand.getDescription());
        existingBrand.setCountry(brand.getCountry());
        existingBrand.setStatus(brand.getStatus());
        return brandRepository.save(existingBrand);
    }

    @Override
    public void deleteBrand(Long id) {
        Brand existingBrand = this.getBrandById(id);
        existingBrand.setStatus(false);
        brandRepository.save(existingBrand);
    }

    @Override
    public Boolean restoreBrand(Long id) {
        Optional<Brand> optional = brandRepository.findById(id);
        if (optional.isPresent()) {
            Brand brand = optional.get();
            brand.setStatus(true); // khôi phục
            brandRepository.save(brand);
            return true;
        }
        return false;
    }

}
