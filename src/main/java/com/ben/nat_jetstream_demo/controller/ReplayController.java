package com.ben.nat_jetstream_demo.controller;

import com.ben.nat_jetstream_demo.service.ReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/replay")
public class ReplayController {

    private final ReplayService replayService;

    public ReplayController(ReplayService replayService) {
        this.replayService = replayService;
    }

    @GetMapping("/sequence")
    public String replayBySequence(@RequestParam long seq) {
        replayService.replayFromSequence(seq);
        return "Replay started from sequence: " + seq + ". Check application logs for message data.";
    }

    @GetMapping("/time")
    public String replayByTime(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startTime) {
        replayService.replayFromTime(startTime);
        return "Replay started from time: " + startTime + ". Check application logs for message data.";
    }
}
