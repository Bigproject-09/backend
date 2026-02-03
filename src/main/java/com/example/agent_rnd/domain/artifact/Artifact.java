package com.example.agent_rnd.domain.artifact;

import com.example.agent_rnd.domain.enums.ArtifactType;
import com.example.agent_rnd.domain.presentation.Presentation;
import com.example.agent_rnd.domain.script.Script;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "artifacts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "artifact_id")
    private Long artifactId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presentation_id", nullable = false)
    private Presentation presentation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_id", nullable = false)
    private Script script;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", length = 50)
    private ArtifactType artifactType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
