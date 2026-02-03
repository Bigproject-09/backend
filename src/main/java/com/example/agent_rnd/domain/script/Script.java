package com.example.agent_rnd.domain.script;

import com.example.agent_rnd.domain.presentation.Presentation;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scripts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Script {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "script_id")
    private Long scriptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presentation_id", nullable = false)
    private Presentation presentation;

    @Column(name = "page_no", nullable = false)
    private Integer pageNo;

    @Column(name = "text_content", nullable = false, columnDefinition = "LONGTEXT")
    private String textContent;
}
