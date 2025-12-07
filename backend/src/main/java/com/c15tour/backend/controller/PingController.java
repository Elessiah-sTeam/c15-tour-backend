package com.c15tour.backend.controller;

import com.c15tour.api.SystemApi;
import com.c15tour.model.Message;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController implements SystemApi {

    @Override
    public ResponseEntity<Message> ping() {
        // On utilise l'objet Message généré par le YAML
        Message response = new Message();
        response.setContent("Pong ! Le backend est connecté au contrat.");

        return ResponseEntity.ok(response);
    }
}