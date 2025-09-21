package com.example.shoozy_shop.service;

import com.example.shoozy_shop.exception.ResourceNotFoundException;
import com.example.shoozy_shop.model.Material;
import com.example.shoozy_shop.model.Size;
import com.example.shoozy_shop.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.eclipse.tags.shaded.org.apache.regexp.RE;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MaterialService implements IMaterialService {

    private final MaterialRepository materialRepository;

    @Override
    public List<Material> getAllMaterials() {
        return materialRepository.findAll();
    }

    @Override
    public List<Material> getActiveMaterials() {
        return materialRepository.getAllByStatusTrue();
    }

    @Override
    public Material getMaterialById(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("material",id));
    }

    @Override
    @Transactional
    public Material addMaterial(Material material) {
        if(materialRepository.existsByName(material.getName().toLowerCase())) {
            throw  new IllegalArgumentException("Material with name +" + material.getName() + "  already exists!");
        }
        material.setStatus(true);
        return materialRepository.save(material);
    }

    @Override
    @Transactional
    public Material updateMaterial(Long id, Material material) {
        Material existingMaterial = this.getMaterialById(id);
        existingMaterial.setName(material.getName());
        existingMaterial.setDescription(material.getDescription());
         existingMaterial.setStatus(material.getStatus());
        return materialRepository.save(existingMaterial);
    }

    @Override
    public void deleteMaterial(Long id) {
        Material existingMaterial = this.getMaterialById(id);
        existingMaterial.setStatus(false);
        materialRepository.save(existingMaterial);
    }

    @Override
    public Boolean restoreMaterial(Long id) {
        Optional<Material> optional = materialRepository.findById(id);
        if (optional.isPresent()) {
            Material material = optional.get();
            material.setStatus(true); // khôi phục
            materialRepository.save(material);
            return true;
        }
        return false;
    }
}
