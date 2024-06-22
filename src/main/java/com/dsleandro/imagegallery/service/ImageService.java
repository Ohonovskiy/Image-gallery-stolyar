package com.dsleandro.imagegallery.service;

import com.dsleandro.imagegallery.entity.Image;
import com.dsleandro.imagegallery.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImageService {

    private final ImageRepository imageRepository;

    @Autowired
    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }


    public List<Image> getAll(){
        return imageRepository.findAll();
    }
}
