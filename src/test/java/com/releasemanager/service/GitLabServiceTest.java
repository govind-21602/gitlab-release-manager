package com.releasemanager.service;

import com.releasemanager.config.AppConfig;
import com.releasemanager.model.CommitInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitLabServiceTest {

    @Mock
    private AppConfig config;

    @BeforeEach
    void setUp() {
        when(config.getGitlabUrl()).thenReturn("https://gitlab.example.com");
        when(config.getGitlabToken()).thenReturn("test-token");
        when(config.getProjectId()).thenReturn("123");
    }

    @Test
    void serviceConstructsWithoutException() {
        assertDoesNotThrow(() -> new GitLabService(config));
    }

    @Test
    void configIsUsed() {
        GitLabService service = new GitLabService(config);
        assertNotNull(service);
        verify(config, atLeastOnce()).getGitlabUrl();
    }
}
