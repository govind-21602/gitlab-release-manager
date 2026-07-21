package com.releasemanager.service;

import com.releasemanager.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MergeRequestServiceTest {

    @Mock
    private AppConfig config;

    @BeforeEach
    void setUp() {
        when(config.getGitlabUrl()).thenReturn("https://gitlab.example.com");
        when(config.getGitlabToken()).thenReturn("test-token");
        when(config.getProjectId()).thenReturn("456");
        when(config.getSourceBranch()).thenReturn("develop");
        when(config.getTargetBranch()).thenReturn("staging");
    }

    @Test
    void serviceConstructsWithoutException() {
        assertDoesNotThrow(() -> new MergeRequestService(config));
    }

    @Test
    void configIsUsed() {
        MergeRequestService service = new MergeRequestService(config);
        assertNotNull(service);
        verify(config, atLeastOnce()).getGitlabUrl();
    }
}
