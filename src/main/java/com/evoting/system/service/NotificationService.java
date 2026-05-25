package com.evoting.system.service;

import com.evoting.system.model.FCMToken;
import com.evoting.system.repository.FCMTokenRepository;
import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private FCMTokenRepository fcmTokenRepository;

    // Tuma kwa mtu mmoja
    public void sendToUser(String username, String title, 
                           String body) {
        try {
            FCMToken fcmToken = fcmTokenRepository
                .findByUsername(username).orElse(null);
            if (fcmToken == null) {
                System.err.println("FCM token not found for user: " + username);
                return;
            }

            Message message = Message.builder()
                .setToken(fcmToken.getToken())
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putData("click_action", "FLUTTER_NOTIFICATION_CLICK")
                .build();

            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Successfully sent message to " + username + ": " + response);
        } catch (Exception e) {
            System.err.println("Send notification error: " 
                + e.getMessage());
            e.printStackTrace();
        }
    }

    // Tuma kwa voters wote
    public int sendToAllVoters(String title, String body) {
        List<FCMToken> tokens = fcmTokenRepository
            .findByRole("VOTER");
        int sent = 0;

        System.out.println("Found " + tokens.size() + " VOTER tokens");

        List<String> tokenList = new ArrayList<>();
        for (FCMToken t : tokens) {
            if (t.getToken() != null && !t.getToken().isEmpty()) {
                tokenList.add(t.getToken());
            }
        }

        if (tokenList.isEmpty()) {
            System.out.println("No valid tokens found for VOTERS");
            return 0;
        }

        System.out.println("Sending to " + tokenList.size() + " valid VOTER tokens");

        // Tuma kwa batch za 500 (Firebase limit)
        int batchSize = 500;
        for (int i = 0; i < tokenList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, tokenList.size());
            List<String> batch = new ArrayList<>();
            for (int j = i; j < end; j++) {
                batch.add(tokenList.get(j));
            }
            try {
                MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(batch)
                    .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                    .putData("type", "ELECTION_UPDATE")
                    .build();

                BatchResponse response = FirebaseMessaging
                    .getInstance()
                    .sendEachForMulticast(message);
                sent += response.getSuccessCount();
                System.out.println("Batch sent: " + response.getSuccessCount() + " successful, " + response.getFailureCount() + " failed");
            } catch (Exception e) {
                System.err.println("Batch send error: "
                    + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("Total messages sent to VOTERS: " + sent);
        return sent;
    }

    // Tuma kwa wote (admin na voters)
    public int sendToAll(String title, String body) {
        List<FCMToken> tokens = fcmTokenRepository.findAll();
        int sent = 0;

        System.out.println("Found " + tokens.size() + " total tokens (VOTERS + ADMIN)");

        for (FCMToken fcmToken : tokens) {
            try {
                if (fcmToken.getToken() == null) {
                    System.out.println("Skipping null token for user: " + fcmToken.getUsername());
                    continue;
                }
                Message message = Message.builder()
                    .setToken(fcmToken.getToken())
                    .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                    .build();
                String response = FirebaseMessaging.getInstance().send(message);
                System.out.println("Successfully sent to " + fcmToken.getUsername() + ": " + response);
                sent++;
            } catch (Exception e) {
                System.err.println("Send error for " + fcmToken.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.out.println("Total messages sent to ALL: " + sent);
        return sent;
    }
}
