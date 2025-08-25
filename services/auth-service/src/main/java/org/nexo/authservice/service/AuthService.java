package org.nexo.authservice.service;

import org.nexo.authservice.dto.RegisterRequest;
import reactor.core.publisher.Mono;

public interface AuthService {
    Mono<String> register(RegisterRequest registerRequest);

}
