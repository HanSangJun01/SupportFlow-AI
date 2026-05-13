package com.supportflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportflow.tenant.TenantController;
import com.supportflow.ticket.TicketController;
import com.supportflow.user.OperationalUserController;
import java.lang.reflect.Method;
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
    void foundationVerificationClassesExist() throws ClassNotFoundException {
        assertThat(Class.forName("com.supportflow.ticket.TicketStatusTransitionPolicyTest")).isNotNull();
        assertThat(Class.forName("com.supportflow.ticket.TenantIsolationIntegrationTest")).isNotNull();
        assertThat(Class.forName("com.supportflow.ticket.TicketApiIntegrationTest")).isNotNull();
        assertThat(Class.forName("com.supportflow.ticket.TenantWorkflowMongoIntegrationTest")).isNotNull();
    }
}
