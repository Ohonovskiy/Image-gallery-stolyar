package com.dsleandro.imagegallery.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.security.Principal;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.dsleandro.imagegallery.entity.Image;
import com.dsleandro.imagegallery.entity.User;
import com.dsleandro.imagegallery.repository.ImageRepository;
import com.dsleandro.imagegallery.repository.UserRepository;
import com.dsleandro.imagegallery.service.ImageService;
import com.dsleandro.imagegallery.storage.StorageService;

import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

@Controller
public class ImageController {
    private final StorageService storageService;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private boolean isAuth;

    @Autowired
    public ImageController(StorageService storageService, ImageRepository imageRepository, UserRepository userRepository, ImageService imageService) {
        this.storageService = storageService;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.imageService = imageService;
    }


    @GetMapping("")
    public String index(Model model, Principal pUser) {
        setAuthStatus();
        model.addAttribute("isAuth", isAuth);

        if(isAuth){
            try {
                User user = userRepository.findByUsername(pUser.getName())
                        .orElseThrow(() -> new Exception("user not found"));

                model.addAttribute("images",
                        storageService.loadAll()
                                .map(path -> MvcUriComponentsBuilder
                                        .fromMethodName(ImageController.class, "serveImage", path.getFileName().toString())
                                        .build().toUri().toString())
                                .collect(Collectors.toList()));

                model.addAttribute("username", pUser.getName());

                return "index";

            } catch (Exception e) {
                return "error";
            }
        }

        return "index";
    }

    @GetMapping("/me")
    public String displayImages(Model model, Principal pUser) throws IOException {
        try {
            setAuthStatus();
            model.addAttribute("isAuth", isAuth);
            User user = userRepository.findByUsername(pUser.getName())
                    .orElseThrow(() -> new Exception("user not found"));

            model.addAttribute("images",
                    storageService.loadAll(user)
                            .map(path -> MvcUriComponentsBuilder
                                    .fromMethodName(ImageController.class, "serveImage", path.getFileName().toString())
                                    .build().toUri().toString())
                            .collect(Collectors.toList()));

            model.addAttribute("username", pUser.getName());

            return "index";

        } catch (Exception e) {
            return "error";
        }

    }

    @GetMapping("/images/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @PostMapping("/")
    public String imageUpload(@RequestParam("file") MultipartFile file, Principal user) {

        if (isImage(file)) {

            try {
                // set an ID to image with its extension
                String fileId = UUID.randomUUID() + file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));

                // save image on server
                storageService.store(fileId, file.getBytes());

                // parse MultipartFile to Image entity
                Image image = parseToImage(fileId, user);

                // save path of image in database
                imageRepository.save(image);

                return "redirect:/";

            } catch (Exception e) {
                e.printStackTrace();
                return "error";
            }

        } else {
            return "error";
        }

    }

    public boolean isImage(MultipartFile file) {

        String type = file.getContentType().split("/")[0];

        if (type.equals("image"))
            return true;
        else
            return false;

    }

    public Image parseToImage(String fileId, Principal user) throws Exception {

        Path path = storageService.getPath(fileId);

        User username = userRepository.findByUsername(user.getName())
                .orElseThrow(() -> new Exception("user not found"));

        return new Image(path.toString(), username);

    }

    public void setAuthStatus(){
        isAuth = !Objects.equals(SecurityContextHolder.getContext().getAuthentication().getName(), "anonymousUser");
    }
}