package com.example.shoozy_shop.service;

import com.example.shoozy_shop.model.Material;

import java.util.List;

public interface IMaterialService {
    List<Material> getAllMaterials();
    List<Material> getActiveMaterials();
    Material getMaterialById(Long id);
    Material addMaterial(Material material);
    Material updateMaterial(Long id, Material material);
    void deleteMaterial(Long id);
    Boolean restoreMaterial(Long id);
}
