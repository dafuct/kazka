package com.kazka.story.showcase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Thin controller: it delegates all validation to {@link ShowcaseService#resolveShowcaseImage}
 * (covered by {@code ShowcaseServiceTest}) and only wraps the resolved path in a streamable
 * resource. The end-to-end image route — including encoded traversal and non-showcase 404 —
 * is exercised in {@code ShowcaseImageRouteIT}.
 */
@ExtendWith(MockitoExtension.class)
class ShowcaseImageControllerTest {

    @Mock ShowcaseService service;
    @InjectMocks ShowcaseImageController controller;

    @Test
    void should_stream_resolved_path_as_filesystem_resource() {
        Path resolved = Path.of("/var/uploads/img.png");
        when(service.resolveShowcaseImage("s1", "img.png")).thenReturn(resolved);

        Resource res = controller.image("s1", "img.png").block();

        assertThat(res).isInstanceOf(FileSystemResource.class);
        assertThat(((FileSystemResource) res).getPath()).isEqualTo(resolved.toString());
    }

    @Test
    void should_propagate_service_exception() {
        when(service.resolveShowcaseImage("s1", "img.png"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> controller.image("s1", "img.png").block())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
