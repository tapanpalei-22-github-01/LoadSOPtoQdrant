package tech.palei.loadsopqdrant.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tech.palei.loadsopqdrant.service.SopService;

@RestController
@RequestMapping("/api")
public class SopController {

    private final SopService sopService;

    @Autowired
    public SopController(SopService sopService) {
        this.sopService = sopService;
    }

    @GetMapping("/sop")
    public String getSopDocument() {
        return sopService.retrieveSopDocument();
    }   

}
