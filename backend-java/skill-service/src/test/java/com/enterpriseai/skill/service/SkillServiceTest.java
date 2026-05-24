package com.enterpriseai.skill.service;

import com.enterpriseai.skill.entity.Skill;
import com.enterpriseai.skill.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillService skillService;

    @Test
    void create_shouldSaveSkill() {
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));
        Skill result = skillService.create("t1", "data_analysis", "Analyzes data", "You are a data analyst", "analysis");
        assertEquals("data_analysis", result.getName());
        assertEquals("analysis", result.getCategory());
        assertEquals("draft", result.getStatus());
        assertNotNull(result.getId());
    }

    @Test
    void publish_shouldUpdateStatusAndVersion() {
        Skill skill = new Skill();
        skill.setId("s1");
        skill.setStatus("draft");
        skill.setVersion(1);
        when(skillRepository.findById("s1")).thenReturn(Optional.of(skill));
        when(skillRepository.save(any(Skill.class))).thenReturn(skill);

        Skill result = skillService.publish("s1");
        assertEquals("published", result.getStatus());
        assertEquals(2, result.getVersion());
    }

    @Test
    void listPublished_shouldFilterByStatus() {
        when(skillRepository.findByTenantIdAndStatus("t1", "published")).thenReturn(List.of(new Skill()));
        List<Skill> result = skillService.listPublished("t1");
        assertEquals(1, result.size());
    }

    @Test
    void delete_shouldCallRepository() {
        skillService.delete("s1");
        verify(skillRepository).deleteById("s1");
    }
}
