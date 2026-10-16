package com.smartfood.expiry.controller;

import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class BarcodePageController {

    private final ProductService productService;

    public BarcodePageController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/barcode/demo")
    public String demoBarcodesSheet(Model model) {
        List<Product> products = productService.getAllActiveProducts().stream()
                .filter(p -> p.getBarcode() != null && p.getBarcode().matches("8901262(0[1-9]|1[0-9]|20)[0-9]{3}"))
                .sorted(java.util.Comparator.comparing(Product::getBarcode))
                .limit(20)
                .toList();
        model.addAttribute("products", products);
        model.addAttribute("activeNav", "barcode-demo");
        return "barcode/demo";
    }
}
