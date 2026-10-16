package com.smartfood.expiry.controller;

import com.smartfood.expiry.dto.BatchItemViewDto;
import com.smartfood.expiry.dto.ProductViewDto;
import com.smartfood.expiry.entity.Product;
import com.smartfood.expiry.entity.ProductBatch;
import com.smartfood.expiry.repository.CategoryRepository;
import com.smartfood.expiry.service.BatchService;
import com.smartfood.expiry.service.ExpiryDetectionService;
import com.smartfood.expiry.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final BatchService batchService;
    private final ExpiryDetectionService expiryDetectionService;

    public ProductController(ProductService productService,
                             CategoryRepository categoryRepository,
                             BatchService batchService,
                             ExpiryDetectionService expiryDetectionService) {
        this.productService = productService;
        this.categoryRepository = categoryRepository;
        this.batchService = batchService;
        this.expiryDetectionService = expiryDetectionService;
    }

    @GetMapping
    public String listProducts(@RequestParam(value = "keyword", required = false) String keyword,
                               @RequestParam(value = "category", required = false) Long categoryId,
                               @RequestParam(value = "expiryFilter", required = false) String expiryFilter,
                               Model model) {
        LocalDate today = LocalDate.now();
        List<ProductViewDto> products = productService.searchAndFilterProducts(keyword, categoryId, expiryFilter, today);

        model.addAttribute("products", products);
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedExpiryFilter", expiryFilter);
        model.addAttribute("activeNav", "products");

        return "products/list";
    }

    @GetMapping("/new")
    public String newProductForm(@RequestParam(value = "barcode", required = false) String barcode, Model model) {
        Product product = new Product();
        if (barcode != null && !barcode.trim().isEmpty()) {
            product.setBarcode(barcode.trim());
        }

        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
        model.addAttribute("isEdit", false);
        model.addAttribute("activeNav", "products");

        return "products/form";
    }

    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return productService.getProductById(id).map(product -> {
            model.addAttribute("product", product);
            model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
            model.addAttribute("isEdit", true);
            model.addAttribute("activeNav", "products");
            return "products/form";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Product not found with ID: " + id);
            return "redirect:/products";
        });
    }

    @PostMapping("/save")
    public String saveProduct(@Valid @ModelAttribute("product") Product product,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        // Check for duplicate barcode if new or if barcode changed
        if (product.getId() == null) {
            if (productService.existsByBarcode(product.getBarcode())) {
                bindingResult.rejectValue("barcode", "duplicate", "This barcode is already assigned to another product");
            }
        } else {
            productService.getProductByBarcode(product.getBarcode()).ifPresent(existing -> {
                if (!existing.getId().equals(product.getId())) {
                    bindingResult.rejectValue("barcode", "duplicate", "This barcode is already assigned to another product");
                }
            });
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
            model.addAttribute("isEdit", product.getId() != null);
            model.addAttribute("activeNav", "products");
            return "products/form";
        }

        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage",
                "Product '" + product.getName() + "' was successfully " + (product.getId() != null ? "updated" : "created") + ".");

        return "redirect:/products";
    }

    @GetMapping("/view/{id}")
    public String viewProductDetails(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        return productService.getProductById(id).map(product -> {
            LocalDate today = LocalDate.now();
            ProductViewDto productView = productService.getProductViewDto(product, today);

            List<ProductBatch> batches = batchService.getBatchesByProduct(id);
            List<BatchItemViewDto> batchViews = batches.stream()
                    .map(b -> expiryDetectionService.wrapBatch(b, today))
                    .sorted((a, b) -> a.getBatch().getExpiryDate().compareTo(b.getBatch().getExpiryDate()))
                    .collect(Collectors.toList());

            model.addAttribute("productView", productView);
            model.addAttribute("product", product);
            model.addAttribute("batchViews", batchViews);
            model.addAttribute("activeNav", "products");

            return "products/detail";
        }).orElseGet(() -> {
            redirectAttributes.addFlashAttribute("errorMessage", "Product not found with ID: " + id);
            return "redirect:/products";
        });
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Product was successfully deleted / deactivated.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/products";
    }
}
