package com.gjira.ingest.staticingest;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@ConditionalOnProperty(prefix = "gjira.sharepoint", name = "enabled", havingValue = "true")
public class SharePointAdapter {

    private static final Logger log = LoggerFactory.getLogger(SharePointAdapter.class);
    private static final Tika TIKA = new Tika();

    private final GraphServiceClient graphClient;

    public SharePointAdapter(
            @Value("${gjira.sharepoint.tenant-id}") String tenantId,
            @Value("${gjira.sharepoint.client-id}") String clientId,
            @Value("${gjira.sharepoint.client-secret}") String clientSecret
    ) {
        var credential = new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
        
        this.graphClient = new GraphServiceClient(credential, "https://graph.microsoft.com/.default");
    }

    public String fetchDriveItemText(String driveId, String itemId) {
        try (InputStream stream = graphClient.drives().byDriveId(driveId).items().byDriveItemId(itemId).content().get()) {
            return TIKA.parseToString(stream);
        } catch (Exception e) {
            log.error("Failed to fetch/parse SharePoint item {}: {}", itemId, e.getMessage());
            throw new RuntimeException("SharePoint extraction failed", e);
        }
    }
}