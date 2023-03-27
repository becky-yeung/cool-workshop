package com.cool.api.controller;

import com.cool.domain.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/exists/{skuCode}")
    @ResponseStatus(HttpStatus.OK)
    public boolean findBySkuCode(@PathVariable String skuCode) {
        return productService.findBySkuCode(skuCode).isPresent();
    }
}
