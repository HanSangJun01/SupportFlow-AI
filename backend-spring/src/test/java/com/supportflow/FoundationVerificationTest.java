package com.supportflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportflow.knowledge.KnowledgeDocumentController;
import com.supportflow.knowledge.KnowledgeDocumentStatus;
import com.supportflow.knowledge.KnowledgeDocumentType;
import com.supportflow.tenant.TenantController;
import com.supportflow.ticket.TicketClassificationAttempt;
import com.supportflow.ticket.TicketController;
import com.supportflow.ticket.TicketHistoryEventType;
import com.supportflow.user.OperationalUserController;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;

class FoundationVerificationTest {

    @Test
    void tenantAndTicketControllersExposeFoundationRoutes() throws NoSuchMethodException {
        Method createTenant = TenantController.class.getMethod("createTenant", TenantController.CreateTenantRequest.class);
        Method listTenants = TenantController.class.getMethod("listTenants");
        Method getTenant = TenantController.class.getMethod("getTenant", String.class);
        Method updateTenant = TenantController.class.getMethod("updateTenant", String.class,
                TenantController.UpdateTenantRequest.class);
        Method createTicket = TicketController.class.getMethod("createTicket", String.class,
                TicketController.CreateTicketRequest.class);
        Method listTickets = TicketController.class.getMethod("listTickets", String.class,
                com.supportflow.ticket.TicketStatus.class, com.supportflow.ticket.TicketPriority.class,
                String.class, java.time.Instant.class, java.time.Instant.class);
        Method getTicket = TicketController.class.getMethod("getTicket", String.class, String.class);
        Method updateStatus = TicketController.class.getMethod("updateStatus", String.class, String.class,
                TicketController.UpdateTicketStatusRequest.class);

        assertThat(createTenant.isAnnotationPresent(PostMapping.class)).isTrue();
        assertThat(listTenants.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(getTenant.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(updateTenant.isAnnotationPresent(PatchMapping.class)).isTrue();
        assertThat(createTicket.isAnnotationPresent(PostMapping.class)).isTrue();
        assertThat(listTickets.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(getTicket.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(updateStatus.isAnnotationPresent(PatchMapping.class)).isTrue();
    }

    @Test
    void phaseTwoControllersExposeWorkflowRoutes() throws NoSuchMethodException {
        Method createUser = OperationalUserController.class.getMethod("createUser", String.class,
                OperationalUserController.CreateOperationalUserRequest.class);
        Method listUsers = OperationalUserController.class.getMethod("listUsers", String.class);
        Method getUser = OperationalUserController.class.getMethod("getUser", String.class, String.class);
        Method updateUserStatus = OperationalUserController.class.getMethod("updateStatus", String.class,
                String.class, OperationalUserController.UpdateOperationalUserStatusRequest.class);
        Method updateWorkflow = TicketController.class.getMethod("updateWorkflow", String.class, String.class,
                TicketController.UpdateTicketWorkflowRequest.class);

        assertThat(createUser.isAnnotationPresent(PostMapping.class)).isTrue();
        assertThat(listUsers.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(getUser.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(updateUserStatus.isAnnotationPresent(PatchMapping.class)).isTrue();
        assertThat(updateWorkflow.isAnnotationPresent(PatchMapping.class)).isTrue();
    }

    @Test
    void knowledgeDocumentControllerExposesPhaseThreeRoutes() throws NoSuchMethodException {
        Method createDocument = KnowledgeDocumentController.class.getMethod("createDocument", String.class,
                KnowledgeDocumentController.CreateKnowledgeDocumentRequest.class);
        Method listDocuments = KnowledgeDocumentController.class.getMethod("listDocuments", String.class,
                KnowledgeDocumentType.class, KnowledgeDocumentStatus.class, String.class, Instant.class);
        Method getDocument = KnowledgeDocumentController.class.getMethod("getDocument", String.class, String.class);
        Method updateDocument = KnowledgeDocumentController.class.getMethod("updateDocument", String.class,
                String.class, KnowledgeDocumentController.UpdateKnowledgeDocumentRequest.class);
        Method archiveDocument = KnowledgeDocumentController.class.getMethod("archiveDocument", String.class,
                String.class, KnowledgeDocumentController.KnowledgeDocumentActorRequest.class);
        Method restoreDocument = KnowledgeDocumentController.class.getMethod("restoreDocument", String.class,
                String.class, KnowledgeDocumentController.KnowledgeDocumentActorRequest.class);

        assertThat(createDocument.isAnnotationPresent(PostMapping.class)).isTrue();
        assertThat(listDocuments.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(getDocument.isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(updateDocument.isAnnotationPresent(PatchMapping.class)).isTrue();
        assertThat(archiveDocument.isAnnotationPresent(PatchMapping.class)).isTrue();
        assertThat(restoreDocument.isAnnotationPresent(PatchMapping.class)).isTrue();
    }

    @Test
    void foundationVerificationClassesExist() throws ClassNotFoundException {
        assertThat(Class.forName("com.supportflow.ticket.TicketStatusTransitionPolicyTest")).isNotNull();
        assertThat(Class.forName("com.supportflow.ticket.TenantIsolationIntegrationTest")).isNotNull();
        assertThat(Class.forName("com.supportflow.ticket.TicketApiIntegrationTest")).isNotNull();
        assertThat(Class.forName("com.supportflow.ticket.TenantWorkflowMongoIntegrationTest")).isNotNull();
        assertThat(Class.forName("com.supportflow.knowledge.KnowledgeDocumentMongoIntegrationTest")).isNotNull();
    }

    @Test
    void phaseFourClassificationArtifactsAndRoutesExist() throws NoSuchMethodException, ClassNotFoundException {
        Method reanalyzeTicket = TicketController.class.getMethod("reanalyzeTicket", String.class, String.class,
                TicketController.ReanalyzeTicketRequest.class);

        assertThat(reanalyzeTicket.isAnnotationPresent(PostMapping.class)).isTrue();
        assertThat(TicketClassificationAttempt.class).isNotNull();
        assertThat(TicketHistoryEventType.AI_CLASSIFICATION_APPLIED).isNotNull();
        assertThat(Class.forName("com.supportflow.ticket.TicketClassificationMongoIntegrationTest")).isNotNull();
        assertThat(Files.exists(Path.of("../ai-service-python"))).isTrue();
        assertThat(Files.exists(Path.of("../.planning/phases/04-ai-classification-integration/04-AI-SPEC.md")))
                .isTrue();
    }
}
