package com.example.Backend_J2EE.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Mock
    private StompEndpointRegistry endpointRegistry;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;

    @Mock
    private SockJsServiceRegistration sockJsServiceRegistration;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private ChannelRegistration channelRegistration;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Test
    void registerStompEndpointsConfiguresSocketEndpoint() {
        when(endpointRegistry.addEndpoint("/ws-chat")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns("*")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(any(HttpSessionHandshakeInterceptor.class))).thenReturn(endpointRegistration);
        when(endpointRegistration.withSockJS()).thenReturn(sockJsServiceRegistration);

        webSocketConfig.registerStompEndpoints(endpointRegistry);

        verify(endpointRegistry).addEndpoint("/ws-chat");
        verify(endpointRegistration).setAllowedOriginPatterns("*");
        verify(endpointRegistration).addInterceptors(any(HttpSessionHandshakeInterceptor.class));
        verify(endpointRegistration).withSockJS();
    }

    @Test
    void configureMessageBrokerEnablesExpectedPrefixes() {
        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        verify(messageBrokerRegistry).enableSimpleBroker("/topic", "/queue");
        verify(messageBrokerRegistry).setApplicationDestinationPrefixes("/app");
    }

    @Test
    void configureClientInboundChannelRegistersAuthInterceptor() {
        webSocketConfig.configureClientInboundChannel(channelRegistration);

        verify(channelRegistration).interceptors(stompAuthChannelInterceptor);
    }
}