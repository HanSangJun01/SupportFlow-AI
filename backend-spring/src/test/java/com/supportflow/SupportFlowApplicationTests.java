package com.supportflow;

import com.supportflow.tenant.TenantService;
import com.supportflow.ticket.TicketService;
import com.supportflow.user.OperationalUserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
})
class SupportFlowApplicationTests {

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private OperationalUserService operationalUserService;

    @Test
    void contextLoads() {
    }
}
