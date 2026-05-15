package tech.palei.isupportalerts.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tech.palei.isupportalerts.service.SopService;

@RestController
@RequestMapping("/api/sop")
public class SopController {

    private final SopService sopService;

    @Autowired
    public SopController(SopService sopService) {
        this.sopService = sopService;
    }

    //Load the SOP document to Vector DB
    @GetMapping("/load")
    public String getSopDocument(            
            @RequestParam(required = false) String spaceKey,
            @RequestParam(required = false) String title) {
        return sopService.loadSopDocument(spaceKey,title);
    }   

    @GetMapping("/children")
    public List<String> listChildTitles(            
            @RequestParam(required = false) String spaceKey,
            @RequestParam(required = false) String title) {
        return sopService.listChildTitles(spaceKey,title);
    } 

}
